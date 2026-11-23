package de.kinderalltag.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kinderalltag.data.FamilyState
import de.kinderalltag.domain.*
import de.kinderalltag.text.*
import java.util.UUID

internal data class ListEditTarget(val kind: FamilyListKind, val item: FamilyListItem? = null)

internal class FamilyListUiState {
    var editor by mutableStateOf<ListEditTarget?>(null)
    var voiceImportOpen by mutableStateOf(false)
    var selected by mutableStateOf<FamilyListItem?>(null)
    val thoughtIndices = mutableStateMapOf<FamilyListKind, Int>()

    fun nextThought(kind: FamilyListKind) {
        thoughtIndices[kind] = (thoughtIndices[kind] ?: -1) + 1
    }
}

internal fun listTitle(kind: FamilyListKind) =
    if (kind == FamilyListKind.SHOPPING) text(TextKey.SHOPPING_LIST) else text(TextKey.GIFT_IDEAS)

internal fun labelGroupTitle(group: LabelGroup) =
    when (group) {
        LabelGroup.SHOPPING -> text(TextKey.SHOPPING_LABELS)
        LabelGroup.OCCASION -> text(TextKey.GIFT_OCCASIONS)
        LabelGroup.GIVER -> text(TextKey.GIFT_GIVERS)
    }

private val shoppingThoughts
    get() =
        listOf(
            text(TextKey.MILK_BREAD_AND_A_LITTLE_ANTICIPATION_KEEP_YOUR_MINDS_FREE_FOR_FAMILY_TIM),
            text(TextKey.THE_LITTLE_ERRANDS_GO_HERE_LEAVE_ROOM_IN_YOUR_MIND_FOR_A_FAMILY_ADVENTUR),
            text(TextKey.A_WELL_FILLED_SHOPPING_BASKET_AND_TIME_FOR_THE_PEOPLE_YOU_LOVE),
        )
private val giftThoughts
    get() =
        listOf(
            text(TextKey.THE_BEST_GIFT_IDEAS_START_WITH_A_THOUGHT_ABOUT_SOMEONE_YOU_LOVE),
            text(TextKey.A_LITTLE_GIFT_A_BIG_SMILE_LET_YOUR_IDEAS_GROW_HERE),
            text(TextKey.SOME_IDEAS_JUST_NEED_A_HOME_UNTIL_THE_RIGHT_MOMENT_TO_GIVE_THEM),
        )

