package de.kinderalltag.domain

import java.time.*

val FAMILY_ZONE: ZoneId = ZoneId.of("Europe/Berlin")
enum class Frequency { ONCE, DAILY, WEEKDAYS }

enum class BearColor(val argb: Long) {
    BLUE(0xFF166FA6), ROSE(0xFFBF4658), GREEN(0xFF4C8061),
    HONEY(0xFFD5A038), BROWN(0xFF825B46), PURPLE(0xFF8060A0);

    companion object {
        fun fromStored(value: String?, childId: String): BearColor =
            entries.firstOrNull { it.name == value } ?: if (childId == "ferdinand") BLUE else ROSE
    }
}

data class Child(val id: String, val name: String,
    val bearColor: BearColor = BearColor.fromStored(null, id))
val HOUSEHOLD_SECTION = Child("haushalt", "Haushalt")
data class Parent(val id: String, val name: String)
fun isResponsible(assignedParentId: String?, uid: String?): Boolean =
    uid != null && (assignedParentId == null || assignedParentId == uid)

data class TaskRevision(
    val id: String,
    val taskId: String,
    val childId: String,
    val title: String,
    val notes: String = "",
    val frequency: Frequency = Frequency.DAILY,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val times: List<LocalTime> = listOf(LocalTime.of(8, 0)),
    val start: LocalDate,
    val end: LocalDate? = null,
    val effective: LocalDate,
    val active: Boolean = true,
    val createdMillis: Long = 0,
    val authorId: String = "",
    val responsibleParentId: String? = null,
    val slotOrigins: List<String> = emptyList(),
) {
    fun validate() {
        require(taskId.isNotBlank() && childId.isNotBlank()) { "Bitte ein Kind auswählen." }
        require(title.isNotBlank() && title.length <= 120) { "Titel: 1 bis 120 Zeichen." }
        require(notes.length <= 2000) { "Notizen: höchstens 2000 Zeichen." }
        require(end == null || end >= start) { "Das Enddatum liegt vor dem Startdatum." }
        require(times.isNotEmpty() && times.size <= 8 && times.distinct().size == times.size) {
            "Bitte 1 bis 8 unterschiedliche Uhrzeiten angeben."
        }
        require(times.all { it.second == 0 && it.nano == 0 }) { "Uhrzeiten bitte minutengenau angeben." }
        require(frequency != Frequency.WEEKDAYS || weekdays.isNotEmpty()) { "Bitte Wochentage auswählen." }
    }
    fun isDue(date: LocalDate): Boolean = active && date >= start && (end == null || date <= end) &&
        when (frequency) {
            Frequency.ONCE -> date == start
            Frequency.DAILY -> true
            Frequency.WEEKDAYS -> date.dayOfWeek in weekdays
        }
}

data class Occurrence(val revision: TaskRevision, val date: LocalDate, val time: LocalTime,
    val stableId: String? = null, val originalDate: LocalDate = date, val originalTime: LocalTime = time,
    val sourceRevisionId: String = revision.id) {
    val id: String get() = stableId ?: "${revision.taskId}_${revision.id}_${date}_${time.toSecondOfDay()}"
    // java.time resolves spring gaps forward and autumn overlaps to the earlier offset.
    val instant: Instant get() = date.atTime(time).atZone(FAMILY_ZONE).toInstant()
}

enum class EventKind { COMPLETE, RETRACT }
data class CareEvent(
    val id: String,
    val occurrenceId: String,
    val date: LocalDate,
    val kind: EventKind,
    val authorId: String,
    val authorName: String,
    val clientMillis: Long,
    val serverMillis: Long? = null,
    val targetId: String? = null,
    val pending: Boolean = false,
    val completedById: String? = null,
    val completedByName: String? = null,
    val taskSnapshot: Occurrence? = null,
)

object Schedule {
    fun reminders(revisions: List<TaskRevision>, events: List<CareEvent>, now: Instant,
        pendingIds: Set<String> = emptySet(), limit: Int = 300): List<Occurrence> {
        val today = now.atZone(FAMILY_ZONE).toLocalDate()
        val byOccurrence = events.groupBy { it.occurrenceId }
        return (0L..6L).flatMap { occurrences(revisions, today.plusDays(it)) }
            .filter { (it.instant > now || it.id in pendingIds) &&
                activeCompletions(byOccurrence[it.id].orEmpty(), it.id).isEmpty() }
            .sortedBy { it.instant }.take(limit)
    }

    fun revisionsOn(revisions: List<TaskRevision>, date: LocalDate): List<TaskRevision> =
        revisions.filter { it.effective <= date }.groupBy { it.taskId }.values.map { group ->
            group.maxWith(compareBy<TaskRevision> { it.effective }.thenBy { it.createdMillis }.thenBy { it.id })
        }

    fun occurrences(revisions: List<TaskRevision>, date: LocalDate): List<Occurrence> =
        revisionsOn(revisions, date).filter { it.isDue(date) }
            .flatMap { revision -> revision.times.map { Occurrence(revision, date, it) } }
            .sortedWith(compareBy<Occurrence> { it.time }.thenBy { it.revision.title })

    fun upcomingTasks(revisions: List<TaskRevision>, today: LocalDate): List<TaskRevision> =
        revisionsOn(revisions, today.plusDays(1)).filter { task ->
            val first = maxOf(today, task.start, task.effective)
            task.active && when (task.frequency) {
                Frequency.ONCE -> task.start >= first && task.isDue(task.start)
                Frequency.DAILY -> task.isDue(first)
                Frequency.WEEKDAYS -> (0L..6L).any { task.isDue(first.plusDays(it)) }
            }
        }

    fun activeCompletions(events: List<CareEvent>, occurrenceId: String): List<CareEvent> {
        val relevant = events.filter { it.occurrenceId == occurrenceId }
        val retracted = relevant.filter { it.kind == EventKind.RETRACT }.mapNotNull { it.targetId }.toSet()
        return relevant.filter { it.kind == EventKind.COMPLETE && it.id !in retracted }
            .sortedWith(compareBy<CareEvent> { it.serverMillis ?: it.clientMillis }.thenBy { it.id })
    }
}

// Clamp stale pages after filtering or a synchronized task change.
data class TaskPage(val tasks: List<TaskRevision>, val index: Long, val total: Int) {
    val first: Int get() = if (total == 0) 0 else (index * 90).toInt() + 1
    val last: Int get() = if (total == 0) 0 else first + tasks.size - 1
    val hasNext: Boolean get() = last < total
}

fun taskPage(tasks: List<TaskRevision>, requestedPage: Long): TaskPage {
    val lastPage = if (tasks.isEmpty()) 0L else (tasks.size - 1).toLong() / 90
    val page = requestedPage.coerceIn(0L, lastPage)
    return TaskPage(tasks.drop((page * 90).toInt()).take(90), page, tasks.size)
}
