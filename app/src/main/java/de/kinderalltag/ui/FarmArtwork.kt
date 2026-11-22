package de.kinderalltag.ui

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*

internal fun DrawScope.drawFarmMotif(art: ThemeArtwork, c: AppPalette) {
    when (art) {
        ThemeArtwork.COW -> cow(c)
        ThemeArtwork.PIG -> pig(c)
        ThemeArtwork.COMBINE -> combine(c)
        ThemeArtwork.BALER -> baler(c)
        ThemeArtwork.BARN -> barn(c)
        ThemeArtwork.WHEAT -> {
            rotate(-18f, Offset(50f, 85f)) { wheat(c, Offset(50f, 85f)) }
            rotate(18f, Offset(50f, 85f)) { wheat(c, Offset(50f, 85f)) }
            wheat(c, Offset(50f, 90f))
        }
        else -> error("Not a farm illustration: $art")
    }
}

private fun DrawScope.wheat(c: AppPalette, base: Offset) {
    drawLine(c.accent, base, base - Offset(0f, 68f), 2.5f, cap = StrokeCap.Round)
    repeat(4) { row ->
        val y = base.y - 30f - row * 10f
        rotate(-38f, Offset(base.x - 6f, y)) {
            drawOval(StatusColors.yellow, Offset(base.x - 10f, y - 9f), Size(8f, 17f))
        }
        rotate(38f, Offset(base.x + 6f, y)) {
            drawOval(StatusColors.yellow, Offset(base.x + 2f, y - 9f), Size(8f, 17f))
        }
    }
    drawOval(StatusColors.yellow, Offset(base.x - 4f, base.y - 77f), Size(8f, 15f))
}

private fun DrawScope.farmEyes(c: AppPalette) {
    listOf(35f, 65f).forEach { x ->
        drawCircle(c.trackerPaper, 4.5f, Offset(x, 47f))
        drawCircle(c.ink, 3f, Offset(x, 47f))
        drawCircle(Color.White, 1f, Offset(x - 1f, 46f))
    }
}

private fun DrawScope.cow(c: AppPalette) {
    drawLine(StatusColors.yellow, Offset(29f, 28f), Offset(25f, 12f), 8f, cap = StrokeCap.Round)
    drawLine(StatusColors.yellow, Offset(71f, 28f), Offset(75f, 12f), 8f, cap = StrokeCap.Round)
    rotate(25f, Offset(19f, 36f)) {
        drawOval(c.trackerPaper, Offset(4f, 28f), Size(30f, 17f))
        drawOval(c.artCoral, Offset(10f, 32f), Size(17f, 8f))
    }
    rotate(-25f, Offset(81f, 36f)) {
        drawOval(c.ink, Offset(66f, 28f), Size(30f, 17f))
        drawOval(c.artCoral, Offset(73f, 32f), Size(17f, 8f))
    }
    drawRoundRect(c.trackerPaper, Offset(21f, 22f), Size(58f, 65f), CornerRadius(24f))
    drawOval(c.ink, Offset(23f, 24f), Size(25f, 30f))
    drawCircle(c.ink, 9f, Offset(65f, 29f))
    drawOval(c.artCoral, Offset(24f, 58f), Size(52f, 30f))
    farmEyes(c)
    listOf(39f, 61f).forEach { drawOval(c.secondary, Offset(it - 3f, 68f), Size(6f, 8f)) }
    drawArc(c.secondary, 15f, 150f, false, Offset(43f, 74f), Size(14f, 8f), style = Stroke(2f))
}

private fun DrawScope.pig(c: AppPalette) {
    listOf(20f, 62f).forEach { x ->
        drawPath(
            Path().apply {
                moveTo(x, 36f)
                lineTo(x + 1f, 10f)
                quadraticTo(x + 18f, 13f, x + 22f, 34f)
                close()
            },
            c.artCoral,
        )
    }
    drawCircle(c.artCoral, 36f, Offset(50f, 53f))
    drawOval(c.secondaryContainer, Offset(27f, 55f), Size(46f, 26f))
    farmEyes(c)
    listOf(41f, 59f).forEach { drawCircle(c.secondary, 3.5f, Offset(it, 66f)) }
    drawArc(c.secondary, 15f, 150f, false, Offset(42f, 74f), Size(16f, 8f), style = Stroke(2f))
    drawArc(c.artCoral, -160f, 300f, false, Offset(79f, 58f), Size(14f, 14f), style = Stroke(4f))
}

