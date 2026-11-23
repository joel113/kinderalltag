package de.kinderalltag.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.kinderalltag.text.*
import java.time.LocalDate

@Composable
internal fun PeriodNavigationCard(today: LocalDate, page: Long, onPageChange: (Long) -> Unit) {
    val formats = rememberDateTimeFormats()
    val from = today.plusDays(page * 90)
    val to = from.plusDays(89)
    PageNavigationCard(
        text(TextKey.VALUE_90_DAYS),
        "${formats.date(from)} – ${formats.date(to)}",
        page > 0,
        true,
        onPrevious = { onPageChange(page - 1) },
        onNext = { onPageChange(page + 1) },
        previousDescription = text(TextKey.PREVIOUS_90_DAYS),
        nextDescription = text(TextKey.NEXT_90_DAYS),
        resetText = if (page > 0) text(TextKey.SHOW_FROM_TODAY) else null,
        onReset = { onPageChange(0) },
    )
}

@Composable
internal fun PageNavigationCard(
    title: String,
    range: String,
    canPrevious: Boolean,
    canNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    previousDescription: String,
    nextDescription: String,
    resetText: String? = null,
    onReset: () -> Unit = {},
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, AppTheme.colors.outline),
        colors =
            CardDefaults.cardColors(
                containerColor = AppTheme.colors.accentContainer,
                contentColor = AppTheme.colors.ink,
            ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledIconButton(
                    onClick = onPrevious,
                    enabled = canPrevious,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = AppTheme.colors.accent.copy(alpha = 0.12f),
                            contentColor = AppTheme.colors.accent,
                        ),
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        previousDescription,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelLarge,
                        color = AppTheme.colors.accent,
                    )
                    Text(
                        range,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                FilledIconButton(
                    onClick = onNext,
                    enabled = canNext,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = AppTheme.colors.accent.copy(alpha = 0.12f),
                            contentColor = AppTheme.colors.accent,
                        ),
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        nextDescription,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            if (resetText != null)
                TextButton(onClick = onReset, modifier = Modifier.align(Alignment.End)) {
                    Text(resetText)
                }
        }
    }
}
