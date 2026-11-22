package de.kinderalltag.domain

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class TaskReminderDeliveryTest {
    private val day = LocalDate.of(2026, 11, 16)
    private val before = day.atStartOfDay(FAMILY_ZONE).toInstant()
    private val task = TaskRevision("r", "task", "johann", "Routine", start = day, effective = day)
    private val occurrence = Occurrence(task, day, task.times.single())
    private val due = occurrence.instant

    private fun completion(id: String = "done", pending: Boolean = false) =
        CareEvent(
            id,
            occurrence.id,
            day,
            EventKind.COMPLETE,
            "b",
            "Mama",
            before.toEpochMilli(),
            pending = pending,
            completedById = "a",
            completedByName = "Papa",
            taskSnapshot = occurrence,
        )

    private fun deliver(
        events: List<CareEvent> = emptyList(),
        operations: List<ItemOperation> = emptyList(),
        at: Instant = due,
        uid: String? = "a",
        scheduled: Instant = due,
        revisions: List<TaskRevision> = listOf(task),
    ) =
        FamilyReminders.taskNeedsReminder(
            occurrence.id,
            day,
            scheduled,
            uid,
            revisions,
            events,
            operations,
            at,
        )

    private fun plan(
        events: List<CareEvent>,
        at: Instant = before,
        pending: Set<String> = emptySet(),
    ) =
        FamilyReminders.plan(
            "a",
            emptyList(),
            listOf(task),
            events,
            emptyList(),
            emptyList(),
            at,
            pending,
        )

    @Test
    fun completedAndPendingOfflineConfirmationsSuppressAlreadyScheduledAlarms() {
        assertTrue(plan(emptyList()).any { it.id == occurrence.id })
        for (pending in listOf(false, true)) {
            val event = completion(pending = pending)
            assertFalse(deliver(listOf(event), at = due.plusSeconds(30)))
            assertTrue(
                plan(listOf(event), at = due.plusSeconds(30), pending = setOf(occurrence.id)).none {
                    it.id == occurrence.id
                }
            )
        }
    }

    @Test
    fun aCorrectionMustRetractEveryActiveConfirmationBeforeDeliveryIsAllowed() {
        val first = completion("first")
        val second = completion("second").copy(authorId = "a")
        val undoFirst = first.copy(id = "undo-first", kind = EventKind.RETRACT, targetId = first.id)
        val undoSecond =
            second.copy(id = "undo-second", kind = EventKind.RETRACT, targetId = second.id)
        assertFalse(deliver(listOf(first, second, undoFirst)))
        assertTrue(deliver(listOf(first, second, undoFirst, undoSecond)))
        assertTrue(
            plan(listOf(first, second, undoFirst, undoSecond)).any { it.id == occurrence.id }
        )
    }

    @Test
    fun deliveryRequiresADueUnexpiredOccurrenceAndResponsibility() {
        assertTrue(deliver())
        assertFalse(deliver(at = due.minusSeconds(1)))
        assertFalse(deliver(at = day.plusDays(1).atStartOfDay(FAMILY_ZONE).toInstant()))
        assertFalse(deliver(uid = null))
        assertFalse(deliver(revisions = listOf(task.copy(responsibleParentId = "b"))))
        assertFalse(deliver(revisions = emptyList()))
    }

    @Test
    fun deletionAndRestorationAreRecheckedWhenAnAlarmFires() {
        val deletion =
            ItemOperation(
                "delete",
                ItemType.TASK,
                task.taskId,
                OperationAction.DELETE,
                OperationScope.SINGLE,
                occurrence.id,
                day,
                occurrence.time,
                task.id,
                task.title,
            )
        assertFalse(deliver(operations = listOf(deletion)))
        val restore =
            deletion.copy(
                id = "restore",
                action = OperationAction.RESTORE,
                restoresId = deletion.id,
            )
        assertTrue(deliver(operations = listOf(deletion, restore)))
    }

    @Test
    fun anOldAlarmCannotDeliverAMovedExecutionAtItsPreviousTime() {
        val draft = task.copy(id = "moved", times = listOf(LocalTime.of(9, 0)))
        val replacement = day.atTime(9, 0).atZone(FAMILY_ZONE).toInstant()
        val edit =
            ItemOperation(
                "edit",
                ItemType.TASK,
                task.taskId,
                OperationAction.EDIT,
                OperationScope.SINGLE,
                occurrence.id,
                day,
                occurrence.time,
                task.id,
                task.title,
                actualDate = day,
                actualStartMillis = due.toEpochMilli(),
                replacementStartMillis = replacement.toEpochMilli(),
                task = draft,
                createdMillis = before.toEpochMilli(),
            )
        assertFalse(deliver(operations = listOf(edit), at = due.plusSeconds(30)))
        assertFalse(deliver(operations = listOf(edit), at = replacement.plusSeconds(30)))
        assertTrue(deliver(operations = listOf(edit), at = replacement, scheduled = replacement))
    }

    @Test
    fun completingOneExecutionDoesNotSuppressOtherDatesOrTimeSlots() {
        val later = task.copy(times = listOf(LocalTime.of(8, 0), LocalTime.of(18, 0)))
        val evening = Occurrence(later, day, LocalTime.of(18, 0))
        assertTrue(
            FamilyReminders.taskNeedsReminder(
                evening.id,
                day,
                evening.instant,
                "a",
                listOf(later),
                listOf(completion()),
                emptyList(),
                evening.instant,
            )
        )
        val tomorrow = Occurrence(task, day.plusDays(1), task.times.single())
        assertTrue(
            FamilyReminders.taskNeedsReminder(
                tomorrow.id,
                tomorrow.date,
                tomorrow.instant,
                "a",
                listOf(task),
                listOf(completion()),
                emptyList(),
                tomorrow.instant,
            )
        )
    }
}
