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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import de.kinderalltag.domain.Child
import de.kinderalltag.domain.BearColor

// Palette and garden motifs inspired by pulli.png. All artwork is drawn locally.
internal object GardenColors {
    val Cream = Color(0xFFFFF8E8)
    val Ink = Color(0xFF333C38)
    val Blue = Color(0xFF166FA6)
    val Coral = Color(0xFFBF4658)
    val Red = Color(0xFFB3261E)
    val Mustard = Color(0xFFE8B643)
    val Sage = Color(0xFF79A78B)
    val Green = Color(0xFF32664D)
    val Burgundy = Color(0xFF713D49)
    val Sky = Color(0xFFE1F0FA)
    val Blush = Color(0xFFFCE6E4)
    val Mint = Color(0xFFE4F0DB)
}

internal enum class GardenMotif { SUN, WORM, WATERING_CAN, SPROUT, RAKE, HOUSE, HEART }

internal fun childColor(child: Child) = if (child.id == "haushalt") GardenColors.Green
    else when (child.bearColor) {
        BearColor.HONEY -> Color(0xFF865E12)
        BearColor.GREEN -> GardenColors.Green
        BearColor.ROSE -> Color(0xFF9E374B)
        else -> Color(child.bearColor.argb)
    }
internal fun childPaper(child: Child) = if (child.id == "haushalt") Color(0xFFFFF0CF)
    else lerp(GardenColors.Cream, childColor(child), 0.12f)

