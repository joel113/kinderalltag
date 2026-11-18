package de.kinderalltag.domain

enum class FamilyNoticeKind { NEW_TASK, NEW_APPOINTMENT, TASK_COMPLETED }
data class FamilyActivityNotice(val id: String, val kind: FamilyNoticeKind, val title: String, val authorId: String, val occurrenceId: String? = null, val date: String? = null)

// A process-local baseline prevents initial loads, restarts and account changes from replaying history.
class FamilyActivityNoticeTracker {
    private var uid: String? = null
    private var initialized = false
    private val taskIds = mutableSetOf<String>()
    private val appointmentEventIds = mutableSetOf<String>()
    private val completedIds = mutableSetOf<String>()

    fun reset() {
        uid = null
        initialized = false
        taskIds.clear()
        appointmentEventIds.clear()
        completedIds.clear()
    }

    fun observe(currentUid: String, ready: Boolean, revisions: List<TaskRevision>,
        appointmentActivity: List<AppointmentActivity>, events: List<CareEvent> = emptyList()): List<FamilyActivityNotice> {
        if (uid != currentUid) { reset(); uid = currentUid }
        if (!ready) return emptyList()
        val currentTaskIds = revisions.map { it.taskId }.toSet()
        val addedAppointments = appointmentActivity.filter { it.kind == AppointmentActivityKind.ADDED }
        val completions = events.groupBy { it.occurrenceId }.mapNotNull { (id, group) ->
            Schedule.activeCompletions(group, id).minWithOrNull(compareBy<CareEvent> {
                if (it.clientMillis > 0) it.clientMillis else it.serverMillis ?: 0
            }.thenBy { it.id })
        }
        if (!initialized) {
            taskIds.addAll(currentTaskIds)
            appointmentEventIds.addAll(addedAppointments.map { it.id })
            completedIds.addAll(completions.map { it.occurrenceId })
            initialized = true
            return emptyList()
        }
        val notices = mutableListOf<FamilyActivityNotice>()
        revisions.filter { it.taskId !in taskIds }.groupBy { it.taskId }.values.forEach { versions ->
            val original = versions.minWith(compareBy<TaskRevision> { it.createdMillis }.thenBy { it.effective }.thenBy { it.id })
            if (original.active && original.authorId.isNotBlank() && original.authorId != currentUid) {
                notices += FamilyActivityNotice("task:${original.taskId}", FamilyNoticeKind.NEW_TASK, original.title, original.authorId)
            }
        }
        addedAppointments.filter { it.id !in appointmentEventIds && it.authorId != currentUid }.forEach { event ->
            notices += FamilyActivityNotice("appointment:${event.id}", FamilyNoticeKind.NEW_APPOINTMENT, event.details.title, event.authorId)
        }
        completions.filter { it.occurrenceId !in completedIds && it.authorId != currentUid }.forEach { event ->
            val title = event.taskSnapshot?.revision?.title ?: revisions.firstOrNull { revision ->
                revision.times.any { Occurrence(revision, event.date, it).id == event.occurrenceId }
            }?.title ?: "Aufgabe"
            notices += FamilyActivityNotice("complete:${event.occurrenceId}", FamilyNoticeKind.TASK_COMPLETED, title,
                event.authorId, event.occurrenceId, (event.taskSnapshot?.date ?: event.date).toString())
        }
        completedIds.clear()
        completedIds.addAll(completions.map { it.occurrenceId })
        // Consume all observations even when delivery is disabled, so switching on does not replay them.
        taskIds.addAll(currentTaskIds)
        appointmentEventIds.addAll(addedAppointments.map { it.id })
        return notices
    }
}
