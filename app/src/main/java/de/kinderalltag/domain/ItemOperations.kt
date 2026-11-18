package de.kinderalltag.domain

import java.time.*

enum class ItemType { TASK, APPOINTMENT }
enum class OperationAction { EDIT, DELETE, RESTORE }
enum class OperationScope { SINGLE, FOLLOWING, ALL }
data class ItemOperation(
    val id: String, val type: ItemType, val itemId: String, val action: OperationAction,
    val scope: OperationScope, val anchorId: String, val anchorDate: LocalDate, val anchorTime: LocalTime,
    val sourceId: String, val title: String, val actualDate: LocalDate = anchorDate, val actualStartMillis: Long = 0, val replacementStartMillis: Long? = null, val childId: String? = null,
    val recipient: AppointmentRecipient? = null, val task: TaskRevision? = null,
    val appointment: AppointmentSeries? = null, val restoresId: String? = null,
    val createdMillis: Long = 0, val authorId: String = "", val authorName: String = "Elternteil", val version: Long = 0,
) {
    val key: String get() = "${type.name}_$itemId"
    val anchorInstant: Instant get() = anchorDate.atTime(anchorTime).atZone(FAMILY_ZONE).toInstant()
}

fun Occurrence.slotOrigin(): String = id.removePrefix("${revision.taskId}_")
    .removeSuffix("_${originalDate}_${originalTime.toSecondOfDay()}") + "|${originalTime.toSecondOfDay()}"

fun activeDeletions(operations: List<ItemOperation>): List<ItemOperation> {
    return operations.filter { deletion -> deletion.action == OperationAction.DELETE && operations.none {
        it.action == OperationAction.RESTORE && it.restoresId == deletion.id && it.key == deletion.key
    } }
}
fun ItemOperation.covers(type: ItemType, itemId: String, id: String, originalDate: LocalDate, originalTime: LocalTime): Boolean =
    this.type == type && this.itemId == itemId && when (scope) {
        OperationScope.ALL -> true
        OperationScope.SINGLE -> anchorId == id
        OperationScope.FOLLOWING -> originalDate.atTime(originalTime).atZone(FAMILY_ZONE).toInstant() >= anchorInstant
    }
fun deletionsOverlap(a: ItemOperation, b: ItemOperation): Boolean {
    if (a.key != b.key) return false
    if (a.scope == OperationScope.ALL || b.scope == OperationScope.ALL) return true
    if (a.scope == OperationScope.SINGLE) return b.covers(a.type, a.itemId, a.anchorId, a.anchorDate, a.anchorTime)
    if (b.scope == OperationScope.SINGLE) return a.covers(b.type, b.itemId, b.anchorId, b.anchorDate, b.anchorTime)
    return true
}

fun appointmentRootId(id: String, series: List<AppointmentSeries>, activity: List<AppointmentActivity>): String {
    val parents = activity.mapNotNull { it.resultSeriesId?.let { child -> child to it.seriesId } }.toMap()
    var current = series.firstOrNull { it.id == id }?.rootId ?: id
    val seen = mutableSetOf<String>()
    while (seen.add(current)) current = parents[current] ?: return current
    return seen.minOrNull() ?: id
}

object ManagedTasks {
    private fun frozen(events: List<CareEvent>, revisions: List<TaskRevision>): List<Occurrence> =
        events.groupBy { it.occurrenceId }.values.mapNotNull { group ->
            val event = Schedule.activeCompletions(group, group.first().occurrenceId)
                .minWithOrNull(compareBy<CareEvent> { if (it.clientMillis > 0) it.clientMillis else it.serverMillis ?: 0 }.thenBy { it.id }) ?: return@mapNotNull null
            event.taskSnapshot ?: revisions.firstNotNullOfOrNull { revision -> revision.times.firstNotNullOfOrNull { time ->
                Occurrence(revision, event.date, time).takeIf { it.id == event.occurrenceId }
            } }
        }

