package de.mef.lebensmittel

/**
 * Supermarkets. The database field "stores" is free text typed by users ("EDEKA, Rewe"),
 * so names are turned into slugs ("edeka", "aldi-sud") – the same form Open Food Facts uses for stores_tags.
 * Chains' own brands (Milbona = Lidl, ja! = Rewe …) also tell where a product is sold.
 */
object Stores {

    /** slug -> nice name. Also lists the chains that are recognised when typed into the search. */
    private val names = linkedMapOf(
        "aldi" to "Aldi",
        "aldi-sud" to "Aldi Süd",
        "aldi-nord" to "Aldi Nord",
        "lidl" to "Lidl",
        "rewe" to "Rewe",
        "edeka" to "Edeka",
        "kaufland" to "Kaufland",
        "netto" to "Netto",
        "penny" to "Penny",
        "norma" to "Norma",
        "globus" to "Globus",
        "real" to "real",
        "marktkauf" to "Marktkauf",
        "tegut" to "tegut",
        "famila" to "famila",
        "hit" to "HIT",
        "nahkauf" to "Nahkauf",
        "denns" to "denn's",
        "alnatura" to "Alnatura",
        "dm" to "dm",
        "rossmann" to "Rossmann",
        "muller" to "Müller",
        "metro" to "Metro",
        "action" to "Action",
    )

    /** Different spellings of the same chain. */
    private val aliases = mapOf(
        "netto-marken-discount" to "netto",
        "netto-marken-discounter" to "netto",
        "rewe-city" to "rewe",
        "rewe-center" to "rewe",
        "edeka-center" to "edeka",
        "e-center" to "edeka",
        "aldi-sued" to "aldi-sud",
        "dm-drogerie-markt" to "dm",
        "drogerie-markt" to "dm",
        "kaufland-de" to "kaufland",
        "lidl-de" to "lidl",
        // Ukrainian spellings typed into the search
        "альди" to "aldi", "альді" to "aldi", "алди" to "aldi",
        "лидл" to "lidl", "лідл" to "lidl",
        "реве" to "rewe", "реве-" to "rewe",
        "едека" to "edeka", "эдека" to "edeka",
        "кауфланд" to "kaufland",
        "нетто" to "netto",
        "пенні" to "penny", "пенни" to "penny",
        "норма" to "norma",
        "глобус" to "globus",
        "дм" to "dm",
        "россманн" to "rossmann", "россман" to "rossmann", "росман" to "rossmann",
    )

    /** Chains' own brands -> chain. Matched against the product's brand. */
    private val privateLabels = mapOf(
        // Lidl
        "milbona" to "lidl", "pilos" to "lidl", "cien" to "lidl", "w5" to "lidl", "solevita" to "lidl",
        "freeway" to "lidl", "deluxe" to "lidl", "chef select" to "lidl", "combino" to "lidl",
        "crownfield" to "lidl", "alesto" to "lidl", "kania" to "lidl", "vitasia" to "lidl",
        "snack day" to "lidl", "favorina" to "lidl", "bellarom" to "lidl", "fin carre" to "lidl",
        "dulano" to "lidl", "baresa" to "lidl", "eridanous" to "lidl", "italiamo" to "lidl",
        "sondey" to "lidl", "nixe" to "lidl", "grafschafter" to "lidl", "lord nelson" to "lidl",
        // Aldi
        "milsani" to "aldi", "moser roth" to "aldi", "lacura" to "aldi", "gut bio" to "aldi",
        "golden bridge" to "aldi", "rio d'oro" to "aldi", "meine metzgerei" to "aldi", "tandil" to "aldi",
        "choceur" to "aldi", "cucina nobile" to "aldi", "river" to "aldi", "almare" to "aldi",
        "lyttos" to "aldi", "goldähren" to "aldi", "golden seafood" to "aldi", "mamia" to "aldi",
        // Rewe
        "ja!" to "rewe", "ja" to "rewe", "rewe beste wahl" to "rewe", "rewe bio" to "rewe",
        "rewe feine welt" to "rewe", "rewe regional" to "rewe",
        // Edeka
        "gut & günstig" to "edeka", "gut & gunstig" to "edeka", "gut&günstig" to "edeka",
        "edeka bio" to "edeka", "edeka herzstücke" to "edeka", "edeka" to "edeka",
        // Kaufland
        "k-classic" to "kaufland", "k-bio" to "kaufland", "k-take it veggie" to "kaufland",
        "k-favourites" to "kaufland", "k-purland" to "kaufland",
        // Netto / Penny
        "netto" to "netto", "penny" to "penny",
        // dm
        "balea" to "dm", "dmbio" to "dm", "dm bio" to "dm", "alverde" to "dm", "mivolis" to "dm",
        "denkmit" to "dm", "babylove" to "dm", "ebelin" to "dm", "sundance" to "dm", "s-quitofree" to "dm",
        // Rossmann
        "isana" to "rossmann", "alouette" to "rossmann", "domol" to "rossmann", "enerbio" to "rossmann",
        "babydream" to "rossmann", "altapharma" to "rossmann", "rival de loop" to "rossmann",
    )

    fun slug(name: String): String {
        val base = Txt.norm(name)
            .replace(Regex("[^\\p{L}\\p{N}]+"), "-")
            .trim('-')
        return aliases[base] ?: base
    }

    fun displayName(slug: String): String =
        names[slug] ?: slug.split('-').joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } }

    /** Stores of a product: from the database field and from the chain's own brand. */
    fun forProduct(storesText: String?, storeTags: List<String>, brand: String?): List<String> {
        val out = LinkedHashSet<String>()
        storeTags.forEach { t -> slug(t.substringAfter(':')).takeIf { it.isNotEmpty() }?.let { out.add(it) } }
        storesText?.split(',', ';')?.forEach { s -> slug(s).takeIf { it.isNotEmpty() }?.let { out.add(it) } }
        brand?.split(',')?.forEach { b -> privateLabels[Txt.norm(b)]?.let { out.add(it) } }
        // "aldi-sud" also counts as "aldi"
        if (out.any { it.startsWith("aldi-") }) out.add("aldi")
        return out.toList()
    }

    /** True if the product is sold in [slug] (Aldi Süd / Aldi Nord match "aldi"). */
    fun matches(product: Product, slug: String): Boolean =
        slug in product.stores || (slug == "aldi" && product.stores.any { it.startsWith("aldi") })

    /**
     * Splits a typed query into product words and a supermarket:
     * "Milch Edeka" -> ("Milch", "edeka"), "молоко лідл" -> ("молоко", "lidl"), "Milch" -> ("Milch", null).
     */
    fun splitQuery(query: String): Pair<String, String?> {
        val words = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.size < 2) return query.trim() to null
        // two-word chains first ("Aldi Süd"), then single words
        for (i in 0 until words.size - 1) {
            val s = slug(words[i] + " " + words[i + 1])
            if (s in names) return (words.take(i) + words.drop(i + 2)).joinToString(" ") to s
        }
        for ((i, w) in words.withIndex()) {
            val s = slug(w)
            if (s in names) return (words.take(i) + words.drop(i + 1)).joinToString(" ") to s
        }
        return query.trim() to null
    }

    /** Stores found in a list of products, most frequent first: slug -> count. */
    fun countIn(products: List<Product>): List<Pair<String, Int>> {
        val counts = LinkedHashMap<String, Int>()
        for (p in products) for (s in p.stores.toSet()) counts[s] = (counts[s] ?: 0) + 1
        return counts.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { displayName(it.key) })
            .map { it.key to it.value }
    }
}
