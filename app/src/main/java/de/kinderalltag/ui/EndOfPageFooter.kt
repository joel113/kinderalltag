package de.kinderalltag.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun EndOfPageFooter(motif: GardenMotif, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 24.dp)
        .semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(shape = CircleShape, color = GardenColors.Cream.copy(alpha = 0.9f)) {
            GardenDrawing(motif, Modifier.padding(12.dp).size(56.dp))
        }
        Text("Du bist am Ende angekommen.", style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold, color = GardenColors.Ink, textAlign = TextAlign.Center)
        Text("Ein kleiner Moment zum Durchatmen.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
