package de.mef.lebensmittel

object Txt {
    /** Lower-case, unify German umlauts and Ukrainian letter variants, drop apostrophes. */
    fun norm(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s.trim().lowercase()) {
            when (ch) {
                'ä' -> sb.append('a')
                'ö' -> sb.append('o')
                'ü' -> sb.append('u')
                'ß' -> sb.append("ss")
                'é', 'è', 'ê' -> sb.append('e')
                'ґ' -> sb.append('г')
                'ё' -> sb.append('е')
                '\'', '’', 'ʼ', '`' -> {}
                else -> sb.append(ch)
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ")
    }

    fun hasCyrillic(s: String): Boolean = s.any { it in 'Ѐ'..'ӿ' }

    fun capitalize(s: String): String = s.trim().replaceFirstChar { it.uppercaseChar() }

    /**
     * Match score of [query] (already normalized) against [candidate] (already normalized).
     * 0 = no match. Higher is better.
     */
    fun score(query: String, candidate: String): Int {
        if (query.isEmpty() || candidate.isEmpty()) return 0
        if (candidate == query) return 100
        if (candidate.startsWith(query)) return 80
        val words = candidate.split(' ', '-', ',', '/', '(', ')').filter { it.isNotEmpty() }
        if (words.any { it.startsWith(query) }) return 60
        // simple stemming: "яблуко" should find "яблука", "tomate" -> "tomaten"
        if (query.length >= 5) {
            val stem = query.dropLast(1)
            if (words.any { it.startsWith(stem) }) return 40
        }
        if (query.length >= 3 && candidate.contains(query)) return 30
        return 0
    }
}
