package de.kinderalltag.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ScheduleTest {
    private val monday = LocalDate.of(2026, 11, 16)
    private fun revision(id: String = "r1", taskId: String = "task1", child: String = "johann") = TaskRevision(
        id, taskId, child, "Testaufgabe", start = monday, effective = monday)
    private fun completion(id: String, occurrence: Occurrence, kind: EventKind = EventKind.COMPLETE, target: String? = null) =
        CareEvent(id, occurrence.id, occurrence.date, kind, "parent", "Joel", 100, targetId = target)

    @Test fun dailyCreatesIndependentDays() {
        val r = revision()
        val a = Schedule.occurrences(listOf(r), monday).single()
        val b = Schedule.occurrences(listOf(r), monday.plusDays(1)).single()
        assertNotEquals(a.id, b.id)
        assertTrue(Schedule.activeCompletions(listOf(completion("e1", a)), b.id).isEmpty())
    }
    @Test fun householdCompletionsDoNotCompleteChildTasks() {
        val household = revision("household-r", "household-task", HOUSEHOLD_SECTION.id)
        val child = revision()
        val all = Schedule.occurrences(listOf(household, child), monday)
        val householdOccurrence = all.single { it.revision.childId == HOUSEHOLD_SECTION.id }
        val childOccurrence = all.single { it.revision.childId == "johann" }
        val events = listOf(completion("household-complete", householdOccurrence))
        assertEquals(1, Schedule.activeCompletions(events, householdOccurrence.id).size)
        assertTrue(Schedule.activeCompletions(events, childOccurrence.id).isEmpty())
        assertEquals(7, Schedule.reminders(listOf(household), emptyList(), monday.atStartOfDay(FAMILY_ZONE).toInstant()).size)
    }
    @Test fun weekdaySchedulesAndDateBounds() {
        val r = revision().copy(frequency = Frequency.WEEKDAYS, weekdays = setOf(DayOfWeek.MONDAY), end = monday.plusDays(7))
        assertEquals(1, Schedule.occurrences(listOf(r), monday).size)
        assertTrue(Schedule.occurrences(listOf(r), monday.plusDays(1)).isEmpty())
        assertEquals(1, Schedule.occurrences(listOf(r), monday.plusDays(7)).size)
        assertTrue(Schedule.occurrences(listOf(r), monday.plusDays(14)).isEmpty())
    }
    @Test fun oneOffOnlyAppearsOnItsDate() {
        val r = revision().copy(frequency = Frequency.ONCE)
        assertEquals(1, Schedule.occurrences(listOf(r), monday).size)
        assertTrue(Schedule.occurrences(listOf(r), monday.minusDays(1)).isEmpty())
        assertTrue(Schedule.occurrences(listOf(r), monday.plusDays(1)).isEmpty())
    }
    @Test fun slotsAndChildrenRemainIndependent() {
        val a = revision().copy(times = listOf(LocalTime.of(8, 0), LocalTime.of(18, 0)))
        val b = revision("r2", "task2", "ferdinand")
        val occurrences = Schedule.occurrences(listOf(a, b), monday)
        assertEquals(3, occurrences.map { it.id }.distinct().size)
        assertEquals(2, occurrences.count { it.revision.childId == "johann" })
    }
    @Test fun editsAndPausesDoNotRewriteTodayOrHistory() {
        val original = revision()
        val edit = original.copy(id = "r2", effective = monday.plusDays(1), title = "Neuer Titel", createdMillis = 20)
        val paused = edit.copy(id = "r3", active = false, effective = monday.plusDays(2))
        val all = listOf(original, edit, paused)
        assertEquals("Testaufgabe", Schedule.occurrences(all, monday).single().revision.title)
        assertEquals("Neuer Titel", Schedule.occurrences(all, monday.plusDays(1)).single().revision.title)
        assertTrue(Schedule.occurrences(all, monday.plusDays(2)).isEmpty())
    }
    @Test fun concurrentRevisionsConvergeRegardlessOfArrivalOrder() {
        val a = revision("a").copy(createdMillis = 10)
        val b = revision("b").copy(createdMillis = 10)
        assertEquals("b", Schedule.revisionsOn(listOf(a, b), monday).single().id)
        assertEquals("b", Schedule.revisionsOn(listOf(b, a), monday).single().id)
    }
    @Test fun concurrentAcknowledgementsRetainBothParents() {
        val occurrence = Schedule.occurrences(listOf(revision()), monday).single()
        val a = completion("a", occurrence)
        val b = completion("b", occurrence).copy(authorId = "other", authorName = "Mama", pending = true)
        assertEquals(2, Schedule.activeCompletions(listOf(a, b), occurrence.id).size)
        assertEquals(1, Schedule.activeCompletions(listOf(a, b, completion("undo", occurrence, EventKind.RETRACT, "a")), occurrence.id).size)
    }
    @Test fun retractionTargetsOnlyItsAcknowledgementAndOccurrence() {
        val occurrence = Schedule.occurrences(listOf(revision()), monday).single()
        val a = completion("a", occurrence)
        val undo = completion("undo", occurrence, EventKind.RETRACT, "a")
        assertTrue(Schedule.activeCompletions(listOf(undo, a), occurrence.id).isEmpty())
        val next = occurrence.copy(date = monday.plusDays(1))
        assertEquals(1, Schedule.activeCompletions(listOf(a, undo.copy(occurrenceId = next.id)), occurrence.id).size)
        assertEquals(1, Schedule.activeCompletions(listOf(a, undo, completion("again", occurrence)), occurrence.id).size)
    }
    @Test fun springGapMovesForwardAndAutumnOverlapOccursOnce() {
        val time = LocalTime.of(2, 30)
        val spring = Occurrence(revision(), LocalDate.of(2026, 3, 29), time)
        assertEquals(Instant.parse("2026-03-29T01:30:00Z"), spring.instant)
        val autumn = Occurrence(revision(), LocalDate.of(2026, 10, 25), time)
        assertEquals(Instant.parse("2026-10-25T00:30:00Z"), autumn.instant)
    }
    @Test fun invalidSchedulesAreRejected() {
        val invalid = listOf(revision().copy(times = emptyList()), revision().copy(title = ""),
            revision().copy(frequency = Frequency.WEEKDAYS), revision().copy(end = monday.minusDays(1)),
            revision().copy(times = listOf(LocalTime.NOON, LocalTime.NOON)))
        invalid.forEach { r -> assertTrue(runCatching { r.validate() }.isFailure) }
    }

    @Test fun delayedInexactAlarmsRemainUntilDeliveredButNewOverdueTasksDoNotAlert() {
        val r = revision()
        val occurrence = Schedule.occurrences(listOf(r), monday).single()
        val now = monday.atTime(8, 5).atZone(FAMILY_ZONE).toInstant()
        assertFalse(Schedule.reminders(listOf(r), emptyList(), now).any { it.id == occurrence.id })
        assertTrue(Schedule.reminders(listOf(r), emptyList(), now, setOf(occurrence.id)).any { it.id == occurrence.id })
        assertFalse(Schedule.reminders(listOf(r), listOf(completion("e", occurrence)), now,
            setOf(occurrence.id)).any { it.id == occurrence.id })
    }

    @Test fun remindersDropPreviousDaysAndBoundTheAlarmCount() {
        val r = revision()
        val yesterday = Schedule.occurrences(listOf(r), monday).single()
        val now = monday.plusDays(1).atStartOfDay(FAMILY_ZONE).toInstant()
        val reminders = Schedule.reminders(listOf(r), emptyList(), now, setOf(yesterday.id), limit = 3)
        assertEquals(3, reminders.size)
        assertFalse(reminders.any { it.id == yesterday.id })
        assertEquals(monday.plusDays(1), reminders.first().date)
    }
}
