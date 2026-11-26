package de.kinderalltag.domain

import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class AppointmentTimeDraftTest {
    private val initial = AppointmentTimeDraft(LocalTime.of(9, 0), LocalTime.of(10, 0))

    @Test
    fun startChangesKeepFollowingTheConfiguredDurationBeforeManualEndSelection() {
        val first = initial.selectStart(LocalTime.of(11, 15), 45)
        assertEquals(LocalTime.NOON, first.end)
        assertEquals(LocalTime.of(14, 45), first.selectStart(LocalTime.of(14, 0), 45).end)
    }

    @Test
    fun manualEndSelectionIsPreservedForSubsequentStartChanges() {
        val edited = initial.selectEnd(LocalTime.of(15, 30)).selectStart(LocalTime.of(12, 0), 30)
        assertEquals(LocalTime.of(15, 30), edited.end)
        assertEquals(LocalTime.of(15, 30), edited.selectStart(LocalTime.of(14, 0), 60).end)
    }

    @Test
    fun confirmingTheExistingEndAlsoDisablesAutomaticUpdates() {
        assertEquals(
            LocalTime.of(10, 0),
            initial.selectEnd(initial.end).selectStart(LocalTime.of(11, 0), 60).end,
        )
    }

    @Test
    fun openingExistingTimesDoesNotChangeThemButSelectingAStartUsesTheDefault() {
        val existing = AppointmentTimeDraft(LocalTime.of(8, 0), LocalTime.of(12, 0))
        assertEquals(LocalTime.NOON, existing.end)
        assertEquals(LocalTime.of(9, 15), existing.selectStart(LocalTime.of(8, 45), 30).end)
        assertEquals(
            LocalTime.of(10, 0),
            defaultAppointmentEnd(LocalTime.of(9, 0), DEFAULT_APPOINTMENT_MINUTES),
        )
    }

    @Test
    fun lateSuggestionsNeverWrapIntoAnEarlierTimeOfDay() {
        val late = initial.selectStart(LocalTime.of(23, 45), 60)
        assertEquals(LocalTime.of(23, 59), late.end)
        assertTrue(late.durationClipped(60))
        assertFalse(late.selectEnd(LocalTime.of(23, 50)).durationClipped(60))
        val exact = initial.selectStart(LocalTime.of(22, 59), 60)
        assertEquals(LocalTime.of(23, 59), exact.end)
        assertFalse(exact.durationClipped(60))
    }

    @Test
    fun supportedDurationBoundsAreAppliedAndInvalidDurationsAreRejected() {
        assertEquals(LocalTime.of(9, 1), defaultAppointmentEnd(LocalTime.of(9, 0), 1))
        assertEquals(LocalTime.of(21, 0), defaultAppointmentEnd(LocalTime.of(9, 0), 720))
        listOf(0, -1, 721).forEach { duration ->
            assertThrows(IllegalArgumentException::class.java) {
                defaultAppointmentEnd(LocalTime.of(9, 0), duration)
            }
        }
    }
}
