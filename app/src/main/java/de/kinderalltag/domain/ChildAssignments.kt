package de.kinderalltag.domain

/** A save-time selection creates independent definitions, never a live "all children" group. */
fun tasksForChildren(
    draft: TaskRevision,
    childIds: Set<String>,
    newId: () -> String,
): List<TaskRevision> {
    require(childIds.size in 1..10 && ("haushalt" !in childIds || childIds.size == 1))
    return childIds.mapIndexed { index, id ->
        require(id.matches(Regex("[A-Za-z0-9_-]{1,100}")))
        if (index == 0) draft.copy(childId = id)
        else draft.copy(id = newId(), taskId = newId(), childId = id)
    }
}
