package de.kinderalltag.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import de.kinderalltag.domain.*
import de.kinderalltag.text.*
import java.time.LocalDate
import java.time.LocalTime

internal fun AppointmentDetails.encodeDetails(): Map<String, Any?> =
    mapOf(
        "title" to title.trim(),
        "recipient" to recipient.stored(),
        "date" to date.toString(),
        "startTime" to startTime.toString(),
        "endTime" to endTime.toString(),
        "allDay" to allDay,
        "responsibleParentId" to responsibleParentId,
        "location" to location.trim(),
        "notes" to notes.trim(),
        "reminderMinutes" to reminderMinutes,
    )

internal fun AppointmentSeries.encodeAppointment(uid: String): Map<String, Any?> =
    mapOf(
        "details" to details.encodeDetails(),
        "repeat" to repeat.name,
        "interval" to interval,
        "until" to until?.toString(),
        "cutoff" to cutoff?.toString(),
        "version" to version,
        "fromDate" to activeFrom.toString(),
        "rootId" to (rootId ?: id),
        "authorId" to uid,
        "createdAt" to FieldValue.serverTimestamp(),
    )

internal fun decodeDetails(data: Map<*, *>) =
    AppointmentDetails(
            title = data["title"] as String,
            recipient = AppointmentRecipient.fromStored(data["recipient"]),
            date = LocalDate.parse(data["date"] as String),
            startTime = LocalTime.parse(data["startTime"] as String),
            endTime = LocalTime.parse(data["endTime"] as String),
            allDay = data["allDay"] as Boolean,
            responsibleParentId = data["responsibleParentId"] as? String,
            location = data["location"] as String,
            notes = data["notes"] as String,
            reminderMinutes = (data["reminderMinutes"] as? Number)?.toInt(),
        )
        .also { it.validate() }

internal fun decodeAppointment(doc: DocumentSnapshot) =
    AppointmentSeries(
            id = doc.id,
            details = decodeDetails(doc.get("details") as Map<*, *>),
            repeat = AppointmentRepeat.valueOf(requireNotNull(doc.getString("repeat"))),
            interval = requireNotNull(doc.getLong("interval")).toInt(),
            until = doc.getString("until")?.let(LocalDate::parse),
            cutoff = doc.getString("cutoff")?.let(LocalDate::parse),
            version = requireNotNull(doc.getLong("version")),
            authorId = requireNotNull(doc.getString("authorId")),
            fromDate = doc.getString("fromDate")?.let(LocalDate::parse),
            createdMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0,
            rootId = doc.getString("rootId"),
        )
        .also { it.validate() }

internal fun decodeAppointmentException(doc: DocumentSnapshot) =
    AppointmentException(
        seriesId = requireNotNull(doc.getString("seriesId")),
        originalDate = LocalDate.parse(doc.getString("originalDate")),
        replacement = (doc.get("replacement") as? Map<*, *>)?.let(::decodeDetails),
        version = requireNotNull(doc.getLong("version")),
        authorId = doc.getString("authorId") ?: "",
        updatedMillis = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0,
    )

internal fun decodeAppointmentActivity(doc: DocumentSnapshot) =
    AppointmentActivity(
        id = doc.id,
        seriesId = requireNotNull(doc.getString("seriesId")),
        originalDate = LocalDate.parse(doc.getString("originalDate")),
        kind = AppointmentActivityKind.valueOf(requireNotNull(doc.getString("kind"))),
        following = doc.getBoolean("following") ?: false,
        details = decodeDetails(doc.get("details") as Map<*, *>),
        resultSeriesId = doc.getString("resultSeriesId"),
        authorId = requireNotNull(doc.getString("authorId")),
        authorName = doc.getString("authorName") ?: text(TextKey.PARENT),
        millis = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0,
    )

internal fun AppointmentActivity.encodeActivity(): Map<String, Any?> =
    mapOf(
        "seriesId" to seriesId,
        "originalDate" to originalDate.toString(),
        "kind" to kind.name,
        "following" to following,
        "details" to details.encodeDetails(),
        "resultSeriesId" to resultSeriesId,
        "authorId" to authorId,
        "authorName" to authorName,
        "createdAt" to FieldValue.serverTimestamp(),
    )
