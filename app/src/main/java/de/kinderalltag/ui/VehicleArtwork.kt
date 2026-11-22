package de.kinderalltag.ui

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*

internal fun DrawScope.drawVehicleMotif(art: ThemeArtwork, c: AppPalette) {
    when (art) {
        ThemeArtwork.TRACTOR -> tractor(c)
        ThemeArtwork.EXCAVATOR -> excavator(c)
        ThemeArtwork.DUMP_TRUCK -> dumpTruck(c)
        ThemeArtwork.BUS -> bus(c)
        ThemeArtwork.GARAGE -> garage(c)
        ThemeArtwork.HEART_CAR -> {
            drawCar(c.copy(artBlue = c.artCoral))
            translate(39f, 47f) {
                scale(0.24f, 0.24f, Offset.Zero) { drawGardenMotif(IllustrationRole.AFFECTION, c) }
            }
        }
        else -> error("Not a vehicle illustration: $art")
    }
}

private fun DrawScope.wheel(c: AppPalette, center: Offset, radius: Float) {
    drawCircle(c.ink, radius, center)
    drawCircle(c.householdSurface, radius * 0.48f, center)
    drawCircle(c.secondary.copy(alpha = 0.6f), radius * 0.19f, center)
}

private fun DrawScope.tractor(c: AppPalette) {
    drawRoundRect(c.artGreen, Offset(26f, 48f), Size(61f, 24f), CornerRadius(6f))
    drawRoundRect(c.artGreen, Offset(30f, 22f), Size(32f, 37f), CornerRadius(4f))
    drawRoundRect(c.primaryContainer, Offset(36f, 28f), Size(20f, 23f), CornerRadius(3f))
    drawLine(c.accent, Offset(27f, 21f), Offset(65f, 21f), 6f, cap = StrokeCap.Round)
    drawRoundRect(c.artGreen, Offset(64f, 43f), Size(23f, 17f), CornerRadius(5f))
    drawLine(c.ink, Offset(74f, 43f), Offset(74f, 26f), 4f, cap = StrokeCap.Round)
    drawLine(c.ink, Offset(74f, 26f), Offset(80f, 26f), 4f, cap = StrokeCap.Round)
    drawRoundRect(StatusColors.yellow, Offset(81f, 49f), Size(8f, 7f), CornerRadius(2f))
    drawCircle(c.secondary, 5f, Offset(45f, 39f))
    drawRoundRect(c.artBlue, Offset(40f, 44f), Size(11f, 8f), CornerRadius(3f))
    wheel(c, Offset(34f, 75f), 18f)
    wheel(c, Offset(79f, 80f), 12f)
    drawArc(c.accent, 185f, 160f, false, Offset(12f, 54f), Size(44f, 44f), style = Stroke(4f))
}

private fun DrawScope.excavator(c: AppPalette) {
    drawRoundRect(c.ink, Offset(10f, 74f), Size(57f, 16f), CornerRadius(8f))
    drawRoundRect(c.trackerOutline, Offset(16f, 79f), Size(45f, 6f), CornerRadius(3f))
    drawRoundRect(StatusColors.yellow, Offset(17f, 53f), Size(46f, 23f), CornerRadius(6f))
    drawRoundRect(StatusColors.yellow, Offset(21f, 30f), Size(33f, 32f), CornerRadius(5f))
    drawRoundRect(c.primaryContainer, Offset(27f, 35f), Size(21f, 22f), CornerRadius(3f))
    drawLine(c.secondary, Offset(26f, 29f), Offset(53f, 29f), 4f, cap = StrokeCap.Round)
    drawLine(StatusColors.yellow, Offset(56f, 52f), Offset(74f, 15f), 11f, cap = StrokeCap.Round)
    drawLine(StatusColors.yellow, Offset(74f, 15f), Offset(91f, 43f), 9f, cap = StrokeCap.Round)
    drawLine(c.secondary.copy(alpha = 0.5f), Offset(58f, 47f), Offset(71f, 23f), 2.5f)
    drawCircle(c.artCoral, 5f, Offset(74f, 15f))
    drawPath(
        Path().apply {
            moveTo(84f, 39f)
            lineTo(97f, 47f)
            lineTo(91f, 62f)
            lineTo(77f, 58f)
            quadraticTo(84f, 53f, 84f, 39f)
            close()
        },
        c.artCoral,
    )
    drawCircle(c.secondary, 3f, Offset(56f, 59f))
}

