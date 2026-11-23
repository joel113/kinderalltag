package de.kinderalltag.domain

import de.kinderalltag.text.*
import java.time.ZoneOffset

/** One resolved occurrence, copied into a calendar editor without identifiers or recurrence. */
data class CalendarEventCopy(
    val title: String,
    val location: String,
    val description: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val timeZoneId: String,
)

fun calendarEventCopy(
    occurrence: AppointmentOccurrence,
    recipientLabel: String,
): CalendarEventCopy {
    val details = occurrence.details
    // Calendar all-day dates are UTC midnight boundaries, independent of Berlin's DST offset.
    val start =
        if (details.allDay) details.date.atStartOfDay().toInstant(ZoneOffset.UTC)
        else details.startInstant
    val end =
        if (details.allDay) details.date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        else details.endInstant
    return CalendarEventCopy(
        details.title,
        details.location,
        listOf(text(TextKey.KINDERALLTAG, recipientLabel), details.notes)
            .filter { it.isNotBlank() }
            .joinToString("\n\n"),
        start.toEpochMilli(),
        end.toEpochMilli(),
        details.allDay,
        if (details.allDay) "UTC" else FAMILY_ZONE.id,
    )
}
