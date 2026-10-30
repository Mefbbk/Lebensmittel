package de.mef.lebensmittel

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Spending report for a date range: totals per day and the most expensive products. */
object Spending {

    data class Report(
        val from: LocalDate,
        val to: LocalDate,
        val total: Double,
        val byDay: List<Pair<LocalDate, Double>>,
        val top: List<Pair<String, Double>>,
        /** lines that have a price */
        val pricedCount: Int,
        /** lines in the range without a price (not counted in [total]) */
        val unpricedCount: Int,
    ) {
        val dayCount: Long get() = ChronoUnit.DAYS.between(from, to) + 1
        val perDay: Double get() = if (dayCount > 0) total / dayCount else 0.0
    }

    enum class Preset {
        WEEK, MONTH, LAST_MONTH, DAYS30, YEAR;

        val label: String
            get() = when (this) {
                WEEK -> T.presetWeek()
                MONTH -> T.presetMonth()
                LAST_MONTH -> T.presetLastMonth()
                DAYS30 -> T.preset30()
                YEAR -> T.presetYear()
            }
    }

    fun range(p: Preset, today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> = when (p) {
        Preset.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to
            today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        Preset.MONTH -> today.withDayOfMonth(1) to today.with(TemporalAdjusters.lastDayOfMonth())
        Preset.LAST_MONTH -> today.minusMonths(1).withDayOfMonth(1).let { it to it.with(TemporalAdjusters.lastDayOfMonth()) }
        Preset.DAYS30 -> today.minusDays(29) to today
        Preset.YEAR -> today.withDayOfYear(1) to today.with(TemporalAdjusters.lastDayOfYear())
    }

    fun report(data: AppData, from: LocalDate, to: LocalDate, onlyBought: Boolean): Report {
        val a = if (from <= to) from else to
        val b = if (from <= to) to else from
        val byDay = ArrayList<Pair<LocalDate, Double>>()
        val perProduct = LinkedHashMap<String, Double>()
        var total = 0.0
        var priced = 0
        var unpriced = 0
        for ((key, list) in data.days) {
            val date = runCatching { LocalDate.parse(key) }.getOrNull() ?: continue
            if (date < a || date > b) continue
            var daySum = 0.0
            for (item in list) {
                if (onlyBought && !item.checked) continue
                val price = item.price
                if (price == null) {
                    unpriced++
                    continue
                }
                priced++
                daySum += price
                val name = item.product.name + (item.product.brand?.let { " ($it)" } ?: "")
                perProduct[name] = (perProduct[name] ?: 0.0) + price
            }
            if (daySum > 0) byDay.add(date to daySum)
            total += daySum
        }
        byDay.sortBy { it.first }
        val top = perProduct.entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }
        return Report(a, b, total, byDay, top, priced, unpriced)
    }
}
