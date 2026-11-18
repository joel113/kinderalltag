package de.kinderalltag.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class CompletionNoticeTest {
    private val date = LocalDate.of(2026, 11, 18)
    private val task = TaskRevision("revision", "task", "johann", "Vitamin D", start = date, effective = date)
    private val occurrence = Occurrence(task, date, LocalTime.of(8, 0))
    private fun event(id: String = "complete", author: String = "other", target: String? = null, time: Long = 100) =
        CareEvent(id, occurrence.id, date, if (target == null) EventKind.COMPLETE else EventKind.RETRACT, author, author,
            time, targetId = target, completedById = "me", taskSnapshot = occurrence)
    private fun FamilyActivityNoticeTracker.observe(events: List<CareEvent>, ready: Boolean = true, uid: String = "me") =
        observe(uid, ready, listOf(task), emptyList(), events)
    @Test fun initialAndRestartedCompletionsAreSilent() {
        val tracker = FamilyActivityNoticeTracker()
        assertTrue(tracker.observe(listOf(event())).isEmpty())
        tracker.reset()
        assertTrue(tracker.observe(listOf(event())).isEmpty())
    }
    @Test fun otherActorNotCreditedParentDeterminesOneNoticeAndDeepLink() {
        val tracker = FamilyActivityNoticeTracker(); tracker.observe(emptyList())
        val notice = tracker.observe(listOf(event())).single()
        assertEquals(FamilyNoticeKind.TASK_COMPLETED, notice.kind)
        assertEquals(occurrence.id, notice.occurrenceId)
        assertEquals(date.toString(), notice.date)
        assertEquals("Vitamin D", notice.title)
        assertTrue(tracker.observe(listOf(event(), event("duplicate", time = 101))).isEmpty())
    }
    @Test fun ownCompletionCreditedToOtherParentRemainsSilent() {
        val tracker = FamilyActivityNoticeTracker(); tracker.observe(emptyList())
        assertTrue(tracker.observe(listOf(event(author = "me").copy(completedById = "other"))).isEmpty())
    }
    @Test fun correctionIsSilentButNextOpenToDoneTransitionCanNotify() {
        val tracker = FamilyActivityNoticeTracker(); tracker.observe(emptyList())
        val first = event(); tracker.observe(listOf(first))
        val undo = event("undo", target = first.id)
        assertTrue(tracker.observe(listOf(first, undo)).isEmpty())
        assertEquals(1, tracker.observe(listOf(first, undo, event("again", time = 200))).size)
    }
    @Test fun pendingAndCachedSnapshotsWaitWithoutReplayingConsumedEvents() {
        val tracker = FamilyActivityNoticeTracker(); tracker.observe(emptyList())
        assertTrue(tracker.observe(listOf(event().copy(pending = true)), ready = false).isEmpty())
        assertEquals(1, tracker.observe(listOf(event())).size)
        assertTrue(tracker.observe(listOf(event())).isEmpty())
        assertTrue(tracker.observe(listOf(event()), uid = "another-user").isEmpty())
    }
    @Test fun simultaneousOwnAndOtherConfirmationUseCanonicalOrder() {
        val tracker = FamilyActivityNoticeTracker(); tracker.observe(emptyList())
        assertTrue(tracker.observe(listOf(event("own", "me", time = 99), event())).isEmpty())
    }
}
