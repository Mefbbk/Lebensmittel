package de.mef.lebensmittel

import org.json.JSONArray
import org.json.JSONObject

/** One line of the nutrition table. Values are already converted to [unit]. */
data class Nutrient(
    val key: String,
    val per100: Double?,
    val perServing: Double?,
    /** Unit code: "kcal", "kJ", "g", "mg", "ug", "%" or as given by the database. */
    val unit: String,
    /** 1 = "of which …" line (saturated fat, sugars …) */
    val indent: Int = 0,
) {
    /** Name in the app language. */
    val label: String get() = T.nutrientName(key)

    val unitLabel: String
        get() = when (unit) {
            "kcal" -> T.kcal()
            "kJ" -> T.kj()
            "g" -> T.unitG()
            "mg" -> T.unitMg()
            "ug" -> T.unitUg()
            else -> unit
        }
}

/** Everything the database knows about a scanned product. */
data class NutritionInfo(
    val product: Product,
    val imageUrl: String?,
    val servingSize: String?,
    val nutrients: List<Nutrient>,
    val nutriScore: String?,
    val novaGroup: Int?,
    val ecoScore: String?,
    val ingredients: String?,
    /** Allergen tags ("milk", "gluten" …); shown with [T.allergenName]. */
    val allergens: List<String>,
    val traces: List<String>,
    val additives: List<String>,
    /** Known label tags ("organic", "vegan" …); shown with [T.labelName]. */
    val labels: List<String>,
) {
    val isLiquid: Boolean
        get() = product.packageQty?.let { Regex("""\d\s*(ml|cl|dl|l|ltr|liter)\b""", RegexOption.IGNORE_CASE).containsMatchIn(it) } == true
}

object NutritionParser {

    const val FIELDS = "code,product_name,product_name_de,generic_name_de,brands,quantity,serving_size," +
        "nutriments,nutriscore_grade,nova_group,ecoscore_grade,ingredients_text_de,ingredients_text," +
        "allergens_tags,traces_tags,additives_tags,labels_tags,image_front_url,image_front_small_url,image_small_url,stores,stores_tags"

    /** Fixed order for the main values; everything else follows alphabetically. */
    private val order = listOf(
        "energy-kcal", "energy-kj", "fat", "saturated-fat", "monounsaturated-fat", "polyunsaturated-fat",
        "trans-fat", "cholesterol", "carbohydrates", "sugars", "added-sugars", "lactose", "polyols", "starch",
        "fiber", "proteins", "salt", "sodium",
    )

    private val indented = setOf(
        "saturated-fat", "monounsaturated-fat", "polyunsaturated-fat", "trans-fat",
        "sugars", "added-sugars", "lactose", "polyols", "starch",
    )




    /** Keys that are scores or helper values, not nutrients. */
    private fun skip(base: String) = base == "energy" || base.startsWith("nutrition-score") ||
        base.startsWith("nova") || base.startsWith("fruits-vegetables") || base.startsWith("carbon-footprint") ||
        base == "ph" || base.startsWith("collagen")

    fun parseResponse(body: String, code: String, source: ProductSource = ProductSource.FOOD): NutritionInfo? {
        val root = JSONObject(body)
        if (root.optInt("status", 0) != 1) return null
        val o = root.optJSONObject("product") ?: return null
        val product = OffParser.parseProduct(o, fallbackCode = code, source = source) ?: Product(
            name = T.noName(), barcode = code, source = source.takeIf { it != ProductSource.FOOD }?.code,
        )
        return parse(o, product)
    }

    fun parse(o: JSONObject, product: Product): NutritionInfo {
        val n = o.optJSONObject("nutriments") ?: JSONObject()
        val bases = LinkedHashSet<String>()
        val it = n.keys()
        while (it.hasNext()) {
            val k = it.next()
            val base = when {
                k.endsWith("_100g") -> k.removeSuffix("_100g")
                k.endsWith("_serving") -> k.removeSuffix("_serving")
                else -> null
            } ?: continue
            if (!skip(base)) bases.add(base)
        }
        // only kJ known under "energy"
        val energyKjFallback = !bases.contains("energy-kj") && (n.has("energy_100g") || n.has("energy_serving"))

        val list = ArrayList<Nutrient>()
        for (base in bases) {
            val unit = unitFor(base, n)
            val per100 = n.optNum("${base}_100g")?.let { convert(it, unit, base) }
            val perServing = n.optNum("${base}_serving")?.let { convert(it, unit, base) }
            if (per100 == null && perServing == null) continue
            list.add(Nutrient(base, per100, perServing, unit, if (base in indented) 1 else 0))
        }
        if (energyKjFallback) {
            list.add(Nutrient("energy-kj", n.optNum("energy_100g"), n.optNum("energy_serving"), "kJ"))
        }
        // if kcal is missing but kJ is known, work it out
        if (list.none { it.key == "energy-kcal" }) {
            list.firstOrNull { it.key == "energy-kj" }?.let { kj ->
                list.add(Nutrient("energy-kcal", kj.per100?.div(4.184), kj.perServing?.div(4.184), "kcal"))
            }
        }
        val sorted = list.sortedWith(
            compareBy<Nutrient> { order.indexOf(it.key).let { i -> if (i < 0) Int.MAX_VALUE else i } }.thenBy { it.key },
        )

        return NutritionInfo(
            product = product,
            imageUrl = str(o, "image_front_url") ?: product.imageUrl,
            servingSize = str(o, "serving_size"),
            nutrients = sorted,
            nutriScore = str(o, "nutriscore_grade")?.lowercase()?.takeIf { it in listOf("a", "b", "c", "d", "e") },
            novaGroup = o.optNum("nova_group")?.toInt()?.takeIf { it in 1..4 },
            ecoScore = str(o, "ecoscore_grade")?.lowercase()?.takeIf { it in listOf("a", "b", "c", "d", "e") },
            ingredients = str(o, "ingredients_text_de") ?: str(o, "ingredients_text"),
            allergens = tags(o, "allergens_tags"),
            traces = tags(o, "traces_tags"),
            additives = tags(o, "additives_tags").map { it.uppercase() },
            labels = tags(o, "labels_tags").filter { T.isKnownLabel(it) }.distinct(),
        )
    }

    private fun unitFor(base: String, n: JSONObject): String = when (base) {
        "energy-kcal" -> "kcal"
        "energy-kj" -> "kJ"
        else -> when (val u = n.optStr("${base}_unit")?.trim()) {
            null, "", "g" -> "g"
            "mg" -> "mg"
            "µg", "μg", "mcg", "ug" -> "ug"
            "kcal" -> "kcal"
            "kJ", "kj" -> "kJ"
            "% vol", "%vol", "% DV", "%" -> "%"
            else -> u
        }
    }

    /** "_100g" values are stored in grams even when the label unit is mg or µg. */
    private fun convert(v: Double, unit: String, base: String): Double = when {
        base.startsWith("energy") -> v
        unit == "mg" -> v * 1000
        unit == "ug" -> v * 1_000_000
        else -> v
    }

    private fun tags(o: JSONObject, key: String): List<String> {
        val arr: JSONArray = o.optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optString(i).trim().takeIf { it.isNotEmpty() && it != "null" }?.let { t ->
                if (t.startsWith("en:")) t.removePrefix("en:") else t.substringAfter(':')
            }
        }.distinct()
    }

    private fun str(o: JSONObject, key: String): String? = o.optStr(key)
}