    fun occurrences(revisions: List<TaskRevision>, events: List<CareEvent>, operations: List<ItemOperation>, date: LocalDate,
        includeDeleted: Boolean = false): List<Occurrence> {
        val edits = operations.filter { it.type == ItemType.TASK && it.action == OperationAction.EDIT }
            .sortedWith(compareBy<ItemOperation> { if (it.scope == OperationScope.SINGLE) 1 else 0 }
                .thenBy { it.createdMillis }.thenBy { it.version }.thenBy { it.id })
        val library = revisions + edits.mapNotNull { it.task }
        val pins = frozen(events, library)
        val pinIds = pins.map { it.id }.toSet()
        val result = Schedule.occurrences(revisions, date).associateBy { it.id }.toMutableMap()
        val templates = Schedule.revisionsOn(revisions, date).associateBy { it.taskId }.toMutableMap()
        // Legacy future-effective versions may have changed an occurrence ID already confirmed offline.
        // Match their slots by unchanged times first, retaining the original confirmation's identity.
        pins.filter { it.date == date }.forEach { pin ->
            if (pin.originalDate == date && pin.id !in result) {
                val template = templates[pin.revision.taskId]
                val origin = pin.slotOrigin()
                val source = library.firstOrNull { it.id == origin.substringBeforeLast('|') }
                if (template != null && source != null) {
                    val mapping = mapSlots(source, template)
                    val index = mapping.slotOrigins.indexOf(origin)
                    if (index >= 0) {
                        val time = template.times[index]
                        val alias = result.values.firstOrNull { it.revision.taskId == pin.revision.taskId && it.time == time && it.originalDate == date }
                        if (alias != null && alias.id !in pinIds) {
                            result.remove(alias.id)
                            val origins = template.times.mapIndexed { i, t -> template.slotOrigins.getOrNull(i) ?: "${template.id}|${t.toSecondOfDay()}" }.toMutableList()
                            origins[index] = origin
                            templates[template.taskId] = template.copy(slotOrigins = origins)
                        }
                    }
                }
            }
            result[pin.id] = pin
        }
        edits.forEach { edit ->
            val draft = edit.task ?: return@forEach
            val created = Instant.ofEpochMilli(edit.createdMillis)
            if (edit.scope == OperationScope.SINGLE) {
                val current = result[edit.anchorId]
                if (edit.anchorId !in pinIds && (edit.actualStartMillis > edit.createdMillis || (edit.actualStartMillis == 0L && (current == null || current.instant > created)))) {
                    result.remove(edit.anchorId)
                    val time = draft.times.singleOrNull() ?: return@forEach
                    val replacement = Occurrence(draft, draft.start, time, edit.anchorId, edit.anchorDate, edit.anchorTime, edit.sourceId)
                    if (replacement.date == date && replacement.instant > created) result[replacement.id] = replacement
                }
            } else {
                val relevant = result.values.filter { it.revision.taskId == edit.itemId &&
                    (it.originalDate.atTime(it.originalTime).atZone(FAMILY_ZONE).toInstant() >= edit.anchorInstant) }
                val protected = relevant.filter { it.id in pinIds || it.instant <= created }.map { it.id }.toSet()
                relevant.filter { it.id !in protected }.forEach { result.remove(it.id) }
                val mapped = remapForDate(draft, templates[draft.taskId], library)
                if (date >= edit.anchorDate) templates[draft.taskId] = mapped
                if (mapped.isDue(date)) mapped.times.forEachIndexed { index, time ->
                    val origin = mapped.slotOrigins[index]
                    val source = origin.substringBeforeLast('|')
                    val originalTime = LocalTime.ofSecondOfDay(origin.substringAfterLast('|').toLong())
                    val id = "${draft.taskId}_${source}_${date}_${originalTime.toSecondOfDay()}"
                    val replacement = Occurrence(mapped, date, time, id, date, originalTime, edit.sourceId)
                    if (replacement.originalDate.atTime(originalTime).atZone(FAMILY_ZONE).toInstant() >= edit.anchorInstant &&
                        replacement.instant > created && id !in protected && id !in pinIds) result[id] = replacement
                }
            }
        }
        // A moved, completed occurrence must stay at its actual day, even if its series has since changed.
        pinIds.forEach { result.remove(it) }
        pins.filter { it.date == date }.forEach { result[it.id] = it }
        val deleted = if (includeDeleted) emptyList() else activeDeletions(operations)
        return result.values.filter { occurrence -> deleted.none {
            it.covers(ItemType.TASK, occurrence.revision.taskId, occurrence.id, occurrence.originalDate, occurrence.originalTime)
        } }.sortedWith(compareBy<Occurrence> { it.time }.thenBy { it.revision.title }.thenBy { it.id })
    }

