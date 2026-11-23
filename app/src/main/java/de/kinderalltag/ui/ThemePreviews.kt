package de.kinderalltag.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import de.kinderalltag.data.FamilyState
import de.kinderalltag.domain.*
import java.time.*

internal class ThemePreviewProvider : PreviewParameterProvider<AppThemeId> {
    override val values = AppThemeId.entries.asSequence()
}

private val themePreviewDay = LocalDate.of(2026, 11, 22)
private val themePreviewNow = themePreviewDay.atTime(10, 0).atZone(FAMILY_ZONE).toInstant()
private val themePreviewTask =
    TaskRevision(
        "preview",
        "routine",
        "johann",
        "Rucksack packen",
        notes = "Wechselkleidung und Trinkflasche einpacken.",
        start = themePreviewDay.minusDays(10),
        effective = themePreviewDay.minusDays(10),
    )

private fun themePreviewState(): FamilyState {
    val occurrence =
        Occurrence(themePreviewTask, themePreviewDay.minusDays(1), themePreviewTask.times.single())
    return FamilyState(
        uid = "parent",
        name = "Papa",
        authorized = true,
        loaded = true,
        fromCache = false,
        children = listOf(Child("ferdinand", "Ferdinand"), Child("johann", "Johann")),
        parents = listOf(Parent("parent", "Papa"), Parent("other", "Mama")),
        revisions =
            listOf(
                themePreviewTask,
                themePreviewTask.copy(
                    id = "once",
                    taskId = "house",
                    childId = "haushalt",
                    title = "Spülmaschine ausräumen",
                    frequency = Frequency.ONCE,
                    start = themePreviewDay,
                ),
            ),
        events =
            listOf(
                CareEvent(
                    "done",
                    occurrence.id,
                    occurrence.date,
                    EventKind.COMPLETE,
                    "other",
                    "Mama",
                    occurrence.instant.toEpochMilli(),
                    taskSnapshot = occurrence,
                )
            ),
        appointments =
            listOf(
                AppointmentSeries(
                    "appointment",
                    AppointmentDetails(
                        "Spielplatz mit Freunden",
                        AppointmentRecipient.BOTH_CHILDREN,
                        themePreviewDay,
                        LocalTime.of(15, 0),
                        LocalTime.of(16, 0),
                        location = "Spielplatz im Stadtpark",
                        notes = "Sandspielzeug mitbringen.",
                    ),
                )
            ),
    )
}

@Preview(name = "Design · Heute", widthDp = 390, heightDp = 820)
@Preview(name = "Design · Heute · große Schrift", widthDp = 320, heightDp = 820, fontScale = 1.5f)
@Composable
private fun ThemeHomePreview(@PreviewParameter(ThemePreviewProvider::class) theme: AppThemeId) {
    KinderTheme(theme) {
        AppBackground(Modifier.fillMaxSize()) {
            Checklist(
                themePreviewState(),
                themePreviewDay,
                themePreviewDay,
                null,
                now = themePreviewNow,
                onSelectDate = {},
                onOpen = {},
            )
        }
    }
}

@Preview(name = "Design · Darstellung", widthDp = 390, heightDp = 820)
@Composable
private fun ThemeSettingsPreview(@PreviewParameter(ThemePreviewProvider::class) theme: AppThemeId) {
    KinderTheme(theme) {
        AppBackground(Modifier.fillMaxSize()) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AppBanner(
                    AppTheme.definition.settingsTitle,
                    "Alles, was euren Kinderalltag persönlich macht.",
                )
                SettingsSection("Darstellung", Icons.Default.Palette, AppTheme.colors.deepAccent) {
                    ThemePicker(ThemeSelection(theme) {})
                }
            }
        }
    }
}

@Preview(name = "Design · Aufgabendetails", widthDp = 390, heightDp = 820)
@Composable
private fun ThemeTaskDetailsPreview(
    @PreviewParameter(ThemePreviewProvider::class) theme: AppThemeId
) {
    KinderTheme(theme) {
        OccurrenceDialog(
            Occurrence(themePreviewTask, themePreviewDay, themePreviewTask.times.single()),
            themePreviewState(),
            themePreviewDay,
            themePreviewNow,
            null,
            onDismiss = {},
            onComplete = {},
            onUndo = {},
            onEdit = {},
            onDelete = {},
        )
    }
}

@Preview(name = "Design · Monats-Tracker", widthDp = 390, heightDp = 820)
@Composable
private fun ThemeTrackerPreview(@PreviewParameter(ThemePreviewProvider::class) theme: AppThemeId) {
    KinderTheme(theme) {
        AppBackground(Modifier.fillMaxSize()) {
            Column(Modifier.padding(20.dp)) {
                TaskCompletionOverview(
                    themePreviewTask.taskId,
                    themePreviewState(),
                    themePreviewNow,
                )
            }
        }
    }
}

@Preview(name = "Design · Illustrationen", widthDp = 390, heightDp = 560)
@Composable
private fun ThemeIllustrationsPreview(
    @PreviewParameter(ThemePreviewProvider::class) theme: AppThemeId
) {
    KinderTheme(theme) {
        AppBackground(Modifier.fillMaxSize()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(theme.label, style = MaterialTheme.typography.headlineMedium)
                IllustrationRole.entries.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        row.forEach { AppIllustration(it, Modifier.size(82.dp)) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ChildIcon(Child("ferdinand", "Ferdinand", BearColor.BLUE), Modifier.size(82.dp))
                    ChildIcon(Child("johann", "Johann", BearColor.ROSE), Modifier.size(82.dp))
                    ChildIcon(HOUSEHOLD_SECTION, Modifier.size(82.dp))
                }
            }
        }
    }
}
