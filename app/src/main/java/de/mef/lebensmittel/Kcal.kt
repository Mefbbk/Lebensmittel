package de.mef.lebensmittel

/** Calorie maths. Open Food Facts stores kcal per 100 g / 100 ml. */
object Kcal {
    private const val UNITS = "(kg|gr|g|mg|ml|cl|dl|ltr|liter|l)"
    private val multi = Regex("""(\d+)\s*[x×*]\s*(\d+(?:[.,]\d+)?)\s*$UNITS\b""", RegexOption.IGNORE_CASE)
    private val single = Regex("""(\d+(?:[.,]\d+)?)\s*$UNITS\b""", RegexOption.IGNORE_CASE)

    private fun toGrams(value: Double, unit: String): Double? = when (unit.lowercase()) {
        "kg" -> value * 1000
        "g", "gr" -> value
        "mg" -> value / 1000
        "ml" -> value
        "cl" -> value * 10
        "dl" -> value * 100
        "l", "ltr", "liter" -> value * 1000
        else -> null
    }

    /** "500 g" -> 500.0, "1,5 l" -> 1500.0, "6 x 0,33 l" -> 1980.0 (ml treated as g). */
    fun packageGrams(quantity: String?): Double? {
        if (quantity.isNullOrBlank()) return null
        multi.find(quantity)?.let { m ->
            val n = m.groupValues[1].toDoubleOrNull() ?: return@let
            val v = m.groupValues[2].replace(',', '.').toDoubleOrNull() ?: return@let
            return toGrams(n * v, m.groupValues[3])?.takeIf { it > 0 }
        }
        single.find(quantity)?.let { m ->
            val v = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@let
            return toGrams(v, m.groupValues[2])?.takeIf { it > 0 }
        }
        return null
    }

    /** Weight (or volume in ml) of the item, if it can be worked out. */
    fun grams(amount: Double, unit: QtyUnit, product: Product): Double? = when (unit) {
        QtyUnit.G, QtyUnit.ML -> amount
        QtyUnit.KG, QtyUnit.L -> amount * 1000
        QtyUnit.PCS, QtyUnit.PACK -> packageGrams(product.packageQty)?.let { it * amount }
    }

    fun total(amount: Double, unit: QtyUnit, product: Product): Double? {
        val k = product.kcal100 ?: return null
        val g = grams(amount, unit, product) ?: return null
        return g * k / 100.0
    }

    fun total(item: ShopItem): Double? = total(item.amount, item.unit, item.product)

    fun per100Label(product: Product): String? {
        val k = product.kcal100 ?: return null
        val isLiquid = product.packageQty?.let { Regex("""\d\s*(ml|cl|dl|l|ltr|liter)\b""", RegexOption.IGNORE_CASE).containsMatchIn(it) } == true
        return "${formatNumber(k, 0)} ${T.kcal()}/100 ${if (isLiquid) T.unitMl() else T.unitG()}"
    }
}