    private fun remapForDate(draft: TaskRevision, template: TaskRevision?, library: List<TaskRevision>): TaskRevision {
        if (template == null) return draft.copy(slotOrigins = draft.times.mapIndexed { i, time ->
            draft.slotOrigins.getOrNull(i) ?: "${draft.id}|${time.toSecondOfDay()}"
        })
        val available = template.times.indices.toMutableSet()
        val origins = template.times.mapIndexed { i, time -> template.slotOrigins.getOrNull(i) ?: "${template.id}|${time.toSecondOfDay()}" }
        val wanted = draft.times.mapIndexed { i, time -> draft.slotOrigins.getOrNull(i) ?: "${draft.id}|${time.toSecondOfDay()}" }
        val assigned = mutableMapOf<Int, String>()
        fun assign(index: Int, candidate: Int?) {
            if (candidate != null && candidate in available) { assigned[index] = origins[candidate]; available.remove(candidate) }
        }
        wanted.indices.forEach { index -> assign(index, available.firstOrNull { origins[it] == wanted[index] }) }
        wanted.indices.filter { it !in assigned && wanted[it].substringBeforeLast('|') != draft.id }.forEach { index ->
            assign(index, available.firstOrNull { origins[it].substringAfterLast('|') == wanted[index].substringAfterLast('|') })
        }
        wanted.indices.filter { it !in assigned && wanted[it].substringBeforeLast('|') != draft.id }.forEach { index ->
            val source = library.firstOrNull { it.id == wanted[index].substringBeforeLast('|') }
            val time = LocalTime.ofSecondOfDay(wanted[index].substringAfterLast('|').toLong())
            val position = source?.times?.indexOf(time)
            assign(index, position?.takeIf { it >= 0 })
        }
        return draft.copy(slotOrigins = wanted.indices.map { assigned[it] ?: wanted[it] })
    }

    // Inspect one weekly cycle after each finite boundary instead of imposing a future horizon.
    fun futureOccurrences(revisions: List<TaskRevision>, events: List<CareEvent>, operations: List<ItemOperation>, taskId: String, now: Instant): List<Occurrence> {
        val today = now.atZone(FAMILY_ZONE).toLocalDate()
        val boundaries = mutableSetOf(today)
        revisions.filter { it.taskId == taskId }.forEach {
            boundaries += it.start; boundaries += it.effective; it.end?.let { end -> boundaries += end.plusDays(1) }
        }
        operations.filter { it.type == ItemType.TASK && it.itemId == taskId }.forEach {
            boundaries += it.anchorDate; boundaries += it.actualDate
            it.task?.let { task -> boundaries += task.start; task.end?.let { end -> boundaries += end.plusDays(1) } }
        }
        events.filter { it.occurrenceId.startsWith("${taskId}_") }.forEach { boundaries += it.date }
        return boundaries.filter { it >= today }.flatMap { boundary -> (0L..7L).map(boundary::plusDays) }.distinct()
            .flatMap { occurrences(revisions, events, operations, it) }.filter { it.revision.taskId == taskId && it.instant > now }
            .distinctBy { it.id }.sortedWith(compareBy<Occurrence> { it.instant }.thenBy { it.id })
    }
    fun definitions(revisions: List<TaskRevision>, events: List<CareEvent>, operations: List<ItemOperation>, now: Instant,
        includePaused: Boolean = false): List<TaskRevision> {
        val candidates = (revisions + operations.filter { it.action == OperationAction.EDIT && it.scope == OperationScope.FOLLOWING }.mapNotNull { op ->
            op.task?.copy(createdMillis = op.createdMillis, authorId = op.authorId)
        })
            .groupBy { it.taskId }.mapNotNull { (_, group) -> group.maxWithOrNull(compareBy<TaskRevision> { it.createdMillis }.thenBy { it.id }) }
        return candidates.filter { task -> activeDeletions(operations).none {
            it.type == ItemType.TASK && it.itemId == task.taskId && it.scope == OperationScope.ALL
        } && if (includePaused && !task.active) true else task.active && futureOccurrences(revisions, events, operations, task.taskId, now).isNotEmpty() }
    }

    fun mapSlots(original: TaskRevision, replacement: TaskRevision): TaskRevision {
        val available = original.times.mapIndexed { i, time -> time to (original.slotOrigins.getOrNull(i)
            ?: "${original.id}|${time.toSecondOfDay()}") }.toMutableList()
        val assigned = mutableMapOf<LocalTime, String>()
        replacement.times.forEach { time -> available.firstOrNull { it.first == time }?.let { assigned[time] = it.second; available.remove(it) } }
        replacement.times.filter { it !in assigned }.forEach { time ->
            assigned[time] = available.minByOrNull { it.first }?.also { available.remove(it) }?.second
                ?: "${replacement.id}|${time.toSecondOfDay()}"
        }
        return replacement.copy(slotOrigins = replacement.times.map { assigned.getValue(it) })
    }
}

