package it.matteocolombo.tacca.domain

import java.text.Normalizer
import java.util.Locale

/** Case- and diacritic-insensitive "contains" matching for search fields. */
object TextMatch {
    private val combiningMarks = Regex("\\p{Mn}+")

    fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(combiningMarks, "").lowercase(Locale.ROOT).trim()

    fun matches(query: String, vararg fields: String): Boolean {
        val q = normalize(query)
        if (q.isEmpty()) return true
        return fields.any { normalize(it).contains(q) }
    }
}
