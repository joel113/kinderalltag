package de.kinderalltag.domain

import de.kinderalltag.text.*
import java.util.Locale

const val MAX_SHOPPING_IMPORT_ITEMS = 50

/** Only explicit list separators are interpreted; quantities and decimal commas remain text. */
fun splitShoppingSpeech(speech: String, language: String = "de"): List<String> =
    speech
        .split(
            Regex(
                if (language == "en")
                    "(?<!\\d),|,(?!\\d)|[;\\n\\r]+|\\b(?:and|comma|semicolon|new line|next item)\\b"
                else
                    "(?<!\\d),|,(?!\\d)|[;\\n\\r]+|\\b(?:und|komma|semikolon|neue zeile|(?:nächster|neuer) (?:artikel|eintrag))\\b",
                RegexOption.IGNORE_CASE,
            )
        )
        .map { it.trim().trim('.', '!', '?').trim() }
        .filter { it.isNotEmpty() }

/**
 * Review text uses one item per line, allowing the user to merge product names containing 'und'.
 */
fun shoppingImportTitles(review: String, alreadyOpen: List<String>): List<String> {
    fun key(value: String) = value.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
    val seen = alreadyOpen.map(::key).toMutableSet()
    return review
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && seen.add(key(it)) }
        .toList()
}

fun validateShoppingImport(titles: List<String>) {
    requireLocalized(titles.isNotEmpty()) {
        message(TextKey.PLEASE_ENTER_AT_LEAST_ONE_NEW_SHOPPING_ITEM)
    }
    requireLocalized(titles.size <= MAX_SHOPPING_IMPORT_ITEMS) {
        message(
            TextKey.PLEASE_IMPORT_NO_MORE_THAN_SHOPPING_ITEMS_AT_ONCE,
            MAX_SHOPPING_IMPORT_ITEMS,
        )
    }
    requireLocalized(titles.all { it.length <= 120 }) {
        message(TextKey.A_SHOPPING_ITEM_IS_LONGER_THAN_120_CHARACTERS_PLEASE_SHORTEN_IT_OR_SPLIT)
    }
}
