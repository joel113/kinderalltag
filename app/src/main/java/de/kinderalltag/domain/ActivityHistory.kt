package de.kinderalltag.domain

import java.time.LocalDate

enum class AppointmentActivityKind { ADDED, CHANGED, CANCELLED }
data class AppointmentActivity(
    val id: String,
    val seriesId: String,
    val originalDate: LocalDate,
    val kind: AppointmentActivityKind,
    val following: Boolean,
    val details: AppointmentDetails,
    val resultSeriesId: String?,
    val authorId: String,
    val authorName: String,
    val millis: Long,
)

enum class HistoryKind {
    SHOPPING_ADDED, SHOPPING_CHANGED, SHOPPING_BOUGHT, SHOPPING_REOPENED, SHOPPING_DELETED, SHOPPING_RESTORED, GIFT_ADDED, GIFT_CHANGED, GIFT_BOUGHT, GIFT_REOPENED, GIFT_DELETED, GIFT_RESTORED,
    TASK_COMPLETED, TASK_CORRECTED, TASK_UPDATED,
    APPOINTMENT_ADDED, APPOINTMENT_CHANGED, APPOINTMENT_CANCELLED,
    TASK_DELETED, TASK_RESTORED, APPOINTMENT_DELETED, APPOINTMENT_RESTORED,
}

data class HistoryEntry(
    val id: String,
    val millis: Long,
    val action: String,
    val title: String,
    val authorId: String,
    val authorName: String,
    val childId: String? = null,
    val recipient: AppointmentRecipient? = null,
    val creditedId: String? = null,
    val creditedName: String? = null,
    val subjectDate: LocalDate? = null,
    val pending: Boolean = false,
    val legacy: Boolean = false,
    val kind: HistoryKind,
    val operationScope: OperationScope? = null,
    val listKind: FamilyListKind? = null,
    val labels: List<String> = emptyList(),
)

data class HistoryPeriod(val from: LocalDate, val to: LocalDate)
fun historyPeriod(today: LocalDate, page: Long): HistoryPeriod {
    require(page >= 0)
    val to = today.minusDays(page * 90)
    return HistoryPeriod(to.minusDays(89), to)
}

