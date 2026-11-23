package de.kinderalltag.domain

import de.kinderalltag.text.*

internal const val MAX_LIST_ORDER_ITEMS = 1000

data class FamilyListOrder(
    val kind: FamilyListKind,
    val itemIds: List<String> = emptyList(),
    val version: Long = 0,
) {
    fun validate() {
        requireLocalized(itemIds.size <= MAX_LIST_ORDER_ITEMS) {
            message(TextKey.UP_TO_1000_ENTRIES_CAN_BE_MANUALLY_SORTED)
        }
        requireLocalized(
            itemIds.distinct().size == itemIds.size &&
                itemIds.all { it.matches(Regex("[A-Za-z0-9_-]{1,100}")) }
        ) {
            message(TextKey.INVALID_ORDER_PLEASE_REOPEN_THE_LIST)
        }
        requireLocalized(version >= 0) { message(TextKey.INVALID_ORDER_VERSION) }
    }
}

fun orderedListIds(
    items: List<FamilyListItem>,
    kind: FamilyListKind,
    saved: List<String>,
): List<String> {
    // Include bought/deleted items so reopening/restoring preserves their position.
    val all = items.filter { it.kind == kind }
    val known = all.map { it.id }.toSet()
    val existing = saved.distinct().filter { it in known }
    val remaining =
        listItems(all.map { it.copy(deleted = false) }, emptyList(), kind, false)
            .map { it.id }
            .filter { it !in existing }
    return existing + remaining
}

fun moveListItem(ids: List<String>, moving: String, target: String, after: Boolean): List<String> {
    if (moving == target || moving !in ids || target !in ids) return ids
    val result = ids.toMutableList()
    result.remove(moving)
    result.add(result.indexOf(target) + if (after) 1 else 0, moving)
    return result
}
