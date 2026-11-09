package de.mef.lebensmittel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.concurrent.Executors

/** All app data in one JSON file in the app's private storage. Small, simple, offline. */
class Store(private val file: File) {

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private val writer = Executors.newSingleThreadExecutor { r -> Thread(r, "store-writer").apply { isDaemon = true } }

    private fun load(): AppData = runCatching {
        if (!file.exists()) return@runCatching AppData()
        Mapping.dataFromJson(JSONObject(file.readText(Charsets.UTF_8)))
    }.getOrElse { AppData() }

    @Synchronized
    private fun update(change: (AppData) -> AppData) {
        val next = change(_data.value)
        if (next == _data.value) return
        _data.value = next
        val json = Mapping.dataToJson(next).toString()
        writer.execute {
            runCatching {
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText(json, Charsets.UTF_8)
                if (!tmp.renameTo(file)) {
                    file.writeText(json, Charsets.UTF_8)
                    tmp.delete()
                }
            }
        }
    }

    private fun AppData.withDay(date: LocalDate, f: (List<ShopItem>) -> List<ShopItem>): AppData {
        val key = date.toString()
        val newList = f(days[key].orEmpty())
        val newDays = if (newList.isEmpty()) days - key else days + (key to newList)
        return copy(days = newDays)
    }

    // ---------- day lists ----------

    fun items(date: LocalDate): List<ShopItem> = _data.value.days[date.toString()].orEmpty()

    fun addItem(date: LocalDate, item: ShopItem) = update { d ->
        d.withDay(date) { list ->
            // same product already on the list and not bought yet -> add up if units match
            // (a Pfand receipt is always its own line)
            val idx = if (item.pfandReturn) -1 else list.indexOfFirst {
                it.product.key == item.product.key && it.listId == item.listId && !it.checked && it.unit == item.unit &&
                    it.promo == item.promo && it.card == item.card && it.pfand == item.pfand && !it.pfandReturn
            }
            if (idx >= 0) {
                list.toMutableList().also {
                    val old = it[idx]
                    val price = if (old.price == null && item.price == null) null else (old.price ?: 0.0) + (item.price ?: 0.0)
                    it[idx] = old.copy(amount = old.amount + item.amount, price = price)
                }
            } else {
                list + item
            }
        }.remember(item)
    }

    fun updateItem(date: LocalDate, item: ShopItem) = update { d ->
        d.withDay(date) { list -> list.map { if (it.id == item.id) item else it } }
            .remember(item, countUse = false)
    }

    fun removeItem(date: LocalDate, id: String) = update { d -> d.withDay(date) { list -> list.filterNot { it.id == id } } }

    fun toggle(date: LocalDate, id: String) = update { d ->
        d.withDay(date) { list -> list.map { if (it.id == id) it.copy(checked = !it.checked) else it } }
    }

    fun removeChecked(date: LocalDate, listId: String) = update { d ->
        d.withDay(date) { list -> list.filterNot { it.checked && it.listId == listId } }
    }

    /** Empties one list of the day (the other lists stay). */
    fun clearList(date: LocalDate, listId: String) = update { d ->
        d.withDay(date) { list -> list.filterNot { it.listId == listId } }
    }

    /** Appends items to list [listId] of [to] as new, unchecked entries. Returns how many were added. */
    fun addAll(to: LocalDate, items: List<ShopItem>, listId: String = MAIN_LIST): Int {
        if (items.isEmpty()) return 0
        update { d ->
            var acc = d.withDay(to) { list -> list + items.map { it.copy(id = newId(), checked = false, listId = listId) } }
            for (i in items) acc = acc.remember(i, countUse = false)
            acc
        }
        return items.size
    }

    /** Copies every list of [from] into list [listId] of [to]. Pfand receipts belong to their day, so they are not copied. */
    fun copyDay(from: LocalDate, to: LocalDate, listId: String): Int =
        addAll(to, items(from).filterNot { it.pfandReturn }, listId)

    /** Adds a Pfand receipt (money back) to the day; it counts as already done. */
    fun addPfandReturn(date: LocalDate, amount: Double, listId: String) = update { d ->
        d.withDay(date) { list ->
            list + ShopItem(
                product = Product(name = PFAND_RETURN_NAME),
                amount = 1.0,
                unit = QtyUnit.PCS,
                checked = true,
                price = -amount,
                pfandReturn = true,
                listId = listId,
            )
        }
    }

    // ---------- several lists per day ----------

    private fun AppData.withLists(date: LocalDate, f: (List<DayList>) -> List<DayList>): AppData {
        val key = date.toString()
        val next = f(listsOf(date))
        // only the main list with its default name: nothing to store
        val plain = next.size == 1 && next[0].id == MAIN_LIST && next[0].name == null
        return copy(dayLists = if (plain) dayLists - key else dayLists + (key to next))
    }

    /** Adds an empty list to the day and returns its id. */
    fun addList(date: LocalDate, name: String?): String {
        val id = newId()
        update { d -> d.withLists(date) { it + DayList(id, name?.trim()?.ifBlank { null }) } }
        return id
    }

    fun renameList(date: LocalDate, listId: String, name: String) = update { d ->
        d.withLists(date) { lists -> lists.map { if (it.id == listId) it.copy(name = name.trim().ifBlank { null }) else it } }
    }

