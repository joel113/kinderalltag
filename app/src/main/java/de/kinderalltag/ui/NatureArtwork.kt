package de.kinderalltag.ui

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*

internal fun DrawScope.drawNatureMotif(art: ThemeArtwork, c: AppPalette) {
    when (art) {
        ThemeArtwork.TREE -> natureTree(c)
        ThemeArtwork.FLOWER -> {
            drawLine(c.primary, Offset(50f, 43f), Offset(50f, 89f), 5f, cap = StrokeCap.Round)
            natureLeaf(c, Offset(31f, 65f))
            rotate(85f, Offset(58f, 75f)) { natureLeaf(c, Offset(58f, 75f)) }
            drawNatureBlossom(c, Offset(50f, 35f), 26f)
        }
        ThemeArtwork.WATERFALL -> waterfall(c)
        ThemeArtwork.BUTTERFLY -> butterfly(c)
        ThemeArtwork.MOUNTAINS -> mountains(c)
        ThemeArtwork.NATURE_COTTAGE -> natureCottage(c)
        ThemeArtwork.BLOSSOM_HEART -> {
            scale(0.78f, 0.78f, Offset(50f, 50f)) {
                drawGardenMotif(IllustrationRole.AFFECTION, c.copy(secondary = c.artCoral))
            }
            natureLeaf(c, Offset(13f, 66f))
            drawNatureBlossom(c, Offset(77f, 28f), 13f)
        }
        else -> error("Not a nature illustration: $art")
    }
}

internal fun DrawScope.drawNatureBlossom(c: AppPalette, center: Offset, radius: Float) {
    repeat(6) { petal ->
        rotate(petal * 60f, center) {
            drawOval(
                c.artCoral,
                Offset(center.x - radius * 0.36f, center.y - radius),
                Size(radius * 0.72f, radius * 1.15f),
            )
        }
    }
    drawCircle(StatusColors.yellow, radius * 0.36f, center)
}

