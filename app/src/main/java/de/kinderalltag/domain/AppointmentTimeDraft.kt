package de.kinderalltag.domain

import java.io.Serializable
import java.time.LocalTime

const val DEFAULT_APPOINTMENT_MINUTES = 60
const val MAX_DEFAULT_APPOINTMENT_MINUTES = 720

/** Appointments currently end on their start date; never wrap an automatic end into morning. */
fun defaultAppointmentEnd(start: LocalTime, minutes: Int): LocalTime {
    require(minutes in 1..MAX_DEFAULT_APPOINTMENT_MINUTES)
    val endMinute = (start.hour * 60 + start.minute + minutes).coerceAtMost(23 * 60 + 59)
    return LocalTime.of(endMinute / 60, endMinute % 60)
}

data class AppointmentTimeDraft(
    val start: LocalTime,
    val end: LocalTime,
    val endEdited: Boolean = false,
    val endAutomaticallySet: Boolean = false,
) : Serializable {
    fun selectStart(value: LocalTime, durationMinutes: Int): AppointmentTimeDraft =
        copy(
            start = value,
            end = if (endEdited) end else defaultAppointmentEnd(value, durationMinutes),
            endAutomaticallySet = !endEdited,
        )

    fun selectEnd(value: LocalTime): AppointmentTimeDraft =
        copy(end = value, endEdited = true, endAutomaticallySet = false)

    fun durationClipped(minutes: Int): Boolean =
        endAutomaticallySet && start.hour * 60 + start.minute + minutes > 23 * 60 + 59
}