private fun DrawScope.dumpTruck(c: AppPalette) {
    listOf(23f, 37f, 52f).forEach {
        drawCircle(c.deepAccent.copy(alpha = 0.4f), 8f, Offset(it, 32f))
    }
    drawPath(
        Path().apply {
            moveTo(8f, 29f)
            lineTo(62f, 29f)
            lineTo(59f, 62f)
            lineTo(21f, 62f)
            close()
        },
        c.artCoral,
    )
    repeat(3) { index ->
        val x = 24f + index * 12
        drawLine(
            c.secondary.copy(alpha = 0.35f),
            Offset(x, 36f),
            Offset(x + 5f, 55f),
            3f,
            cap = StrokeCap.Round,
        )
    }
    drawRoundRect(c.artBlue, Offset(65f, 43f), Size(25f, 29f), CornerRadius(5f))
    drawRoundRect(c.primaryContainer, Offset(70f, 48f), Size(14f, 12f), CornerRadius(3f))
    drawRoundRect(StatusColors.yellow, Offset(85f, 64f), Size(9f, 6f), CornerRadius(2f))
    drawLine(c.ink, Offset(14f, 69f), Offset(88f, 69f), 5f, cap = StrokeCap.Round)
    wheel(c, Offset(28f, 78f), 12f)
    wheel(c, Offset(76f, 78f), 12f)
}

private fun DrawScope.bus(c: AppPalette) {
    wheel(c, Offset(26f, 85f), 8f)
    wheel(c, Offset(74f, 85f), 8f)
    drawRoundRect(StatusColors.yellow, Offset(13f, 16f), Size(74f, 68f), CornerRadius(14f))
    drawRoundRect(c.primaryContainer, Offset(22f, 28f), Size(56f, 28f), CornerRadius(7f))
    drawLine(c.artBlue, Offset(50f, 29f), Offset(50f, 55f), 3f)
    drawCircle(c.ink, 2.5f, Offset(37f, 42f))
    drawCircle(c.ink, 2.5f, Offset(63f, 42f))
    drawRoundRect(c.artCoral, Offset(15f, 60f), Size(70f, 6f), CornerRadius(2f))
    drawCircle(c.trackerPaper, 5f, Offset(27f, 73f))
    drawCircle(c.trackerPaper, 5f, Offset(73f, 73f))
    drawArc(
        c.ink,
        10f,
        160f,
        false,
        Offset(42f, 68f),
        Size(16f, 10f),
        style = Stroke(2f, cap = StrokeCap.Round),
    )
    repeat(3) { drawCircle(c.secondary, 2f, Offset(42f + it * 8f, 22f)) }
}

private fun DrawScope.garage(c: AppPalette) {
    drawRoundRect(c.secondaryContainer, Offset(21f, 31f), Size(58f, 56f), CornerRadius(5f))
    drawPath(
        Path().apply {
            moveTo(12f, 37f)
            lineTo(50f, 11f)
            lineTo(88f, 37f)
            close()
        },
        c.artBlue,
    )
    drawRoundRect(c.trackerPaper, Offset(30f, 44f), Size(40f, 43f), CornerRadius(4f))
    listOf(55f, 66f, 77f).forEach { y -> drawLine(c.outline, Offset(32f, y), Offset(68f, y), 2f) }
    drawRoundRect(c.secondary, Offset(46f, 71f), Size(8f, 3f), CornerRadius(1f))
    drawRoundRect(c.artGreen, Offset(61f, 68f), Size(31f, 14f), CornerRadius(5f))
    drawRoundRect(c.artGreen, Offset(68f, 60f), Size(17f, 15f), CornerRadius(5f))
    drawRoundRect(c.primaryContainer, Offset(71f, 63f), Size(9f, 7f), CornerRadius(2f))
    wheel(c, Offset(67f, 83f), 5f)
    wheel(c, Offset(86f, 83f), 5f)
}
