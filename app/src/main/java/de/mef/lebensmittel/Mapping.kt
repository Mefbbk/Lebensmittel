package de.mef.lebensmittel

import org.json.JSONArray
import org.json.JSONObject

/** JSON (de)serialization for local storage. Short keys keep share links small too. */
object Mapping {

    fun productToJson(p: Product, withImage: Boolean = true): JSONObject = JSONObject().apply {
        put("n", p.name)
        p.brand?.let { put("b", it) }
        p.barcode?.let { put("c", it) }
        p.packageQty?.let { put("q", it) }
        p.kcal100?.let { put("k", it) }
        if (withImage) p.imageUrl?.let { put("i", it) }
        p.nameUk?.let { put("u", it) }
        p.source?.let { put("s", it) }
        if (p.stores.isNotEmpty()) put("st", p.stores.joinToString(","))
    }

    fun productFromJson(o: JSONObject): Product = Product(
        name = o.optString("n", "?"),
        brand = o.optStr("b"),
        barcode = o.optStr("c"),
        packageQty = o.optStr("q"),
        kcal100 = if (o.has("k") && !o.isNull("k")) o.optDouble("k").takeIf { !it.isNaN() } else null,
        imageUrl = o.optStr("i"),
        nameUk = o.optStr("u"),
        source = o.optStr("s"),
        stores = o.optStr("st")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty(),
    )

    fun itemToJson(i: ShopItem, withImage: Boolean = true, withState: Boolean = true): JSONObject =
        JSONObject().apply {
            if (withState) put("id", i.id)
            put("p", productToJson(i.product, withImage))
            put("a", i.amount)
            put("un", i.unit.code)
            if (withState && i.checked) put("x", true)
            i.price?.let { put("pr", it) }
            if (i.promo) put("pa", true)
            if (i.card) put("pc", true)
        }

    fun itemFromJson(o: JSONObject): ShopItem = ShopItem(
        id = o.optStr("id") ?: newId(),
        product = productFromJson(o.getJSONObject("p")),
        amount = o.optDouble("a", 1.0).takeIf { !it.isNaN() && it > 0 } ?: 1.0,
        unit = QtyUnit.fromCode(o.optStr("un")),
        checked = o.optBoolean("x", false),
        price = o.optNum("pr")?.takeIf { it >= 0 },
        promo = o.optBoolean("pa", false),
        card = o.optBoolean("pc", false),
    )

    private fun savedPriceToJson(s: SavedPrice): JSONObject = JSONObject().apply {
        put("v", s.perUnit)
        put("un", s.unit.code)
    }

    private fun savedPriceFromJson(o: JSONObject?): SavedPrice? =
        o?.optNum("v")?.takeIf { it >= 0 }?.let { SavedPrice(it, QtyUnit.fromCode(o.optStr("un"))) }

    fun dataToJson(d: AppData): JSONObject = JSONObject().apply {
        put("version", 1)
        put("showPhotos", d.showPhotos)
        d.backgroundId?.let { put("bg", it) }
        d.backgroundLuma?.let { put("bgLuma", it.toDouble()) }
        put("bgDim", d.backgroundDim.toDouble())
        put("theme", d.themeMode)
        d.nameColor?.let { put("nameColor", it) }
        put("showBrand", d.showBrand)
        put("showKcal", d.showKcal)
        put("lang", d.language)
        val rem = JSONArray()
        d.reminders.forEach { r ->
            rem.put(JSONObject().apply {
                put("id", r.id)
                put("d", r.date)
                put("t", r.time)
                put("x", r.text)
                put("on", r.enabled)
            })
        }
        put("reminders", rem)
        val hidden = JSONArray()
        d.hiddenTabs.forEach { hidden.put(it) }
        put("hiddenTabs", hidden)
        val days = JSONObject()
        for ((date, items) in d.days) {
            if (items.isEmpty()) continue
            val arr = JSONArray()
            items.forEach { arr.put(itemToJson(it)) }
            days.put(date, arr)
        }
        put("days", days)
        val known = JSONArray()
        for (k in d.known.values) {
            known.put(JSONObject().apply {
                put("p", productToJson(k.product))
                put("a", k.lastAmount)
                put("un", k.lastUnit.code)
                put("uses", k.uses)
                put("t", k.lastUsed)
                k.unitPrice?.let { put("up", it) }
                k.promoPrice?.let { put("pa", savedPriceToJson(it)) }
                k.cardPrice?.let { put("pc", savedPriceToJson(it)) }
            })
        }
        put("known", known)
    }

