package com.cdiacal.tasks.ui

import java.text.Normalizer

private val DIACRITICS = Regex("\\p{Mn}+")
private val WHITESPACE = Regex("\\s+")

/** Lower-case and strip accents so "Cafe" finds "café". */
fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(DIACRITICS, "").lowercase()

/**
 * True when every word of [query] appears somewhere in [fields] (any order, ignoring case and
 * accents). A blank query matches everything.
 */
fun matchesQuery(query: String, vararg fields: String): Boolean {
    val words = normalizeForSearch(query).trim().split(WHITESPACE).filter { it.isNotEmpty() }
    if (words.isEmpty()) return true
    val haystack = fields.joinToString(" ") { normalizeForSearch(it) }
    return words.all { it in haystack }
}