    /** Removes an extra list with its items; the main list can only be emptied. */
    fun deleteList(date: LocalDate, listId: String) {
        if (listId == MAIN_LIST) return
        update { d ->
            d.withDay(date) { list -> list.filterNot { it.listId == listId } }
                .withLists(date) { lists -> lists.filterNot { it.id == listId } }
        }
    }

    /** Days that have a list, newest first. */
    fun daysWithLists(): List<Pair<LocalDate, List<ShopItem>>> =
        _data.value.days.mapNotNull { (k, v) ->
            runCatching { LocalDate.parse(k) }.getOrNull()?.takeIf { v.isNotEmpty() }?.let { it to v }
        }.sortedByDescending { it.first }

    // ---------- remembered products ----------

    private fun AppData.remember(item: ShopItem, countUse: Boolean = true): AppData {
        if (item.pfandReturn) return this
        val p = item.product
        val amount = item.amount
        val unit = item.unit
        val perUnit = item.price?.takeIf { amount > 0 }?.let { it / amount }
        val old = known[p.key]
        val merged = if (old == null) p else p.copy(
            nameUk = p.nameUk ?: old.product.nameUk,
            imageUrl = p.imageUrl ?: old.product.imageUrl,
            kcal100 = p.kcal100 ?: old.product.kcal100,
            packageQty = p.packageQty ?: old.product.packageQty,
            source = p.source ?: old.product.source,
            stores = p.stores.ifEmpty { old.product.stores },
        )
        val kp = KnownProduct(
            product = merged,
            lastAmount = amount,
            lastUnit = unit,
            uses = (old?.uses ?: 0) + if (countUse || old == null) 1 else 0,
            lastUsed = System.currentTimeMillis(),
            // a promo / card price must not replace the regular one
            unitPrice = if (perUnit != null && !item.promo && !item.card) perUnit
            else old?.unitPrice?.takeIf { old.lastUnit == unit },
            promoPrice = if (perUnit != null && item.promo) SavedPrice(perUnit, unit) else old?.promoPrice,
            cardPrice = if (perUnit != null && item.card) SavedPrice(perUnit, unit) else old?.cardPrice,
            pfand = item.pfand,
        )
        return copy(known = known + (p.key to kp))
    }

    fun forget(key: String) = update { d -> d.copy(known = d.known - key) }

    fun knownByBarcode(code: String): KnownProduct? = _data.value.known["c:$code"]

    fun frequent(limit: Int = 30): List<KnownProduct> =
        _data.value.known.values.sortedWith(compareByDescending<KnownProduct> { it.uses }.thenByDescending { it.lastUsed }).take(limit)

    /** Remembered products matching the typed text (German, brand, or Ukrainian name). */
    fun searchKnown(query: String, germanQuery: String?, limit: Int = 10): List<KnownProduct> {
        val q = Txt.norm(query)
        val g = germanQuery?.let { Txt.norm(it) }?.takeIf { it != q }
        if (q.isEmpty()) return emptyList()
        return _data.value.known.values.map { k ->
            val p = k.product
            // search the product name only (and the translated name), not brand or other details
            val fields = listOfNotNull(p.name, p.nameUk).map { Txt.norm(it) }
            var s = fields.maxOf { Txt.score(q, it) }
            if (g != null) s = maxOf(s, fields.maxOf { Txt.score(g, it) })
            k to s
        }.filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<KnownProduct, Int>> { it.second }.thenByDescending { it.first.uses })
            .take(limit).map { it.first }
    }

    // ---------- settings ----------

    fun setShowPhotos(show: Boolean) = update { it.copy(showPhotos = show) }

    fun setBackground(id: Long?, luma: Float? = null) = update { it.copy(backgroundId = id, backgroundLuma = luma) }

    fun setBackgroundLuma(luma: Float) = update { it.copy(backgroundLuma = luma) }

    fun setThemeMode(mode: String) = update { it.copy(themeMode = mode) }

    fun setNameColor(argb: Int?) = update { it.copy(nameColor = argb) }

    fun setShowBrand(show: Boolean) = update { it.copy(showBrand = show) }

    fun setShowKcal(show: Boolean) = update { it.copy(showKcal = show) }

    fun setLanguage(code: String) = update { it.copy(language = code) }

    // ---------- reminders ----------

    /** Adds a new reminder or replaces the one with the same id. */
    fun saveReminder(r: Reminder) = update { d ->
        val exists = d.reminders.any { it.id == r.id }
        d.copy(reminders = if (exists) d.reminders.map { if (it.id == r.id) r else it } else d.reminders + r)
    }

    fun deleteReminder(id: String) = update { d -> d.copy(reminders = d.reminders.filterNot { it.id == id }) }

    fun remindersFor(date: LocalDate): List<Reminder> =
        _data.value.reminders.filter { it.date == date.toString() }.sortedBy { it.time }

    /** Enabled reminders still to come, soonest first. */
    fun upcomingReminders(now: Long = System.currentTimeMillis()): List<Reminder> =
        _data.value.reminders.filter { it.enabled && !it.isPast(now) }.sortedBy { it.triggerAt() }

    fun setTabHidden(tab: String, hidden: Boolean) = update { d ->
        d.copy(hiddenTabs = if (hidden) d.hiddenTabs + tab else d.hiddenTabs - tab)
    }

    fun setBackgroundDim(dim: Float) = update { it.copy(backgroundDim = dim.coerceIn(0f, 0.95f)) }
}
