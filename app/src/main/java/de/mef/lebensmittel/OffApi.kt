package de.mef.lebensmittel

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** The four sister databases. They share the same API, only the domain differs. */
enum class ProductSource(val code: String, val domain: String) {
    FOOD("food", "openfoodfacts.org"),
    BEAUTY("beauty", "openbeautyfacts.org"),
    PRODUCTS("products", "openproductsfacts.org"),
    PET("pet", "openpetfoodfacts.org");

    /** Tag shown under the product name ("гігієна" …); none for food. */
    val label: String?
        get() = when (this) {
            FOOD -> null
            BEAUTY -> T.srcBeauty()
            PRODUCTS -> T.srcProducts()
            PET -> T.srcPet()
        }

    companion object {
        fun fromCode(c: String?): ProductSource? = entries.firstOrNull { it.code == c }
    }
}

/**
 * Client for Open Food Facts and its sister databases (Open Beauty Facts, Open Products Facts,
 * Open Pet Food Facts). Blocking calls: run them on a background thread.
 * Limits: ~10 searches/minute and ~100 product lookups/minute per site.
 */
object OffApi {
    private const val USER_AGENT = "Lebensmittel-Android/1.0 (personal shopping list)"
    private const val FIELDS =
        "code,product_name,product_name_de,generic_name_de,brands,quantity,nutriments,image_front_small_url,image_small_url,stores,stores_tags"

    class RateLimitException : IOException("rate limit")

    private val pool = Executors.newFixedThreadPool(4) { r -> Thread(r, "off-search").apply { isDaemon = true } }

    /**
     * Searches all four databases at the same time (products sold in Germany).
     * Databases whose results match the search word best come first.
     */
    fun search(term: String, store: String? = null, pageSize: Int = 50): List<Product> {
        val q = term.trim()
        if (q.isEmpty()) return emptyList()
        val tasks = ProductSource.entries.map { src -> src to pool.submit(Callable { searchIn(src, q, store, pageSize) }) }
        val results = LinkedHashMap<ProductSource, List<Product>>()
        val errors = ArrayList<Throwable>()
        for ((src, f) in tasks) {
            try {
                results[src] = f.get(25, TimeUnit.SECONDS)
            } catch (e: ExecutionException) {
                errors.add(e.cause ?: e)
            } catch (e: Exception) {
                errors.add(e)
            }
        }
        if (results.isEmpty() || results.values.all { it.isEmpty() }) {
            if (errors.any { it is RateLimitException }) throw RateLimitException()
            if (errors.size == ProductSource.entries.size) throw IOException("no connection", errors.first())
        }
        val nq = Txt.norm(q)
        fun relevance(list: List<Product>): Int = list.count { p ->
            Txt.score(nq, Txt.norm(p.name)) > 0 || (p.brand?.let { Txt.score(nq, Txt.norm(it)) > 0 } == true)
        }
        return results.entries
            .sortedWith(compareByDescending<Map.Entry<ProductSource, List<Product>>> { relevance(it.value) }.thenBy { it.key.ordinal })
            .flatMap { it.value }
            .distinctBy { it.key }
            // the databases also search ingredients, categories …: keep only name matches
            .filter { nameMatches(it.name, q) }
    }

    /** [store]: supermarket slug ("edeka") to ask the server only for products tagged with it. */
    /**
     * True if every searched word occurs in the product name ("Milch" matches "Vollmilch 3,5 %",
     * but not a chocolate that only has milk in its ingredients).
     */
    fun nameMatches(name: String, query: String): Boolean {
        val n = Txt.norm(name)
        val words = Txt.norm(query).split(' ', '-').filter { it.length >= 2 }
        return words.isNotEmpty() && words.all { n.contains(it) }
    }

