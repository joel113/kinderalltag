package de.kinderalltag.data

import org.junit.Assert.*
import org.junit.Test

class CareWriteErrorTest {
    @Test
    fun permissionRejectionExplainsTheSharedAccessChecks() {
        val text = careWriteError("PERMISSION_DENIED")
        assertTrue(text.contains("Firebase-Projekt"))
        assertTrue(text.contains("App-Check"))
        assertTrue(text.contains("Familienzugang"))
        assertFalse(text.contains("Verbindung"))
    }

    @Test
    fun expiredAuthenticationAndTransportFailuresHaveDifferentRecoveryInstructions() {
        assertTrue(careWriteError("UNAUTHENTICATED").contains("erneut anmelden"))
        assertTrue(careWriteError("UNAVAILABLE").contains("Verbindung"))
        assertEquals(careWriteError("UNAVAILABLE"), careWriteError("DEADLINE_EXCEEDED"))
        assertTrue(careWriteError(null).contains("erneut versuchen"))
    }
}
