package de.mef.lebensmittel

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Packs a day list into a link: lebensmittel://import?d=<compressed base64url JSON>.
 * No server needed: the whole list lives inside the link.
 */
object ShareCodec {
    const val SCHEME_PREFIX = "lebensmittel://import?d="
    private val linkRegex = Regex("""lebensmittel://import\?d=([A-Za-z0-9_-]+)""")

    data class Shared(val date: LocalDate?, val items: List<ShopItem>)

    fun encode(date: LocalDate, items: List<ShopItem>): String {
        val json = JSONObject().apply {
            put("v", 1)
            put("d", date.toString())
            val arr = JSONArray()
            // Images are left out on purpose: they make links long and can be re-fetched.
            items.forEach { arr.put(Mapping.itemToJson(it, withImage = false, withState = false)) }
            put("i", arr)
        }.toString()
        val input = json.toByteArray(Charsets.UTF_8)
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        deflater.setInput(input)
        deflater.finish()
        val out = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        while (!deflater.finished()) {
            val n = deflater.deflate(buf)
            out.write(buf, 0, n)
        }
        deflater.end()
        return Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray())
    }

    fun link(date: LocalDate, items: List<ShopItem>): String = SCHEME_PREFIX + encode(date, items)

    fun decode(payload: String): Shared? = runCatching {
        val bytes = Base64.getUrlDecoder().decode(payload)
        val inflater = Inflater(true)
        inflater.setInput(bytes)
        val out = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        var guard = 0
        while (!inflater.finished()) {
            val n = inflater.inflate(buf)
            if (n == 0) {
                if (inflater.needsInput() || inflater.needsDictionary()) break
                if (++guard > 1000) break
            }
            out.write(buf, 0, n)
            if (out.size() > 2_000_000) break // safety
        }
        inflater.end()
        val o = JSONObject(String(out.toByteArray(), Charsets.UTF_8))
        val date = o.optStr("d")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val arr = o.getJSONArray("i")
        val items = (0 until arr.length()).map { idx ->
            Mapping.itemFromJson(arr.getJSONObject(idx)).copy(id = newId(), checked = false)
        }
        Shared(date, items)
    }.getOrNull()?.takeIf { it.items.isNotEmpty() }

    /** Finds a list link anywhere inside a text (a forwarded message, the clipboard…). */
    fun findInText(text: String?): Shared? {
        if (text.isNullOrBlank()) return null
        val m = linkRegex.find(text) ?: return null
        return decode(m.groupValues[1])
    }

    /** Human readable list + link, for messengers. */
    fun shareText(date: LocalDate, items: List<ShopItem>): String = buildString {
        append(T.shareHeader(Dates.full(date))).append('\n')
        for (i in items) {
            append(if (i.checked) "☑ " else "☐ ")
            append(i.product.name)
            i.product.brand?.let { append(" (").append(it).append(')') }
            if (!i.pfandReturn) append(" — ").append(i.amountLabel)
            i.price?.let { append(if (i.pfandReturn) ": " else ", ").append(formatMoney(it)) }
            i.pfandTotal?.let { append(" + ").append(T.pfandShort(formatMoney(it))) }
            append('\n')
        }
        val sum = items.mapNotNull { it.cost }
        if (sum.isNotEmpty() && sum.size == items.size) append(T.shareTotal(formatMoney(sum.sum()))).append('\n')
        append('\n')
        append(T.openInApp()).append('\n')
        append(link(date, items))
    }
}
