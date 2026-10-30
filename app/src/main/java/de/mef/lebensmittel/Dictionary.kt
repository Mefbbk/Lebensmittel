package de.mef.lebensmittel

/**
 * Built-in dictionary of everyday groceries (assets/dictionary.txt): German ⇄ Ukrainian, Russian, English.
 * Line format:  German;Ukrainian;more|Ukrainian|names;Russian|names;English|names
 * Used so that typing "хліб", "хлеб" or "bread" finds "Brot", and for generic products without a brand.
 */
data class DictEntry(
    val de: String,
    val uk: String,
    val alts: List<String>,
    val ru: List<String> = emptyList(),
    val en: List<String> = emptyList(),
) {
    internal val deKeys: List<String> = listOf(Txt.norm(de))
    internal val ukKeys: List<String> = (listOf(uk) + alts).map { Txt.norm(it) }
    internal val ruKeys: List<String> = ru.map { Txt.norm(it) }
    internal val enKeys: List<String> = en.map { Txt.norm(it) }
    internal val allKeys: List<String> = deKeys + ukKeys + ruKeys + enKeys

    /** Name in the app language, shown next to the German word (null when the app is German or Arabic). */
    val localName: String?
        get() = when (I18n.lang) {
            Lang.UK -> uk
            Lang.RU -> ru.firstOrNull()
            Lang.EN -> en.firstOrNull()
            else -> null
        }
}

class Dictionary(val entries: List<DictEntry>) {

    companion object {
        fun parse(tsv: String): Dictionary {
            val list = tsv.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull { line ->
                    val parts = line.split(';')
                    if (parts.size < 2) return@mapNotNull null
                    fun names(i: Int) = parts.getOrNull(i)?.split('|')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
                    DictEntry(parts[0].trim(), parts[1].trim(), names(2), names(3), names(4))
                }
                .toList()
            return Dictionary(list)
        }
    }

    private fun scoreOf(e: DictEntry, q: String): Int = e.allKeys.maxOf { Txt.score(q, it) }

    fun search(query: String, limit: Int = 8): List<DictEntry> {
        val q = Txt.norm(query)
        if (q.length < 2) return emptyList()
        return entries
            .map { it to scoreOf(it, q) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<DictEntry, Int>> { it.second }.thenBy { it.first.de.length })
            .take(limit)
            .map { it.first }
    }

    /** Entry whose German or Ukrainian name is exactly [query]. */
    fun exact(query: String): DictEntry? {
        val q = Txt.norm(query)
        return entries.firstOrNull { e -> q in e.deKeys } ?: entries.firstOrNull { e -> q in e.allKeys }
    }

    /**
     * Search term for Open Food Facts (German): "хліб", "хлеб", "bread" -> "Brot".
     * German words are left as they are.
     */
    fun toGerman(query: String): String {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return trimmed
        exact(trimmed)?.let { return it.de }
        if (Txt.hasCyrillic(trimmed)) return search(trimmed, 1).firstOrNull()?.de ?: trimmed
        // Latin letters: German or English? Leave it if it looks like a German word.
        val q = Txt.norm(trimmed)
        if (q.length < 3) return trimmed
        if (entries.any { e -> e.deKeys.any { Txt.score(q, it) >= 60 } }) return trimmed
        val best = entries
            .mapNotNull { e ->
                e.enKeys.filter { Txt.score(q, it) >= 60 }.minByOrNull { it.length }?.let { k -> Triple(e, Txt.score(q, k), k.length) }
            }
            .sortedWith(compareByDescending<Triple<DictEntry, Int, Int>> { it.second }.thenBy { it.third })
            .firstOrNull()
        return best?.first?.de ?: trimmed
    }
}
