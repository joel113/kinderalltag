package de.kinderalltag.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class TaskOverviewTest {
    private val day = LocalDate.of(2026, 11, 18)
    private val now = day.atTime(12, 0).atZone(FAMILY_ZONE).toInstant()
    private fun task(id: String, date: LocalDate, hour: Int, frequency: Frequency = Frequency.ONCE) =
        TaskRevision(id, id, "johann", id, frequency = frequency, times = listOf(LocalTime.of(hour, 0)), start = date, effective = day.minusDays(30))
    @Test fun tasksSortByNextDateThenTimeRatherThanTitleOrOldRoutineStart() {
        val routine = task("A routine", day.minusDays(20), 18, Frequency.DAILY)
        val sooner = task("Z one-off", day, 14)
        val later = task("B tomorrow", day.plusDays(1), 8)
        val tasks = listOf(later, routine, sooner)
        val timings = taskOverviewTimings(tasks.map { it.taskId }.toSet(), tasks, emptyList(), emptyList(), now)
        assertEquals(listOf(sooner, routine, later), chronologicalTasks(tasks, timings))
        assertEquals(day, timings[routine.taskId]?.date)
    }
    @Test fun movedOnceTaskUsesActualReplacementDateAndTime() {
        val source = task("one", day, 18)
        val original = Occurrence(source, day, LocalTime.of(18, 0))
        val moved = source.copy(id = "moved", start = day.plusDays(3), times = listOf(LocalTime.of(10, 0)))
        val operation = ItemOperation("move", ItemType.TASK, source.taskId, OperationAction.EDIT, OperationScope.SINGLE,
            original.id, day, original.time, source.id, "Moved", actualStartMillis = original.instant.toEpochMilli(), task = moved, createdMillis = now.toEpochMilli())
        val timings = taskOverviewTimings(setOf(source.taskId), listOf(source), emptyList(), listOf(operation), now)
        assertEquals(TaskOverviewTiming(day.plusDays(3), listOf(LocalTime.of(10, 0))), timings[source.taskId])
    }
    @Test fun calendarPeriodSortsHistoricalActualExecutionsAndKeepsMultipleTimes() {
        val first = task("Z first", day.minusDays(2), 8).copy(times = listOf(LocalTime.of(18, 0), LocalTime.of(8, 0)))
        val second = task("A second", day.minusDays(1), 7)
        val timings = taskOverviewTimings(setOf(first.taskId, second.taskId), listOf(first, second), emptyList(), emptyList(), now,
            HistoryPeriod(day.minusDays(3), day))
        assertEquals(listOf(first, second), chronologicalTasks(listOf(second, first), timings))
        assertEquals(TaskOverviewTiming(first.start, listOf(LocalTime.of(8, 0), LocalTime.of(18, 0))), timings[first.taskId])
    }
    @Test fun deletedEarliestSlotAndElapsedTimesDoNotDetermineUpcomingOrder() {
        val source = task("routine", day.minusDays(10), 8, Frequency.DAILY).copy(times = listOf(LocalTime.of(8, 0), LocalTime.of(18, 0)))
        val deletion = ItemOperation("delete", ItemType.TASK, source.taskId, OperationAction.DELETE, OperationScope.SINGLE,
            Occurrence(source, day, LocalTime.of(18, 0)).id, day, LocalTime.of(18, 0), source.id, source.title)
        val timings = taskOverviewTimings(setOf(source.taskId), listOf(source), emptyList(), listOf(deletion), now)
        assertEquals(TaskOverviewTiming(day.plusDays(1), source.times), timings[source.taskId])
    }
    @Test fun missingOrPausedSchedulesSortLastWithStableTieBreaks() {
        val active = task("active", day, 18)
        val paused = task("paused", day.minusDays(1), 8).copy(active = false)
        val same = active.copy(id = "same", taskId = "same", title = active.title)
        val tasks = listOf(paused, same, active)
        val timings = taskOverviewTimings(tasks.map { it.taskId }.toSet(), tasks, emptyList(), emptyList(), now)
        assertEquals(listOf(active, same, paused), chronologicalTasks(tasks, timings))
    }
}
