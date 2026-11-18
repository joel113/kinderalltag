package de.kinderalltag.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class AppointmentTest {
    private val day = LocalDate.of(2026, 11, 16)
    private fun details(date: LocalDate = day) = AppointmentDetails("Kinderarzt", AppointmentRecipient.BOTH_CHILDREN, date)
    private fun series(repeat: AppointmentRepeat = AppointmentRepeat.ONCE) = AppointmentSeries("series", details(), repeat)

    @Test fun oneOffAndInclusiveUntil() {
        assertEquals(listOf(day), series().datesBetween(day.minusDays(1), day.plusYears(1)))
        val daily = series(AppointmentRepeat.DAYS).copy(interval = 2, until = day.plusDays(4))
        assertEquals(listOf(day, day.plusDays(2), day.plusDays(4)), daily.datesBetween(day, day.plusDays(9)))
        assertTrue(daily.datesBetween(day.plusDays(5), day.plusDays(10)).isEmpty())
    }
    @Test fun weeklyIntervalsKeepTheirAnchor() {
        val weekly = series(AppointmentRepeat.WEEKS).copy(interval = 2)
        assertEquals(listOf(day, day.plusDays(14), day.plusDays(28)), weekly.datesBetween(day, day.plusDays(30)))
        assertFalse(weekly.contains(day.plusDays(7)))
    }
    @Test fun monthEndsClampWithoutDrifting() {
        val monthly = series(AppointmentRepeat.MONTHS).copy(details = details(LocalDate.of(2027, 1, 31)))
        assertEquals(listOf(LocalDate.of(2027, 1, 31), LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 31)),
            monthly.datesBetween(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 3, 31)))
        assertTrue(monthly.contains(LocalDate.of(2027, 2, 28)))
        assertFalse(monthly.contains(LocalDate.of(2027, 3, 28)))
    }
    @Test fun yearlyLeapDatesReturnToFebruary29() {
        val yearly = series(AppointmentRepeat.YEARS).copy(details = details(LocalDate.of(2024, 2, 29)))
        assertEquals(LocalDate.of(2025, 2, 28), yearly.dateAt(1))
        assertEquals(LocalDate.of(2028, 2, 29), yearly.dateAt(4))
    }
    @Test fun changingFutureMetadataKeepsTheOriginalMonthEndAnchor() {
        val jan31 = LocalDate.of(2027, 1, 31)
        val feb28 = LocalDate.of(2027, 2, 28)
        val replacement = AppointmentSeries("replacement", details(jan31).copy(title = "Neuer Titel"),
            AppointmentRepeat.MONTHS, fromDate = feb28)
        replacement.validate()
        assertEquals(listOf(feb28, LocalDate.of(2027, 3, 31)), replacement.datesBetween(jan31, LocalDate.of(2027, 3, 31)))
        assertFalse(replacement.contains(jan31))
    }
    @Test fun longRunningDailySeriesCanBeQueriedInASmallWindow() {
        val daily = series(AppointmentRepeat.DAYS).copy(details = details(LocalDate.of(2000, 1, 1)))
        assertEquals(listOf(day, day.plusDays(1)), daily.datesBetween(day, day.plusDays(1)))
    }
    @Test fun bothChildrenMatchIndividuallyButProduceOneOccurrence() {
        val occurrences = AppointmentSchedule.occurrences(listOf(series()), emptyList(), day, day)
        assertEquals(1, occurrences.size)
        assertTrue(occurrences.single().details.recipient.matches("ferdinand"))
        assertTrue(occurrences.single().details.recipient.matches("johann"))
        assertFalse(occurrences.single().details.recipient.matches("haushalt"))
        assertTrue(AppointmentRecipient.HOUSEHOLD.matches("haushalt"))
        assertFalse(AppointmentRecipient.FERDINAND.matches("johann"))
    }
    @Test fun movedOccurrencesAreFoundOutsideTheirOriginalWindowAndKeepIdentity() {
        val moved = AppointmentException("series", day, details(day.plusMonths(3)))
        assertTrue(AppointmentSchedule.occurrences(listOf(series()), listOf(moved), day, day).isEmpty())
        val occurrence = AppointmentSchedule.occurrences(listOf(series()), listOf(moved), moved.replacement!!.date, moved.replacement.date).single()
        assertEquals("appointment_series_$day", occurrence.id)
        assertEquals(day, occurrence.originalDate)
    }
    @Test fun cancellationAffectsOnlyOneOccurrence() {
        val daily = series(AppointmentRepeat.DAYS)
        val canceled = AppointmentException("series", day.plusDays(1), null)
        val occurrences = AppointmentSchedule.occurrences(listOf(daily), listOf(canceled), day, day.plusDays(2))
        assertEquals(listOf(day, day.plusDays(2)), occurrences.map { it.originalDate })
    }
    @Test fun splitPreservesHistoryAndDiscardsFutureExceptions() {
        val original = series(AppointmentRepeat.DAYS).copy(cutoff = day.plusDays(2), version = 1)
        val replacement = series(AppointmentRepeat.WEEKS).copy(id = "replacement", details = details(day.plusDays(3)))
        val exceptions = listOf(AppointmentException("series", day.plusDays(2), details(day.plusDays(4))))
        val occurrences = AppointmentSchedule.occurrences(listOf(original, replacement), exceptions, day, day.plusDays(5))
        assertEquals(listOf(day, day.plusDays(1), day.plusDays(3)), occurrences.map { it.details.date })
        assertTrue(original.contains(day.plusDays(1)))
        assertFalse(original.contains(day.plusDays(2)))
    }
    @Test fun invalidOriginalDateCannotInjectAnException() {
        val weekly = series(AppointmentRepeat.WEEKS)
        val invalid = AppointmentException("series", day.plusDays(1), details(day.plusDays(2)))
        assertTrue(AppointmentSchedule.occurrences(listOf(weekly), listOf(invalid), day.plusDays(2), day.plusDays(2)).isEmpty())
    }
    @Test fun splittingCannotEraseAnOccurrenceMovedIntoHistory() {
        val daily = series(AppointmentRepeat.DAYS)
        val now = day.atTime(12, 0).atZone(FAMILY_ZONE).toInstant()
        val past = AppointmentException(daily.id, day.plusDays(5), details(day.minusDays(1)))
        assertFalse(AppointmentSchedule.canSplit(daily, day.plusDays(1), listOf(past), now))
        assertTrue(AppointmentSchedule.canSplit(daily, day.plusDays(6), listOf(past), now))
        val movedFuture = past.copy(replacement = details(day.plusDays(2)))
        assertTrue(AppointmentSchedule.canSplit(daily, day.plusDays(1), listOf(movedFuture), now))
    }
    @Test fun notificationLookupFindsTheCurrentMovedDateAndRejectsCanceledOrClosedSeries() {
        val item = series()
        val id = "appointment_series_$day"
        val moved = AppointmentException(item.id, day, details(day.plusMonths(1)))
        assertEquals(day.plusMonths(1), AppointmentSchedule.find(id, listOf(item), listOf(moved))!!.details.date)
        assertNull(AppointmentSchedule.find(id, listOf(item), listOf(moved.copy(replacement = null))))
        assertNull(AppointmentSchedule.find(id, listOf(item.copy(cutoff = day)), emptyList()))
    }
    @Test fun daylightSavingAndAllDayDurations() {
        val spring = details(LocalDate.of(2026, 3, 29)).copy(startTime = LocalTime.of(2, 30), endTime = LocalTime.of(3, 0))
        assertEquals(Instant.parse("2026-03-29T01:30:00Z"), spring.startInstant)
        assertTrue(spring.endInstant > spring.startInstant)
        val allDay = spring.copy(allDay = true, reminderMinutes = 1440)
        assertEquals(23, Duration.between(allDay.startInstant, allDay.endInstant).toHours())
        val autumn = allDay.copy(date = LocalDate.of(2026, 10, 25))
        assertEquals(25, Duration.between(autumn.startInstant, autumn.endInstant).toHours())
    }
    @Test fun invalidTimesIntervalsAndRemindersAreRejected() {
        val invalid = listOf(series().copy(interval = 0), series().copy(until = day.minusDays(1)),
            series().copy(details = details().copy(endTime = LocalTime.of(8, 0))),
            series().copy(details = details().copy(reminderMinutes = -10)),
            series().copy(details = details().copy(allDay = true, reminderMinutes = 30)))
        invalid.forEach { assertTrue(runCatching { it.validate() }.isFailure) }
    }
}