private fun DrawScope.farmWheel(c: AppPalette, center: Offset, radius: Float) {
    drawCircle(c.ink, radius, center)
    drawCircle(StatusColors.yellow, radius * 0.51f, center)
    drawCircle(c.accent, radius * 0.18f, center)
}

private fun DrawScope.combine(c: AppPalette) {
    drawRoundRect(c.artGreen, Offset(11f, 33f), Size(54f, 36f), CornerRadius(6f))
    drawRoundRect(c.artGreen, Offset(51f, 23f), Size(28f, 39f), CornerRadius(5f))
    drawRoundRect(c.artBlue, Offset(56f, 28f), Size(18f, 23f), CornerRadius(3f))
    drawLine(c.primary, Offset(48f, 22f), Offset(81f, 22f), 5f, cap = StrokeCap.Round)
    drawLine(c.primary, Offset(21f, 33f), Offset(21f, 17f), 7f, cap = StrokeCap.Round)
    drawLine(c.primary, Offset(21f, 17f), Offset(4f, 17f), 7f, cap = StrokeCap.Round)
    drawRoundRect(StatusColors.yellow, Offset(16f, 41f), Size(29f, 8f), CornerRadius(3f))
    farmWheel(c, Offset(25f, 76f), 12f)
    farmWheel(c, Offset(63f, 72f), 17f)
    drawLine(c.primary, Offset(76f, 59f), Offset(85f, 74f), 6f)
    drawRoundRect(c.secondary, Offset(78f, 65f), Size(17f, 17f), CornerRadius(4f))
    drawLine(StatusColors.yellow, Offset(77f, 78f), Offset(97f, 78f), 4f)
    repeat(4) { index ->
        drawLine(c.accent, Offset(80f + index * 5f, 79f), Offset(80f + index * 5f, 87f), 2f)
    }
    translate(77f, 1f) { scale(0.22f, 0.22f, Offset.Zero) { wheat(c, Offset(50f, 90f)) } }
}

private fun DrawScope.baler(c: AppPalette) {
    drawLine(c.primary, Offset(7f, 62f), Offset(29f, 62f), 5f, cap = StrokeCap.Round)
    drawRoundRect(c.artGreen, Offset(25f, 28f), Size(49f, 43f), CornerRadius(10f))
    drawRoundRect(c.primary, Offset(35f, 20f), Size(30f, 12f), CornerRadius(4f))
    drawCircle(StatusColors.yellow, 12f, Offset(48f, 47f))
    drawArc(c.accent, 15f, 290f, false, Offset(41f, 40f), Size(14f, 14f), style = Stroke(2f))
    drawRoundRect(c.secondary, Offset(15f, 66f), Size(24f, 10f), CornerRadius(3f))
    repeat(4) { drawLine(c.accent, Offset(18f + it * 5f, 72f), Offset(18f + it * 5f, 81f), 2f) }
    farmWheel(c, Offset(51f, 76f), 12f)
    drawCircle(StatusColors.yellow, 16f, Offset(81f, 77f))
    listOf(11f, 6f).forEach {
        drawCircle(c.accent.copy(alpha = 0.6f), it, Offset(81f, 77f), style = Stroke(1.5f))
    }
    drawLine(c.accent, Offset(79f, 61f), Offset(79f, 93f), 2f)
}

private fun DrawScope.barn(c: AppPalette) {
    drawRoundRect(c.secondary, Offset(19f, 39f), Size(62f, 48f), CornerRadius(4f))
    drawPath(
        Path().apply {
            moveTo(10f, 43f)
            lineTo(25f, 21f)
            lineTo(50f, 10f)
            lineTo(75f, 21f)
            lineTo(90f, 43f)
            close()
        },
        c.primary,
    )
    drawRoundRect(c.householdSurface, Offset(30f, 51f), Size(40f, 36f), CornerRadius(2f))
    drawLine(c.secondary, Offset(50f, 53f), Offset(50f, 85f), 2f)
    drawLine(c.secondary, Offset(33f, 54f), Offset(67f, 84f), 2.5f)
    drawLine(c.secondary, Offset(67f, 54f), Offset(33f, 84f), 2.5f)
    drawCircle(c.artBlue, 6f, Offset(50f, 34f))
    translate(-1f, 49f) { scale(0.45f, 0.45f, Offset.Zero) { wheat(c, Offset(32f, 90f)) } }
}
