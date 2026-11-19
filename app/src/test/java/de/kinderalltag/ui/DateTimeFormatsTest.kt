package de.kinderalltag.ui

import java.text.SimpleDateFormat
import java.time.*
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class DateTimeFormatsTest {
    @Test
    fun germanAndUsDatesUseTheirRegionalOrder() {
        val date = LocalDate.of(2026, 11, 16)
        val german = DateTimeFormats(Locale.GERMANY, SimpleDateFormat("HH:mm", Locale.GERMANY))
        val us = DateTimeFormats(Locale.US, SimpleDateFormat("h:mm a", Locale.US))
        assertEquals("16.11.2026", german.date(date))
        assertEquals("Nov 16, 2026", us.date(date))
        assertTrue(german.fullDate(date).contains("Montag"))
        assertTrue(us.fullDate(date).contains("Monday"))
    }

    @Test
    fun suppliedClockPreferenceOverridesLocaleDefaults() {
        val time = LocalTime.of(13, 5)
        val clock24 = DateTimeFormats(Locale.US, SimpleDateFormat("HH:mm", Locale.US))
        val clock12 = DateTimeFormats(Locale.US, SimpleDateFormat("h:mm a", Locale.US))
        assertEquals("13:05", clock24.time(time))
        assertEquals("1:05 PM", clock12.time(time))
        assertEquals("12:00 AM", clock12.time(LocalTime.MIDNIGHT))
        assertEquals("12:00 PM", clock12.time(LocalTime.NOON))
    }

    @Test
    fun displayedFamilyTimesDoNotShiftToThePhonesTimezone() {
        val clock =
            SimpleDateFormat("HH:mm", Locale.GERMANY).apply {
                timeZone = TimeZone.getTimeZone("America/Los_Angeles")
            }
        val formats = DateTimeFormats(Locale.GERMANY, clock)
        assertEquals("08:15", formats.time(LocalTime.of(8, 15)))
        assertEquals("16.11.2026, 00:30", formats.timestamp(Instant.parse("2026-11-15T23:30:00Z")))
    }
}