object ManagedAppointments {
    private fun ordinal(series: AppointmentSeries, date: LocalDate): Long {
        val estimate = when (series.repeat) {
            AppointmentRepeat.ONCE -> 0L
            AppointmentRepeat.DAYS -> java.time.temporal.ChronoUnit.DAYS.between(series.details.date, date) / series.interval
            AppointmentRepeat.WEEKS -> java.time.temporal.ChronoUnit.WEEKS.between(series.details.date, date) / series.interval
            AppointmentRepeat.MONTHS -> java.time.temporal.ChronoUnit.MONTHS.between(YearMonth.from(series.details.date), YearMonth.from(date)) / series.interval
            AppointmentRepeat.YEARS -> (date.year - series.details.date.year).toLong() / series.interval
        }
        return ((estimate - 2).coerceAtLeast(0)..estimate + 2).firstOrNull { series.dateAt(it) == date } ?: estimate.coerceAtLeast(0)
    }
    fun occurrences(series: List<AppointmentSeries>, exceptions: List<AppointmentException>, activity: List<AppointmentActivity>,
        operations: List<ItemOperation>, from: LocalDate, to: LocalDate, includeDeleted: Boolean = false): List<AppointmentOccurrence> {
        val edits = operations.filter { it.type == ItemType.APPOINTMENT && it.action == OperationAction.EDIT }
            .sortedWith(compareBy<ItemOperation> { if (it.scope == OperationScope.SINGLE) 1 else 0 }
                .thenBy { it.createdMillis }.thenBy { it.version }.thenBy { it.id })
        val result = AppointmentSchedule.occurrences(series, exceptions, from, to).associateBy { it.id }.toMutableMap()
        // Include originals outside the window when an override moves them into it.
        edits.filter { it.scope == OperationScope.SINGLE }.forEach { op ->
            AppointmentSchedule.find(op.anchorId, series, exceptions)?.let { result.putIfAbsent(it.id, it) }
        }
        edits.forEach { edit ->
            val draft = edit.appointment ?: return@forEach
            val created = Instant.ofEpochMilli(edit.createdMillis)
            val source = series.firstOrNull { it.id == edit.sourceId } ?: return@forEach
            if (edit.scope == OperationScope.SINGLE) {
                val current = result[edit.anchorId]
                if ((edit.actualStartMillis > edit.createdMillis || (edit.actualStartMillis == 0L && current?.details?.startInstant?.let { it > created } == true)) &&
                    draft.details.startInstant > created) {
                    result[edit.anchorId] = AppointmentOccurrence(
                        (current?.series ?: source).copy(rootId = edit.itemId), edit.anchorDate, draft.details, edit.sourceId, edit.anchorTime)
                }
            } else {
                val relevant = result.values.filter { appointmentRootId(it.sourceSeriesId, series, activity) == edit.itemId &&
                    it.originalDate.atTime(it.originTime).atZone(FAMILY_ZONE).toInstant() >= edit.anchorInstant }
                val protected = relevant.filter { occurrence -> occurrence.details.startInstant <= created ||
                    exceptions.any { it.id == "${occurrence.sourceSeriesId}_${occurrence.originalDate}" } ||
                    edits.any { it.scope == OperationScope.SINGLE && it.anchorId == occurrence.id } }
                relevant.filter { it !in protected }.forEach { result.remove(it.id) }
                val anchorIndex = ordinal(source, edit.anchorDate)
                val firstIndex = ordinal(draft, draft.activeFrom)
                draft.datesBetween(from, to).forEach { date ->
                    val index = ordinal(draft, date) - firstIndex
                    val original = source.dateAt(anchorIndex + index)
                    val previous = relevant.firstOrNull { it.originalDate == original }
                        ?: AppointmentSchedule.find("appointment_${source.id}_$original", series, exceptions)
                    val hasSingleOverride = edits.any { it.scope == OperationScope.SINGLE && it.sourceId == source.id && it.anchorDate == original } ||
                        exceptions.any { it.seriesId == source.id && it.originalDate == original }
                    if (protected.none { it.originalDate == original } && !hasSingleOverride &&
                        (previous == null || previous.details.startInstant > created)) {
                        val details = draft.details.copy(date = date)
                        if (details.startInstant > created) {
                            val occurrence = AppointmentOccurrence(draft.copy(id = source.id, rootId = edit.itemId), original, details,
                                source.id, edit.anchorTime)
                            result[occurrence.id] = occurrence
                        }
                    }
                }
            }
        }
        val deleted = if (includeDeleted) emptyList() else activeDeletions(operations)
        return result.values.filter { occurrence -> occurrence.details.date in from..to && deleted.none {
            it.covers(ItemType.APPOINTMENT, appointmentRootId(occurrence.sourceSeriesId, series, activity), occurrence.id,
                occurrence.originalDate, occurrence.originTime)
        } }.sortedWith(compareBy<AppointmentOccurrence> { it.details.date }
            .thenBy { if (it.details.allDay) LocalTime.MIDNIGHT else it.details.startTime }.thenBy { it.id })
    }
}
