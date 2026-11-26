package de.kinderalltag.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.kinderalltag.data.FamilyState
import de.kinderalltag.domain.*
import de.kinderalltag.text.*
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChildrenSettings(
    state: FamilyState,
    busy: Boolean,
    save: (Child, () -> Unit) -> Unit,
    retry: () -> Unit,
) {
    var editing by remember { mutableStateOf<Child?>(null) }
    var confirmation by remember { mutableStateOf<Child?>(null) }
    val writable = !busy && !state.fromCache && state.childRegistryReady
    val full = state.children.count { !it.archived } >= 10
    Text(text(TextKey.CHILDREN_HELP), style = MaterialTheme.typography.bodySmall)
    if (!state.childRegistryReady) {
        Text(text(TextKey.CHILD_SETUP_FAILED))
        TextButton(onClick = retry, enabled = !busy) { Text(text(TextKey.TRY_AGAIN)) }
    }
    state.children.forEach { child ->
        OutlinedCard(Modifier.fillMaxWidth()) {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(12.dp)) {
                val compact = maxWidth < 360.dp
                val archiveLabel =
                    text(if (child.archived) TextKey.CHILD_RESTORE else TextKey.CHILD_ARCHIVE)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ChildIcon(child, Modifier.size(42.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Text(
                            child.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (child.archived)
                            Text(
                                text(TextKey.CHILD_ARCHIVED),
                                style = MaterialTheme.typography.bodySmall,
                            )
                    }
                    if (compact) {
                        IconButton(onClick = { editing = child }, enabled = writable) {
                            Icon(Icons.Default.Edit, text(TextKey.EDIT))
                        }
                        IconButton(
                            onClick = { confirmation = child },
                            enabled = writable && (!child.archived || !full),
                        ) {
                            Icon(
                                if (child.archived) Icons.Default.Unarchive
                                else Icons.Default.Archive,
                                archiveLabel,
                            )
                        }
                    } else {
                        TextButton(onClick = { editing = child }, enabled = writable) {
                            Text(text(TextKey.EDIT))
                        }
                        TextButton(
                            onClick = { confirmation = child },
                            enabled = writable && (!child.archived || !full),
                        ) {
                            Text(archiveLabel)
                        }
                    }
                }
            }
        }
    }
    Button(
        onClick = { editing = Child(UUID.randomUUID().toString(), "") },
        enabled = writable && !full,
    ) {
        Text(text(TextKey.ADD_CHILD))
    }
    if (full) Text(text(TextKey.TEN_ACTIVE_CHILDREN), style = MaterialTheme.typography.bodySmall)
    confirmation?.let { child ->
        AlertDialog(
            onDismissRequest = { if (!busy) confirmation = null },
            title = {
                Text(text(if (child.archived) TextKey.CHILD_RESTORE else TextKey.CHILD_ARCHIVE))
            },
            text = {
                Text(
                    text(
                        if (child.archived) TextKey.CHILD_RESTORE_HELP
                        else TextKey.CHILD_ARCHIVE_HELP
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        save(child.copy(archived = !child.archived)) { confirmation = null }
                    },
                    enabled = writable,
                ) {
                    Text(text(TextKey.CONFIRM))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmation = null }, enabled = !busy) {
                    Text(text(TextKey.CANCEL))
                }
            },
        )
    }
    editing?.let { original ->
        var name by remember(original.id) { mutableStateOf(original.name) }
        var color by remember(original.id) { mutableStateOf(original.bearColor) }
        var icon by remember(original.id) { mutableStateOf(original.icon) }
        val preview = original.copy(name = name.trim(), bearColor = color, icon = icon)
        AlertDialog(
            onDismissRequest = { if (!busy) editing = null },
            title = { Text(text(TextKey.CHILD_PROFILE)) },
            text = {
                Column(
                    Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ChildIcon(preview, Modifier.size(72.dp).align(Alignment.CenterHorizontally))
                    OutlinedTextField(
                        name,
                        { name = it },
                        label = { Text(text(TextKey.NAME)) },
                        enabled = !busy,
                        singleLine = true,
                        isError = name.trim().length > 60,
                    )
                    Text(text(TextKey.CHILD_ICON))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChildIconId.entries.forEach { choice ->
                            FilterChip(
                                icon == choice,
                                { icon = choice },
                                enabled = !busy,
                                label = {
                                    ChildIcon(preview.copy(icon = choice), Modifier.size(40.dp))
                                },
                                modifier = Modifier.childIconDescription(choice),
                            )
                        }
                    }
                    Text(text(TextKey.CHILD_COLOR))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BearColor.entries.forEach { choice ->
                            Box(
                                Modifier.size(44.dp)
                                    .background(Color(choice.argb), CircleShape)
                                    .border(
                                        if (color == choice) 3.dp else 0.dp,
                                        AppTheme.colors.ink,
                                        CircleShape,
                                    )
                                    .clickable(enabled = !busy) { color = choice }
                                    .childColorDescription(choice),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (color == choice) Text("✓", color = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { save(preview) { editing = null } },
                    enabled = writable && name.isNotBlank() && name.trim().length <= 60,
                ) {
                    Text(text(if (busy) TextKey.SAVING else TextKey.SAVE))
                }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }, enabled = !busy) {
                    Text(text(TextKey.CANCEL))
                }
            },
        )
    }
}

private fun Modifier.childIconDescription(icon: ChildIconId) = semantics {
    contentDescription =
        text(
            when (icon) {
                ChildIconId.BEAR -> TextKey.CHILD_ICON_BEAR
                ChildIconId.FOX -> TextKey.CHILD_ICON_FOX
                ChildIconId.RABBIT -> TextKey.CHILD_ICON_RABBIT
                ChildIconId.LION -> TextKey.CHILD_ICON_LION
                ChildIconId.ELEPHANT -> TextKey.CHILD_ICON_ELEPHANT
                ChildIconId.WHALE -> TextKey.CHILD_ICON_WHALE
                ChildIconId.PENGUIN -> TextKey.CHILD_ICON_PENGUIN
                ChildIconId.DINOSAUR -> TextKey.CHILD_ICON_DINOSAUR
                ChildIconId.TRACTOR -> TextKey.CHILD_ICON_TRACTOR
                ChildIconId.UNICORN -> TextKey.CHILD_ICON_UNICORN
            }
        )
}

private fun Modifier.childColorDescription(color: BearColor) = semantics {
    contentDescription =
        text(
            when (color) {
                BearColor.BLUE -> TextKey.BLUE
                BearColor.ROSE -> TextKey.PINK
                BearColor.GREEN -> TextKey.GREEN
                BearColor.HONEY -> TextKey.HONEY
                BearColor.BROWN -> TextKey.BROWN
                BearColor.PURPLE -> TextKey.PURPLE
                BearColor.TEAL -> TextKey.CHILD_COLOR_TEAL
                BearColor.ORANGE -> TextKey.CHILD_COLOR_ORANGE
                BearColor.CORAL -> TextKey.CHILD_COLOR_CORAL
                BearColor.INDIGO -> TextKey.CHILD_COLOR_INDIGO
            }
        )
}
