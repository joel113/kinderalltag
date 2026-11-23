package de.kinderalltag.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.kinderalltag.text.*

@Composable
internal fun ItemMetadataIndicators(notes: String, location: String = "") {
    if (notes.isBlank() && location.isBlank()) return
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (notes.isNotBlank())
            MetadataIndicator(text(TextKey.NOTES), Icons.Default.Notes, AppTheme.colors.primary)
        if (location.isNotBlank())
            MetadataIndicator(
                location.trim(),
                Icons.Default.Place,
                AppTheme.colors.accent,
                text(TextKey.LOCATION),
            )
    }
}

@Composable
private fun MetadataIndicator(
    text: String,
    icon: ImageVector,
    color: Color,
    iconDescription: String? = null,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = AppTheme.colors.background,
        contentColor = color,
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, iconDescription, Modifier.size(16.dp))
            Text(
                text,
                Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
