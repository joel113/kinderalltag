package de.kinderalltag.domain

internal const val MAX_LIST_ORDER_ITEMS = 1000

data class FamilyListOrder(
    val kind: FamilyListKind,
    val itemIds: List<String> = emptyList(),
    val version: Long = 0,
) {
    fun validate() {
        require(itemIds.size <= MAX_LIST_ORDER_ITEMS) {
            "Höchstens 1000 Einträge können manuell sortiert werden."
        }
        require(
            itemIds.distinct().size == itemIds.size &&
                itemIds.all { it.matches(Regex("[A-Za-z0-9_-]{1,100}")) }
        ) {
            "Ungültige Reihenfolge. Bitte die Liste neu öffnen."
        }
        require(version >= 0) { "Ungültige Version der Reihenfolge." }
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
