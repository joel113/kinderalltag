package de.kinderalltag.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import de.kinderalltag.domain.moveListItem

internal class ListReorderState(
    val list: LazyListState,
    private val onSave: (List<String>, Long, (Boolean) -> Unit) -> Unit,
) {
    var order by mutableStateOf<List<String>?>(null)
        private set

    var draggingId by mutableStateOf<String?>(null)
        private set

    var dragTop by mutableFloatStateOf(0f)
        private set

    var saving by mutableStateOf(false)
        private set

    private var dragHeight = 0
    private var baseVersion = 0L
    private var waitingVersion: Long? = null
    private var savedOrder = emptyList<String>()
    private var visible by mutableStateOf(emptyList<String>())
    private var version = 0L

    fun update(
        order: List<String>,
        visibleIds: List<String>,
        serverVersion: Long,
        enabled: Boolean,
    ) {
        if (
            draggingId != null &&
                (!enabled || serverVersion != baseVersion || visible.toSet() != visibleIds.toSet())
        )
            cancelDrag()
        savedOrder = order
        visible = visibleIds
        version = serverVersion
        if (waitingVersion?.let { serverVersion >= it } == true) reset()
    }

    fun start(id: String) {
        if (saving || id !in visible) return
        val item = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id } ?: return
        order = savedOrder
        baseVersion = version
        dragTop = item.offset.toFloat()
        dragHeight = item.size
        draggingId = id
    }

    fun drag(delta: Float) {
        if (draggingId == null) return
        dragTop += delta
        moveAtCenter()
    }

    fun moveAtCenter() {
        val id = draggingId ?: return
        val ids = order ?: return
        val center = dragTop + dragHeight / 2f
        val target =
            list.layoutInfo.visibleItemsInfo.firstOrNull {
                it.key != id &&
                    it.key in visible &&
                    center >= it.offset &&
                    center <= it.offset + it.size
            } ?: return
        val targetId = target.key as String
        val after = ids.indexOf(targetId) > ids.indexOf(id)
        if (
            (after && center > target.offset + target.size / 2f) ||
                (!after && center < target.offset + target.size / 2f)
        ) {
            order = moveListItem(ids, id, targetId, after)
        }
    }

    fun offset(id: String): Float =
        if (draggingId == id) {
            val item = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id }
            dragTop - (item?.offset ?: dragTop.toInt())
        } else 0f

    fun finish() {
        if (draggingId == null) return
        draggingId = null
        val changed = order ?: return
        if (changed == savedOrder) reset() else save(changed, baseVersion)
    }

    fun cancelDrag() {
        if (draggingId != null) reset()
    }

    fun canMove(id: String, direction: Int): Boolean =
        !saving && visible.indexOf(id).let { it >= 0 && it + direction in visible.indices }

    fun moveOne(id: String, direction: Int): Boolean {
        if (!canMove(id, direction)) return false
        val target = visible[visible.indexOf(id) + direction]
        save(moveListItem(savedOrder, id, target, direction > 0), version)
        return true
    }

    private fun save(ids: List<String>, expected: Long) {
        order = ids
        saving = true
        onSave(ids, expected) { success ->
            if (!success) reset()
            else {
                waitingVersion = expected + 1
                if (version >= expected + 1) reset()
            }
        }
    }

    private fun reset() {
        draggingId = null
        order = null
        saving = false
        waitingVersion = null
    }
}

@Composable
internal fun ListReorderAutoScroll(reorder: ListReorderState) {
    val density = LocalDensity.current
    val edge = with(density) { 72.dp.toPx() }
    val step = with(density) { 10.dp.toPx() }
    LaunchedEffect(reorder.draggingId) {
        while (reorder.draggingId != null) {
            withFrameNanos {}
            val viewport = reorder.list.layoutInfo
            val distance =
                when {
                    reorder.dragTop < viewport.viewportStartOffset + edge ->
                        (reorder.dragTop - viewport.viewportStartOffset - edge) / edge
                    reorder.dragTop + edge > viewport.viewportEndOffset ->
                        (reorder.dragTop + edge - viewport.viewportEndOffset) / edge
                    else -> 0f
                }
            if (distance != 0f) {
                reorder.list.scrollBy(distance.coerceIn(-1f, 1f) * step)
                reorder.moveAtCenter()
            }
        }
    }
}

@Composable
internal fun ListDragHandle(
    id: String,
    title: String,
    enabled: Boolean,
    reorder: ListReorderState,
) {
    var menu by remember(id) { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { menu = true },
            enabled = enabled,
            modifier =
                Modifier.size(48.dp)
                    .semantics {
                        contentDescription = "$title verschieben"
                        if (enabled)
                            customActions =
                                listOfNotNull(
                                    if (reorder.canMove(id, -1))
                                        CustomAccessibilityAction("Nach oben") {
                                            reorder.moveOne(id, -1)
                                        }
                                    else null,
                                    if (reorder.canMove(id, 1))
                                        CustomAccessibilityAction("Nach unten") {
                                            reorder.moveOne(id, 1)
                                        }
                                    else null,
                                )
                    }
                    .pointerInput(id, enabled) {
                        if (enabled)
                            detectDragGesturesAfterLongPress(
                                onDragStart = { reorder.start(id) },
                                onDragEnd = reorder::finish,
                                onDragCancel = reorder::cancelDrag,
                                onDrag = { change, amount ->
                                    change.consume()
                                    reorder.drag(amount.y)
                                },
                            )
                    },
        ) {
            Icon(
                Icons.Default.DragHandle,
                null,
                tint =
                    if (enabled) AppTheme.colors.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            listOf(-1 to "Nach oben", 1 to "Nach unten").forEach { (direction, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    leadingIcon = {
                        Icon(
                            if (direction < 0) Icons.Default.ArrowUpward
                            else Icons.Default.ArrowDownward,
                            null,
                        )
                    },
                    enabled = enabled && reorder.canMove(id, direction),
                    onClick = {
                        menu = false
                        reorder.moveOne(id, direction)
                    },
                )
            }
        }
    }
}