@Composable
internal fun FamilyListPage(
    kind: FamilyListKind,
    state: FamilyState,
    vm: FamilyViewModel,
    showIntroduction: Boolean,
) {
    var labels by rememberSaveable(kind) { mutableStateOf(arrayListOf<String>()) }
    var child by rememberSaveable(kind) { mutableStateOf<String?>(null) }
    var showBought by rememberSaveable(kind) { mutableStateOf(false) }
    val thoughts = if (kind == FamilyListKind.SHOPPING) shoppingThoughts else giftThoughts
    val thought = thoughts[(vm.listUi.thoughtIndices[kind] ?: 0) % thoughts.size]
    val busy by vm.busy.collectAsStateWithLifecycle()
    val order = state.listOrders.firstOrNull { it.kind == kind } ?: FamilyListOrder(kind)
    val fullOrder =
        remember(state.listItems, order.itemIds, kind) {
            orderedListIds(state.listItems, kind, order.itemIds)
        }
    val listState = rememberLazyListState()
    val saveOrder by
        rememberUpdatedState<(List<String>, Long, (Boolean) -> Unit) -> Unit>({ ids, version, done
            ->
            vm.reorderList(kind, ids, version, done)
        })
    val reorder =
        remember(kind, listState) {
            ListReorderState(listState) { ids, version, done -> saveOrder(ids, version, done) }
        }
    fun visible(bought: Boolean) =
        listItems(state.listItems, state.listActivity, kind, bought, reorder.order ?: order.itemIds)
            .filter {
                (kind == FamilyListKind.SHOPPING || it.recipient?.matches(child) == true) &&
                    matchesListLabels(it, labels.toSet(), state.listLabels)
            }
    val open = visible(false)
    val bought = visible(true)
    val canReorder =
        state.authorized &&
            state.loaded &&
            !state.fromCache &&
            !state.pending &&
            state.error == null &&
            !busy &&
            !reorder.saving &&
            fullOrder.size <= MAX_LIST_ORDER_ITEMS
    SideEffect { reorder.update(fullOrder, open.map { it.id }, order.version, canReorder) }
    ListReorderAutoScroll(reorder)
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showIntroduction)
            item {
                AppBanner(
                    if (kind == FamilyListKind.SHOPPING) text(TextKey.YOUR_SHOPPING_LIST)
                    else text(TextKey.YOUR_GIFT_IDEAS),
                    thought,
                )
            }
        if (kind == FamilyListKind.GIFTS)
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(child == null, { child = null }, label = { Text(text(TextKey.ALL)) })
                    state.children.forEach { kid ->
                        FilterChip(child == kid.id, { child = kid.id }, label = { Text(kid.name) })
                    }
                }
            }
        val applicable =
            state.listLabels.filter { label ->
                (if (kind == FamilyListKind.SHOPPING) label.group == LabelGroup.SHOPPING
                else label.group != LabelGroup.SHOPPING) &&
                    (label.active ||
                        state.listItems.any {
                            label.id in it.labelIds && !it.deleted && it.kind == kind
                        })
            }
        if (applicable.isNotEmpty())
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text(TextKey.FILTER_BY_LABELS),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    LabelGroup.entries.forEach { group ->
                        val groupLabels = applicable.filter { it.group == group }
                        if (groupLabels.isNotEmpty()) {
                            Text(
                                labelGroupTitle(group),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                groupLabels
                                    .sortedBy { it.name }
                                    .forEach { label ->
                                        LabelChip(label, label.id in labels) {
                                            labels =
                                                ArrayList(
                                                    if (label.id in labels) labels - label.id
                                                    else labels + label.id
                                                )
                                        }
                                    }
                            }
                        }
                    }
                    if (labels.isNotEmpty())
                        TextButton(onClick = { labels = arrayListOf() }) {
                            Text(text(TextKey.SHOW_ALL_LABELS))
                        }
                }
            }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text(TextKey.OPEN_50DD9F, open.size), fontWeight = FontWeight.SemiBold)
                if (open.size > 1)
                    Text(
                        if (reorder.saving) text(TextKey.SAVING_ORDER)
                        else if (fullOrder.size > MAX_LIST_ORDER_ITEMS)
                            text(TextKey.THE_LIST_IS_TOO_LARGE_FOR_MANUAL_SORTING)
                        else if (!canReorder && !busy)
                            text(TextKey.SORTING_REQUIRES_A_CURRENT_CONNECTION)
                        else
                            text(
                                TextKey.TO_SORT_HOLD_THE_HANDLE_AND_DRAG_TAP_IT_FOR_UP_DOWN_ARROWS
                            ),
                        style = MaterialTheme.typography.bodySmall,
                    )
            }
        }
        if (open.isEmpty())
            item {
                Text(
                    if (kind == FamilyListKind.SHOPPING)
                        text(TextKey.EVERYTHING_IS_BOUGHT_ADD_NEW_ITEMS_WITH_SHOPPING)
                    else text(TextKey.HERE_S_ROOM_FOR_YOUR_NEXT_GIFT_IDEAS)
                )
            }
        items(open, key = { it.id }) { item ->
            FamilyListCard(
                item,
                state,
                busy || reorder.draggingId != null,
                onOpen = { if (reorder.draggingId == null) vm.listUi.selected = item },
                onBought = { vm.buy(item, true) },
                modifier =
                    (if (reorder.draggingId == item.id) Modifier else Modifier.animateItem())
                        .zIndex(if (reorder.draggingId == item.id) 1f else 0f)
                        .graphicsLayer { translationY = reorder.offset(item.id) },
                dragHandle = { ListDragHandle(item.id, item.title, canReorder, reorder) },
            )
        }
        item {
            TextButton(onClick = { showBought = !showBought }) {
                Text(
                    text(
                        TextKey.BOUGHT_5F96A7,
                        bought.size,
                        if (showBought) text(TextKey.HIDE) else text(TextKey.SHOW),
                    )
                )
            }
        }
        if (showBought)
            items(bought, key = { "bought:${it.id}" }) { item ->
                FamilyListCard(
                    item,
                    state,
                    busy,
                    onOpen = { vm.listUi.selected = item },
                    onBought = { vm.buy(item, false) },
                )
            }
        item {
            EndOfPageFooter(
                if (kind == FamilyListKind.SHOPPING) IllustrationRole.BRAND
                else IllustrationRole.AFFECTION
            )
        }
    }
}

