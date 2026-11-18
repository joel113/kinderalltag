package de.kinderalltag.domain

enum class FamilyNoticeKind { NEW_TASK, NEW_APPOINTMENT }
data class FamilyActivityNotice(val id: String, val kind: FamilyNoticeKind, val title: String, val authorId: String)

// A process-local baseline prevents initial loads, restarts and account changes from replaying history.
class FamilyActivityNoticeTracker {
    private var uid: String? = null
    private var initialized = false
    private val taskIds = mutableSetOf<String>()
    private val appointmentEventIds = mutableSetOf<String>()

    fun reset() {
        uid = null
        initialized = false
        taskIds.clear()
        appointmentEventIds.clear()
    }

    fun observe(currentUid: String, ready: Boolean, revisions: List<TaskRevision>,
        appointmentActivity: List<AppointmentActivity>): List<FamilyActivityNotice> {
        if (uid != currentUid) { reset(); uid = currentUid }
        if (!ready) return emptyList()
        val currentTaskIds = revisions.map { it.taskId }.toSet()
        val addedAppointments = appointmentActivity.filter { it.kind == AppointmentActivityKind.ADDED }
        if (!initialized) {
            taskIds.addAll(currentTaskIds)
            appointmentEventIds.addAll(addedAppointments.map { it.id })
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
        // Consume all observations even when delivery is disabled, so switching on does not replay them.
        taskIds.addAll(currentTaskIds)
        appointmentEventIds.addAll(addedAppointments.map { it.id })
        return notices
    }
}
