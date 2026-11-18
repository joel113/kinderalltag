package de.kinderalltag.data

import de.kinderalltag.domain.*
import java.time.*

fun FamilyState.taskOccurrences(date: LocalDate, includeDeleted: Boolean = false) =
    ManagedTasks.occurrences(revisions, events, itemOperations, date, includeDeleted)
fun FamilyState.appointmentOccurrences(from: LocalDate, to: LocalDate, includeDeleted: Boolean = false) =
    ManagedAppointments.occurrences(appointments, appointmentExceptions, appointmentActivity, itemOperations, from, to, includeDeleted)
fun FamilyState.rootId(occurrence: AppointmentOccurrence) = appointmentRootId(occurrence.sourceSeriesId, appointments, appointmentActivity)
fun FamilyState.version(type: ItemType, itemId: String) = itemOperations.filter { it.type == type && it.itemId == itemId }.maxOfOrNull { it.version } ?: 0L

fun FamilyState.futureTaskOccurrences(taskId: String, now: Instant) =
    ManagedTasks.futureOccurrences(revisions, events, itemOperations, taskId, now)
fun FamilyState.taskDefinitions(now: Instant, includePaused: Boolean = false) =
    ManagedTasks.definitions(revisions, events, itemOperations, now, includePaused)
