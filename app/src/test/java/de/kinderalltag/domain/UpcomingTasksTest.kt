package de.kinderalltag.domain

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class UpcomingTasksTest {
    private val day = LocalDate.of(2026, 11, 21)

    private fun task(
        id: String,
        date: LocalDate = day,
        frequency: Frequency = Frequency.DAILY,
        hour: Int = 8,
        child: String = "johann",
        parent: String? = null,
    ) =
        TaskRevision(
            id,
            id,
            child,
            id,
            frequency = frequency,
            times = listOf(LocalTime.of(hour, 0)),
            start = date,
            effective = day.minusDays(1),
            responsibleParentId = parent,
        )

    private fun completed(occurrence: Occurrence) =
        CareEvent(
            "done-${occurrence.id}",
            occurrence.id,
            occurrence.date,
            EventKind.COMPLETE,
            "parent",
            "Papa",
            day.atStartOfDay(FAMILY_ZONE).toInstant().toEpochMilli(),
            taskSnapshot = occurrence,
        )

    @Test
    fun tasksAlreadyOnTheDisplayedDayDoNotAlsoAppearInTheFuturePreview() {
        val recurring =
            listOf(Frequency.DAILY, Frequency.WEEKS, Frequency.MONTHS).map {
                task(it.name, frequency = it)
            }
        val tomorrow = task("once-tomorrow", day.plusDays(1), Frequency.ONCE)
        val revisions = recurring + tomorrow
        val due = ManagedTasks.occurrences(revisions, emptyList(), emptyList(), day)
        listOf(emptyList(), due.map(::completed)).forEach { events ->
            val result =
                upcomingTaskOccurrences(
                    revisions,
                    events,
                    emptyList(),
                    day,
                    5,
                    excludedTaskIds = due.map { it.revision.taskId }.toSet(),
                )
            assertEquals(listOf(tomorrow.taskId), result.map { it.revision.taskId })
        }
    }

    @Test
    fun suppressingVisibleTasksStillFillsThePreviewLimitWithOtherTasks() {
        val today = task("daily-today")
        val futureRecurring = task("daily-starts-tomorrow", day.plusDays(1))
        val futureOnce = task("once-tomorrow", day.plusDays(1), Frequency.ONCE, 10)
        val result =
            upcomingTaskOccurrences(
                listOf(today, futureRecurring, futureOnce),
                emptyList(),
                emptyList(),
                day,
                2,
                excludedTaskIds = setOf(today.taskId),
            )
        assertEquals(
            listOf(futureRecurring.taskId, futureOnce.taskId),
            result.map { it.revision.taskId },
        )
        assertTrue(result.all { it.date == day.plusDays(1) })
    }

    @Test
    fun ordersOneOffAndRecurringTasksChronologicallyAndLimitsDistinctTasks() {
        val daily = task("daily", hour = 0).copy(times = listOf(LocalTime.MIDNIGHT, LocalTime.NOON))
        val once = task("once", day.plusDays(1), Frequency.ONCE, 10)
        val distant = task("distant", day.plusDays(365), Frequency.ONCE)
        val result =
            upcomingTaskOccurrences(listOf(distant, once, daily), emptyList(), emptyList(), day, 2)
        assertEquals(listOf("daily", "once"), result.map { it.revision.taskId })
        assertEquals(listOf(day.plusDays(1), day.plusDays(1)), result.map { it.date })
        assertEquals(LocalTime.MIDNIGHT, result.first().time)
        assertEquals(
            distant.start,
            upcomingTaskOccurrences(listOf(distant), emptyList(), emptyList(), day, 5, days = 365)
                .single()
                .date,
        )
    }

    @Test
    fun skipsCompletedSlotsAndRestoresCorrectedSlots() {
        val daily = task("daily").copy(times = listOf(LocalTime.of(8, 0), LocalTime.of(18, 0)))
        val first = Occurrence(daily, day.plusDays(1), LocalTime.of(8, 0))
        val completion = completed(first)
        val next =
            upcomingTaskOccurrences(listOf(daily), listOf(completion), emptyList(), day, 5).single()
        assertEquals(day.plusDays(1), next.date)
        assertEquals(LocalTime.of(18, 0), next.time)
        val undo = completion.copy(id = "undo", kind = EventKind.RETRACT, targetId = completion.id)
        assertEquals(
            first.id,
            upcomingTaskOccurrences(listOf(daily), listOf(completion, undo), emptyList(), day, 5)
                .single()
                .id,
        )
    }

    @Test
    fun findsTheNextOpenExecutionAfterMoreThanAWeekOfAdvanceCompletions() {
        val daily = task("daily")
        val events =
            (1L..15L).map { completed(Occurrence(daily, day.plusDays(it), daily.times.single())) }
        assertEquals(
            day.plusDays(16),
            upcomingTaskOccurrences(listOf(daily), events, emptyList(), day, 5, days = 30)
                .single()
                .date,
        )
        val once = task("once", day.plusDays(3), Frequency.ONCE)
        assertTrue(
            upcomingTaskOccurrences(
                    listOf(once),
                    listOf(completed(Occurrence(once, once.start, once.times.single()))),
                    emptyList(),
                    day,
                    5,
                )
                .isEmpty()
        )
    }

    @Test
    fun respectsChildAndParentFiltersIncludingSharedAndHouseholdTasks() {
        val tasks =
            listOf(
                task("mine", parent = "a"),
                task("other", parent = "b"),
                task("shared"),
                task("ferdinand", child = "ferdinand"),
                task("household", child = "haushalt"),
            )
        val mine =
            upcomingTaskOccurrences(tasks, emptyList(), emptyList(), day, 50, "johann", "a", true)
        assertEquals(setOf("mine", "shared"), mine.map { it.revision.taskId }.toSet())
        assertEquals(
            "household",
            upcomingTaskOccurrences(tasks, emptyList(), emptyList(), day, 5, "haushalt")
                .single()
                .revision
                .taskId,
        )
        assertTrue(
            upcomingTaskOccurrences(
                    tasks,
                    emptyList(),
                    emptyList(),
                    day,
                    50,
                    uid = null,
                    onlyMine = true,
                )
                .isEmpty()
        )
    }

    @Test
    fun excludesEndedAndPausedTasksAndUsesDaysAfterTheDisplayedDate() {
        val ended = task("ended").copy(end = day)
        val paused = task("paused").copy(active = false)
        val daily = task("daily")
        val selected = day.plusDays(30)
        val result =
            upcomingTaskOccurrences(
                listOf(ended, paused, daily),
                emptyList(),
                emptyList(),
                selected,
                5,
            )
        assertEquals(listOf("daily"), result.map { it.revision.taskId })
        assertEquals(selected.plusDays(1), result.single().date)
        assertTrue(
            upcomingTaskOccurrences(listOf(daily), emptyList(), emptyList(), day, 0).isEmpty()
        )
    }

    @Test
    fun honorsMovedDeletedAndRestoredExecutions() {
        val source = task("once", day.plusDays(1), Frequency.ONCE)
        val original = Occurrence(source, source.start, source.times.single())
        val moved =
            source.copy(id = "moved", start = day.plusDays(4), times = listOf(LocalTime.of(11, 0)))
        val edit =
            ItemOperation(
                "move",
                ItemType.TASK,
                source.taskId,
                OperationAction.EDIT,
                OperationScope.SINGLE,
                original.id,
                original.date,
                original.time,
                source.id,
                source.title,
                actualDate = original.date,
                actualStartMillis = original.instant.toEpochMilli(),
                replacementStartMillis =
                    moved.start
                        .atTime(moved.times.single())
                        .atZone(FAMILY_ZONE)
                        .toInstant()
                        .toEpochMilli(),
                task = moved,
                createdMillis = day.atStartOfDay(FAMILY_ZONE).toInstant().toEpochMilli(),
            )
        val result =
            upcomingTaskOccurrences(listOf(source), emptyList(), listOf(edit), day, 5).single()
        assertEquals(moved.start, result.date)
        assertEquals(moved.times.single(), result.time)
        assertEquals(original.id, result.id)
        val deletion =
            edit.copy(
                id = "delete",
                action = OperationAction.DELETE,
                scope = OperationScope.ALL,
                task = null,
            )
        assertTrue(
            upcomingTaskOccurrences(listOf(source), emptyList(), listOf(edit, deletion), day, 5)
                .isEmpty()
        )
        val restore =
            deletion.copy(
                id = "restore",
                action = OperationAction.RESTORE,
                restoresId = deletion.id,
            )
        assertEquals(
            result,
            upcomingTaskOccurrences(
                    listOf(source),
                    emptyList(),
                    listOf(edit, deletion, restore),
                    day,
                    5,
                )
                .single(),
        )
    }

    @Test
    fun includesTheLastPreviewDayAndExcludesOccurrencesBeyondIt() {
        val tomorrow = task("tomorrow", day.plusDays(1), Frequency.ONCE, 0)
        val last =
            task("last", day.plusDays(7), Frequency.ONCE, 23)
                .copy(times = listOf(LocalTime.of(23, 59)))
        val beyond = task("beyond", day.plusDays(8), Frequency.ONCE, 0)
        val today = task("today", day, Frequency.ONCE)
        val tasks = listOf(beyond, last, today, tomorrow)
        assertEquals(
            listOf("tomorrow", "last"),
            upcomingTaskOccurrences(tasks, emptyList(), emptyList(), day, 50, days = 7).map {
                it.revision.taskId
            },
        )
        assertEquals(
            "tomorrow",
            upcomingTaskOccurrences(tasks, emptyList(), emptyList(), day, 50, days = 1)
                .single()
                .revision
                .taskId,
        )
        assertTrue(
            upcomingTaskOccurrences(tasks, emptyList(), emptyList(), day, 50, days = 0).isEmpty()
        )
    }

    @Test
    fun doesNotReplaceAnEarlyCompletedTaskWithAnExecutionOutsideThePreviewWindow() {
        val daily = task("daily")
        val events =
            (1L..7L).map { completed(Occurrence(daily, day.plusDays(it), daily.times.single())) }
        assertTrue(
            upcomingTaskOccurrences(listOf(daily), events, emptyList(), day, 5, days = 7).isEmpty()
        )
        assertEquals(
            day.plusDays(8),
            upcomingTaskOccurrences(listOf(daily), events, emptyList(), day, 5, days = 8)
                .single()
                .date,
        )
    }

    @Test
    fun previewWindowFollowsTheSelectedDayRatherThanTheDeviceDate() {
        val selected = day.plusDays(30)
        val near = task("near", selected.plusDays(3), Frequency.ONCE)
        val far = task("far", selected.plusDays(4), Frequency.ONCE)
        assertEquals(
            "near",
            upcomingTaskOccurrences(
                    listOf(far, near),
                    emptyList(),
                    emptyList(),
                    selected,
                    50,
                    days = 3,
                )
                .single()
                .revision
                .taskId,
        )
    }

    @Test
    fun midnightAfterBerlinDstBoundaryIsIncluded() {
        val after = LocalDate.of(2026, 3, 29)
        val daily =
            task("midnight", after.minusDays(1), hour = 0).copy(effective = after.minusDays(1))
        val next =
            upcomingTaskOccurrences(listOf(daily), emptyList(), emptyList(), after, 5).single()
        assertEquals(after.plusDays(1), next.date)
        assertEquals(LocalTime.MIDNIGHT, next.time)
        assertEquals(Instant.parse("2026-03-29T22:00:00Z"), next.instant)
    }
}
