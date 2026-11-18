package de.kinderalltag.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FamilyActivityNoticeTrackerTest {
    private val date = LocalDate.of(2026, 11, 18)
    private fun revision(id: String = "r1", task: String = "task1", author: String = "other") =
        TaskRevision(id, task, "johann", "Vitamin D", start = date, effective = date, authorId = author, createdMillis = 100)
    private fun appointment(id: String = "added", author: String = "other", kind: AppointmentActivityKind = AppointmentActivityKind.ADDED) =
        AppointmentActivity(id, "series1", date, kind, false,
            AppointmentDetails("Kinderarzt", AppointmentRecipient.BOTH_CHILDREN, date), null, author, "Mama", 100)
    private fun FamilyActivityNoticeTracker.observe(revisions: List<TaskRevision> = emptyList(),
        appointments: List<AppointmentActivity> = emptyList(), ready: Boolean = true, uid: String = "me") =
        observe(uid, ready, revisions, appointments)

    @Test fun initialServerSnapshotDoesNotReplayExistingData() {
        val tracker = FamilyActivityNoticeTracker()
        assertTrue(tracker.observe(listOf(revision()), listOf(appointment())).isEmpty())
        assertTrue(tracker.observe(listOf(revision()), listOf(appointment())).isEmpty())
    }
    @Test fun cachedOrIncompleteSnapshotDoesNotBecomeTheBaseline() {
        val tracker = FamilyActivityNoticeTracker()
        assertTrue(tracker.observe(ready = false).isEmpty())
        assertTrue(tracker.observe(listOf(revision()), listOf(appointment())).isEmpty())
        assertTrue(tracker.observe(listOf(revision()), listOf(appointment())).isEmpty())
    }
    @Test fun otherParentAdditionsNotifyOnceAndOwnAdditionsRemainSilent() {
        val tracker = FamilyActivityNoticeTracker()
        tracker.observe()
        val revisions = listOf(revision(), revision("own", "own", "me"))
        val appointments = listOf(appointment(), appointment("own", "me"))
        val notices = tracker.observe(revisions, appointments)
        assertEquals(listOf(FamilyNoticeKind.NEW_TASK, FamilyNoticeKind.NEW_APPOINTMENT), notices.map { it.kind })
        assertEquals(listOf("Vitamin D", "Kinderarzt"), notices.map { it.title })
        assertTrue(notices.all { it.authorId == "other" })
        assertTrue(tracker.observe(revisions, appointments).isEmpty())
    }
    @Test fun editsPausesResumesAndSeriesSplitsAreNotAdditions() {
        val tracker = FamilyActivityNoticeTracker()
        val task = revision()
        tracker.observe(listOf(task), listOf(appointment()))
        val pause = task.copy(id = "pause", active = false, createdMillis = 101)
        val resume = task.copy(id = "resume", createdMillis = 102)
        val split = appointment("split", kind = AppointmentActivityKind.CHANGED).copy(following = true, resultSeriesId = "new-series")
        assertTrue(tracker.observe(listOf(task, pause, resume), listOf(appointment(), split,
            appointment("cancel", kind = AppointmentActivityKind.CANCELLED))).isEmpty())
    }
    @Test fun multipleRevisionsOfNewTaskProduceOneCreationNotice() {
        val tracker = FamilyActivityNoticeTracker()
        tracker.observe()
        val first = revision()
        val later = first.copy(id = "edit", createdMillis = 200, authorId = "me", title = "Edited")
        val notice = tracker.observe(listOf(later, first)).single()
        assertEquals("Vitamin D", notice.title)
        assertEquals("other", notice.authorId)
    }
    @Test fun accountChangesAndExplicitResetEstablishFreshBaselines() {
        val tracker = FamilyActivityNoticeTracker()
        tracker.observe()
        assertTrue(tracker.observe(listOf(revision()), listOf(appointment()), uid = "another-account").isEmpty())
        tracker.reset()
        assertTrue(tracker.observe(listOf(revision()), listOf(appointment())).isEmpty())
    }
    @Test fun disabledDeliveryDoesNotQueueAReplay() {
        val tracker = FamilyActivityNoticeTracker()
        tracker.observe()
        // The Android delivery layer discards notices while their preference is off.
        tracker.observe(listOf(revision()), listOf(appointment()))
        assertTrue(tracker.observe(listOf(revision()), listOf(appointment())).isEmpty())
    }
    @Test fun transientOfflineOrPendingStateDefersDeliveryUntilServerConfirmation() {
        val tracker = FamilyActivityNoticeTracker()
        tracker.observe()
        assertTrue(tracker.observe(listOf(revision()), listOf(appointment()), ready = false).isEmpty())
        assertEquals(2, tracker.observe(listOf(revision()), listOf(appointment())).size)
        assertTrue(tracker.observe(listOf(revision()), listOf(appointment())).isEmpty())
    }
    @Test fun temporaryMissingSnapshotsDoNotForgetSeenIds() {
        val tracker = FamilyActivityNoticeTracker()
        tracker.observe(listOf(revision()), listOf(appointment()))
        tracker.observe()
        assertTrue(tracker.observe(listOf(revision()), listOf(appointment())).isEmpty())
    }
    @Test fun earlierOwnCreationEditedByPartnerDoesNotBecomeNewTaskNotification() {
        val tracker = FamilyActivityNoticeTracker()
        tracker.observe()
        val own = revision(author = "me")
        val edit = own.copy(id = "edit", authorId = "other", createdMillis = 200)
        assertTrue(tracker.observe(listOf(edit, own)).isEmpty())
    }
}
