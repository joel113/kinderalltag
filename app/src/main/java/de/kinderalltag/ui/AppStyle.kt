package de.kinderalltag.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.kinderalltag.domain.BearColor
import de.kinderalltag.domain.Child
import kotlin.math.cos
import kotlin.math.sin

internal enum class IllustrationRole {
    JOY,
    FRIEND,
    HELPER,
    BRAND,
    ACTIVITY,
    HOUSEHOLD,
    AFFECTION,
}

internal fun childColor(child: Child, colors: AppPalette) =
    if (child.id == "haushalt") colors.accent
    else
        when (child.bearColor) {
            BearColor.HONEY -> Color(0xFF865E12)
            BearColor.GREEN -> StatusColors.success
            BearColor.ROSE -> Color(0xFF9E374B)
            else -> Color(child.bearColor.argb)
        }

@Composable internal fun childColor(child: Child) = childColor(child, AppTheme.colors)

@Composable
internal fun childPaper(child: Child): Color {
    val colors = AppTheme.colors
    return if (child.id == "haushalt") colors.householdSurface
    else lerp(colors.background, childColor(child, colors), 0.12f)
}

@Composable
internal fun ChildIcon(child: Child, modifier: Modifier = Modifier) {
    val theme = AppTheme.definition
    val colors = theme.colors
    if (child.id == "haushalt") {
        AppIllustration(IllustrationRole.HOUSEHOLD, modifier)
    } else {
        Canvas(modifier.clearAndSetSemantics {}) {
            scale(size.width / 100f, size.height / 100f, pivot = Offset.Zero) {
                if (theme.id == AppThemeId.SPACE) {
                    drawCircle(colors.primaryContainer, 47f, Offset(50f, 50f))
                    drawCircle(colors.artBlue, 47f, Offset(50f, 50f), style = Stroke(4f))
                    drawRoundRect(
                        colors.primary,
                        Offset(28f, 83f),
                        Size(44f, 12f),
                        CornerRadius(5f),
                    )
                    scale(0.78f, 0.78f, pivot = Offset(50f, 50f)) { drawBear(child, colors) }
                    drawArc(
                        Color.White.copy(alpha = 0.8f),
                        195f,
                        55f,
                        false,
                        Offset(9f, 9f),
                        Size(82f, 82f),
                        style = Stroke(3f),
                    )
                } else {
                    drawBear(child, colors)
                    if (theme.id == AppThemeId.MAGIC) {
                        translate(28f, 10f) {
                            scale(0.44f, 0.44f, Offset.Zero) { drawMagicCrown(colors) }
                        }
                    }
                    if (theme.id == AppThemeId.FARM) {
                        drawRoundRect(
                            StatusColors.yellow,
                            Offset(29f, 11f),
                            Size(42f, 23f),
                            CornerRadius(9f),
                        )
                        drawRoundRect(
                            colors.secondary,
                            Offset(29f, 28f),
                            Size(42f, 6f),
                            CornerRadius(2f),
                        )
                        drawOval(StatusColors.yellow, Offset(15f, 30f), Size(70f, 10f))
                    }
                    if (theme.id == AppThemeId.NATURE) {
                        drawNatureBlossom(colors, Offset(77f, 26f), 10f)
                    }
                    if (theme.id == AppThemeId.VEHICLES) {
                        drawPath(
                            Path().apply {
                                moveTo(27f, 36f)
                                quadraticTo(27f, 12f, 50f, 12f)
                                quadraticTo(73f, 12f, 73f, 36f)
                                close()
                            },
                            StatusColors.yellow,
                        )
                        drawLine(
                            colors.secondary.copy(alpha = 0.35f),
                            Offset(50f, 15f),
                            Offset(50f, 32f),
                            3f,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        )
                        drawRoundRect(
                            StatusColors.yellow,
                            Offset(21f, 33f),
                            Size(58f, 7f),
                            CornerRadius(3f),
                        )
                    }
                    if (theme.id == AppThemeId.ANIMALS) {
                        drawPath(
                            Path().apply {
                                moveTo(15f, 81f)
                                quadraticTo(5f, 67f, 18f, 62f)
                                quadraticTo(31f, 75f, 15f, 81f)
                                close()
                            },
                            colors.artGreen,
                        )
                        drawLine(colors.accent, Offset(13f, 84f), Offset(19f, 69f), 1.8f)
                    }
                    if (theme.id == AppThemeId.PLAY) {
                        drawLine(
                            colors.deepAccent.copy(alpha = 0.35f),
                            Offset(50f, 33f),
                            Offset(50f, 43f),
                            1.5f,
                            pathEffect =
                                androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(2f, 2f)
                                ),
                        )
                        drawCircle(colors.artCoral, 5f, Offset(43f, 91f))
                        drawCircle(colors.artCoral, 5f, Offset(57f, 91f))
                        drawCircle(colors.secondary, 3f, Offset(50f, 91f))
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawBear(child: Child, colors: AppPalette) {
    val fur = Color(child.bearColor.argb)
    val innerEar = lerp(colors.background, fur, 0.18f)
    // Round ears, cream muzzle and rosy cheeks from bear.jpg.
    drawCircle(fur, 15f, Offset(24f, 25f))
    drawCircle(fur, 15f, Offset(76f, 25f))
    drawCircle(innerEar, 8f, Offset(24f, 25f))
    drawCircle(innerEar, 8f, Offset(76f, 25f))
    drawOval(fur, Offset(12f, 28f), Size(76f, 65f))
    drawCircle(colors.ink, 3.8f, Offset(35f, 54f))
    drawCircle(colors.ink, 3.8f, Offset(65f, 54f))
    drawCircle(Color.White, 1.1f, Offset(34f, 53f))
    drawCircle(Color.White, 1.1f, Offset(64f, 53f))
    val cheeks = if (colors == AppThemes.garden.colors) Color(0xFFE99CAB) else colors.artCoral
    drawCircle(cheeks, 5f, Offset(25f, 67f))
    drawCircle(cheeks, 5f, Offset(75f, 67f))
    drawOval(colors.background, Offset(35f, 63f), Size(30f, 24f))
    drawPath(
        Path().apply {
            moveTo(44f, 68f)
            quadraticTo(50f, 65f, 56f, 68f)
            quadraticTo(57f, 70f, 50f, 75f)
            quadraticTo(43f, 70f, 44f, 68f)
            close()
        },
        colors.ink,
    )
    drawPath(
        Path().apply {
            moveTo(50f, 74f)
            lineTo(50f, 79f)
            quadraticTo(46f, 83f, 43f, 79f)
            moveTo(50f, 79f)
            quadraticTo(54f, 83f, 57f, 79f)
        },
        colors.ink,
        style = Stroke(2.3f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
    )
}

@Composable
internal fun AppIllustration(motif: IllustrationRole, modifier: Modifier = Modifier) {
    val theme = AppTheme.definition
    Canvas(modifier.clearAndSetSemantics {}) {
        scale(size.width / 100f, size.height / 100f, pivot = Offset.Zero) {
            drawThemeMotif(theme, motif)
        }
    }
}

@Composable
internal fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val theme = AppTheme.definition
    Box(modifier.background(theme.colors.background)) {
        Canvas(Modifier.matchParentSize().clipToBounds().alpha(0.16f).clearAndSetSemantics {}) {
            val spacing = 125.dp.toPx()
            val iconSize = 68.dp.toPx()
            val motifs =
                listOf(
                    IllustrationRole.JOY,
                    IllustrationRole.HELPER,
                    IllustrationRole.BRAND,
                    IllustrationRole.ACTIVITY,
                    IllustrationRole.FRIEND,
                )
            val columns = kotlin.math.ceil(size.width / spacing).toInt() + 1
            val rows = kotlin.math.ceil(size.height / spacing).toInt() + 1
            for (row in 0 until rows) {
                for (column in 0 until columns) {
                    val index = row * 3 + column
                    val x = column * spacing + if (row % 2 == 0) 16.dp.toPx() else -48.dp.toPx()
                    val y = row * spacing + 24.dp.toPx()
                    translate(x, y) {
                        rotate(
                            if (index % 2 == 0) -14f else 12f,
                            Offset(iconSize / 2, iconSize / 2),
                        ) {
                            scale(iconSize / 100f, iconSize / 100f, pivot = Offset.Zero) {
                                drawThemeMotif(theme, motifs[index % motifs.size])
                            }
                        }
                    }
                }
            }
        }
        content()
    }
}

internal fun DrawScope.drawGardenMotif(motif: IllustrationRole, colors: AppPalette) {
    when (motif) {
        IllustrationRole.HOUSEHOLD -> {
            drawRoundRect(colors.outline, Offset(23f, 40f), Size(54f, 45f), CornerRadius(5f))
            drawPath(
                Path().apply {
                    moveTo(12f, 44f)
                    lineTo(50f, 12f)
                    lineTo(88f, 44f)
                    close()
                },
                colors.secondary,
            )
            drawRoundRect(colors.background, Offset(43f, 60f), Size(16f, 25f), CornerRadius(3f))
            drawRoundRect(colors.background, Offset(29f, 50f), Size(10f, 10f), CornerRadius(2f))
            drawRoundRect(colors.background, Offset(63f, 50f), Size(10f, 10f), CornerRadius(2f))
        }
        IllustrationRole.JOY -> {
            repeat(10) { index ->
                val angle = index * Math.PI * 2 / 10
                drawLine(
                    StatusColors.yellow,
                    Offset(50 + cos(angle).toFloat() * 34, 50 + sin(angle).toFloat() * 34),
                    Offset(50 + cos(angle).toFloat() * 43, 50 + sin(angle).toFloat() * 43),
                    5f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
            drawCircle(StatusColors.yellow, 25f, Offset(50f, 50f))
            drawCircle(colors.deepAccent, 2f, Offset(43f, 47f))
            drawCircle(colors.deepAccent, 2f, Offset(57f, 47f))
            drawArc(
                colors.deepAccent,
                15f,
                150f,
                false,
                Offset(42f, 49f),
                Size(16f, 12f),
                style = Stroke(2f),
            )
        }
        IllustrationRole.AFFECTION -> {
            drawPath(
                Path().apply {
                    moveTo(50f, 84f)
                    cubicTo(37f, 73f, 12f, 55f, 12f, 34f)
                    cubicTo(12f, 12f, 39f, 9f, 50f, 29f)
                    cubicTo(61f, 9f, 88f, 12f, 88f, 34f)
                    cubicTo(88f, 55f, 63f, 73f, 50f, 84f)
                    close()
                },
                colors.secondary,
            )
            drawCircle(colors.background.copy(alpha = 0.7f), 5f, Offset(29f, 30f))
        }
        IllustrationRole.FRIEND -> {
            listOf(
                    Offset(15f, 74f),
                    Offset(29f, 69f),
                    Offset(43f, 72f),
                    Offset(57f, 62f),
                    Offset(69f, 49f),
                )
                .forEach { drawCircle(colors.outline, 12f, it) }
            drawCircle(colors.outline, 16f, Offset(69f, 32f))
            drawCircle(colors.ink, 2f, Offset(64f, 31f))
            drawCircle(colors.ink, 2f, Offset(74f, 31f))
            drawArc(
                colors.ink,
                10f,
                155f,
                false,
                Offset(64f, 33f),
                Size(10f, 7f),
                style = Stroke(1.7f),
            )
            drawRoundRect(colors.deepAccent, Offset(57f, 8f), Size(25f, 15f), CornerRadius(7f))
            drawRoundRect(colors.deepAccent, Offset(51f, 20f), Size(35f, 7f), CornerRadius(3f))
        }
        IllustrationRole.HELPER -> {
            drawRoundRect(colors.primary, Offset(35f, 39f), Size(44f, 40f), CornerRadius(9f))
            drawArc(
                colors.primary,
                180f,
                180f,
                false,
                Offset(43f, 18f),
                Size(29f, 35f),
                style = Stroke(5f),
            )
            drawRoundRect(
                colors.primary,
                Offset(72f, 46f),
                Size(19f, 23f),
                CornerRadius(4f),
                style = Stroke(5f),
            )
            drawPath(
                Path().apply {
                    moveTo(40f, 64f)
                    lineTo(17f, 47f)
                    lineTo(12f, 51f)
                    lineTo(36f, 78f)
                    close()
                },
                colors.primary,
            )
            drawLine(
                colors.primary,
                Offset(9f, 46f),
                Offset(20f, 40f),
                6f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            listOf(Offset(7f, 58f), Offset(16f, 66f), Offset(9f, 73f)).forEach {
                drawOval(colors.outline, it, Size(3f, 6f))
            }
        }
        IllustrationRole.BRAND -> {
            drawLine(
                colors.accent,
                Offset(50f, 83f),
                Offset(50f, 25f),
                4f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            listOf(Offset(26f, 40f), Offset(54f, 26f), Offset(28f, 64f), Offset(55f, 52f)).forEach {
                drawOval(colors.outline, it, Size(20f, 13f))
            }
            drawOval(colors.outline, Offset(44f, 10f), Size(12f, 19f))
            drawArc(
                StatusColors.yellow,
                180f,
                180f,
                false,
                Offset(31f, 81f),
                Size(38f, 12f),
                style = Stroke(4f),
            )
        }
        IllustrationRole.ACTIVITY -> {
            drawRoundRect(
                colors.secondary,
                Offset(37f, 8f),
                Size(27f, 22f),
                CornerRadius(7f),
                style = Stroke(5f),
            )
            drawLine(colors.secondary, Offset(50f, 30f), Offset(50f, 67f), 7f)
            drawRoundRect(colors.secondary, Offset(27f, 62f), Size(46f, 12f), CornerRadius(6f))
            repeat(4) {
                drawLine(
                    colors.secondary,
                    Offset(30f + it * 13f, 69f),
                    Offset(30f + it * 13f, 87f),
                    6f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
internal fun AppBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = AppTheme.colors.accentContainer,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppTheme.colors.accent,
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.ink,
                    )
                }
                AppIllustration(IllustrationRole.JOY, Modifier.padding(start = 8.dp).size(66.dp))
            }
            progress?.let {
                LinearProgressIndicator(
                    progress = { it },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = AppTheme.colors.accent,
                    trackColor = Color.White.copy(alpha = 0.7f),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(
                        IllustrationRole.HELPER,
                        IllustrationRole.BRAND,
                        IllustrationRole.FRIEND,
                        IllustrationRole.ACTIVITY,
                    )
                    .forEach { AppIllustration(it, Modifier.size(48.dp)) }
            }
        }
    }
}

@Composable
internal fun ChildBadge(child: Child, modifier: Modifier = Modifier) {
    Box(
        modifier.size(48.dp).background(childPaper(child), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        ChildIcon(child, Modifier.size(40.dp))
    }
}