    private fun searchIn(src: ProductSource, q: String, store: String?, pageSize: Int): List<Product> {
        if (src == ProductSource.FOOD) {
            // search-a-licious (newer, better ranking), only exists for food
            val first = runCatching {
                val query = "$q countries_tags:\"en:germany\"" + (store?.let { " stores_tags:\"$it\"" } ?: "")
                val url = "https://search.openfoodfacts.org/search?q=${enc(query)}&langs=de&page_size=$pageSize&fields=${enc(FIELDS)}"
                OffParser.parseSearch(get(url), src)
            }
            first.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return it }
            if (first.exceptionOrNull() is RateLimitException) throw RateLimitException()
        }
        // classic search on the German site (only shows products sold in Germany)
        val url = "https://de.${src.domain}/cgi/search.pl?search_terms=${enc(q)}" +
            "&search_simple=1&action=process&json=1&page_size=$pageSize&fields=${enc(FIELDS)}" +
            (store?.let { "&tagtype_0=stores&tag_contains_0=contains&tag_0=${enc(it)}" } ?: "")
        return OffParser.parseSearch(get(url), src)
    }

    /** Looks a barcode up in food, then hygiene, household and pet food. Null if not found anywhere. */
    fun byBarcode(code: String): Product? {
        val clean = code.filter { it.isDigit() }
        if (clean.isEmpty()) return null
        var failures = 0
        for (src in ProductSource.entries) {
            try {
                val url = "https://world.${src.domain}/api/v2/product/$clean?fields=${enc(FIELDS)}"
                OffParser.parseProductResponse(get(url), clean, src)?.let { return it }
            } catch (e: NotFound) {
                // try the next database
            } catch (e: IOException) {
                failures++
            }
        }
        if (failures == ProductSource.entries.size) throw IOException("no connection")
        return null
    }

    /** Full product card (all nutrients, ingredients, allergens …) for a barcode, from any of the four databases. */
    fun nutritionByBarcode(code: String): NutritionInfo? {
        val clean = code.filter { it.isDigit() }
        if (clean.isEmpty()) return null
        var failures = 0
        for (src in ProductSource.entries) {
            try {
                val url = "https://world.${src.domain}/api/v2/product/$clean?fields=${enc(NutritionParser.FIELDS)}"
                NutritionParser.parseResponse(get(url), clean, src)?.let { return it }
            } catch (e: NotFound) {
                // try the next database
            } catch (e: IOException) {
                failures++
            }
        }
        if (failures == ProductSource.entries.size) throw IOException("no connection")
        return null
    }

    private class NotFound : IOException("404")

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 10_000
            c.readTimeout = 15_000
            c.setRequestProperty("User-Agent", USER_AGENT)
            c.setRequestProperty("Accept", "application/json")
            val code = c.responseCode
            if (code == 429) throw RateLimitException()
            if (code == 404) throw NotFound()
            if (code !in 200..299) throw IOException("HTTP $code")
            return c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            c.disconnect()
        }
    }
}

/** Pure parsing, kept separate so it can be tested without network. */
object OffParser {

    fun parseSearch(body: String, source: ProductSource = ProductSource.FOOD): List<Product> {
        val root = JSONObject(body)
        val arr: JSONArray = root.optJSONArray("hits") ?: root.optJSONArray("products") ?: return emptyList()
        val seen = HashSet<String>()
        val out = ArrayList<Product>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val p = parseProduct(o, source = source) ?: continue
            if (seen.add(p.key)) out.add(p)
        }
        return out
    }

    fun parseProductResponse(body: String, code: String, source: ProductSource = ProductSource.FOOD): Product? {
        val root = JSONObject(body)
        if (root.optInt("status", 0) != 1) return null
        val o = root.optJSONObject("product") ?: return null
        return parseProduct(o, fallbackCode = code, source = source)
    }

    fun parseProduct(o: JSONObject, fallbackCode: String? = null, source: ProductSource = ProductSource.FOOD): Product? {
        val name = text(o, "product_name_de") ?: text(o, "product_name") ?: text(o, "generic_name_de") ?: return null
        val brand = text(o, "brands")?.split(',')?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        val n = o.optJSONObject("nutriments")
        // energy_100g is in kJ, convert if kcal is missing
        val kcal = n?.let { num(it, "energy-kcal_100g") ?: num(it, "energy_100g")?.div(4.184) }
        return Product(
            name = name,
            brand = brand,
            barcode = text(o, "code") ?: fallbackCode,
            packageQty = text(o, "quantity"),
            kcal100 = kcal?.takeIf { it >= 0 && it < 1000 },
            imageUrl = text(o, "image_front_small_url") ?: text(o, "image_small_url"),
            source = source.takeIf { it != ProductSource.FOOD }?.code,
            stores = Stores.forProduct(text(o, "stores"), tagList(o, "stores_tags"), text(o, "brands")),
        )
    }

    /** Field may be a string, a list of strings, or a {lang: value} object depending on the endpoint. */
    private fun text(o: JSONObject, key: String): String? {
        if (!o.has(key) || o.isNull(key)) return null
        return when (val v = o.opt(key)) {
            is String -> v.trim().takeIf { it.isNotEmpty() }
            is JSONArray -> (0 until v.length()).mapNotNull { v.optString(it).trim().takeIf { s -> s.isNotEmpty() && s != "null" } }
                .joinToString(", ").takeIf { it.isNotEmpty() }
            is JSONObject -> listOf("de", "main", "en").firstNotNullOfOrNull { v.optStr(it) }
                ?: v.keys().asSequence().firstNotNullOfOrNull { v.optStr(it) }
            is Number -> v.toString()
            else -> null
        }
    }

    private fun tagList(o: JSONObject, key: String): List<String> {
        val arr = o.optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optString(it).trim().takeIf { s -> s.isNotEmpty() && s != "null" } }
    }

    private fun num(o: JSONObject, key: String): Double? {
        if (!o.has(key) || o.isNull(key)) return null
        return when (val v = o.opt(key)) {
            is Number -> v.toDouble()
            is String -> v.replace(',', '.').toDoubleOrNull()
            else -> null
        }?.takeIf { !it.isNaN() }
    }
}
