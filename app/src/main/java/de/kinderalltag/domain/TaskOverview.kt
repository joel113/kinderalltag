package de.kinderalltag.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Locale

data class TaskOverviewTiming(val date: LocalDate, val times: List<LocalTime>)

/** Use actual managed dates/times, including moved executions and deletion masks. */
fun taskOverviewTimings(taskIds: Set<String>, revisions: List<TaskRevision>, events: List<CareEvent>,
    operations: List<ItemOperation>, now: Instant, period: HistoryPeriod? = null): Map<String, TaskOverviewTiming> {
    val occurrences = if (period == null) taskIds.flatMap { ManagedTasks.futureOccurrences(revisions, events, operations, it, now) }
        else (0L..ChronoUnit.DAYS.between(period.from, period.to)).flatMap {
            ManagedTasks.occurrences(revisions, events, operations, period.from.plusDays(it))
        }.filter { it.revision.taskId in taskIds }
    return occurrences.groupBy { it.revision.taskId }.mapValues { (_, executions) ->
        val firstDay = executions.minOf { it.date }
        TaskOverviewTiming(firstDay, executions.filter { it.date == firstDay }.map { it.time }.distinct().sorted())
    }
}

fun chronologicalTasks(tasks: List<TaskRevision>, timings: Map<String, TaskOverviewTiming>): List<TaskRevision> =
    tasks.sortedWith(compareBy<TaskRevision> { timings[it.taskId]?.date ?: LocalDate.MAX }
        .thenBy { timings[it.taskId]?.times?.firstOrNull() ?: LocalTime.MAX }
        .thenBy { it.title.lowercase(Locale.ROOT) }.thenBy { it.taskId })