@Composable
internal fun LabelChip(label: FamilyLabel, selected: Boolean, onClick: () -> Unit) {
    val color = if (label.color == BearColor.HONEY) Color(0xFF865E12) else Color(label.color.argb)
    FilterChip(
        selected,
        onClick,
        label = { Text(label.name + if (!label.active) text(TextKey.ARCHIVED) else "") },
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = color.copy(alpha = 0.12f),
                selectedLabelColor = color,
            ),
    )
}

@Composable
internal fun FamilyListCard(
    item: FamilyListItem,
    state: FamilyState,
    busy: Boolean,
    onOpen: () -> Unit,
    onBought: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: (@Composable () -> Unit)? = null,
) {
    val bought = activePurchases(state.listActivity, item.id).isNotEmpty()
    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (bought) StatusColors.successSurface
                    else if (item.kind == FamilyListKind.SHOPPING) AppTheme.colors.primaryContainer
                    else AppTheme.colors.secondaryContainer,
                contentColor = AppTheme.colors.ink,
            ),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            dragHandle?.invoke()
            Checkbox(
                bought,
                { onBought() },
                enabled = !busy && state.authorized,
                modifier =
                    Modifier.semanticsLabel(
                        "${item.title}: ${if (bought) text(TextKey.REOPEN_3CC598) else text(TextKey.MARK_AS_BOUGHT_5ACF70)}"
                    ),
            )
            Column(
                Modifier.weight(1f).padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    item.title,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (bought) TextDecoration.LineThrough else null,
                )
                item.recipient?.let {
                    Text(
                        appointmentRecipientName(it, state.sections),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    state.listLabels
                        .filter { it.id in item.labelIds }
                        .forEach { label ->
                            val color =
                                if (label.color == BearColor.HONEY) Color(0xFF865E12)
                                else Color(label.color.argb)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = color.copy(alpha = 0.1f),
                            ) {
                                Text(
                                    label.name,
                                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = color,
                                )
                            }
                        }
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = AppTheme.colors.primary,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

private fun Modifier.semanticsLabel(label: String) = semantics { contentDescription = label }

internal fun LazyListScope.shoppingHome(state: FamilyState, vm: FamilyViewModel, busy: Boolean) {
    item {
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text(TextKey.CURRENT_SHOPPING_LIST),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            FilledTonalButton(onClick = { vm.listUi.voiceImportOpen = true }) {
                Icon(Icons.Default.Add, null)
                Text(text(TextKey.SHOPPING_ITEM))
            }
        }
    }
    val open =
        listItems(
            state.listItems,
            state.listActivity,
            FamilyListKind.SHOPPING,
            false,
            state.listOrders.firstOrNull { it.kind == FamilyListKind.SHOPPING }?.itemIds.orEmpty(),
        )
    if (open.isEmpty()) item { Text(text(TextKey.EVERYTHING_BOUGHT_YOUR_SHOPPING_LIST_IS_EMPTY)) }
    items(open, key = { "shopping:${it.id}" }) { item ->
        FamilyListCard(item, state, busy, { vm.listUi.selected = item }, { vm.buy(item, true) })
    }
}