private fun DrawScope.natureLeaf(c: AppPalette, origin: Offset) {
    translate(origin.x, origin.y) {
        drawPath(
            Path().apply {
                moveTo(0f, 0f)
                quadraticTo(22f, -12f, 25f, 10f)
                quadraticTo(6f, 19f, 0f, 0f)
                close()
            },
            c.artGreen,
        )
        drawLine(c.primary, Offset(5f, 3f), Offset(21f, 10f), 1.5f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.natureTree(c: AppPalette) {
    drawOval(c.primaryContainer, Offset(12f, 83f), Size(76f, 10f))
    drawRoundRect(c.deepAccent, Offset(44f, 44f), Size(12f, 44f), CornerRadius(4f))
    drawCircle(c.artGreen, 24f, Offset(29f, 44f))
    drawCircle(c.artGreen, 24f, Offset(71f, 44f))
    drawCircle(c.artGreen, 28f, Offset(50f, 29f))
    drawLine(c.primary, Offset(50f, 69f), Offset(50f, 39f), 4f, cap = StrokeCap.Round)
    drawLine(c.primary, Offset(50f, 60f), Offset(33f, 48f), 3f, cap = StrokeCap.Round)
    drawLine(c.primary, Offset(50f, 54f), Offset(67f, 42f), 3f, cap = StrokeCap.Round)
    drawNatureBlossom(c, Offset(28f, 34f), 7f)
    drawNatureBlossom(c, Offset(65f, 23f), 6f)
}

private fun DrawScope.waterfall(c: AppPalette) {
    drawPath(
        Path().apply {
            moveTo(10f, 73f)
            lineTo(17f, 28f)
            quadraticTo(23f, 14f, 38f, 19f)
            lineTo(73f, 19f)
            lineTo(91f, 75f)
            close()
        },
        c.trackerOutline,
    )
    drawRoundRect(c.artGreen, Offset(13f, 14f), Size(65f, 15f), CornerRadius(7f))
    drawOval(c.artBlue, Offset(8f, 69f), Size(85f, 23f))
    drawPath(
        Path().apply {
            moveTo(39f, 23f)
            lineTo(65f, 23f)
            quadraticTo(60f, 48f, 71f, 78f)
            lineTo(32f, 78f)
            quadraticTo(43f, 51f, 39f, 23f)
            close()
        },
        c.artBlue,
    )
    listOf(47f, 57f).forEach { x ->
        drawLine(c.accentContainer, Offset(x, 32f), Offset(x, 64f), 3f, cap = StrokeCap.Round)
    }
    drawArc(
        c.accentContainer,
        0f,
        180f,
        false,
        Offset(24f, 71f),
        Size(56f, 12f),
        style = Stroke(2f),
    )
    listOf(Offset(32f, 70f), Offset(71f, 68f), Offset(76f, 76f)).forEach {
        drawCircle(c.accentContainer, 2f, it)
    }
    drawNatureBlossom(c, Offset(22f, 21f), 8f)
}

private fun DrawScope.butterfly(c: AppPalette) {
    rotate(-12f, Offset(50f, 50f)) {
        drawOval(c.artCoral, Offset(7f, 18f), Size(42f, 44f))
        drawOval(c.artCoral, Offset(51f, 18f), Size(42f, 44f))
        drawOval(c.artBlue, Offset(16f, 53f), Size(33f, 31f))
        drawOval(c.artBlue, Offset(51f, 53f), Size(33f, 31f))
        drawCircle(c.secondaryContainer, 8f, Offset(29f, 37f))
        drawCircle(c.secondaryContainer, 8f, Offset(71f, 37f))
        drawLine(c.ink, Offset(47f, 32f), Offset(40f, 17f), 2f, cap = StrokeCap.Round)
        drawLine(c.ink, Offset(53f, 32f), Offset(60f, 17f), 2f, cap = StrokeCap.Round)
        drawRoundRect(c.primary, Offset(45f, 34f), Size(10f, 43f), CornerRadius(5f))
        drawCircle(c.primary, 8f, Offset(50f, 34f))
        drawCircle(c.trackerPaper, 1.5f, Offset(47f, 32f))
        drawCircle(c.trackerPaper, 1.5f, Offset(53f, 32f))
    }
}

private fun DrawScope.mountains(c: AppPalette) {
    drawCircle(StatusColors.yellow, 12f, Offset(78f, 22f))
    drawPath(
        Path().apply {
            moveTo(4f, 78f)
            lineTo(37f, 14f)
            lineTo(70f, 78f)
            close()
        },
        c.artGreen,
    )
    drawPath(
        Path().apply {
            moveTo(38f, 81f)
            lineTo(66f, 31f)
            lineTo(96f, 81f)
            close()
        },
        c.primary,
    )
    drawPath(
        Path().apply {
            moveTo(25f, 37f)
            lineTo(37f, 14f)
            lineTo(49f, 37f)
            lineTo(38f, 31f)
            lineTo(32f, 39f)
            close()
        },
        c.trackerPaper,
    )
    drawPath(
        Path().apply {
            moveTo(47f, 71f)
            cubicTo(79f, 79f, 17f, 83f, 39f, 95f)
            lineTo(63f, 95f)
            cubicTo(36f, 83f, 90f, 78f, 55f, 71f)
            close()
        },
        c.artBlue,
    )
}

private fun DrawScope.natureCottage(c: AppPalette) {
    drawOval(c.primaryContainer, Offset(4f, 76f), Size(92f, 17f))
    translate(-3f, 27f) { scale(0.58f, 0.58f, Offset.Zero) { natureTree(c) } }
    drawRoundRect(c.householdSurface, Offset(36f, 44f), Size(47f, 40f), CornerRadius(4f))
    drawPath(
        Path().apply {
            moveTo(28f, 48f)
            lineTo(59f, 19f)
            lineTo(91f, 48f)
            close()
        },
        c.artGreen,
    )
    drawRoundRect(c.deepAccent, Offset(54f, 60f), Size(14f, 24f), CornerRadius(5f))
    drawCircle(StatusColors.yellow, 1.5f, Offset(64f, 73f))
    drawRoundRect(c.artBlue, Offset(72f, 55f), Size(9f, 12f), CornerRadius(2f))
    drawNatureBlossom(c, Offset(87f, 80f), 9f)
}
