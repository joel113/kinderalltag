package de.kinderalltag.domain

import java.util.Locale

const val MAX_SHOPPING_IMPORT_ITEMS = 50

/** Only explicit list separators are interpreted; quantities and decimal commas remain text. */
fun splitShoppingSpeech(speech: String): List<String> = speech.split(Regex(
    "(?<!\\d),|,(?!\\d)|[;\\n\\r]+|\\b(?:und|komma|semikolon|neue zeile|(?:nächster|neuer) (?:artikel|eintrag))\\b",
    RegexOption.IGNORE_CASE,
)).map { it.trim().trim('.', '!', '?').trim() }.filter { it.isNotEmpty() }

/** Review text uses one item per line, allowing the user to merge product names containing 'und'. */
fun shoppingImportTitles(review: String, alreadyOpen: List<String>): List<String> {
    fun key(value: String) = value.trim().replace(Regex("\\s+"), " ").lowercase(Locale.GERMAN)
    val seen = alreadyOpen.map(::key).toMutableSet()
    return review.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && seen.add(key(it)) }.toList()
}

fun validateShoppingImport(titles: List<String>) {
    require(titles.isNotEmpty()) { "Bitte mindestens einen neuen Einkauf eintragen." }
    require(titles.size <= MAX_SHOPPING_IMPORT_ITEMS) { "Bitte höchstens $MAX_SHOPPING_IMPORT_ITEMS Einkäufe auf einmal übernehmen." }
    require(titles.all { it.length <= 120 }) { "Ein Einkauf ist länger als 120 Zeichen. Bitte kürzen oder auf mehrere Zeilen aufteilen." }
}
