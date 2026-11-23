package de.kinderalltag.domain

import de.kinderalltag.text.*
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

data class StatisticsCounts(
    val byKind: Map<HistoryKind, Int>,
    val completedTasks: Int,
    val boughtShopping: Int = 0,
    val boughtGifts: Int = 0,
) {
    val events: Int
        get() = byKind.values.sum()
}

data class StatisticsRow(val id: String, val label: String, val counts: StatisticsCounts)

data class EventStatistics(
    val total: StatisticsCounts,
    val parents: List<StatisticsRow>,
    val subjects: List<StatisticsRow>,
    val weekdays: List<StatisticsRow>,
    val reconstructedEvents: Int,
    val pendingEvents: Int,
)

fun statisticsPeriod(today: LocalDate, days: Int, page: Long): HistoryPeriod {
    require(days in listOf(7, 30, 90) && page >= 0)
    val to = today.minusDays(page * days)
    return HistoryPeriod(to.minusDays(days - 1L), to)
}

// Entries have already been limited to the period; corrections must come from the full event
// history.
fun eventStatistics(
    entries: List<HistoryEntry>,
    allEvents: List<CareEvent>,
    parents: List<Parent>,
    children: List<Child>,
    listActivity: List<ListActivity> = emptyList(),
): EventStatistics {
    val uniqueEntries = entries.distinctBy { it.id }
    val canonicalCompletions =
        allEvents
            .groupBy { it.occurrenceId }
            .values
            .mapNotNull { events ->
                Schedule.activeCompletions(events, events.first().occurrenceId)
                    .minWithOrNull(
                        compareBy<CareEvent> {
                                if (it.clientMillis > 0) it.clientMillis else it.serverMillis ?: 0
                            }
                            .thenBy { it.id }
                    )
            }
            .map { "task-event:${it.id}" }
            .toSet()
    val canonicalPurchases =
        listActivity
            .map { it.itemId }
            .distinct()
            .mapNotNull {
                activePurchases(listActivity, it).firstOrNull()?.let { event ->
                    "list-event:${event.id}"
                }
            }
            .toSet()
    fun counts(group: List<HistoryEntry>) =
        StatisticsCounts(
            group.groupingBy { it.kind }.eachCount(),
            group.count { it.kind == HistoryKind.TASK_COMPLETED && it.id in canonicalCompletions },
            group.count { it.kind == HistoryKind.SHOPPING_BOUGHT && it.id in canonicalPurchases },
            group.count { it.kind == HistoryKind.GIFT_BOUGHT && it.id in canonicalPurchases },
        )
    fun parentId(entry: HistoryEntry): String =
        (if (entry.kind == HistoryKind.TASK_COMPLETED)
                entry.creditedId?.takeIf { it.isNotBlank() } ?: entry.authorId
            else entry.authorId)
            .ifBlank { "__unknown__" }
    val parentNames = linkedMapOf<String, String>()
    parents.forEach { parentNames[it.id] = it.name }
    uniqueEntries.forEach { entry ->
        val id = parentId(entry)
        parentNames.putIfAbsent(
            id,
            if (id == "__unknown__") text(TextKey.UNKNOWN)
            else if (entry.kind == HistoryKind.TASK_COMPLETED && !entry.creditedId.isNullOrBlank())
                entry.creditedName?.takeIf { it.isNotBlank() } ?: text(TextKey.PARENT)
            else entry.authorName,
        )
    }
    val subjectNames =
        linkedMapOf(
            "ferdinand" to (children.firstOrNull { it.id == "ferdinand" }?.name ?: "Ferdinand"),
            "johann" to (children.firstOrNull { it.id == "johann" }?.name ?: "Johann"),
            "both" to text(TextKey.BOTH_CHILDREN),
            "haushalt" to text(TextKey.HOUSEHOLD),
            "shopping" to text(TextKey.SHOPPING),
        )
    fun subjectId(entry: HistoryEntry): String =
        when {
            entry.listKind == FamilyListKind.SHOPPING -> "shopping"
            entry.childId in listOf("ferdinand", "johann", "haushalt") ->
                requireNotNull(entry.childId)
            entry.recipient == AppointmentRecipient.FERDINAND -> "ferdinand"
            entry.recipient == AppointmentRecipient.JOHANN -> "johann"
            entry.recipient == AppointmentRecipient.BOTH_CHILDREN -> "both"
            entry.recipient == AppointmentRecipient.HOUSEHOLD -> "haushalt"
            else -> "unknown"
        }
    val bySubject = uniqueEntries.groupBy(::subjectId)
    if ("unknown" in bySubject) subjectNames["unknown"] = text(TextKey.UNKNOWN)
    val byParent = uniqueEntries.groupBy(::parentId)
    val byWeekday =
        uniqueEntries.groupBy { Instant.ofEpochMilli(it.millis).atZone(FAMILY_ZONE).dayOfWeek }
    return EventStatistics(
        counts(uniqueEntries),
        parentNames.map { (id, name) -> StatisticsRow(id, name, counts(byParent[id].orEmpty())) },
        subjectNames.map { (id, name) -> StatisticsRow(id, name, counts(bySubject[id].orEmpty())) },
        DayOfWeek.entries.map { day ->
            StatisticsRow(day.name, day.name, counts(byWeekday[day].orEmpty()))
        },
        uniqueEntries.count { it.legacy },
        uniqueEntries.count { it.pending },
    )
}
