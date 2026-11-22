package de.kinderalltag.domain

import java.time.Instant
import java.time.LocalDate

/** Carry open one-offs and only the latest due recurring execution into the displayed day. */
fun overdueTaskOccurrences(
    revisions: List<TaskRevision>,
    events: List<CareEvent>,
    operations: List<ItemOperation>,
    date: LocalDate,
    now: Instant,
    days: Int,
    childId: String? = null,
    uid: String? = null,
    onlyMine: Boolean = false,
): List<Occurrence> {
    if (days <= 0) return emptyList()
    val past =
        (days.toLong() downTo 1L).flatMap { offset ->
            ManagedTasks.occurrences(revisions, events, operations, date.minusDays(offset))
        }
    // Today advances with the clock; browsing another day evaluates its full schedule.
    val cutoff =
        if (date == now.atZone(FAMILY_ZONE).toLocalDate()) now
        else date.plusDays(1).atStartOfDay(FAMILY_ZONE).toInstant().minusNanos(1)
    val latestDue =
        (past + ManagedTasks.occurrences(revisions, events, operations, date))
            .filter { it.instant <= cutoff }
            .groupBy { it.revision.taskId }
            .mapValues { (_, occurrences) ->
                occurrences.maxWith(compareBy<Occurrence> { it.instant }.thenBy { it.id }).id
            }
    val eventsByOccurrence = events.groupBy { it.occurrenceId }
    return past
        .filter {
            (it.revision.frequency == Frequency.ONCE || latestDue[it.revision.taskId] == it.id) &&
                Schedule.activeCompletions(eventsByOccurrence[it.id].orEmpty(), it.id).isEmpty() &&
                (childId == null || it.revision.childId == childId) &&
                (!onlyMine || isResponsible(it.revision.responsibleParentId, uid))
        }
        .sortedWith(
            compareBy<Occurrence> { it.instant }.thenBy { it.revision.title }.thenBy { it.id }
        )
}
