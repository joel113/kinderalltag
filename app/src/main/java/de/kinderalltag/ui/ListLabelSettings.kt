package de.kinderalltag.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kinderalltag.data.FamilyState
import de.kinderalltag.domain.*
import de.kinderalltag.text.*
import java.util.UUID

@Composable
internal fun ListLabelSettings(state: FamilyState, vm: FamilyViewModel, busy: Boolean) {
    var open by remember { mutableStateOf(false) }
    SettingsSection(text(TextKey.LABELS), Icons.AutoMirrored.Filled.Label, AppTheme.colors.accent) {
        Text(text(TextKey.SHARED_LABELS_FOR_YOUR_SHOPPING_GIFT_OCCASIONS_AND_GIFT_GIVERS))
        OutlinedButton(onClick = { open = true }) { Text(text(TextKey.MANAGE_LABELS)) }
    }
    if (open) LabelManager(state, vm, busy) { open = false }
}

@Composable
private fun LabelManager(
    state: FamilyState,
    vm: FamilyViewModel,
    busy: Boolean,
    onDismiss: () -> Unit,
) {
    var group by remember { mutableStateOf(LabelGroup.SHOPPING) }
    var editing by remember { mutableStateOf<FamilyLabel?>(null) }
    var creating by remember { mutableStateOf(false) }
    val saveError by vm.listError.collectAsStateWithLifecycle()
    LaunchedEffect(group, editing) { vm.listError.value = null }
    val online = state.loaded && !state.fromCache && !state.pending && state.error == null && !busy
    ItemDetailsDialog(
        text(TextKey.MANAGE_LABELS),
        text(TextKey.SETTINGS),
        onDismiss,
        busy,
        actions = {
            DetailActions(
                secondary =
                    listOf(
                        DetailActionSpec(
                            text(TextKey.NEW_LABEL),
                            Icons.Default.Add,
                            online,
                            onClick = {
                                editing = FamilyLabel(UUID.randomUUID().toString(), group, "")
                                creating = true
                            },
                        ),
                        DetailActionSpec(
                            text(TextKey.SUGGESTIONS),
                            Icons.Default.AutoAwesome,
                            online,
                            onClick = { vm.addListStarters(group) },
                        ),
                    )
            )
        },
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabelGroup.entries.forEach { value ->
                FilterChip(
                    group == value,
                    { group = value },
                    label = { Text(labelGroupTitle(value)) },
                )
            }
        }
        Text(
            text(TextKey.ARCHIVED_LABELS_STAY_ON_EXISTING_ENTRIES_AND_IN_THE_HISTORY_SUGGESTIONS),
            style = MaterialTheme.typography.bodySmall,
        )
        val labels = state.listLabels.filter { it.group == group }.sortedBy { it.name }
        if (labels.isEmpty())
            Text(text(TextKey.NO_LABELS_YET_CREATE_YOUR_OWN_OR_USE_THE_SUGGESTIONS))
        labels.forEach { label ->
            DetailSection(
                label.name + if (!label.active) text(TextKey.ARCHIVED) else "",
                colorForLabel(label),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        enabled = online,
                        onClick = {
                            editing = label
                            creating = false
                        },
                    ) {
                        Text(text(TextKey.EDIT))
                    }
                    TextButton(
                        enabled = online,
                        onClick = { vm.saveListLabel(label.copy(active = !label.active), false) {} },
                    ) {
                        Text(if (label.active) text(TextKey.ARCHIVE) else text(TextKey.ENABLE))
                    }
                }
            }
        }
        saveError?.let { DetailNotice(it) }
        if (!online) DetailNotice(text(TextKey.MANAGING_LABELS_REQUIRES_A_CURRENT_CONNECTION))
    }
    editing?.let { label ->
        key(label.id) {
            var name by remember { mutableStateOf(label.name) }
            var color by remember { mutableStateOf(label.color) }
            var error by remember { mutableStateOf<String?>(null) }
            val fresh =
                creating ||
                    state.listLabels.firstOrNull { it.id == label.id }?.version == label.version
            ItemDetailsDialog(
                if (creating) text(TextKey.NEW_LABEL) else text(TextKey.EDIT_LABEL),
                labelGroupTitle(label.group),
                { editing = null },
                busy,
                actions = {
                    Button(
                        enabled = online && fresh,
                        onClick = {
                            try {
                                val updated = label.copy(name = name.trim(), color = color)
                                updated.validate()
                                requireLocalized(
                                    state.listLabels.none {
                                        it.id != label.id &&
                                            it.group == label.group &&
                                            it.name.equals(updated.name, ignoreCase = true)
                                    }
                                ) {
                                    message(TextKey.THIS_LABEL_ALREADY_EXISTS)
                                }
                                vm.saveListLabel(updated, creating) { editing = null }
                            } catch (e: IllegalArgumentException) {
                                error = e.message
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text(TextKey.SAVE))
                    }
                },
            ) {
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text(text(TextKey.NAME)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Text(text(TextKey.COLOUR))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BearColor.entries.forEach { choice ->
                        LabelChip(
                            label.copy(
                                name =
                                    when (choice) {
                                        BearColor.BLUE -> text(TextKey.BLUE)
                                        BearColor.ROSE -> text(TextKey.PINK)
                                        BearColor.GREEN -> text(TextKey.GREEN)
                                        BearColor.HONEY -> text(TextKey.YELLOW)
                                        BearColor.BROWN -> text(TextKey.BROWN)
                                        BearColor.PURPLE -> text(TextKey.PURPLE)
                                    },
                                color = choice,
                            ),
                            color == choice,
                        ) {
                            color = choice
                        }
                    }
                }
                saveError?.let { DetailNotice(it) }
                if (!fresh)
                    DetailNotice(text(TextKey.THIS_LABEL_HAS_CHANGED_PLEASE_CLOSE_AND_REOPEN_IT))
                error?.let { DetailNotice(it) }
            }
        }
    }
}

private fun colorForLabel(label: FamilyLabel) =
    androidx.compose.ui.graphics.Color(label.color.argb).copy(alpha = 0.10f)