@Composable
internal fun ChildIcon(child: Child, modifier: Modifier = Modifier) {
    if (child.id == "haushalt") {
        GardenDrawing(GardenMotif.HOUSE, modifier)
    } else {
        Canvas(modifier.clearAndSetSemantics {}) {
            scale(size.width / 100f, size.height / 100f, pivot = Offset.Zero) {
                val fur = Color(child.bearColor.argb)
                val innerEar = lerp(GardenColors.Cream, fur, 0.18f)
                // Round ears, cream muzzle and rosy cheeks from bear.jpg.
                drawCircle(fur, 15f, Offset(24f, 25f))
                drawCircle(fur, 15f, Offset(76f, 25f))
                drawCircle(innerEar, 8f, Offset(24f, 25f))
                drawCircle(innerEar, 8f, Offset(76f, 25f))
                drawOval(fur, Offset(12f, 28f), Size(76f, 65f))
                drawCircle(GardenColors.Ink, 3.8f, Offset(35f, 54f))
                drawCircle(GardenColors.Ink, 3.8f, Offset(65f, 54f))
                drawCircle(Color.White, 1.1f, Offset(34f, 53f))
                drawCircle(Color.White, 1.1f, Offset(64f, 53f))
                drawCircle(Color(0xFFE99CAB), 5f, Offset(25f, 67f))
                drawCircle(Color(0xFFE99CAB), 5f, Offset(75f, 67f))
                drawOval(GardenColors.Cream, Offset(35f, 63f), Size(30f, 24f))
                drawPath(Path().apply {
                    moveTo(44f, 68f); quadraticTo(50f, 65f, 56f, 68f)
                    quadraticTo(57f, 70f, 50f, 75f); quadraticTo(43f, 70f, 44f, 68f); close()
                }, GardenColors.Ink)
                drawPath(Path().apply {
                    moveTo(50f, 74f); lineTo(50f, 79f)
                    quadraticTo(46f, 83f, 43f, 79f)
                    moveTo(50f, 79f); quadraticTo(54f, 83f, 57f, 79f)
                }, GardenColors.Ink, style = Stroke(2.3f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
        }
    }
}

@Composable
internal fun GardenDrawing(motif: GardenMotif, modifier: Modifier = Modifier) {
    Canvas(modifier.clearAndSetSemantics {}) {
        scale(size.width / 100f, size.height / 100f, pivot = Offset.Zero) { drawMotif(motif) }
    }
}

@Composable
internal fun GardenBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.background(GardenColors.Cream)) {
        Canvas(Modifier.matchParentSize().clipToBounds().alpha(0.16f).clearAndSetSemantics {}) {
            val spacing = 125.dp.toPx()
            val iconSize = 68.dp.toPx()
            val motifs = listOf(GardenMotif.SUN, GardenMotif.WATERING_CAN, GardenMotif.SPROUT,
                GardenMotif.RAKE, GardenMotif.WORM)
            val columns = kotlin.math.ceil(size.width / spacing).toInt() + 1
            val rows = kotlin.math.ceil(size.height / spacing).toInt() + 1
            for (row in 0 until rows) {
                for (column in 0 until columns) {
                    val index = row * 3 + column
                    val x = column * spacing + if (row % 2 == 0) 16.dp.toPx() else -48.dp.toPx()
                    val y = row * spacing + 24.dp.toPx()
                    translate(x, y) {
                        rotate(if (index % 2 == 0) -14f else 12f, Offset(iconSize / 2, iconSize / 2)) {
                            scale(iconSize / 100f, iconSize / 100f, pivot = Offset.Zero) {
                                drawMotif(motifs[index % motifs.size])
                            }
                        }
                    }
                }
            }
        }
        content()
    }
}

private fun DrawScope.drawMotif(motif: GardenMotif) {
    when (motif) {
        GardenMotif.HOUSE -> {
            drawRoundRect(GardenColors.Sage, Offset(23f, 40f), Size(54f, 45f), CornerRadius(5f))
            drawPath(Path().apply {
                moveTo(12f, 44f); lineTo(50f, 12f); lineTo(88f, 44f); close()
            }, GardenColors.Coral)
            drawRoundRect(GardenColors.Cream, Offset(43f, 60f), Size(16f, 25f), CornerRadius(3f))
            drawRoundRect(GardenColors.Cream, Offset(29f, 50f), Size(10f, 10f), CornerRadius(2f))
            drawRoundRect(GardenColors.Cream, Offset(63f, 50f), Size(10f, 10f), CornerRadius(2f))
        }
        GardenMotif.SUN -> {
            repeat(10) { index ->
                val angle = index * Math.PI * 2 / 10
                drawLine(GardenColors.Mustard,
                    Offset(50 + cos(angle).toFloat() * 34, 50 + sin(angle).toFloat() * 34),
                    Offset(50 + cos(angle).toFloat() * 43, 50 + sin(angle).toFloat() * 43), 5f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
            drawCircle(GardenColors.Mustard, 25f, Offset(50f, 50f))
            drawCircle(GardenColors.Burgundy, 2f, Offset(43f, 47f))
            drawCircle(GardenColors.Burgundy, 2f, Offset(57f, 47f))
            drawArc(GardenColors.Burgundy, 15f, 150f, false, Offset(42f, 49f), Size(16f, 12f), style = Stroke(2f))
        }
        GardenMotif.HEART -> {
            drawPath(Path().apply {
                moveTo(50f, 84f)
                cubicTo(37f, 73f, 12f, 55f, 12f, 34f)
                cubicTo(12f, 12f, 39f, 9f, 50f, 29f)
                cubicTo(61f, 9f, 88f, 12f, 88f, 34f)
                cubicTo(88f, 55f, 63f, 73f, 50f, 84f)
                close()
            }, GardenColors.Coral)
            drawCircle(GardenColors.Cream.copy(alpha = 0.7f), 5f, Offset(29f, 30f))
        }
        GardenMotif.WORM -> {
            listOf(Offset(15f, 74f), Offset(29f, 69f), Offset(43f, 72f), Offset(57f, 62f), Offset(69f, 49f)).forEach {
                drawCircle(GardenColors.Sage, 12f, it)
            }
            drawCircle(GardenColors.Sage, 16f, Offset(69f, 32f))
            drawCircle(GardenColors.Ink, 2f, Offset(64f, 31f))
            drawCircle(GardenColors.Ink, 2f, Offset(74f, 31f))
            drawArc(GardenColors.Ink, 10f, 155f, false, Offset(64f, 33f), Size(10f, 7f), style = Stroke(1.7f))
            drawRoundRect(GardenColors.Burgundy, Offset(57f, 8f), Size(25f, 15f), CornerRadius(7f))
            drawRoundRect(GardenColors.Burgundy, Offset(51f, 20f), Size(35f, 7f), CornerRadius(3f))
        }
        GardenMotif.WATERING_CAN -> {
            drawRoundRect(GardenColors.Blue, Offset(35f, 39f), Size(44f, 40f), CornerRadius(9f))
            drawArc(GardenColors.Blue, 180f, 180f, false, Offset(43f, 18f), Size(29f, 35f), style = Stroke(5f))
            drawRoundRect(GardenColors.Blue, Offset(72f, 46f), Size(19f, 23f), CornerRadius(4f), style = Stroke(5f))
            drawPath(Path().apply { moveTo(40f, 64f); lineTo(17f, 47f); lineTo(12f, 51f); lineTo(36f, 78f); close() }, GardenColors.Blue)
            drawLine(GardenColors.Blue, Offset(9f, 46f), Offset(20f, 40f), 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            listOf(Offset(7f, 58f), Offset(16f, 66f), Offset(9f, 73f)).forEach { drawOval(GardenColors.Sage, it, Size(3f, 6f)) }
        }
        GardenMotif.SPROUT -> {
            drawLine(GardenColors.Green, Offset(50f, 83f), Offset(50f, 25f), 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            listOf(Offset(26f, 40f), Offset(54f, 26f), Offset(28f, 64f), Offset(55f, 52f)).forEach {
                drawOval(GardenColors.Sage, it, Size(20f, 13f))
            }
            drawOval(GardenColors.Sage, Offset(44f, 10f), Size(12f, 19f))
            drawArc(GardenColors.Mustard, 180f, 180f, false, Offset(31f, 81f), Size(38f, 12f), style = Stroke(4f))
        }
        GardenMotif.RAKE -> {
            drawRoundRect(GardenColors.Coral, Offset(37f, 8f), Size(27f, 22f), CornerRadius(7f), style = Stroke(5f))
            drawLine(GardenColors.Coral, Offset(50f, 30f), Offset(50f, 67f), 7f)
            drawRoundRect(GardenColors.Coral, Offset(27f, 62f), Size(46f, 12f), CornerRadius(6f))
            repeat(4) { drawLine(GardenColors.Coral, Offset(30f + it * 13f, 69f), Offset(30f + it * 13f, 87f), 6f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round) }
        }
    }
}

@Composable
internal fun GardenBanner(title: String, subtitle: String, modifier: Modifier = Modifier, progress: Float? = null) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = GardenColors.Mint) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold,
                        color = GardenColors.Green)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = GardenColors.Ink)
                }
                GardenDrawing(GardenMotif.SUN, Modifier.padding(start = 8.dp).size(66.dp))
            }
            progress?.let {
                LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = GardenColors.Green, trackColor = Color.White.copy(alpha = 0.7f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(GardenMotif.WATERING_CAN, GardenMotif.SPROUT, GardenMotif.WORM, GardenMotif.RAKE).forEach {
                    GardenDrawing(it, Modifier.size(48.dp))
                }
            }
        }
    }
}

@Composable
internal fun ChildBadge(child: Child, modifier: Modifier = Modifier) {
    Box(modifier.size(48.dp).background(childPaper(child), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
        ChildIcon(child, Modifier.size(40.dp))
    }
}
