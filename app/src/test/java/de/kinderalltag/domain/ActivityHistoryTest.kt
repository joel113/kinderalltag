package de.kinderalltag.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ActivityHistoryTest {
    private val today = LocalDate.of(2026, 11, 18)
    private val period = historyPeriod(today, 0)
    private val parents = listOf(Parent("a", "Papa"), Parent("b", "Mama"))
    private fun millis(date: LocalDate, hour: Int = 12) = date.atTime(hour, 0).atZone(FAMILY_ZONE).toInstant().toEpochMilli()
    private val task = TaskRevision("revision_with_underscores", "task_with_underscores", "johann", "Vitamin D",
        start = today.minusDays(1), effective = today.minusDays(1), authorId = "a")
    private val details = AppointmentDetails("Kinderarzt", AppointmentRecipient.BOTH_CHILDREN, today.plusDays(100))
    private fun history(revisions: List<TaskRevision> = listOf(task), events: List<CareEvent> = emptyList(),
        series: List<AppointmentSeries> = emptyList(), exceptions: List<AppointmentException> = emptyList(),
        activity: List<AppointmentActivity> = emptyList(), section: String? = null, mine: Boolean = false) =
        activityHistory(revisions, events, series, exceptions, activity, parents, period, section, mine, "a")

    @Test fun periodsAreInclusiveContiguousAndUseFamilyTimezone() {
        val previous = historyPeriod(today, 1)
        assertEquals(90L, java.time.temporal.ChronoUnit.DAYS.between(period.from, period.to.plusDays(1)))
        assertEquals(period.from.minusDays(1), previous.to)
        val entries = listOf(period.from.minusDays(1), period.from, today, today.plusDays(1)).mapIndexed { i, date ->
            CareEvent("e$i", "unknown", today, EventKind.COMPLETE, "a", "Papa", millis(date, 0))
        }
        assertEquals(listOf("task-event:e2", "task-event:e1"), history(events = entries).map { it.id })
        val dst = historyPeriod(LocalDate.of(2026, 10, 25), 0)
        assertEquals(90L, java.time.temporal.ChronoUnit.DAYS.between(dst.from, dst.to.plusDays(1)))
    }
    @Test fun confirmationsUseActionTimeRatherThanScheduledDateOrDelayedSyncTime() {
        val occurrence = Occurrence(task, today.minusDays(1), task.times.single())
        val event = CareEvent("complete", occurrence.id, occurrence.date, EventKind.COMPLETE, "a", "Papa",
            millis(today), serverMillis = millis(today.plusDays(1)), completedById = "b", completedByName = "Mama", pending = true)
        val entry = history(events = listOf(event)).single()
        assertEquals("Vitamin D", entry.title)
        assertEquals(HistoryKind.TASK_COMPLETED, entry.kind)
        assertEquals(millis(today), entry.millis)
        assertEquals(today.minusDays(1), entry.subjectDate)
        assertEquals("a", entry.authorId)
        assertEquals("b", entry.creditedId)
        assertTrue(entry.pending)
        assertEquals(1, history(events = listOf(event), section = "johann", mine = true).size)
        assertTrue(history(events = listOf(event), section = "ferdinand").isEmpty())
    }
    @Test fun retractionsRemainAlongsideCompletionsAndUnknownTasksRemainVisible() {
        val complete = CareEvent("done", "missing", today, EventKind.COMPLETE, "b", "Mama", millis(today, 10))
        val undo = complete.copy(id = "undo", kind = EventKind.RETRACT, targetId = "done", clientMillis = millis(today, 11))
        val entries = history(events = listOf(complete, undo))
        assertEquals(listOf("Bestätigung zurückgenommen", "Aufgabe erledigt"), entries.map { it.action })
        assertEquals("Aufgabe", entries.first().title)
        assertEquals(listOf(HistoryKind.TASK_CORRECTED, HistoryKind.TASK_COMPLETED), entries.map { it.kind })
    }
    @Test fun taskRevisionsUseCreationTimeNotTheirFutureEffectiveDate() {
        val original = task.copy(createdMillis = millis(today.minusDays(1)))
        val pause = task.copy(id = "pause", active = false, effective = today.plusDays(1), createdMillis = millis(today, 9))
        val resume = task.copy(id = "resume", effective = today.plusDays(2), createdMillis = millis(today, 10))
        assertEquals(listOf("Aufgabe fortgesetzt", "Aufgabe pausiert", "Aufgabe angelegt"),
            history(revisions = listOf(resume, original, pause)).map { it.action })
    }
    @Test fun appointmentLogRetainsEveryChangeAndSuppressesLegacyDuplicates() {
        val original = AppointmentSeries("s1", details, authorId = "a", createdMillis = millis(today, 8))
        val added = AppointmentActivity("added", "s1", details.date, AppointmentActivityKind.ADDED,
            false, details, null, "a", "Papa", millis(today, 8))
        val changed = added.copy(id = "changed", kind = AppointmentActivityKind.CHANGED,
            details = details.copy(title = "Zahnarzt"), millis = millis(today, 9))
        val cancelled = changed.copy(id = "cancelled", kind = AppointmentActivityKind.CANCELLED, millis = millis(today, 10))
        val exception = AppointmentException("s1", details.date, null, 1, "a", millis(today, 10))
        val entries = history(series = listOf(original), exceptions = listOf(exception), activity = listOf(cancelled, changed, added))
        assertEquals(3, entries.size)
        assertEquals(listOf("Termin abgesagt", "Termin geändert", "Termin angelegt"), entries.map { it.action })
        assertEquals("Zahnarzt", entries.first().title)
        assertEquals(listOf(HistoryKind.APPOINTMENT_CANCELLED, HistoryKind.APPOINTMENT_CHANGED, HistoryKind.APPOINTMENT_ADDED),
            entries.map { it.kind })
        assertTrue(entries.none { it.legacy })
        assertEquals(3, history(activity = listOf(added, changed, cancelled), section = "ferdinand").size)
        assertEquals(3, history(activity = listOf(added, changed, cancelled), section = "johann").size)
        assertTrue(history(activity = listOf(added), section = "haushalt").isEmpty())
    }
    @Test fun legacyCreationAndLatestExceptionHaveOnlyTheirKnownTimestamps() {
        val old = AppointmentSeries("old", details, authorId = "b", createdMillis = millis(today, 8), cutoff = details.date)
        val exception = AppointmentException("old", details.date, null, 2, "b", millis(today, 10))
        val entries = history(series = listOf(old), exceptions = listOf(exception))
        assertEquals(2, entries.size)
        assertTrue(entries.all { it.legacy })
        assertEquals(listOf(HistoryKind.APPOINTMENT_CANCELLED, HistoryKind.APPOINTMENT_ADDED), entries.map { it.kind })
        assertEquals("Mama", entries.first().authorName)
        assertTrue(history(series = listOf(old.copy(createdMillis = 0))).isEmpty())
    }
    @Test fun splitDoesNotDuplicateNewSeriesAndMineIncludesCreditedParent() {
        val split = AppointmentSeries("new", details, createdMillis = millis(today))
        val change = AppointmentActivity("split", "old", details.date, AppointmentActivityKind.CHANGED,
            true, details, "new", "a", "Papa", millis(today))
        assertEquals(1, history(series = listOf(split), activity = listOf(change)).size)
        val done = CareEvent("done", "missing", today, EventKind.COMPLETE, "b", "Mama", millis(today),
            completedById = "a", completedByName = "Papa")
        assertEquals(1, history(events = listOf(done), mine = true).size)
        assertTrue(history(events = listOf(done.copy(completedById = "b")), mine = true).isEmpty())
    }
    @Test fun equalTimestampsHaveStableOrdering() {
        val a = CareEvent("a", "missing", today, EventKind.COMPLETE, "a", "Papa", millis(today))
        assertEquals(history(events = listOf(a, a.copy(id = "b"))), history(events = listOf(a.copy(id = "b"), a)))
    }
}
