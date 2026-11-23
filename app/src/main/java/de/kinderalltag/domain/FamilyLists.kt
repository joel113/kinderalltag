package de.kinderalltag.domain

import java.text.Collator
import java.util.Locale

enum class FamilyListKind {
    SHOPPING,
    GIFTS,
}

enum class LabelGroup {
    SHOPPING,
    OCCASION,
    GIVER,
}

enum class ListAction {
    ADDED,
    CHANGED,
    BOUGHT,
    REOPENED,
    DELETED,
    RESTORED,
}

data class FamilyLabel(
    val id: String,
    val group: LabelGroup,
    val name: String,
    val color: BearColor = BearColor.BLUE,
    val active: Boolean = true,
    val version: Long = 0,
) {
    fun validate() {
        require(name.isNotBlank() && name.length <= 60) { "Label: 1 bis 60 Zeichen." }
    }
}

data class FamilyListItem(
    val id: String,
    val kind: FamilyListKind,
    val title: String,
    val notes: String = "",
    val recipient: AppointmentRecipient? = null,
    val labelIds: Set<String> = emptySet(),
    val version: Long = 0,
    val deleted: Boolean = false,
    val lastActivityId: String = "",
) {
    fun validate() {
        require(title.isNotBlank() && title.length <= 120) { "Titel: 1 bis 120 Zeichen." }
        require(notes.length <= 2000) { "Notizen: höchstens 2000 Zeichen." }
        require(labelIds.size <= 6) { "Bitte höchstens 6 Labels auswählen." }
        require(
            if (kind == FamilyListKind.SHOPPING) recipient == null
            else
                recipient in
                    listOf(
                        AppointmentRecipient.FERDINAND,
                        AppointmentRecipient.JOHANN,
                        AppointmentRecipient.BOTH_CHILDREN,
                    )
        ) {
            "Bitte ein Kind oder beide Kinder auswählen."
        }
    }
}

data class ListActivity(
    val id: String,
    val itemId: String,
    val action: ListAction,
    val snapshot: FamilyListItem,
    val labels: List<FamilyLabel>,
    val authorId: String,
    val authorName: String,
    val clientMillis: Long,
    val serverMillis: Long? = null,
    val targetId: String? = null,
    val pending: Boolean = false,
) {
    val millis: Long
        get() = if (clientMillis > 0) clientMillis else serverMillis ?: 0
}

fun activePurchases(events: List<ListActivity>, itemId: String): List<ListActivity> {
    val relevant = events.filter { it.itemId == itemId }
    val retracted =
        relevant.filter { it.action == ListAction.REOPENED }.mapNotNull { it.targetId }.toSet()
    return relevant
        .filter { it.action == ListAction.BOUGHT && it.id !in retracted }
        .sortedWith(compareBy<ListActivity> { it.millis }.thenBy { it.id })
}

fun listItems(
    items: List<FamilyListItem>,
    events: List<ListActivity>,
    kind: FamilyListKind,
    bought: Boolean,
    order: List<String> = emptyList(),
): List<FamilyListItem> {
    val alphabet = Collator.getInstance(Locale.GERMAN)
    val positions = order.withIndex().associate { it.value to it.index }
    return items
        .filter {
            !it.deleted && it.kind == kind && activePurchases(events, it.id).isNotEmpty() == bought
        }
        .sortedWith(
            if (bought)
                compareByDescending<FamilyListItem> {
                        activePurchases(events, it.id).lastOrNull()?.millis ?: 0
                    }
                    .thenBy { it.id }
            else
                compareBy<FamilyListItem> { positions[it.id] ?: Int.MAX_VALUE }
                    .then(Comparator { a, b -> alphabet.compare(a.title, b.title) })
                    .thenBy { it.id }
        )
}

fun matchesListLabels(
    item: FamilyListItem,
    selected: Set<String>,
    labels: List<FamilyLabel>,
): Boolean =
    LabelGroup.entries.all { group ->
        val groupIds = labels.filter { it.group == group && it.id in selected }.map { it.id }
        groupIds.isEmpty() || groupIds.any { it in item.labelIds }
    }

fun starterLabels(group: LabelGroup): List<FamilyLabel> =
    when (group) {
        LabelGroup.SHOPPING -> listOf("supermarkt" to "Supermarkt", "drogerie" to "Drogerie")
        LabelGroup.OCCASION ->
            listOf(
                "weihnachten" to "Weihnachten",
                "ostern" to "Ostern",
                "geburtstag" to "Geburtstag",
            )
        LabelGroup.GIVER -> listOf("oma" to "Oma", "opa" to "Opa")
    }.mapIndexed { index, (key, name) ->
        FamilyLabel(
            "preset_${group.name}_$key",
            group,
            name,
            BearColor.entries[index % BearColor.entries.size],
        )
    }

fun appendDictation(current: String, spoken: String, notes: Boolean = false): String =
    when {
        spoken.isBlank() -> current
        current.isBlank() -> spoken.trim()
        else -> current.trimEnd() + (if (notes) "\n" else " ") + spoken.trim()
    }
