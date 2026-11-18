package de.kinderalltag.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import de.kinderalltag.domain.*
import java.time.*

internal fun TaskRevision.snapshotMap(): Map<String, Any?> = mapOf(
    "id" to id, "taskId" to taskId, "childId" to childId, "title" to title, "notes" to notes,
    "frequency" to frequency.name, "weekdays" to weekdays.map { it.value }, "times" to times.map { it.toString() },
    "start" to start.toString(), "end" to end?.toString(), "effective" to effective.toString(), "active" to active,
    "createdMillis" to createdMillis, "authorId" to authorId, "responsibleParentId" to responsibleParentId, "slotOrigins" to slotOrigins)
internal fun taskSnapshot(data: Map<*, *>): TaskRevision = TaskRevision(
    id = data["id"] as String, taskId = data["taskId"] as String, childId = data["childId"] as String,
    title = data["title"] as String, notes = data["notes"] as String,
    frequency = Frequency.valueOf(data["frequency"] as String), weekdays = (data["weekdays"] as List<*>).map { DayOfWeek.of((it as Number).toInt()) }.toSet(),
    times = (data["times"] as List<*>).map { LocalTime.parse(it as String) }, start = LocalDate.parse(data["start"] as String),
    end = (data["end"] as? String)?.let(LocalDate::parse), effective = LocalDate.parse(data["effective"] as String),
    active = data["active"] as Boolean, createdMillis = (data["createdMillis"] as Number).toLong(), authorId = data["authorId"] as String,
    responsibleParentId = data["responsibleParentId"] as? String, slotOrigins = (data["slotOrigins"] as List<*>).map { it as String },
).also { it.validate() }
internal fun Occurrence.snapshotMap(): Map<String, Any?> = mapOf("revision" to revision.snapshotMap(),
    "date" to date.toString(), "time" to time.toString(), "id" to id, "originalDate" to originalDate.toString(),
    "originalTime" to originalTime.toString(), "sourceId" to sourceRevisionId)
internal fun occurrenceSnapshot(data: Map<*, *>) = Occurrence(taskSnapshot(data["revision"] as Map<*, *>),
    LocalDate.parse(data["date"] as String), LocalTime.parse(data["time"] as String), data["id"] as String,
    LocalDate.parse(data["originalDate"] as String), LocalTime.parse(data["originalTime"] as String), data["sourceId"] as String)
private fun AppointmentSeries.snapshotMap(): Map<String, Any?> = mapOf("id" to id, "details" to details.encodeDetails(),
    "repeat" to repeat.name, "interval" to interval, "until" to until?.toString(), "fromDate" to activeFrom.toString())
private fun appointmentSnapshot(data: Map<*, *>) = AppointmentSeries(data["id"] as String,
    decodeDetails(data["details"] as Map<*, *>), AppointmentRepeat.valueOf(data["repeat"] as String),
    (data["interval"] as Number).toInt(), (data["until"] as? String)?.let(LocalDate::parse),
    fromDate = LocalDate.parse(data["fromDate"] as String)).also { it.validate() }
internal fun ItemOperation.encodeOperation(): Map<String, Any?> = mapOf(
    "type" to type.name, "itemId" to itemId, "action" to action.name, "scope" to scope.name,
    "anchorId" to anchorId, "anchorDate" to anchorDate.toString(), "anchorTime" to anchorTime.toString(), "sourceId" to sourceId,
    "title" to title, "actualDate" to actualDate.toString(),
    "actualStartAt" to com.google.firebase.Timestamp(java.util.Date(actualStartMillis)),
    "replacementStartAt" to replacementStartMillis?.let { com.google.firebase.Timestamp(java.util.Date(it)) }, "childId" to childId, "recipient" to recipient?.name, "task" to task?.snapshotMap(),
    "appointment" to appointment?.snapshotMap(), "restoresId" to restoresId, "createdAt" to FieldValue.serverTimestamp(),
    "authorId" to authorId, "authorName" to authorName, "version" to version)
internal fun decodeItemOperation(doc: DocumentSnapshot) = ItemOperation(
    id = doc.id, type = ItemType.valueOf(requireNotNull(doc.getString("type"))), itemId = requireNotNull(doc.getString("itemId")),
    action = OperationAction.valueOf(requireNotNull(doc.getString("action"))), scope = OperationScope.valueOf(requireNotNull(doc.getString("scope"))),
    anchorId = requireNotNull(doc.getString("anchorId")), anchorDate = LocalDate.parse(doc.getString("anchorDate")),
    anchorTime = LocalTime.parse(doc.getString("anchorTime")), sourceId = requireNotNull(doc.getString("sourceId")), title = requireNotNull(doc.getString("title")),
    actualDate = LocalDate.parse(doc.getString("actualDate")),
    actualStartMillis = doc.getTimestamp("actualStartAt")?.toDate()?.time ?: 0,
    replacementStartMillis = doc.getTimestamp("replacementStartAt")?.toDate()?.time, childId = doc.getString("childId"), recipient = doc.getString("recipient")?.let(AppointmentRecipient::valueOf),
    task = (doc.get("task") as? Map<*, *>)?.let(::taskSnapshot)?.copy(
        createdMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0, authorId = doc.getString("authorId") ?: ""), appointment = (doc.get("appointment") as? Map<*, *>)?.let(::appointmentSnapshot),
    restoresId = doc.getString("restoresId"), createdMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0,
    authorId = requireNotNull(doc.getString("authorId")), authorName = requireNotNull(doc.getString("authorName")), version = requireNotNull(doc.getLong("version")))
