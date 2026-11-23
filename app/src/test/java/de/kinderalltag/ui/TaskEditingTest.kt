package de.kinderalltag.ui

import de.kinderalltag.data.FamilyState
import de.kinderalltag.domain.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class TaskEditingTest {
    private val day = LocalDate.of(2026, 11, 23)
    private val now = day.atTime(12, 0).atZone(FAMILY_ZONE).toInstant()
    private val task =
        TaskRevision(
            "source",
            "task",
            "johann",
            "Einmalig",
            frequency = Frequency.ONCE,
            start = day,
            effective = day,
        )
    private val occurrence = Occurrence(task, day, LocalTime.of(8, 0))

    @Test
    fun occurrencePagesKeepAllSameDaySlotsAndDoNotJumpToDistantEditedDates() {
        val recurring =
            task.copy(frequency = Frequency.DAILY, times = (8..15).map { LocalTime.of(it, 0) })
        val anchor = Occurrence(recurring, day.plusDays(60), recurring.times.first())
        val edit =
            taskOperation(
                    anchor,
                    OperationScope.SINGLE,
                    OperationAction.EDIT,
                    task.copy(id = "far", start = day.plusDays(61)),
                )
                .copy(createdMillis = now.toEpochMilli())
        val state = FamilyState(revisions = listOf(recurring), itemOperations = listOf(edit))
        val first =
            taskActionChoices(state, recurring, day.atStartOfDay(FAMILY_ZONE).toInstant(), 4)
        val second = taskActionChoices(state, recurring, first.last().instant.plusNanos(1), 4)
        val third = taskActionChoices(state, recurring, second.last().instant.plusNanos(1), 4)
        assertEquals((8..11).map { LocalTime.of(it, 0) }, first.map { it.time })
        assertEquals((12..15).map { LocalTime.of(it, 0) }, second.map { it.time })
        assertTrue(third.all { it.date == day.plusDays(1) })
        assertEquals(8, (first + second).map { it.id }.toSet().size)
    }

    @Test
    fun occurrenceChoicesIncludeMovedInstancesAtTheirActualDateAndEndAtTheScheduleBoundary() {
        val recurring = task.copy(frequency = Frequency.DAILY, end = day.plusDays(1))
        val original = Occurrence(recurring, day, recurring.times.single())
        val edit =
            taskOperation(
                    original,
                    OperationScope.SINGLE,
                    OperationAction.EDIT,
                    task.copy(id = "moved", start = day.minusDays(1)),
                )
                .copy(createdMillis = original.instant.minusSeconds(60).toEpochMilli())
        val state = FamilyState(revisions = listOf(recurring), itemOperations = listOf(edit))
        val choices =
            taskActionChoices(
                state,
                recurring,
                day.minusDays(2).atStartOfDay(FAMILY_ZONE).toInstant(),
            )
        assertEquals(listOf(day.minusDays(1), day.plusDays(1)), choices.map { it.date })
        assertEquals(original.id, choices.first().id)
        assertTrue(
            taskActionChoices(state, recurring, choices.last().instant.plusNanos(1)).isEmpty()
        )
    }

    @Test
    fun sparseAndPausedSchedulesOfferCombinedChoicesAcrossLongIntervals() {
        val recurring = task.copy(frequency = Frequency.WEEKS, interval = 3)
        listOf(true, false).forEach { active ->
            val definition = recurring.copy(active = active)
            val state = FamilyState(revisions = listOf(definition))
            val choices =
                taskActionChoices(state, definition, day.atStartOfDay(FAMILY_ZONE).toInstant(), 4)
            assertEquals((0L..3L).map { day.plusWeeks(it * 3) }, choices.map { it.date })
        }
    }

    @Test
    fun overdueOneOffIsSelectedOnOpeningTheDialogWithoutChangingItsDate() {
        val overdue = task.copy(start = day.minusDays(1), effective = day.minusDays(1))
        val state = FamilyState(revisions = listOf(overdue), fromCache = false, loaded = true)
        val selected =
            initialTaskActionOccurrence(ItemActionTarget(false, definition = overdue), state, now)
        assertNotNull(selected)
        assertEquals(overdue.start, selected!!.date)
        assertNull(taskEditBlockReason(selected, state.revisions, state.events, now))
    }

    @Test
    fun overdueMovedOneOffIsSelectedAtItsActualDateInsteadOfItsOriginalDate() {
        val overdue = task.copy(start = day.minusDays(2), effective = day.minusDays(2))
        val original = Occurrence(overdue, overdue.start, overdue.times.single())
        val moved = overdue.copy(id = "moved", start = day.minusDays(1))
        val edit =
            taskOperation(original, OperationScope.SINGLE, OperationAction.EDIT, moved)
                .copy(createdMillis = original.instant.minusSeconds(60).toEpochMilli())
        val state = FamilyState(revisions = listOf(overdue), itemOperations = listOf(edit))
        val selected =
            initialTaskActionOccurrence(ItemActionTarget(false, definition = overdue), state, now)!!
        assertEquals(original.id, selected.id)
        assertEquals(moved.start, selected.date)
        assertNull(taskEditBlockReason(selected, state.revisions, state.events, now))
    }

    @Test
    fun overdueOneOffCanProceedButAnElapsedRecurringInstanceCannot() {
        assertNull(taskEditBlockReason(occurrence, listOf(task), emptyList(), now))
        val recurring = task.copy(frequency = Frequency.DAILY)
        assertNotNull(
            taskEditBlockReason(
                Occurrence(recurring, day, occurrence.time),
                listOf(recurring),
                emptyList(),
                now,
            )
        )
        // A single override of a recurring task has ONCE frequency, but retains its recurring
        // source.
        assertNotNull(taskEditBlockReason(occurrence, listOf(recurring), emptyList(), now))
        assertNull(
            taskEditBlockReason(
                Occurrence(recurring, day.plusDays(1), occurrence.time),
                listOf(recurring),
                emptyList(),
                now,
            )
        )
    }

    @Test
    fun confirmedOneOffStaysProtectedUntilItsConfirmationIsRetracted() {
        val completion =
            CareEvent(
                "done",
                occurrence.id,
                day,
                EventKind.COMPLETE,
                "parent",
                "Papa",
                now.toEpochMilli(),
            )
        assertNotNull(taskEditBlockReason(occurrence, listOf(task), listOf(completion), now))
        val correction =
            completion.copy(id = "undo", kind = EventKind.RETRACT, targetId = completion.id)
        assertNull(
            taskEditBlockReason(occurrence, listOf(task), listOf(completion, correction), now)
        )
        assertNotNull(taskEditBlockReason(null, listOf(task), emptyList(), now))
    }
}
