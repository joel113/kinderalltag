package de.kinderalltag.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.kinderalltag.domain.Parent
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
