package de.kinderalltag.ui

import de.kinderalltag.domain.FAMILY_ZONE
import java.text.DateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Presentation only: database values remain ISO dates and minute-precision local times. */
internal class DateTimeFormats(val locale: Locale, clockFormat: DateFormat) {
    private val dates = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    private val fullDates = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    private val clock =
        (clockFormat.clone() as DateFormat).apply { timeZone = TimeZone.getTimeZone(FAMILY_ZONE) }

    fun date(value: LocalDate): String = dates.format(value)

    fun fullDate(value: LocalDate): String = fullDates.format(value)

    fun time(value: LocalTime): String =
        clock.format(
            Date.from(LocalDate.of(2000, 1, 1).atTime(value).atZone(FAMILY_ZONE).toInstant())
        )

    fun timestamp(value: Instant): String =
        "${date(value.atZone(FAMILY_ZONE).toLocalDate())}, ${clock.format(Date.from(value))}"

    fun timeRange(start: LocalTime, end: LocalTime): String = "${time(start)}–${time(end)}"
}
