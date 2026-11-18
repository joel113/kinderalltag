package de.kinderalltag.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class CalendarExchangeTest {
    private val now = Instant.parse("2026-11-16T00:00:00Z")
    private fun calendar(vararg fields: String) = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nBEGIN:VEVENT\r\n${fields.joinToString("\r\n")}\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"
    private fun parse(vararg fields: String) = CalendarExchange.parse(calendar(*fields), now)

    @Test fun exportsAndImportsAnOccurrenceWithUnicodeAndEscapedText() {
        val details = AppointmentDetails("Arzt, Bär; Test\\", AppointmentRecipient.BOTH_CHILDREN, LocalDate.of(2027, 1, 20),
            responsibleParentId = "private-parent-id", location = "Straße 1, Berlin", notes = "Zeile 1\nZeile 2; wichtig")
        val occurrence = AppointmentOccurrence(AppointmentSeries("series", details), details.date, details)
        val exported = CalendarExchange.export(occurrence, now, "Beide Kinder")
        assertTrue(exported.contains("CATEGORIES:Beide Kinder"))
        assertFalse(exported.contains("private-parent-id"))
        assertFalse(exported.contains("RRULE:"))
        assertTrue(exported.contains("DTSTART:20270120T080000Z"))
        val imported = CalendarExchange.parse(exported, now).appointments.single().series.details
        assertEquals(details.title, imported.title)
        assertEquals(details.date, imported.date)
        assertEquals(details.startTime, imported.startTime)
        assertEquals(details.endTime, imported.endTime)
        assertEquals(details.location, imported.location)
        assertEquals(details.notes, imported.notes)
        assertEquals(AppointmentRecipient.HOUSEHOLD, imported.recipient)
        assertNull(imported.responsibleParentId)
    }
    @Test fun foldsUtf8WithoutBreakingCharactersAndUnfoldsCorrectly() {
        val title = "Bär 🐻 ".repeat(12)
        val details = AppointmentDetails(title, AppointmentRecipient.JOHANN, LocalDate.of(2027, 1, 20))
        val text = CalendarExchange.export(AppointmentOccurrence(AppointmentSeries("s", details), details.date, details), now)
        assertTrue(text.split("\r\n").all { it.toByteArray(Charsets.UTF_8).size <= 75 })
        assertEquals(title, CalendarExchange.parse(text, now).appointments.single().series.details.title)
    }
    @Test fun allDayDatesUseExclusiveEndAndRejectMultipleDays() {
        val imported = parse("DTSTART;VALUE=DATE:20270120", "DTEND;VALUE=DATE:20270121", "SUMMARY:Ferientag")
        assertTrue(imported.appointments.single().series.details.allDay)
        val invalid = parse("DTSTART;VALUE=DATE:20270120", "DTEND;VALUE=DATE:20270123")
        assertTrue(invalid.appointments.isEmpty()); assertTrue(invalid.errors.isNotEmpty())
        val duration = parse("DTSTART;VALUE=DATE:20270120", "DURATION:PT25H")
        assertTrue(duration.appointments.isEmpty())
    }
    @Test fun convertsUtcAndNamedTimezonesButKeepsFloatingTimesInFamilyZone() {
        val utc = parse("DTSTART:20270120T080000Z", "DTEND:20270120T090000Z").appointments.single().series.details
        val berlin = parse("DTSTART;TZID=Europe/Berlin:20270120T090000", "DTEND;TZID=Europe/Berlin:20270120T100000").appointments.single().series.details
        val floating = parse("DTSTART:20270120T090000", "DTEND:20270120T100000").appointments.single().series.details
        val foreign = parse("DTSTART;TZID=America/New_York:20270120T030000", "DTEND;TZID=America/New_York:20270120T040000").appointments.single().series.details
        assertEquals(utc.startInstant, berlin.startInstant)
        assertEquals(utc.startInstant, floating.startInstant)
        assertEquals(utc.startInstant, foreign.startInstant)
    }
    @Test fun supportsDurationsAndWarnsWhenEndTimeWasMissing() {
        val duration = parse("DTSTART:20270120T090000", "DURATION:PT90M").appointments.single()
        assertEquals(LocalTime.of(10, 30), duration.series.details.endTime)
        assertTrue(duration.warnings.isEmpty())
        val missing = parse("DTSTART:20270120T090000").appointments.single()
        assertTrue(missing.warnings.isNotEmpty())
    }
    @Test fun importsIntervalsCountsAndEndDates() {
        val weekly = parse("DTSTART:20270120T090000", "DTEND:20270120T100000", "RRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=WE;COUNT=3").appointments.single().series
        assertEquals(AppointmentRepeat.WEEKS, weekly.repeat)
        assertEquals(2, weekly.interval)
        assertEquals(LocalDate.of(2027, 2, 17), weekly.until)
        val bounded = parse("DTSTART:20270120T090000", "DTEND:20270120T100000", "RRULE:FREQ=DAILY;UNTIL=20270122T070000Z").appointments.single().series
        assertEquals(LocalDate.of(2027, 1, 21), bounded.until)
    }
    @Test fun importsAnOngoingSeriesAtItsNextFutureOccurrenceWithoutLosingItsAnchor() {
        val imported = parse("DTSTART:20260131T090000", "DTEND:20260131T100000", "RRULE:FREQ=MONTHLY").appointments.single()
        assertEquals(LocalDate.of(2026, 1, 31), imported.series.details.date)
        assertEquals(LocalDate.of(2026, 11, 30), imported.series.activeFrom)
        assertTrue(imported.warnings.isNotEmpty())
    }
    @Test fun unsupportedRulesAreReportedInsteadOfSilentlyChanged() {
        val unsupported = parse("DTSTART:20270120T090000", "DTEND:20270120T100000", "RRULE:FREQ=WEEKLY;BYDAY=MO,WE")
        assertTrue(unsupported.appointments.isEmpty()); assertTrue(unsupported.errors.isNotEmpty())
        val exceptions = parse("DTSTART:20270120T090000", "DTEND:20270120T100000", "EXDATE:20270127T090000")
        assertTrue(exceptions.appointments.isEmpty())
        val unknown = parse("DTSTART;TZID=Unknown/Zone:20270120T090000", "DTEND:20270120T100000")
        assertTrue(unknown.appointments.isEmpty())
    }
    @Test fun ignoresNestedAlarmDescriptionAndReadsOnlySupportedTrigger() {
        val imported = parse("DTSTART:20270120T090000", "DTEND:20270120T100000", "DESCRIPTION:Eigene Notiz",
            "BEGIN:VALARM", "TRIGGER:-PT15M", "DESCRIPTION:Alarmtext", "END:VALARM").appointments.single()
        assertEquals("Eigene Notiz", imported.series.details.notes)
        assertEquals(15, imported.series.details.reminderMinutes)
    }
    @Test fun canceledInvalidAndOversizedFilesAreNotAccepted() {
        assertTrue(parse("DTSTART:20270120T090000", "DTEND:20270120T100000", "STATUS:CANCELLED").appointments.isEmpty())
        assertTrue(parse("DTSTART:20270230T090000").appointments.isEmpty())
        assertTrue(runCatching { CalendarExchange.parse("not a calendar", now) }.isFailure)
        assertTrue(runCatching { CalendarExchange.parse("x".repeat(CalendarExchange.MAX_BYTES + 1), now) }.isFailure)
        val tooMany = (0..CalendarExchange.MAX_EVENTS).joinToString("\r\n") { calendar("DTSTART:20270120T090000", "DTEND:20270120T100000") }
        assertTrue(runCatching { CalendarExchange.parse(tooMany, now) }.isFailure)
    }
    @Test fun multipleFilesKeepValidEventsWhileReportingUnsupportedOnes() {
        val source = calendar("DTSTART:20270120T090000", "DTEND:20270120T100000", "SUMMARY:Eins") +
            calendar("DTSTART:20270121T090000", "DTEND:20270121T100000", "SUMMARY:Zwei") +
            calendar("DTSTART:20270122T090000", "RRULE:FREQ=HOURLY")
        val result = CalendarExchange.parse(source, now)
        assertEquals(listOf("Eins", "Zwei"), result.appointments.map { it.series.details.title })
        assertEquals(1, result.errors.size)
    }
    @Test fun exactDuplicatesInSharedFilesAreOnlyQueuedOnce() {
        val source = calendar("DTSTART:20270120T090000", "DTEND:20270120T100000", "SUMMARY:Gleicher Termin")
        assertEquals(1, CalendarExchange.parse(source + source, now).appointments.size)
    }
}
