package de.kinderalltag.data

import org.junit.Assert.*
import org.junit.Test

class SyncStatusTest {
    @Test fun permissionErrorsGroupAffectedCollectionsAndExplainRulesUpdate() {
        val message = syncErrorSummary(listOf(SyncIssue("members", "PERMISSION_DENIED"),
            SyncIssue("appointmentSeries", "PERMISSION_DENIED"), SyncIssue("appointmentExceptions", "PERMISSION_DENIED")))!!
        assertTrue(message.contains("Elternliste, Termine, Terminänderungen"))
        assertTrue(message.contains("Firestore-Regeln veröffentlichen"))
        assertFalse(message.contains("Keine Serververbindung"))
        assertEquals(1, message.lines().size)
    }
    @Test fun connectionDataAndAuthenticationProblemsHaveDifferentRemedies() {
        assertTrue(syncErrorSummary(listOf(SyncIssue("revisions", "UNAVAILABLE")))!!.contains("Gespeicherte Daten bleiben verfügbar"))
        assertTrue(syncErrorSummary(listOf(SyncIssue("events", "INVALID_DATA")))!!.contains("Datenformat prüfen"))
        assertTrue(syncErrorSummary(listOf(SyncIssue("membership", "UNAUTHENTICATED")))!!.contains("erneut anmelden"))
        assertNull(syncErrorSummary(emptyList()))
    }
    @Test fun mixedAndUnknownFailuresRetainDiagnosticDetails() {
        val message = syncErrorSummary(listOf(SyncIssue("revisions", "UNAVAILABLE"), SyncIssue("events", "UNKNOWN")))!!
        assertTrue(message.contains("Aufgaben")); assertTrue(message.contains("Bestätigungen")); assertTrue(message.contains("UNKNOWN"))
    }
}