@Composable
internal fun FamilyListDialogs(state: FamilyState, vm: FamilyViewModel) {
    val ui = vm.listUi
    val saveError by vm.listError.collectAsStateWithLifecycle()
    LaunchedEffect(ui.editor, ui.selected) { vm.listError.value = null }
    val busy by vm.busy.collectAsStateWithLifecycle()
    ui.selected?.let { item ->
        val current = state.listItems.firstOrNull { it.id == item.id }
        val fresh = current != null && !current.deleted && current.version == item.version
        val canManage =
            fresh &&
                !busy &&
                state.loaded &&
                !state.fromCache &&
                !state.pending &&
                state.error == null
        var deleting by remember(item.id) { mutableStateOf(false) }
        ItemDetailsDialog(
            item.title,
            listTitle(item.kind),
            { ui.selected = null },
            busy,
            actions = {
                DetailActions(
                    primary = {
                        DetailAction(
                            if (activePurchases(state.listActivity, item.id).isEmpty())
                                text(TextKey.MARK_AS_BOUGHT)
                            else text(TextKey.REOPEN),
                            if (activePurchases(state.listActivity, item.id).isEmpty())
                                Icons.Default.Check
                            else Icons.Default.Restore,
                            fresh && !busy,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                vm.buy(item, activePurchases(state.listActivity, item.id).isEmpty())
                            },
                        )
                    },
                    secondary =
                        listOf(
                            DetailActionSpec(
                                text(TextKey.EDIT),
                                Icons.Default.Edit,
                                canManage,
                                onClick = {
                                    ui.selected = null
                                    ui.editor = ListEditTarget(item.kind, item)
                                },
                            )
                        ),
                    deleteAction =
                        DetailActionSpec(
                            text(TextKey.DELETE),
                            Icons.Default.DeleteOutline,
                            canManage,
                            onClick = { deleting = true },
                        ),
                )
            },
        ) {
            DetailSection(text(TextKey.AT_A_GLANCE)) {
                item.recipient?.let {
                    DetailValue(
                        Icons.Default.Face,
                        text(TextKey.FOR),
                        appointmentRecipientName(it, state.sections),
                    )
                }
                DetailValue(
                    Icons.Default.CheckCircle,
                    text(TextKey.STATUS),
                    if (activePurchases(state.listActivity, item.id).isEmpty()) text(TextKey.OPEN)
                    else text(TextKey.BOUGHT),
                )
                LabelGroup.entries.forEach { group ->
                    val names =
                        state.listLabels
                            .filter { it.id in item.labelIds && it.group == group }
                            .map { it.name + if (!it.active) text(TextKey.ARCHIVED) else "" }
                    if (names.isNotEmpty())
                        DetailValue(
                            Icons.AutoMirrored.Filled.Label,
                            labelGroupTitle(group),
                            names.joinToString(" · "),
                        )
                }
            }
            if (item.notes.isNotBlank())
                DetailSection(text(TextKey.NOTES), AppTheme.colors.background) { Text(item.notes) }
            saveError?.let { DetailNotice(it) }
            if (!fresh)
                DetailNotice(text(TextKey.THIS_ENTRY_HAS_CHANGED_PLEASE_CLOSE_AND_REOPEN_IT_2192A4))
            if (state.fromCache)
                DetailNotice(
                    text(
                        TextKey
                            .SAVED_DATA_BUYING_AND_REOPENING_ARE_QUEUED_OFFLINE_EDITING_AND_DELETING
                    )
                )
        }
        if (deleting)
            AlertDialog(
                onDismissRequest = { if (!busy) deleting = false },
                title = { Text(text(TextKey.DELETE_ENTRY)) },
                text = {
                    Column {
                        Text(
                            text(
                                TextKey.IS_KEPT_IN_TRASH_PURCHASE_STATUS_AND_HISTORY_ARE_PRESERVED,
                                item.title,
                            )
                        )
                        saveError?.let { Text(it, color = StatusColors.error) }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = canManage,
                        onClick = {
                            vm.changeListItem(item, ListAction.DELETED) {
                                deleting = false
                                ui.selected = null
                            }
                        },
                    ) {
                        Text(text(TextKey.DELETE), color = StatusColors.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleting = false }, enabled = !busy) {
                        Text(text(TextKey.CANCEL))
                    }
                },
            )
    }
    if (ui.voiceImportOpen) ShoppingVoiceImportDialog(state, vm, busy)
    ui.editor?.let { target ->
        FamilyListEditor(target, state, busy, saveError, { ui.editor = null }) { draft ->
            vm.saveListItem(draft, target.item == null) { ui.editor = null }
        }
    }
}

