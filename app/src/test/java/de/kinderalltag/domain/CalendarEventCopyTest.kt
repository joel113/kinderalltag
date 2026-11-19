package de.kinderalltag.domain

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class CalendarEventCopyTest {
    private val originalDate = LocalDate.of(2026, 11, 18)
    private val series =
        AppointmentSeries(
            "private-series",
            AppointmentDetails(
                "Kinderärztin",
                AppointmentRecipient.BOTH_CHILDREN,
                originalDate,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                location = "München",
                notes = "Impfausweis mitnehmen\nFragen notieren",
            ),
            AppointmentRepeat.WEEKS,
        )

    @Test
    fun movedOccurrenceUsesActualDetailsAndBerlinInstantsRatherThanSeriesAnchor() {
        val details =
            series.details.copy(
                title = "Neuer Termin",
                date = originalDate.plusDays(2),
                startTime = LocalTime.of(14, 30),
                endTime = LocalTime.of(15, 15),
            )
        val copy =
            calendarEventCopy(AppointmentOccurrence(series, originalDate, details), "Beide Kinder")
        assertEquals("Neuer Termin", copy.title)
        assertEquals(Instant.parse("2026-11-20T13:30:00Z").toEpochMilli(), copy.startMillis)
        assertEquals(Instant.parse("2026-11-20T14:15:00Z").toEpochMilli(), copy.endMillis)
        assertEquals("Europe/Berlin", copy.timeZoneId)
        assertFalse(copy.allDay)
    }

    @Test
    fun allDayCopyUsesUtcMidnightsAndExclusiveNextDateAcrossBothDstChanges() {
        for (date in listOf(LocalDate.of(2026, 3, 29), LocalDate.of(2026, 10, 25))) {
            val copy =
                calendarEventCopy(
                    AppointmentOccurrence(
                        series,
                        originalDate,
                        series.details.copy(date = date, allDay = true),
                    ),
                    "Beide Kinder",
                )
            assertTrue(copy.allDay)
            assertEquals("UTC", copy.timeZoneId)
            assertEquals(
                date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
                copy.startMillis,
            )
            assertEquals(
                date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
                copy.endMillis,
            )
            assertEquals(Duration.ofDays(1).toMillis(), copy.endMillis - copy.startMillis)
        }
    }

    @Test
    fun timedSpringGapUsesSameResolvedDurationAsAppointment() {
        val date = LocalDate.of(2026, 3, 29)
        val details =
            series.details.copy(
                date = date,
                startTime = LocalTime.of(2, 30),
                endTime = LocalTime.of(3, 0),
            )
        val copy = calendarEventCopy(AppointmentOccurrence(series, date, details), "Johann")
        assertEquals(details.startInstant.toEpochMilli(), copy.startMillis)
        assertEquals(details.endInstant.toEpochMilli(), copy.endMillis)
        assertTrue(copy.endMillis > copy.startMillis)
    }

    @Test
    fun descriptionRetainsNotesAndRecipientWithoutPrivateDatabaseIdentifiers() {
        val copy =
            calendarEventCopy(
                AppointmentOccurrence(series, originalDate, series.details),
                "Beide Kinder",
            )
        assertEquals("München", copy.location)
        assertEquals(
            "Kinderalltag · Beide Kinder\n\nImpfausweis mitnehmen\nFragen notieren",
            copy.description,
        )
        assertFalse(copy.description.contains(series.id))
        val noNotes =
            calendarEventCopy(
                AppointmentOccurrence(series, originalDate, series.details.copy(notes = "")),
                "Haushalt",
            )
        assertEquals("Kinderalltag · Haushalt", noNotes.description)
    }
}
