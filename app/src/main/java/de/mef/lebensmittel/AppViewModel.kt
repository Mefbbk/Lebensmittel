package de.mef.lebensmittel

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

sealed interface Screen {
    data object Calendar : Screen
    data class Day(val date: LocalDate) : Screen
    data class Add(val date: LocalDate) : Screen
    data object Spending : Screen
    data object Settings : Screen
    data object Nutrition : Screen
    data object Reminders : Screen
}

/** A product waiting for the quantity dialog (new item or editing an existing one). */
data class PendingAdd(
    val date: LocalDate,
    val product: Product,
    val amount: Double,
    val unit: QtyUnit,
    val editItemId: String? = null,
    /** Known price for 1 [unitPriceUnit]; the dialog uses it to suggest the price. */
    val unitPrice: Double? = null,
    val unitPriceUnit: QtyUnit? = null,
    /** Last promo / customer-card prices; suggested when the matching box is ticked. */
    val promoPrice: SavedPrice? = null,
    val cardPrice: SavedPrice? = null,
    /** Initial state of the "promo" / "card" boxes (when editing an item). */
    val promo: Boolean = false,
    val card: Boolean = false,
)

data class UiMessage(val id: Long, val text: String)

fun AppData.daysSorted(): List<Pair<LocalDate, List<ShopItem>>> =
    days.mapNotNull { (k, v) ->
        runCatching { LocalDate.parse(k) }.getOrNull()?.takeIf { v.isNotEmpty() }?.let { it to v }
    }.sortedByDescending { it.first }

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val store = Store(File(app.filesDir, "data.json"))

    val dict: Dictionary = runCatching {
        app.assets.open("dictionary.txt").bufferedReader(Charsets.UTF_8).use { Dictionary.parse(it.readText()) }
    }.getOrElse { Dictionary(emptyList()) }

    // ---------------- navigation ----------------

    var screen by mutableStateOf<Screen>(Screen.Calendar)
        private set
    private val backStack = ArrayList<Screen>()

    fun open(s: Screen) {
        backStack.add(screen)
        screen = s
    }

    fun back(): Boolean {
        if (backStack.isEmpty()) {
            if (screen == Screen.Calendar) return false
            screen = Screen.Calendar
            return true
        }
        screen = backStack.removeAt(backStack.lastIndex)
        return true
    }

    fun goToDay(date: LocalDate) {
        backStack.clear()
        backStack.add(Screen.Calendar)
        screen = Screen.Day(date)
    }

    var calendarMonth by mutableStateOf(YearMonth.now())

    // ---------------- messages (snackbar) ----------------

    var message by mutableStateOf<UiMessage?>(null)
        private set
    private var messageSeq = 0L

    fun toast(text: String) {
        message = UiMessage(++messageSeq, text)
    }

    // ---------------- adding products ----------------

    var query by mutableStateOf("")
    var offTerm by mutableStateOf<String?>(null)
        private set
    /** Selected supermarket filter (slug like "edeka"), null = all. Kept for the next search. */
    var offStore by mutableStateOf<String?>(null)
        private set
    /** Results for the term without a store filter. */
    private var offAll by mutableStateOf<List<Product>>(emptyList())
    /** Results the server returned for term + selected store. */
    private var offInStore by mutableStateOf<List<Product>>(emptyList())

    /** What the list shows: all results, or only those sold in the selected supermarket. */
    val offResults: List<Product>
        get() {
            val st = offStore ?: return offAll
            return (offInStore + offAll.filter { Stores.matches(it, st) }).distinctBy { it.key }
        }

    /** Supermarkets that appear in the found products, with how many products each: for the drop-down. */
    val offStoreOptions: List<Pair<String, Int>>
        get() = Stores.countIn((offAll + offInStore).distinctBy { it.key })

    val offAllCount: Int get() = offAll.size

    var offLoading by mutableStateOf(false)
        private set
    var offError by mutableStateOf<String?>(null)
        private set
    var scanLoading by mutableStateOf(false)
        private set

    /** Barcode scanned but not found online: attached to the next product added by hand. */
    private var pendingBarcode: String? = null
    /** key: "term|store" */
    private val offCache = HashMap<String, List<Product>>()

    var pending by mutableStateOf<PendingAdd?>(null)

    fun startAdding(date: LocalDate) {
        query = ""
        offTerm = null
        offAll = emptyList()
        offInStore = emptyList()
        offError = null
        pendingBarcode = null
        open(Screen.Add(date))
    }

    /** Name used for "add without choosing": German dictionary name if the text matches exactly. */
    fun plainName(q: String): String {
        val t = q.trim()
        return dict.exact(t)?.de ?: Txt.capitalize(t)
    }

    /** Product words of the query (German) and a supermarket typed into it: "Milch Edeka" -> ("Milch", "edeka"). */
    fun searchParts(query: String): Pair<String, String?> {
        val (words, store) = Stores.splitQuery(query)
        return dict.toGerman(words) to store
    }

    /**
     * Searches the databases. [store]: supermarket typed into the query; if null, the store picked
     * earlier in the drop-down stays selected.
     */
    fun searchOff(term: String, store: String? = null) {
        val t = term.trim()
        if (t.length < 2) return
        if (store != null) offStore = store
        offTerm = t
        offError = null
        offAll = emptyList()
        offInStore = emptyList()
        load(t, null) { offAll = it }
        offStore?.let { st -> load(t, st) { offInStore = it } }
    }

    /** Picked in the drop-down. Asks the server for more products of that store. */
    fun selectStore(slug: String?) {
        offStore = slug
        offError = null
        offInStore = emptyList()
        val t = offTerm ?: return
        if (slug != null) load(t, slug) { offInStore = it }
        else if (offAll.isEmpty()) load(t, null) { offAll = it }
    }

    private var running = 0

    private fun load(t: String, store: String?, onResult: (List<Product>) -> Unit) {
        val cacheKey = Txt.norm(t) + "|" + (store ?: "")
        offCache[cacheKey]?.let {
            onResult(it)
            return
        }
        val term = offTerm
        running++
        offLoading = true
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { OffApi.search(t, store) }
                offCache[cacheKey] = result
                // ignore answers for an older search word
                if (offTerm == term && (store == null || store == offStore)) {
                    onResult(result)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: OffApi.RateLimitException) {
                if (offTerm == term) offError = T.tooManySearches()
            } catch (e: Exception) {
                if (offTerm == term) offError = T.noConnection()
            } finally {
                running--
                if (running <= 0) {
                    running = 0
                    offLoading = false
                }
            }
        }
    }

    /** Shown when a finished search has nothing to show. */
    val offEmptyMessage: String?
        get() {
            if (offLoading || offError != null) return null
            val t = offTerm ?: return null
            if (offResults.isNotEmpty()) return null
            val st = offStore
            return if (st != null && offAll.isNotEmpty()) {
                T.noneInStore(t, Stores.displayName(st))
            } else {
                T.nothingFound(t, st?.let { T.inStoreShort(Stores.displayName(it)) } ?: "")
            }
        }

    private fun withKnown(p: Product): Product {
        val k = store.data.value.known[p.key] ?: return p
        return p.copy(
            nameUk = p.nameUk ?: k.product.nameUk,
            imageUrl = p.imageUrl ?: k.product.imageUrl,
            kcal100 = p.kcal100 ?: k.product.kcal100,
            packageQty = p.packageQty ?: k.product.packageQty,
            source = p.source ?: k.product.source,
            stores = p.stores.ifEmpty { k.product.stores },
        )
    }

    fun choose(date: LocalDate, product: Product) {
        val k = store.data.value.known[product.key]
        pending = PendingAdd(
            date, withKnown(product), k?.lastAmount ?: 1.0, k?.lastUnit ?: QtyUnit.PCS,
            unitPrice = k?.unitPrice, unitPriceUnit = k?.lastUnit,
            promoPrice = k?.promoPrice, cardPrice = k?.cardPrice,
        )
    }

    fun chooseKnown(date: LocalDate, k: KnownProduct) {
        pending = PendingAdd(
            date, k.product, k.lastAmount, k.lastUnit, unitPrice = k.unitPrice, unitPriceUnit = k.lastUnit,
            promoPrice = k.promoPrice, cardPrice = k.cardPrice,
        )
    }

    fun chooseOff(date: LocalDate, p: Product) {
        val q = Stores.splitQuery(query).first.trim()
        // Remember the Ukrainian word the user searched with, so typing it again finds this product.
        val withUk = if (p.nameUk == null && Txt.hasCyrillic(q)) p.copy(nameUk = q) else p
        choose(date, withUk)
    }

    fun chooseDict(date: LocalDate, e: DictEntry) = choose(date, Product(name = e.de, nameUk = e.uk))

    fun choosePlain(date: LocalDate, text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        val e = dict.exact(t)
        val product = if (e != null) {
            Product(name = e.de, nameUk = e.uk, barcode = pendingBarcode)
        } else {
            Product(
                name = Txt.capitalize(t),
                barcode = pendingBarcode,
                nameUk = null,
            )
        }
        choose(date, product)
    }

    fun editItem(date: LocalDate, item: ShopItem) {
        val k = store.data.value.known[item.product.key]
        // the item's own price fills the slot of its kind, the other kinds come from what was remembered
        val own = item.price?.let { SavedPrice(it / item.amount, item.unit) }
        val special = item.promo || item.card
        pending = PendingAdd(
            date, item.product, item.amount, item.unit, editItemId = item.id,
            unitPrice = if (special) k?.unitPrice else own?.perUnit,
            unitPriceUnit = if (special) k?.lastUnit else item.unit,
            promoPrice = if (item.promo && own != null) own else k?.promoPrice,
            cardPrice = if (item.card && own != null) own else k?.cardPrice,
            promo = item.promo,
            card = item.card,
        )
    }

    fun confirmPending(amount: Double, unit: QtyUnit, price: Double?, promo: Boolean = false, card: Boolean = false) {
        val p = pending ?: return
        if (p.editItemId != null) {
            store.items(p.date).firstOrNull { it.id == p.editItemId }?.let {
                store.updateItem(p.date, it.copy(amount = amount, unit = unit, price = price, promo = promo, card = card))
            }
        } else {
            store.addItem(
                p.date,
                ShopItem(product = p.product, amount = amount, unit = unit, price = price, promo = promo, card = card),
            )
            if (p.product.barcode != null && p.product.barcode == pendingBarcode) pendingBarcode = null
            toast(T.added(p.product.name, "${formatNumber(amount)} ${unit.label}"))
            query = ""
        }
        pending = null
    }

    fun deletePending() {
        val p = pending ?: return
        p.editItemId?.let { store.removeItem(p.date, it) }
        pending = null
    }

    // ---------------- spending ----------------

    var spendFrom by mutableStateOf(LocalDate.now().withDayOfMonth(1))
    var spendTo by mutableStateOf(LocalDate.now())
    var spendOnlyBought by mutableStateOf(true)

    fun openSpending() {
        if (screen != Screen.Spending) open(Screen.Spending)
    }

    /** Bottom tabs: Calendar, Nutrition, Spending, Settings. Back from a tab returns to the calendar. */
    fun openTab(s: Screen) {
        if (screen == s) return
        backStack.clear()
        if (s != Screen.Calendar) backStack.add(Screen.Calendar)
        screen = s
    }

    // ---------------- reminders ----------------

    /** Day shown in the reminders tab. */
    var remindersDate by mutableStateOf(LocalDate.now())

    /** Reminder open in the edit dialog (new or existing). */
    var editingReminder by mutableStateOf<Reminder?>(null)

    fun openReminders(date: LocalDate? = null) {
        if (date != null) remindersDate = date
        openTab(Screen.Reminders)
    }

    /** New reminder for the selected day: next full hour today, 09:00 on other days. */
    fun newReminder() {
        val now = java.time.LocalTime.now()
        val time = if (remindersDate == LocalDate.now() && now.hour < 23) timeText(now.hour + 1, 0) else timeText(9, 0)
        editingReminder = Reminder(date = remindersDate.toString(), time = time, text = "")
    }

    fun saveReminder(r: Reminder) {
        store.saveReminder(r)
        ReminderScheduler.schedule(getApplication<Application>(), r)
        editingReminder = null
        r.localDate?.let { remindersDate = it }
        toast(if (r.isPast()) T.reminderInPast() else T.reminderSaved(r.localDate?.let { Dates.full(it) } ?: r.date, r.time))
    }

    fun setReminderEnabled(r: Reminder, enabled: Boolean) {
        val updated = r.copy(enabled = enabled)
        store.saveReminder(updated)
        ReminderScheduler.schedule(getApplication<Application>(), updated)
    }

    fun deleteReminder(r: Reminder) {
        ReminderScheduler.cancel(getApplication<Application>(), r)
        store.deleteReminder(r.id)
        if (editingReminder?.id == r.id) editingReminder = null
    }

    // ---------------- nutrition lookup ----------------

    var nutritionCode by mutableStateOf("")
    var nutritionLoading by mutableStateOf(false)
        private set
    var nutritionResult by mutableStateOf<NutritionInfo?>(null)
        private set
    var nutritionError by mutableStateOf<String?>(null)
        private set
    /** Products checked in this session, newest first. */
    var nutritionHistory by mutableStateOf<List<NutritionInfo>>(emptyList())
        private set
    private val nutritionCache = HashMap<String, NutritionInfo>()

    fun scanForNutrition(activity: Activity) = startScanner(activity) { code ->
        nutritionCode = code
        lookupNutrition(code)
    }

    fun lookupNutrition(code: String) {
        val clean = code.filter { it.isDigit() }
        if (clean.length < 6) {
            nutritionError = T.enterBarcode()
            return
        }
        nutritionError = null
        nutritionCache[clean]?.let {
            showNutrition(it)
            return
        }
        nutritionLoading = true
        nutritionResult = null
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { OffApi.nutritionByBarcode(clean) } }
            nutritionLoading = false
            val info = result.getOrNull()
            when {
                info != null -> {
                    nutritionCache[clean] = info
                    showNutrition(info)
                }
                result.isFailure -> nutritionError = T.noConnection()
                else -> nutritionError = T.barcodeNotFoundAnywhere(clean)
            }
        }
    }

    fun showNutrition(info: NutritionInfo) {
        nutritionResult = info
        nutritionError = null
        info.product.barcode?.let { nutritionCode = it }
        nutritionHistory = (listOf(info) + nutritionHistory.filter { it.product.key != info.product.key }).take(15)
    }

    /** "Add to today's list" from the nutrition card. */
    fun addNutritionProductToday() {
        val info = nutritionResult ?: return
        choose(LocalDate.now(), info.product)
    }

    // ---------------- background picture ----------------

    /** Average brightness of a picture, read from a small (≈64 px) copy so it is quick. */
    private fun measureLuma(file: File): Float? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 64 && bounds.outHeight / (sample * 2) >= 64) sample *= 2
        val bmp = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return@runCatching null
        val small = Bitmap.createScaledBitmap(bmp, 32, 32, true)
        val pixels = IntArray(32 * 32)
        small.getPixels(pixels, 0, 32, 0, 0, 32, 32)
        if (small != bmp) small.recycle()
        bmp.recycle()
        averageLuma(pixels)
    }.getOrNull()

    init {
        // pictures chosen before brightness was measured
        val d = store.data.value
        if (d.backgroundId != null && d.backgroundLuma == null) {
            viewModelScope.launch {
                val luma = withContext(Dispatchers.IO) { measureLuma(backgroundFile(d.backgroundId)) }
                if (luma != null) store.setBackgroundLuma(luma)
            }
        }
    }

    fun backgroundFile(id: Long): File = File(getApplication<Application>().filesDir, "bg_$id.jpg")

    /** Copies the picked picture into app storage (gallery links stop working after a while). */
    fun setBackground(context: Context, uri: Uri) {
        viewModelScope.launch {
            val id = System.currentTimeMillis()
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val target = backgroundFile(id)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { input.copyTo(it) }
                    } ?: error("no stream")
                    // remove older pictures
                    getApplication<Application>().filesDir.listFiles()
                        ?.filter { it.name.startsWith("bg_") && it.name != target.name }
                        ?.forEach { it.delete() }
                }.isSuccess
            }
            if (ok) {
                val luma = withContext(Dispatchers.IO) { measureLuma(backgroundFile(id)) }
                store.setBackground(id, luma)
            } else {
                toast(T.cantOpenPicture())
            }
        }
    }

    /** Live value while the slider is being dragged (saved when released). */
    var backgroundDimPreview by mutableStateOf<Float?>(null)

    fun removeBackground() {
        val id = store.data.value.backgroundId
        store.setBackground(null)
        if (id != null) viewModelScope.launch { withContext(Dispatchers.IO) { backgroundFile(id).delete() } }
    }

    // ---------------- barcode ----------------

    fun scan(activity: Activity, date: LocalDate) = startScanner(activity) { code -> onBarcode(date, code) }

    private fun startScanner(activity: Activity, onCode: (String) -> Unit) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .build()
        GmsBarcodeScanning.getClient(activity, options)
            .startScan()
            .addOnSuccessListener { barcode ->
                val code = barcode.rawValue
                if (code.isNullOrBlank()) toast(T.cantReadBarcode()) else onCode(code)
            }
            .addOnFailureListener { e ->
                toast(T.scannerUnavailable(e.message ?: T.error()))
            }
    }

    private fun onBarcode(date: LocalDate, code: String) {
        store.knownByBarcode(code)?.let {
            chooseKnown(date, it)
            return
        }
        scanLoading = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { OffApi.byBarcode(code) } }
            scanLoading = false
            val product = result.getOrNull()
            when {
                product != null -> choose(date, product)
                result.isFailure -> toast(T.noConnection())
                else -> {
                    pendingBarcode = code
                    toast(T.barcodeNotFoundEnterName(code))
                }
            }
        }
    }

    // ---------------- sharing ----------------

    fun shareDay(context: Context, date: LocalDate) {
        val items = store.items(date)
        if (items.isEmpty()) {
            toast(T.listEmpty())
            return
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, T.shareSubject(Dates.full(date)))
            putExtra(Intent.EXTRA_TEXT, ShareCodec.shareText(date, items))
        }
        context.startActivity(Intent.createChooser(send, T.shareChooser()))
    }

    fun copyDayToClipboard(context: Context, date: LocalDate) {
        val items = store.items(date)
        if (items.isEmpty()) return
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(T.appTitle(), ShareCodec.shareText(date, items)))
        toast(T.listCopied())
    }

    fun copyDay(from: LocalDate, to: LocalDate) {
        val n = store.copyDay(from, to)
        toast(T.copiedFrom(Dates.productsCount(n), Dates.short(from)))
    }

    // ---------------- receiving ----------------

    var pendingImport by mutableStateOf<ShareCodec.Shared?>(null)

    /** Called with text from a link, a shared message or the clipboard. */
    fun receiveText(text: String?, silent: Boolean = false): Boolean {
        val shared = ShareCodec.findInText(text)
        if (shared == null) {
            if (!silent) toast(T.noListLink())
            return false
        }
        pendingImport = shared
        return true
    }

    fun importFromClipboard(context: Context) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
        if (text.isNullOrBlank()) {
            toast(T.clipboardEmpty())
            return
        }
        receiveText(text)
    }

    fun confirmImport(date: LocalDate) {
        val s = pendingImport ?: return
        val n = store.addAll(date, s.items)
        pendingImport = null
        goToDay(date)
        toast(T.addedCount(Dates.productsCount(n)))
    }
}