    fun dataFromJson(o: JSONObject): AppData {
        val days = mutableMapOf<String, List<ShopItem>>()
        o.optJSONObject("days")?.let { dj ->
            val it = dj.keys()
            while (it.hasNext()) {
                val date = it.next()
                val arr = dj.optJSONArray(date) ?: continue
                days[date] = (0 until arr.length()).mapNotNull { idx ->
                    arr.optJSONObject(idx)?.let { runCatching { itemFromJson(it) }.getOrNull() }
                }
            }
        }
        val known = mutableMapOf<String, KnownProduct>()
        o.optJSONArray("known")?.let { arr ->
            for (idx in 0 until arr.length()) {
                val ko = arr.optJSONObject(idx) ?: continue
                val p = ko.optJSONObject("p")?.let { productFromJson(it) } ?: continue
                known[p.key] = KnownProduct(
                    product = p,
                    lastAmount = ko.optDouble("a", 1.0).takeIf { !it.isNaN() && it > 0 } ?: 1.0,
                    lastUnit = QtyUnit.fromCode(ko.optStr("un")),
                    uses = ko.optInt("uses", 1),
                    lastUsed = ko.optLong("t", 0L),
                    unitPrice = ko.optNum("up")?.takeIf { it >= 0 },
                    promoPrice = savedPriceFromJson(ko.optJSONObject("pa")),
                    cardPrice = savedPriceFromJson(ko.optJSONObject("pc")),
                )
            }
        }
        return AppData(
            days = days,
            known = known,
            showPhotos = o.optBoolean("showPhotos", true),
            backgroundId = if (o.has("bg") && !o.isNull("bg")) o.optLong("bg", 0L).takeIf { it > 0 } else null,
            backgroundLuma = o.optNum("bgLuma")?.toFloat()?.coerceIn(0f, 1f),
            backgroundDim = (o.optNum("bgDim") ?: 0.6).toFloat().coerceIn(0f, 0.95f),
            themeMode = o.optStr("theme")?.takeIf { it in listOf("light", "dark", "system") } ?: "light",
            nameColor = o.optNum("nameColor")?.toLong()?.toInt(),
            showBrand = o.optBoolean("showBrand", true),
            showKcal = o.optBoolean("showKcal", true),
            language = o.optStr("lang")?.takeIf { it == "system" || Lang.fromCode(it) != null } ?: "uk",
            reminders = o.optJSONArray("reminders")?.let { arr ->
                (0 until arr.length()).mapNotNull { i ->
                    val r = arr.optJSONObject(i) ?: return@mapNotNull null
                    Reminder(
                        id = r.optStr("id") ?: newId(),
                        date = r.optStr("d") ?: return@mapNotNull null,
                        time = r.optStr("t") ?: return@mapNotNull null,
                        text = r.optStr("x") ?: "",
                        enabled = r.optBoolean("on", true),
                    )
                }
            }.orEmpty(),
            hiddenTabs = o.optJSONArray("hiddenTabs")?.let { arr ->
                (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }.toSet()
            }.orEmpty(),
        )
    }
}

fun JSONObject.optNum(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return when (val v = opt(key)) {
        is Number -> v.toDouble()
        is String -> v.replace(',', '.').toDoubleOrNull()
        else -> null
    }?.takeIf { !it.isNaN() && !it.isInfinite() }
}

/** org.json's optString returns "" for missing keys and "null" for JSON null; this returns null instead. */
fun JSONObject.optStr(key: String): String? {
    if (!has(key) || isNull(key)) return null
    val v = opt(key)
    return when (v) {
        is String -> v.trim().takeIf { it.isNotEmpty() }
        is Number, is Boolean -> v.toString()
        else -> null
    }
}