@Composable
private fun FamilyListEditor(
    target: ListEditTarget,
    state: FamilyState,
    busy: Boolean,
    saveError: String?,
    onDismiss: () -> Unit,
    onSave: (FamilyListItem) -> Unit,
) {
    val existing = target.item
    val id = rememberSaveable { existing?.id ?: UUID.randomUUID().toString() }
    var title by rememberSaveable { mutableStateOf(existing?.title.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes.orEmpty()) }
    var recipient by rememberSaveable {
        mutableStateOf(existing?.recipient ?: AppointmentRecipient.BOTH_CHILDREN)
    }
    var selectedLabels by rememberSaveable {
        mutableStateOf(ArrayList(existing?.labelIds.orEmpty()))
    }
    var error by remember { mutableStateOf<String?>(null) }
    val stale =
        existing != null &&
            state.listItems
                .firstOrNull { it.id == existing.id }
                ?.let { it.version != existing.version || it.deleted } != false
    val canSave =
        !busy &&
            !stale &&
            (existing == null || (!state.fromCache && !state.pending && state.error == null))
    ItemDetailsDialog(
        if (existing == null)
            if (target.kind == FamilyListKind.SHOPPING) text(TextKey.NEW_SHOPPING_ITEM)
            else text(TextKey.NEW_GIFT_IDEA)
        else text(TextKey.EDIT_ENTRY),
        listTitle(target.kind),
        onDismiss,
        busy,
        actions = {
            Button(
                onClick = {
                    try {
                        val draft =
                            FamilyListItem(
                                id,
                                target.kind,
                                title.trim(),
                                notes.trim(),
                                if (target.kind == FamilyListKind.SHOPPING) null else recipient,
                                selectedLabels.toSet(),
                                existing?.version ?: 0,
                                lastActivityId = existing?.lastActivityId.orEmpty(),
                            )
                        draft.validate()
                        error = null
                        onSave(draft)
                    } catch (e: IllegalArgumentException) {
                        error = e.message
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (busy) text(TextKey.SAVING) else text(TextKey.SAVE))
            }
        },
    ) {
        DictationField(
            title,
            { title = it },
            text(TextKey.TITLE),
            Modifier.fillMaxWidth(),
            enabled = !busy,
        )
        DictationField(
            notes,
            { notes = it },
            text(TextKey.NOTES_OPTIONAL),
            Modifier.fillMaxWidth(),
            notes = true,
            enabled = !busy,
        )
        if (target.kind == FamilyListKind.GIFTS)
            DetailSection(text(TextKey.WHO_IS_IT_FOR), AppTheme.colors.background) {
                listOf(
                        AppointmentRecipient.FERDINAND,
                        AppointmentRecipient.JOHANN,
                        AppointmentRecipient.BOTH_CHILDREN,
                    )
                    .forEach { choice ->
                        FilterChip(
                            recipient == choice,
                            { recipient = choice },
                            label = { Text(appointmentRecipientName(choice, state.sections)) },
                        )
                    }
            }
        Text(
            text(TextKey.UP_TO_6_LABELS_IN_TOTAL_YOU_CAN_MANAGE_SHARED_LABELS_IN_SETTINGS),
            style = MaterialTheme.typography.bodySmall,
        )
        LabelGroup.entries
            .filter {
                if (target.kind == FamilyListKind.SHOPPING) it == LabelGroup.SHOPPING
                else it != LabelGroup.SHOPPING
            }
            .forEach { group ->
                DetailSection(labelGroupTitle(group), AppTheme.colors.background) {
                    val labels =
                        state.listLabels
                            .filter { it.group == group && (it.active || it.id in selectedLabels) }
                            .sortedBy { it.name }
                    if (labels.isEmpty())
                        Text(
                            text(TextKey.YOU_CAN_CREATE_LABELS_IN_SETTINGS),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        labels.forEach { label ->
                            LabelChip(label, label.id in selectedLabels) {
                                selectedLabels =
                                    ArrayList(
                                        if (label.id in selectedLabels) selectedLabels - label.id
                                        else selectedLabels + label.id
                                    )
                            }
                        }
                    }
                }
            }
        saveError?.let { DetailNotice(it) }
        if (stale)
            DetailNotice(text(TextKey.THIS_ENTRY_HAS_CHANGED_PLEASE_CLOSE_AND_REOPEN_IT_2192A4))
        error?.let { DetailNotice(it) }
    }
}