fun activityHistory(revisions: List<TaskRevision>, events: List<CareEvent>, series: List<AppointmentSeries>,
    exceptions: List<AppointmentException>, activity: List<AppointmentActivity>, parents: List<Parent>,
    period: HistoryPeriod, sectionId: String? = null, onlyMine: Boolean = false, uid: String? = null, operations: List<ItemOperation> = emptyList(), listActivity: List<ListActivity> = emptyList()): List<HistoryEntry> {
    fun name(id: String) = parents.firstOrNull { it.id == id }?.name ?: "Elternteil"
    val entries = mutableListOf<HistoryEntry>()
    events.forEach { event ->
        val task = event.taskSnapshot?.revision ?: revisions.firstOrNull { revision ->
            revision.times.any { time -> Occurrence(revision, event.date, time).id == event.occurrenceId }
        }
        entries += HistoryEntry("task-event:${event.id}", if (event.clientMillis > 0) event.clientMillis else event.serverMillis ?: 0,
            if (event.kind == EventKind.COMPLETE) "Aufgabe erledigt" else "Bestätigung zurückgenommen",
            task?.title ?: "Aufgabe", event.authorId, event.authorName, childId = task?.childId,
            creditedId = event.completedById, creditedName = event.completedByName,
            subjectDate = event.date, pending = event.pending,
            kind = if (event.kind == EventKind.COMPLETE) HistoryKind.TASK_COMPLETED else HistoryKind.TASK_CORRECTED)
    }
    revisions.groupBy { it.taskId }.values.forEach { versions ->
        val ordered = versions.sortedWith(compareBy<TaskRevision> { it.createdMillis }.thenBy { it.effective }.thenBy { it.id })
        ordered.forEachIndexed { index, revision ->
            if (revision.createdMillis > 0) entries += HistoryEntry("task-revision:${revision.id}", revision.createdMillis,
                when {
                    !revision.active -> "Aufgabe pausiert"
                    index == 0 -> "Aufgabe angelegt"
                    !ordered[index - 1].active -> "Aufgabe fortgesetzt"
                    else -> "Aufgabe geändert"
                }, revision.title, revision.authorId, name(revision.authorId), childId = revision.childId,
                subjectDate = revision.effective, kind = HistoryKind.TASK_UPDATED)
        }
    }
    activity.forEach { event ->
        entries += HistoryEntry("appointment-event:${event.id}", event.millis,
            when (event.kind) {
                AppointmentActivityKind.ADDED -> "Termin angelegt"
                AppointmentActivityKind.CHANGED -> if (event.following) "Termin und folgende geändert" else "Termin geändert"
                AppointmentActivityKind.CANCELLED -> if (event.following) "Termin und folgende abgesagt" else "Termin abgesagt"
            }, event.details.title, event.authorId, event.authorName, recipient = event.details.recipient,
            subjectDate = event.details.date, kind = when (event.kind) {
                AppointmentActivityKind.ADDED -> HistoryKind.APPOINTMENT_ADDED
                AppointmentActivityKind.CHANGED -> HistoryKind.APPOINTMENT_CHANGED
                AppointmentActivityKind.CANCELLED -> HistoryKind.APPOINTMENT_CANCELLED
            })
    }
    // Reconstruct only timestamps actually retained by older app versions.
    series.filter { item -> item.createdMillis > 0 && activity.none {
        (it.seriesId == item.id && it.kind == AppointmentActivityKind.ADDED) || it.resultSeriesId == item.id
    } }.forEach { item ->
        entries += HistoryEntry("legacy-series:${item.id}", item.createdMillis, "Termin angelegt",
            item.details.title, item.authorId, name(item.authorId), recipient = item.details.recipient,
            subjectDate = item.activeFrom, legacy = true, kind = HistoryKind.APPOINTMENT_ADDED)
    }
    exceptions.filter { exception -> exception.updatedMillis > 0 && activity.none {
        it.seriesId == exception.seriesId && it.originalDate == exception.originalDate && it.kind != AppointmentActivityKind.ADDED
    } }.forEach { exception ->
        val original = series.firstOrNull { it.id == exception.seriesId }
        val details = exception.replacement ?: original?.details
        entries += HistoryEntry("legacy-exception:${exception.id}", exception.updatedMillis,
            if (exception.replacement == null) "Termin abgesagt" else "Termin geändert",
            details?.title ?: "Termin", exception.authorId, name(exception.authorId), recipient = details?.recipient,
            subjectDate = exception.replacement?.date ?: exception.originalDate, legacy = true,
            kind = if (exception.replacement == null) HistoryKind.APPOINTMENT_CANCELLED else HistoryKind.APPOINTMENT_CHANGED)
    }
    operations.forEach { operation ->
        val task = operation.type == ItemType.TASK
        val kind = when (operation.action) {
            OperationAction.EDIT -> if (task) HistoryKind.TASK_UPDATED else HistoryKind.APPOINTMENT_CHANGED
            OperationAction.DELETE -> if (task) HistoryKind.TASK_DELETED else HistoryKind.APPOINTMENT_DELETED
            OperationAction.RESTORE -> if (task) HistoryKind.TASK_RESTORED else HistoryKind.APPOINTMENT_RESTORED
        }
        val action = (if (task) "Aufgabe" else "Termin") + when (operation.action) {
            OperationAction.EDIT -> if (task && operation.task?.active == false) " pausiert" else " geändert"
            OperationAction.DELETE -> " gelöscht"
            OperationAction.RESTORE -> " wiederhergestellt"
        }
        entries += HistoryEntry("operation:${operation.id}", operation.createdMillis, action, operation.title,
            operation.authorId, operation.authorName, childId = operation.childId, recipient = operation.recipient,
            subjectDate = operation.actualDate, kind = kind, operationScope = operation.scope)
    }
    listActivity.forEach { event ->
        val item = event.snapshot
        val prefix = if (item.kind == FamilyListKind.SHOPPING) "Einkauf" else "Geschenkidee"
        val action = when (event.action) {
            ListAction.ADDED -> "angelegt"
            ListAction.CHANGED -> "geändert"
            ListAction.BOUGHT -> "gekauft"
            ListAction.REOPENED -> "wieder geöffnet"
            ListAction.DELETED -> "gelöscht"
            ListAction.RESTORED -> "wiederhergestellt"
        }
        entries += HistoryEntry("list-event:${event.id}", event.millis, "$prefix $action", item.title,
            event.authorId, event.authorName, recipient = item.recipient, pending = event.pending,
            kind = HistoryKind.valueOf("${if (item.kind == FamilyListKind.SHOPPING) "SHOPPING" else "GIFT"}_${event.action.name}"),
            listKind = item.kind, labels = event.labels.map { it.name })
    }
    val start = period.from.atStartOfDay(FAMILY_ZONE).toInstant().toEpochMilli()
    val end = period.to.plusDays(1).atStartOfDay(FAMILY_ZONE).toInstant().toEpochMilli()
    return entries.filter { entry ->
        entry.millis in start until end &&
            (sectionId == null || entry.childId == sectionId || entry.recipient?.matches(sectionId) == true ||
                (sectionId == "haushalt" && entry.listKind == FamilyListKind.SHOPPING)) &&
            (!onlyMine || (uid != null && (entry.authorId == uid || entry.creditedId == uid)))
    }.sortedWith(compareByDescending<HistoryEntry> { it.millis }.thenBy { it.id })
}
