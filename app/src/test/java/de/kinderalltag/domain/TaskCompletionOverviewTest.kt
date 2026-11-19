package de.kinderalltag.domain

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class TaskCompletionOverviewTest {
    private val day = LocalDate.of(2026, 11, 18)
    private val now = day.atTime(12, 0).atZone(FAMILY_ZONE).toInstant()
    private val task =
        TaskRevision(
            "r",
            "task",
            "johann",
            "Routine",
            times = listOf(LocalTime.of(8, 0), LocalTime.of(18, 0)),
            start = day.minusDays(5),
            effective = day.minusDays(5),
        )
    private val morning = Occurrence(task, day, LocalTime.of(8, 0))

    private fun completion(occurrence: Occurrence) =
        CareEvent(
            "complete",
            occurrence.id,
            occurrence.date,
            EventKind.COMPLETE,
            "parent",
            "Mama",
            now.toEpochMilli(),
            taskSnapshot = occurrence,
        )

    private fun cells(
        events: List<CareEvent> = emptyList(),
        ops: List<ItemOperation> = emptyList(),
    ) = taskCompletionOverview(task.taskId, listOf(task), events, ops, HistoryPeriod(day, day), now)

    @Test
    fun eachTimeSlotHasItsOwnStatusAndFutureSlotsAreNotRed() {
        assertEquals(
            listOf(TaskCompletionStatus.MISSING, TaskCompletionStatus.UPCOMING),
            cells().map { it.status },
        )
        assertEquals(
            listOf(TaskCompletionStatus.COMPLETED, TaskCompletionStatus.UPCOMING),
            cells(listOf(completion(morning))).map { it.status },
        )
    }

    @Test
    fun correctedConfirmationsTurnRedButAnAlreadyConfirmedFutureSlotStaysGreen() {
        val event = completion(morning)
        val correction = event.copy(id = "undo", kind = EventKind.RETRACT, targetId = event.id)
        assertEquals(TaskCompletionStatus.MISSING, cells(listOf(event, correction)).first().status)
        val evening = Occurrence(task, day, LocalTime.of(18, 0))
        assertEquals(
            TaskCompletionStatus.COMPLETED,
            cells(listOf(completion(evening))).last().status,
        )
    }

    @Test
    fun deletedOccurrencesAreExcludedAndRestorationRestoresTheirConfirmation() {
        val deletion =
            ItemOperation(
                "delete",
                ItemType.TASK,
                task.taskId,
                OperationAction.DELETE,
                OperationScope.SINGLE,
                morning.id,
                day,
                morning.time,
                task.id,
                task.title,
            )
        val events = listOf(completion(morning))
        assertEquals(1, cells(events, listOf(deletion)).size)
        val restore =
            deletion.copy(
                id = "restore",
                action = OperationAction.RESTORE,
                restoresId = deletion.id,
            )
        assertEquals(
            TaskCompletionStatus.COMPLETED,
            cells(events, listOf(deletion, restore)).first().status,
        )
    }

    @Test
    fun weekdayAndPeriodBoundariesDoNotInventUnscheduledExecutions() {
        val weekday =
            task.copy(
                frequency = Frequency.WEEKDAYS,
                weekdays = setOf(DayOfWeek.WEDNESDAY),
                times = listOf(LocalTime.of(8, 0)),
            )
        val result =
            taskCompletionOverview(
                task.taskId,
                listOf(weekday),
                emptyList(),
                emptyList(),
                HistoryPeriod(day.minusDays(2), day),
                now,
            )
        assertEquals(listOf(day), result.map { it.occurrence.date })
    }

    @Test
    fun pausedSchedulesRetainEarlierHistoryWithoutRedBoxesAfterPause() {
        val pause =
            ItemOperation(
                "pause",
                ItemType.TASK,
                task.taskId,
                OperationAction.EDIT,
                OperationScope.FOLLOWING,
                morning.id,
                day,
                morning.time,
                task.id,
                task.title,
                actualStartMillis = morning.instant.toEpochMilli(),
                task = ManagedTasks.mapSlots(task, task.copy(id = "pause-draft", active = false)),
                createdMillis = day.atTime(7, 0).atZone(FAMILY_ZONE).toInstant().toEpochMilli(),
            )
        val result =
            taskCompletionOverview(
                task.taskId,
                listOf(task),
                emptyList(),
                listOf(pause),
                HistoryPeriod(day.minusDays(1), day),
                now,
            )
        assertEquals(2, result.size)
        assertTrue(result.all { it.occurrence.date == day.minusDays(1) })
    }

    @Test
    fun monthlyTrackerContainsEveryRealDateIncludingUnscheduledLeapDays() {
        val month = YearMonth.of(2024, 2)
        val once =
            task.copy(
                frequency = Frequency.ONCE,
                start = month.atDay(5),
                end = null,
                effective = month.atDay(1),
            )
        val result =
            taskCompletionMonth(task.taskId, listOf(once), emptyList(), emptyList(), month, now)
        assertEquals(29, result.days.size)
        assertEquals(month.atEndOfMonth(), result.days.last().date)
        assertEquals(2, result.missing)
        assertEquals(1, result.days.count { it.cells.isNotEmpty() })
        assertEquals(0, result.percentage)
    }

    @Test
    fun monthlyPercentageCountsExecutionsAndExcludesFutureSlots() {
        val routine = task.copy(start = day, end = day)
        val first = Occurrence(routine, day, LocalTime.of(8, 0))
        val result =
            taskCompletionMonth(
                task.taskId,
                listOf(routine),
                listOf(completion(first)),
                emptyList(),
                YearMonth.from(day),
                now,
            )
        assertEquals(30, result.days.size)
        assertEquals(1, result.confirmed)
        assertEquals(1, result.upcoming)
        assertEquals(1, result.eligible)
        assertEquals(100, result.percentage)
    }

    @Test
    fun emptyOrEntirelyFutureMonthsDoNotInventACompletionPercentage() {
        val future = task.copy(start = day.plusDays(1), effective = day)
        val result =
            taskCompletionMonth(
                task.taskId,
                listOf(future),
                emptyList(),
                emptyList(),
                YearMonth.from(day),
                now,
            )
        assertEquals(0, result.eligible)
        assertTrue(result.upcoming > 0)
        assertNull(result.percentage)
        val empty =
            taskCompletionMonth(
                task.taskId,
                emptyList(),
                emptyList(),
                emptyList(),
                YearMonth.of(2026, 12),
                now,
            )
        assertEquals(31, empty.days.size)
        assertTrue(empty.days.all { it.cells.isEmpty() })
        assertNull(empty.percentage)
    }

    @Test
    fun monthWeeksAlwaysAlignMondayThroughSundayWithoutAdjacentMonthDates() {
        for (year in listOf(2024, 2026)) for (number in 1..12) {
            val month = YearMonth.of(year, number)
            val tracker =
                taskCompletionMonth(task.taskId, emptyList(), emptyList(), emptyList(), month, now)
            assertTrue(tracker.weeks.all { it.size == 7 })
            assertEquals(tracker.days, tracker.weeks.flatten().filterNotNull())
            tracker.weeks.forEach { week ->
                week.forEachIndexed { column, date ->
                    if (date != null) assertEquals(column + 1, date.date.dayOfWeek.value)
                }
            }
            assertEquals(
                month.atDay(1).dayOfWeek.value - 1,
                tracker.weeks.first().takeWhile { it == null }.size,
            )
        }
    }

    @Test
    fun mondayFebruaryNeedsFourRowsAndSundayStartsWithSixEmptyCells() {
        val february =
            taskCompletionMonth(
                task.taskId,
                emptyList(),
                emptyList(),
                emptyList(),
                YearMonth.of(2021, 2),
                now,
            )
        assertEquals(4, february.weeks.size)
        assertEquals(28, february.weeks.last().last()?.date?.dayOfMonth)
        val november =
            taskCompletionMonth(
                task.taskId,
                emptyList(),
                emptyList(),
                emptyList(),
                YearMonth.of(2026, 11),
                now,
            )
        assertEquals(6, november.weeks.size)
        assertTrue(november.weeks.first().take(6).all { it == null })
        assertEquals(1, november.weeks.first().last()?.date?.dayOfMonth)
        assertEquals(30, november.weeks.last().first()?.date?.dayOfMonth)
        assertTrue(november.weeks.last().drop(1).all { it == null })
    }
}
