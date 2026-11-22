package de.kinderalltag.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun ThemePicker(selection: ThemeSelection = LocalThemeSelection.current) {
    Text("Design", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Text(
        "Farben, kleine Begleiter und App-Symbol · nur auf diesem Gerät",
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        "Das App-Symbol auf dem Startbildschirm passt sich an. Je nach Launcher kann das einen Moment dauern.",
        style = MaterialTheme.typography.bodySmall,
    )
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppThemeId.entries.forEach { id ->
            val selected = id == selection.id
            val preview = AppThemes.get(id)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = preview.colors.background,
                contentColor = preview.colors.ink,
                border =
                    BorderStroke(
                        if (selected) 2.dp else 1.dp,
                        if (selected) preview.colors.primary else preview.colors.outline,
                    ),
                modifier =
                    Modifier.fillMaxWidth()
                        .selectable(
                            selected,
                            role = Role.RadioButton,
                            onClick = { selection.select(id) },
                        ),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    KinderTheme(id) {
                        AppIllustration(IllustrationRole.BRAND, Modifier.size(44.dp))
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                id.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                listOf(
                                        preview.colors.artBlue,
                                        preview.colors.artCoral,
                                        preview.colors.artGreen,
                                        StatusColors.yellow,
                                    )
                                    .forEach { color ->
                                        Surface(
                                            color = color,
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.size(14.dp),
                                        ) {}
                                    }
                            }
                        }
                        RadioButton(selected = selected, onClick = null)
                    }
                }
            }
        }
    }
}
