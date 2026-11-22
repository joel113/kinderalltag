package de.kinderalltag.domain

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class OverdueTasksTest {
    private val day = LocalDate.of(2026, 11, 22)
    private val now = day.atTime(7, 0).atZone(FAMILY_ZONE).toInstant()

    private fun task(
        id: String,
        date: LocalDate,
        frequency: Frequency = Frequency.ONCE,
        child: String = "johann",
        parent: String? = null,
    ) =
        TaskRevision(
            id,
            id,
            child,
            id,
            frequency = frequency,
            times = listOf(LocalTime.of(8, 0)),
            start = date,
            effective = day.minusDays(30),
            responsibleParentId = parent,
        )

    private fun completion(task: TaskRevision, date: LocalDate) =
        Occurrence(task, date, task.times.single()).let {
            CareEvent(
                "done-${it.id}",
                it.id,
                date,
                EventKind.COMPLETE,
                "a",
                "Papa",
                now.toEpochMilli(),
                taskSnapshot = it,
            )
        }

    private fun overdue(
        tasks: List<TaskRevision>,
        events: List<CareEvent> = emptyList(),
        operations: List<ItemOperation> = emptyList(),
        at: Instant = now,
        days: Int = 7,
        date: LocalDate = day,
    ) = overdueTaskOccurrences(tasks, events, operations, date, at, days)

    @Test
    fun keepsOpenOneOffsWithinTheInclusiveLookbackAndRemovesCompletedOnes() {
        val boundary = task("boundary", day.minusDays(7))
        val recent = task("recent", day.minusDays(1))
        val expired = task("expired", day.minusDays(8))
        val today = task("today", day)
        val future = task("future", day.plusDays(1))
        assertEquals(
            listOf("boundary", "recent"),
            overdue(listOf(recent, expired, today, future, boundary)).map { it.revision.taskId },
        )
        assertEquals(
            listOf("boundary"),
            overdue(listOf(recent, boundary), listOf(completion(recent, recent.start))).map {
                it.revision.taskId
            },
        )
        assertTrue(overdue(listOf(recent), days = 0).isEmpty())
    }

    @Test
    fun replacesYesterdayOnlyWhenTodaysRecurringExecutionBecomesDue() {
        val daily = task("daily", day.minusDays(10), Frequency.DAILY)
        assertEquals(day.minusDays(1), overdue(listOf(daily)).single().date)
        val deadline = day.atTime(8, 0).atZone(FAMILY_ZONE).toInstant()
        assertTrue(overdue(listOf(daily), at = deadline).isEmpty())
        assertTrue(overdue(listOf(daily), listOf(completion(daily, day)), at = deadline).isEmpty())
    }

    @Test
    fun weeklyTasksKeepOnlyTheLatestScheduledOccurrenceAndDoNotReviveOlderFailures() {
        val weekly =
            task("weekly", day.minusDays(30), Frequency.WEEKDAYS)
                .copy(weekdays = setOf(DayOfWeek.WEDNESDAY))
        val previousWednesday = day.minusDays(4)
        assertEquals(previousWednesday, overdue(listOf(weekly), days = 21).single().date)
        assertTrue(
            overdue(listOf(weekly), listOf(completion(weekly, previousWednesday)), days = 21)
                .isEmpty()
        )
        val event = completion(weekly, previousWednesday)
        val correction = event.copy(id = "undo", kind = EventKind.RETRACT, targetId = event.id)
        assertEquals(
            previousWednesday,
            overdue(listOf(weekly), listOf(event, correction), days = 21).single().date,
        )
    }

    @Test
    fun multipleDailyTimesRetainOnlyTheLatestPastExecution() {
        val daily =
            task("daily", day.minusDays(10), Frequency.DAILY)
                .copy(times = listOf(LocalTime.of(8, 0), LocalTime.of(18, 0)))
        val remaining = overdue(listOf(daily)).single()
        assertEquals(day.minusDays(1), remaining.date)
        assertEquals(LocalTime.of(18, 0), remaining.time)
    }

    @Test
    fun deletingAndRestoringAnOverdueOccurrencePreservesItsIdentity() {
        val once = task("once", day.minusDays(2))
        val occurrence = Occurrence(once, once.start, once.times.single())
        val deletion =
            ItemOperation(
                "delete",
                ItemType.TASK,
                once.taskId,
                OperationAction.DELETE,
                OperationScope.SINGLE,
                occurrence.id,
                occurrence.date,
                occurrence.time,
                once.id,
                once.title,
            )
        assertTrue(overdue(listOf(once), operations = listOf(deletion)).isEmpty())
        val restore =
            deletion.copy(
                id = "restore",
                action = OperationAction.RESTORE,
                restoresId = deletion.id,
            )
        assertEquals(
            occurrence,
            overdue(listOf(once), operations = listOf(deletion, restore)).single(),
        )
    }

    @Test
    fun parentAndChildFiltersApplyToTheRetainedExecution() {
        val mine = task("mine", day.minusDays(1), parent = "a")
        val shared = task("shared", day.minusDays(2))
        val other = task("other", day.minusDays(1), parent = "b")
        val household = task("household", day.minusDays(1), child = "haushalt")
        val tasks = listOf(mine, shared, other, household)
        assertEquals(
            listOf("shared", "mine"),
            overdueTaskOccurrences(
                    tasks,
                    emptyList(),
                    emptyList(),
                    day,
                    now,
                    7,
                    "johann",
                    "a",
                    true,
                )
                .map { it.revision.taskId },
        )
        assertEquals(
            "household",
            overdueTaskOccurrences(tasks, emptyList(), emptyList(), day, now, 7, "haushalt")
                .single()
                .revision
                .taskId,
        )
    }

    @Test
    fun browsingAnotherDayUsesThatDaysScheduleAndLookback() {
        val viewedDay = day.plusDays(20)
        val once = task("once", viewedDay.minusDays(1))
        val daily = task("daily", day.minusDays(10), Frequency.DAILY)
        assertEquals(
            listOf("once"),
            overdue(listOf(daily, once), date = viewedDay).map { it.revision.taskId },
        )
    }

    @Test
    fun berlinDstDoesNotChangeTheNumberOfLookbackCalendarDays() {
        val viewed = LocalDate.of(2026, 3, 30)
        val once = task("dst", viewed.minusDays(1)).copy(effective = viewed.minusDays(10))
        assertEquals(
            viewed.minusDays(1),
            overdue(listOf(once), date = viewed, days = 1).single().date,
        )
    }
}
