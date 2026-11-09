package de.mef.lebensmittel

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

enum class QtyUnit(val code: String) {
    PCS("pcs"),
    G("g"),
    KG("kg"),
    ML("ml"),
    L("l"),
    PACK("pack");

    /** Short unit name in the app language: "шт", "pcs", "Stk." … */
    val label: String
        get() = when (this) {
            PCS -> T.unitPcs()
            G -> T.unitG()
            KG -> T.unitKg()
            ML -> T.unitMl()
            L -> T.unitL()
            PACK -> T.unitPack()
        }

    companion object {
        fun fromCode(code: String?): QtyUnit = entries.firstOrNull { it.code == code } ?: PCS
    }
}

/** A product: either from Open Food Facts, from the built-in dictionary, or typed by the user. */
data class Product(
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val packageQty: String? = null,
    val kcal100: Double? = null,
    val imageUrl: String? = null,
    val nameUk: String? = null,
    /** Which database it came from: null = food, "beauty", "products", "pet". */
    val source: String? = null,
    /** Supermarkets (slugs like "edeka", "lidl") where the product is sold, if known. */
    val stores: List<String> = emptyList(),
) {
    /** Stable identity used to remember products the user already entered. */
    val key: String
        get() = barcode?.takeIf { it.isNotBlank() }?.let { "c:$it" }
            ?: "n:${Txt.norm(name)}|${Txt.norm(brand ?: "")}"

    val subtitle: String?
        get() = listOfNotNull(
            brand?.takeIf { it.isNotBlank() },
            packageQty?.takeIf { it.isNotBlank() },
            ProductSource.fromCode(source)?.label,
        ).joinToString(" · ").ifBlank { null }
}

data class ShopItem(
    val id: String = newId(),
    val product: Product,
    val amount: Double,
    val unit: QtyUnit,
    val checked: Boolean = false,
    /** What this line costs in total (EUR), if the user entered it. */
    val price: Double? = null,
    /** The price is a special offer (Aktion). */
    val promo: Boolean = false,
    /** The price is the one with the store's customer card (Lidl Plus, Payback …). */
    val card: Boolean = false,
    /** Deposit (Pfand) for one bottle / can (EUR), null = no deposit. */
    val pfand: Double? = null,
    /** A Pfand receipt from the return machine: [price] is negative, not a product to buy. */
    val pfandReturn: Boolean = false,
    /** Which of the day's shopping lists the item is on; "" = the main list. */
    val listId: String = MAIN_LIST,
) {
    val amountLabel: String get() = "${formatNumber(amount)} ${unit.label}"

    /** Deposit for the whole line: per piece / pack, or once for weighed amounts. */
    val pfandTotal: Double?
        get() = pfand?.let { it * if (unit == QtyUnit.PCS || unit == QtyUnit.PACK) amount else 1.0 }

    /** What the line costs with the deposit; null when there is no price yet. */
    val cost: Double?
        get() = price?.let { it + (pfandTotal ?: 0.0) }
}

/** Id of the list every day has; extra lists get their own ids. */
const val MAIN_LIST = ""

/** One shopping list of a day. [name] null = default name ("Список 1" …). */
data class DayList(val id: String, val name: String? = null)

/** Name shown on the tab: the user's name or "Список N". */
fun DayList.title(index: Int): String = name?.takeIf { it.isNotBlank() } ?: T.listN(index + 1)

/** Lists of a day in order; a day without extra lists has just the main one. */
fun AppData.listsOf(date: java.time.LocalDate): List<DayList> =
    dayLists[date.toString()]?.takeIf { l -> l.any { it.id == MAIN_LIST } } ?: listOf(DayList(MAIN_LIST))

/** Items on one list of the day; items of a list that no longer exists show up on the main list. */
fun AppData.itemsOf(date: java.time.LocalDate, listId: String): List<ShopItem> {
    val ids = listsOf(date).map { it.id }.toSet()
    return days[date.toString()].orEmpty().filter { (if (it.listId in ids) it.listId else MAIN_LIST) == listId }
}

/** Name of a Pfand receipt line in the list (product names stay German). */
const val PFAND_RETURN_NAME = "Pfand-Rückgabe"

/** Usual German deposit for a single-use bottle or can. */
const val DEFAULT_PFAND = 0.25

/** A remembered price for 1 [unit] (EUR). */
data class SavedPrice(val perUnit: Double, val unit: QtyUnit)

