package de.kinderalltag.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.kinderalltag.domain.*
import de.kinderalltag.text.*

internal fun responsibilityName(parentId: String?, parents: List<Parent>): String =
    if (parentId == null) text(TextKey.BOTH_PARENTS)
    else parents.firstOrNull { it.id == parentId }?.name ?: text(TextKey.PARENT)

@Composable
internal fun AssignmentPicker(
    parents: List<Parent>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text(TextKey.ASSIGNED_TO), style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected == null,
                { onSelect(null) },
                label = { Text(text(TextKey.BOTH_PARENTS)) },
            )
            parents.forEach { parent ->
                FilterChip(
                    selected == parent.id,
                    { onSelect(parent.id) },
                    label = { Text(parent.name) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChildRecipientPicker(
    children: List<Child>,
    selected: AppointmentRecipient,
    household: Boolean = true,
    enabled: Boolean = true,
    onSelect: (AppointmentRecipient) -> Unit,
) {
    val active = children.filter { !it.archived && it.id != "haushalt" }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (active.isNotEmpty())
            FilterChip(
                !selected.household && selected.childIds == active.map { it.id }.toSet(),
                { onSelect(AppointmentRecipient.children(active.map { it.id }.toSet())) },
                enabled = enabled,
                label = { Text(text(TextKey.ALL_CHILDREN)) },
            )
        children
            .filter { it.id != "haushalt" && (!it.archived || it.id in selected.childIds) }
            .forEach { child ->
                FilterChip(
                    !selected.household && child.id in selected.childIds,
                    { onSelect(selected.toggleChild(child.id)) },
                    enabled =
                        enabled && (child.id in selected.childIds || selected.childIds.size < 10),
                    label = {
                        Text(
                            child.name +
                                if (child.archived) " (" + text(TextKey.CHILD_ARCHIVED) + ")"
                                else ""
                        )
                    },
                    leadingIcon = { ChildIcon(child, Modifier.size(24.dp)) },
                )
            }
        if (household)
            FilterChip(
                selected.household,
                { onSelect(AppointmentRecipient.HOUSEHOLD) },
                enabled = enabled,
                label = { Text(text(TextKey.HOUSEHOLD)) },
            )
    }
}
