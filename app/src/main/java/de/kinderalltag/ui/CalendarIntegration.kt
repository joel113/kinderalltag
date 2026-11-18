package de.kinderalltag.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import de.kinderalltag.domain.AppointmentOccurrence
import de.kinderalltag.domain.calendarEventCopy

internal fun saveAppointmentToCalendar(context: Context, appointment: AppointmentOccurrence, recipientLabel: String) {
    val copy = calendarEventCopy(appointment, recipientLabel)
    val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
        putExtra(CalendarContract.Events.TITLE, copy.title)
        putExtra(CalendarContract.Events.EVENT_LOCATION, copy.location)
        putExtra(CalendarContract.Events.DESCRIPTION, copy.description)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, copy.startMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, copy.endMillis)
        putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, copy.allDay)
        putExtra(CalendarContract.Events.EVENT_TIMEZONE, copy.timeZoneId)
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    // Launch directly: querying first would need package-visibility declarations on newer Android.
    // The calendar editor owns saving, calendar/account choice and reminders. No provider access is needed.
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Keine passende Kalender-App gefunden. Du kannst den Termin stattdessen über „Teilen“ weitergeben.", Toast.LENGTH_LONG).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, "Die Kalender-App konnte nicht geöffnet werden. Bitte versuche es über „Teilen“.", Toast.LENGTH_LONG).show()
    }
}