data class KnownProduct(
    val product: Product,
    val lastAmount: Double,
    val lastUnit: QtyUnit,
    val uses: Int,
    val lastUsed: Long,
    /** Last known regular price for 1 [lastUnit] (EUR), used to pre-fill the price next time. */
    val unitPrice: Double? = null,
    /** Last special-offer price; suggested when the "promo" box is ticked. */
    val promoPrice: SavedPrice? = null,
    /** Last customer-card price; suggested when the "card" box is ticked. */
    val cardPrice: SavedPrice? = null,
    /** Deposit per piece the product had last time; the Pfand box is ticked again next time. */
    val pfand: Double? = null,
)

data class AppData(
    val days: Map<String, List<ShopItem>> = emptyMap(),
    val known: Map<String, KnownProduct> = emptyMap(),
    val showPhotos: Boolean = true,
    /** Id of the background picture (file bg_<id>.jpg in app storage), null = no picture. */
    val backgroundId: Long? = null,
    /** How strongly the background picture is covered so text stays readable (0..1). */
    val backgroundDim: Float = 0.6f,
    /** Average brightness of the background picture, 0 = black .. 1 = white (null = unknown). */
    val backgroundLuma: Float? = null,
    /** "light" (default), "dark" or "system". */
    val themeMode: String = "light",
    /** Colour of product names as 0xAARRGGBB, null = default text colour. */
    val nameColor: Int? = null,
    /** Show brand / package size under the product name in the list. */
    val showBrand: Boolean = true,
    /** Show calories in the list. */
    val showKcal: Boolean = true,
    /** App language: "uk", "en", "de", "ru", "ar" or "system". */
    val language: String = "uk",
    val reminders: List<Reminder> = emptyList(),
    /** Bottom tabs the user switched off: "nutrition", "spending", "reminders". */
    val hiddenTabs: Set<String> = emptySet(),
    /** Shopping lists per day ("2026-11-09" -> lists), only for days that have more than one or a renamed one. */
    val dayLists: Map<String, List<DayList>> = emptyMap(),
)

/** A note for a day with a time; a notification is shown at that time. */
data class Reminder(
    val id: String = newId(),
    /** "2026-10-30" */
    val date: String,
    /** "18:30" */
    val time: String,
    val text: String,
    val enabled: Boolean = true,
) {
    val localDate: java.time.LocalDate? get() = runCatching { java.time.LocalDate.parse(date) }.getOrNull()
    val localTime: java.time.LocalTime? get() = runCatching { java.time.LocalTime.parse(time) }.getOrNull()

    /** When to notify, in the phone's time zone; null if the date or time is broken. */
    fun triggerAt(zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): Long? {
        val d = localDate ?: return null
        val t = localTime ?: return null
        return java.time.LocalDateTime.of(d, t).atZone(zone).toInstant().toEpochMilli()
    }

    fun isPast(now: Long = System.currentTimeMillis()): Boolean = (triggerAt() ?: 0L) <= now
}

/** "18:30" from hour and minute. */
fun timeText(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

/** Dark picture -> light text (dark theme). Null when there is no picture or it was not measured yet. */
val AppData.backgroundIsDark: Boolean?
    get() = if (backgroundId == null) null else backgroundLuma?.let { it < 0.5f }

/** Average perceived brightness (0..1) of ARGB pixels. */
fun averageLuma(pixels: IntArray): Float {
    if (pixels.isEmpty()) return 1f
    var sum = 0.0
    for (p in pixels) {
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        sum += (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
    }
    return (sum / pixels.size).toFloat()
}

/** "#1E88E5" -> 0xFF1E88E5.toInt(); null if not a valid colour. */
fun parseHexColor(text: String): Int? {
    val t = text.trim().removePrefix("#")
    if (t.length != 6 || !t.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    return (0xFF000000L or t.toLong(16)).toInt()
}

/** 0xFF1E88E5 -> "#1E88E5" */
fun toHexColor(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)

fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(16)

/** 1.0 -> "1", 0.5 -> "0,5", 1234.0 -> "1234" */
fun formatNumber(value: Double, maxDecimals: Int = 2): String {
    if (value.isNaN() || value.isInfinite()) return "?"
    val bd = BigDecimal(value).setScale(maxDecimals, RoundingMode.HALF_UP).stripTrailingZeros()
    return bd.toPlainString().let { if (I18n.lang.decimalComma) it.replace('.', ',') else it }
}

/** 2.5 -> "2,50 €" */
fun formatMoney(value: Double): String = formatMoneyPlain(value) + " €"

/** 2.5 -> "2,50" */
fun formatMoneyPlain(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "?"
    return BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toPlainString()
        .let { if (I18n.lang.decimalComma) it.replace('.', ',') else it }
}

/** "2,5" / "2.50" / "2,50 €" -> 2.5; blank -> null; 0 allowed (free). */
fun parseMoney(text: String): Double? =
    text.replace("€", "").trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 && !it.isInfinite() }

fun parseAmount(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && !it.isInfinite() }
