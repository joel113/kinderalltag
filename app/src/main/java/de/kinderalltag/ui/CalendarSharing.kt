package de.kinderalltag.ui

import android.content.ClipData
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import de.kinderalltag.domain.AppointmentOccurrence
import de.kinderalltag.domain.CalendarExchange
import java.io.File
import java.util.UUID

internal fun shareAppointment(context: Context, appointment: AppointmentOccurrence, recipientLabel: String) {
    try {
        val directory = File(context.cacheDir, "calendar-share").apply { mkdirs() }
        directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 7 * 86400000L }?.forEach { it.delete() }
        val file = File(directory, "termin-${UUID.randomUUID()}.ics")
        file.writeText(CalendarExchange.export(appointment, recipientLabel = recipientLabel))
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).setType("text/calendar")
            .putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_SUBJECT, appointment.details.title)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newUri(context.contentResolver, "Termin", uri)
        val chooser = Intent.createChooser(send, "Termin teilen")
        if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (_: Exception) { Toast.makeText(context, "Termin konnte nicht geteilt werden.", Toast.LENGTH_LONG).show() }
}
