package de.kinderalltag.domain

import java.time.LocalDate

/** Next unconfirmed execution per task within the days following the displayed day. */
fun upcomingTaskOccurrences(
    revisions: List<TaskRevision>,
    events: List<CareEvent>,
    operations: List<ItemOperation>,
    after: LocalDate,
    limit: Int,
    childId: String? = null,
    uid: String? = null,
    onlyMine: Boolean = false,
    days: Int = 7,
    excludedTaskIds: Set<String> = emptySet(),
): List<Occurrence> {
    if (limit <= 0 || days <= 0) return emptyList()
    val through = after.plusDays(days.toLong())
    val boundary = after.plusDays(1).atStartOfDay(FAMILY_ZONE).toInstant().minusNanos(1)
    val eventsByOccurrence = events.groupBy { it.occurrenceId }
    val operationsByTask = operations.filter { it.type == ItemType.TASK }.groupBy { it.itemId }
    return revisions
        .groupBy { it.taskId }
        .filterKeys { it !in excludedTaskIds }
        .mapNotNull { (taskId, versions) ->
            val taskEvents =
                events.filter { event ->
                    event.taskSnapshot?.revision?.taskId?.let { it == taskId }
                        ?: event.occurrenceId.startsWith("${taskId}_")
                }
            ManagedTasks.futureOccurrences(
                    versions,
                    taskEvents,
                    operationsByTask[taskId].orEmpty(),
                    taskId,
                    boundary,
                )
                .firstOrNull {
                    it.date <= through &&
                        Schedule.activeCompletions(eventsByOccurrence[it.id].orEmpty(), it.id)
                            .isEmpty()
                }
        }
        .filter {
            (childId == null || it.revision.childId == childId) &&
                (!onlyMine || isResponsible(it.revision.responsibleParentId, uid))
        }
        .sortedWith(
            compareBy<Occurrence> { it.instant }.thenBy { it.revision.title }.thenBy { it.id }
        )
        .take(limit)
}
