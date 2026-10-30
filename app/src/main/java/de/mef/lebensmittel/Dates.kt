package de.mef.lebensmittel

import java.time.LocalDate
import java.time.YearMonth

/** Date texts in the current app language (see [I18n]). */
object Dates {
    val weekdaysShort: List<String> get() = T.weekdaysShort()

    fun weekday(d: LocalDate): String = T.weekdaysShort()[d.dayOfWeek.value - 1]

    /** "Жовтень 2026", "October 2026" */
    fun month(m: YearMonth): String = "${T.monthsNominative()[m.monthValue - 1]} ${m.year}"

    /** "пт, 30 жовтня" / "Fri, 30 October" / "Fr., 30. Oktober" / "الجمعة، 30 أكتوبر" (year added if not the current one) */
    fun full(d: LocalDate, today: LocalDate = LocalDate.now()): String {
        val month = T.monthsInDate()[d.monthValue - 1]
        val base = when (I18n.lang) {
            Lang.DE -> "${weekday(d)}., ${d.dayOfMonth}. $month"
            Lang.AR -> "${weekday(d)}، ${d.dayOfMonth} $month"
            else -> "${weekday(d)}, ${d.dayOfMonth} $month"
        }
        return if (d.year != today.year) "$base ${d.year}" else base
    }

    /** "вт, 27.10" */
    fun short(d: LocalDate): String {
        val dm = "${"%02d".format(d.dayOfMonth)}.${"%02d".format(d.monthValue)}"
        return when (I18n.lang) {
            Lang.DE -> "${weekday(d)}., $dm."
            Lang.AR -> "${weekday(d)} $dm"
            else -> "${weekday(d)}, $dm"
        }
    }

    fun relative(d: LocalDate, today: LocalDate = LocalDate.now()): String? = when (d) {
        today -> T.relToday()
        today.plusDays(1) -> T.relTomorrow()
        today.minusDays(1) -> T.relYesterday()
        else -> null
    }

    /** "3 продукти", "5 products" … */
    fun productsCount(n: Int): String = T.productsCount(n)
}
