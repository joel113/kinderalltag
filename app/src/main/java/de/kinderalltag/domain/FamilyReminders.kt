package de.kinderalltag.domain

import java.time.Instant

fun appointmentRecipientName(recipient: AppointmentRecipient, sections: List<Child>): String =
    when (recipient) {
        AppointmentRecipient.FERDINAND ->
            sections.firstOrNull { it.id == "ferdinand" }?.name ?: "Ferdinand"
        AppointmentRecipient.JOHANN -> sections.firstOrNull { it.id == "johann" }?.name ?: "Johann"
        AppointmentRecipient.BOTH_CHILDREN -> "Beide Kinder"
        AppointmentRecipient.HOUSEHOLD -> "Haushalt"
    }

data class FamilyReminder(
    val id: String,
    val date: String,
    val title: String,
    val at: Instant,
    val expiresAt: Instant,
    val kind: String,
    val fingerprint: String = "",
)

fun AppointmentOccurrence.reminderFingerprint(): String = details.toString()

object FamilyReminders {
    fun plan(
        uid: String?,
        sections: List<Child>,
        revisions: List<TaskRevision>,
        events: List<CareEvent>,
        appointments: List<AppointmentSeries>,
        exceptions: List<AppointmentException>,
        now: Instant,
        pendingIds: Set<String> = emptySet(),
        limit: Int = 300,
        operations: List<ItemOperation> = emptyList(),
        activity: List<AppointmentActivity> = emptyList(),
    ): List<FamilyReminder> {
        if (uid == null) return emptyList()
        val today = now.atZone(FAMILY_ZONE).toLocalDate()
        val tasks =
            (0L..6L)
                .flatMap {
                    ManagedTasks.occurrences(revisions, events, operations, today.plusDays(it))
                }
                .filter {
                    (it.instant > now || it.id in pendingIds) &&
                        Schedule.activeCompletions(events, it.id).isEmpty()
                }
                .filter { isResponsible(it.revision.responsibleParentId, uid) }
                .map {
                    FamilyReminder(
                        it.id,
                        it.date.toString(),
                        "${sections.firstOrNull { child -> child.id == it.revision.childId }?.name ?: "Bereich"}: ${it.revision.title}",
                        it.instant,
                        it.date.plusDays(1).atStartOfDay(FAMILY_ZONE).toInstant(),
                        "task",
                    )
                }
        val appointmentsDue =
            ManagedAppointments.occurrences(
                    appointments,
                    exceptions,
                    activity,
                    operations,
                    today.minusDays(1),
                    today.plusDays(8),
                )
                .filter { isResponsible(it.details.responsibleParentId, uid) }
                .mapNotNull {
                    val at = it.details.reminderInstant ?: return@mapNotNull null
                    if (
                        (at <= now && it.id !in pendingIds) ||
                            at > now.plusSeconds(7 * 86400L) ||
                            it.details.endInstant <= now
                    )
                        return@mapNotNull null
                    FamilyReminder(
                        it.id,
                        it.details.date.toString(),
                        "${appointmentRecipientName(it.details.recipient, sections)}: ${it.details.title}",
                        at,
                        it.details.endInstant,
                        "appointment",
                        it.reminderFingerprint(),
                    )
                }
        return (tasks + appointmentsDue)
            .filter { it.expiresAt > now }
            .sortedWith(compareBy<FamilyReminder> { it.at }.thenBy { it.id })
            .take(limit)
    }
}
