package de.kinderalltag.domain

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class TaskIntervalTest {
    private val start = LocalDate.of(2026, 1, 31)

    private fun task(frequency: Frequency, interval: Int = 1, date: LocalDate = start) =
        TaskRevision(
            "source",
            "task",
            "johann",
            "Routine",
            frequency = frequency,
            interval = interval,
            times = listOf(LocalTime.of(8, 0)),
            start = date,
            effective = date,
        )

    @Test
    fun dayIntervalsStayAnchoredAcrossDstAndDoNotMeanElapsed24HourPeriods() {
        val anchor = LocalDate.of(2026, 3, 27)
        for (n in listOf(2, 3)) {
            val task = task(Frequency.DAILY, n, anchor)
            assertEquals(
                (0L..20L).filter { it % n == 0L },
                (0L..20L).filter { task.isDue(anchor.plusDays(it)) },
            )
            assertEquals(anchor.plusDays(n.toLong()), task.nextDueDate(anchor.plusDays(1)))
        }
    }

    @Test
    fun weekIntervalsUseTheSameWeekdayAtMultiplesOfSevenDays() {
        val task = task(Frequency.WEEKS, 3)
        assertFalse(task.isDue(start.plusWeeks(1)))
        assertFalse(task.isDue(start.plusWeeks(2)))
        assertTrue(task.isDue(start.plusWeeks(3)))
        assertEquals(start.plusWeeks(3), task.nextDueDate(start.plusDays(1)))
        assertEquals(start.dayOfWeek, task.nextDueDate(start.plusDays(1))!!.dayOfWeek)
    }

    @Test
    fun monthlyIntervalsClampShortMonthsWithoutDriftingTheOriginalDay() {
        val monthly = task(Frequency.MONTHS)
        assertEquals(LocalDate.of(2026, 2, 28), monthly.nextDueDate(start.plusDays(1)))
        assertEquals(LocalDate.of(2026, 3, 31), monthly.nextDueDate(LocalDate.of(2026, 3, 1)))
        assertFalse(monthly.isDue(LocalDate.of(2026, 3, 28)))
        val quarterly = task(Frequency.MONTHS, 3)
        assertTrue(quarterly.isDue(LocalDate.of(2026, 4, 30)))
        assertTrue(quarterly.isDue(LocalDate.of(2026, 7, 31)))
        assertFalse(quarterly.isDue(LocalDate.of(2026, 2, 28)))
        val leap = task(Frequency.MONTHS, 2, LocalDate.of(2027, 12, 31))
        assertEquals(LocalDate.of(2028, 2, 29), leap.nextDueDate(LocalDate.of(2028, 1, 1)))
        assertEquals(LocalDate.of(2028, 4, 30), leap.nextDueDate(LocalDate.of(2028, 3, 1)))
    }

    @Test
    fun nextDatesRespectEffectiveStartAndInclusiveEndWithoutResettingTheRhythm() {
        val task =
            task(Frequency.WEEKS, 2).copy(effective = start.plusDays(3), end = start.plusDays(28))
        assertEquals(start.plusDays(14), task.nextDueDate(start))
        assertEquals(start.plusDays(28), task.nextDueDate(start.plusDays(15)))
        assertNull(task.nextDueDate(start.plusDays(29)))
        assertNull(task.copy(active = false).nextDueDate(start))
    }

    @Test
    fun invalidIntervalsAreRejectedAndLegacySchedulesStillUseOne() {
        for (n in listOf(0, -1, 1001)) assertThrows(IllegalArgumentException::class.java) {
            task(Frequency.DAILY, n).validate()
        }
        assertThrows(IllegalArgumentException::class.java) { task(Frequency.ONCE, 2).validate() }
        assertThrows(IllegalArgumentException::class.java) {
            task(Frequency.WEEKDAYS, 2).copy(weekdays = setOf(DayOfWeek.MONDAY)).validate()
        }
        task(Frequency.DAILY).validate()
        assertTrue(task(Frequency.DAILY).isDue(start.plusDays(1)))
        assertFalse(task(Frequency.ONCE).isDue(start.plusDays(1)))
    }

    @Test
    fun futureLookupFindsSparseSchedulesAndTheFollowingExecutionAfterTodaysTime() {
        for (frequency in listOf(Frequency.DAILY, Frequency.WEEKS, Frequency.MONTHS)) {
            val task = task(frequency, 12)
            val now = start.atTime(9, 0).atZone(FAMILY_ZONE).toInstant()
            val expected = task.nextDueDate(start.plusDays(1))!!
            assertEquals(
                expected,
                ManagedTasks.futureOccurrences(
                        listOf(task),
                        emptyList(),
                        emptyList(),
                        task.taskId,
                        now,
                    )
                    .first()
                    .date,
            )
            assertEquals(
                expected,
                taskOverviewTimings(
                        setOf(task.taskId),
                        listOf(task),
                        emptyList(),
                        emptyList(),
                        now,
                    )[task.taskId]
                    ?.date,
            )
        }
    }

    @Test
    fun remindersPreviewAndOverdueRetentionUseTheSameInterval() {
        val task = task(Frequency.DAILY, 3)
        val now = start.atStartOfDay(FAMILY_ZONE).toInstant()
        val planned =
            FamilyReminders.plan(
                "a",
                emptyList(),
                listOf(task),
                emptyList(),
                emptyList(),
                emptyList(),
                now,
            )
        assertEquals(
            listOf(start, start.plusDays(3), start.plusDays(6)),
            planned.map { LocalDate.parse(it.date) },
        )
        assertTrue(
            upcomingTaskOccurrences(listOf(task), emptyList(), emptyList(), start, 5, days = 2)
                .isEmpty()
        )
        assertEquals(
            start.plusDays(3),
            upcomingTaskOccurrences(listOf(task), emptyList(), emptyList(), start, 5, days = 3)
                .single()
                .date,
        )
        val secondDay = start.plusDays(2)
        assertEquals(
            start,
            overdueTaskOccurrences(
                    listOf(task),
                    emptyList(),
                    emptyList(),
                    secondDay,
                    secondDay.atTime(10, 0).atZone(FAMILY_ZONE).toInstant(),
                    7,
                )
                .single()
                .date,
        )
        val next = start.plusDays(3)
        assertTrue(
            overdueTaskOccurrences(
                    listOf(task),
                    emptyList(),
                    emptyList(),
                    next,
                    next.atTime(8, 0).atZone(FAMILY_ZONE).toInstant(),
                    7,
                )
                .isEmpty()
        )
    }

    @Test
    fun monthlyTrackerContainsOnlyActualIntervalDates() {
        val task = task(Frequency.MONTHS, 2)
        val period = HistoryPeriod(start, LocalDate.of(2026, 3, 31))
        val cells =
            taskCompletionOverview(
                task.taskId,
                listOf(task),
                emptyList(),
                emptyList(),
                period,
                LocalDate.of(2026, 4, 1).atStartOfDay(FAMILY_ZONE).toInstant(),
            )
        assertEquals(listOf(start, LocalDate.of(2026, 3, 31)), cells.map { it.occurrence.date })
        assertTrue(cells.all { it.status == TaskCompletionStatus.MISSING })
    }

    @Test
    fun followingIntervalEditsPreserveStartedAndEarlyCompletedExecutions() {
        val anchor = LocalDate.of(2026, 11, 1)
        val source = task(Frequency.DAILY, date = anchor)
        val selected = Occurrence(source, anchor.plusDays(3), source.times.single())
        val completed = Occurrence(source, anchor.plusDays(4), source.times.single())
        val changedAt = anchor.plusDays(2).atTime(12, 0).atZone(FAMILY_ZONE).toInstant()
        val event =
            CareEvent(
                "done",
                completed.id,
                completed.date,
                EventKind.COMPLETE,
                "a",
                "Papa",
                changedAt.minusSeconds(60).toEpochMilli(),
                taskSnapshot = completed,
            )
        val draft =
            source.copy(
                id = "draft",
                start = selected.date,
                effective = anchor.plusDays(2),
                interval = 3,
            )
        val edit =
            ItemOperation(
                "edit",
                ItemType.TASK,
                source.taskId,
                OperationAction.EDIT,
                OperationScope.FOLLOWING,
                selected.id,
                selected.date,
                selected.time,
                source.id,
                source.title,
                actualDate = selected.date,
                actualStartMillis = selected.instant.toEpochMilli(),
                task = draft,
                createdMillis = changedAt.toEpochMilli(),
            )
        fun on(date: LocalDate) =
            ManagedTasks.occurrences(listOf(source), listOf(event), listOf(edit), date)
        assertTrue(on(anchor.plusDays(2)).isNotEmpty())
        assertEquals(completed, on(completed.date).single())
        assertTrue(on(anchor.plusDays(5)).isEmpty())
        assertEquals(3, on(anchor.plusDays(6)).single().revision.interval)
    }
}
