package de.kinderalltag.domain

import java.time.Instant
import java.time.temporal.ChronoUnit

enum class TaskCompletionStatus { COMPLETED, MISSING, UPCOMING }
data class TaskCompletionCell(val occurrence: Occurrence, val status: TaskCompletionStatus)

fun taskCompletionOverview(taskId: String, revisions: List<TaskRevision>, events: List<CareEvent>,
    operations: List<ItemOperation>, period: HistoryPeriod, now: Instant): List<TaskCompletionCell> =
    (0L..ChronoUnit.DAYS.between(period.from, period.to)).flatMap { offset ->
        ManagedTasks.occurrences(revisions, events, operations, period.from.plusDays(offset))
            .filter { it.revision.taskId == taskId }.map { occurrence ->
                TaskCompletionCell(occurrence, when {
                    Schedule.activeCompletions(events, occurrence.id).isNotEmpty() -> TaskCompletionStatus.COMPLETED
                    occurrence.instant <= now -> TaskCompletionStatus.MISSING
                    else -> TaskCompletionStatus.UPCOMING
                })
            }
    }

data class TaskCompletionDay(val date: java.time.LocalDate, val cells: List<TaskCompletionCell>)
data class TaskCompletionMonth(val days: List<TaskCompletionDay>) {
    val weeks: List<List<TaskCompletionDay?>> get() {
        if (days.isEmpty()) return emptyList()
        val leading = List<TaskCompletionDay?>(days.first().date.dayOfWeek.value - 1) { null }
        val cells = leading + days
        val trailing = List<TaskCompletionDay?>((7 - cells.size % 7) % 7) { null }
        return (cells + trailing).chunked(7)
    }
    val confirmed: Int get() = days.sumOf { day -> day.cells.count { it.status == TaskCompletionStatus.COMPLETED } }
    val missing: Int get() = days.sumOf { day -> day.cells.count { it.status == TaskCompletionStatus.MISSING } }
    val upcoming: Int get() = days.sumOf { day -> day.cells.count { it.status == TaskCompletionStatus.UPCOMING } }
    val eligible: Int get() = confirmed + missing
    val percentage: Int? get() = if (eligible == 0) null else kotlin.math.round(confirmed * 100.0 / eligible).toInt()
}

fun taskCompletionMonth(taskId: String, revisions: List<TaskRevision>, events: List<CareEvent>,
    operations: List<ItemOperation>, month: java.time.YearMonth, now: Instant): TaskCompletionMonth {
    val cells = taskCompletionOverview(taskId, revisions, events, operations, HistoryPeriod(month.atDay(1), month.atEndOfMonth()), now)
        .groupBy { it.occurrence.date }
    return TaskCompletionMonth((1..month.lengthOfMonth()).map { day ->
        val date = month.atDay(day)
        TaskCompletionDay(date, cells[date].orEmpty())
    })
}
