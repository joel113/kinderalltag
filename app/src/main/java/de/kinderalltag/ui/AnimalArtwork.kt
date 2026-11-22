package de.kinderalltag.ui

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import kotlin.math.*

// Original, scalable illustrations inspired by the soft animal portraits in animals.webp.
internal fun DrawScope.drawAnimalMotif(art: ThemeArtwork, c: AppPalette) {
    when (art) {
        ThemeArtwork.LION -> lion(c)
        ThemeArtwork.FOX -> fox(c)
        ThemeArtwork.RABBIT -> rabbit(c)
        ThemeArtwork.ELEPHANT -> elephant(c)
        ThemeArtwork.WHALE -> whale(c)
        ThemeArtwork.ANIMAL_HOUSE -> animalHouse(c)
        ThemeArtwork.PENGUIN -> penguin(c)
        else -> error("Not an animal illustration: $art")
    }
}

private fun DrawScope.animalEyes(c: AppPalette, y: Float, spacing: Float = 11f) {
    drawOval(c.ink, Offset(50f - spacing - 2.4f, y - 3.5f), Size(4.8f, 7f))
    drawOval(c.ink, Offset(50f + spacing - 2.4f, y - 3.5f), Size(4.8f, 7f))
}

private fun DrawScope.animalSmile(c: AppPalette, y: Float) {
    drawOval(c.ink, Offset(46f, y), Size(8f, 5f))
    drawLine(c.ink, Offset(50f, y + 4f), Offset(50f, y + 9f), 1.7f, cap = StrokeCap.Round)
    drawArc(
        c.ink,
        10f,
        160f,
        false,
        Offset(43f, y + 4f),
        Size(14f, 11f),
        style = Stroke(1.8f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.lion(c: AppPalette) {
    repeat(12) { index ->
        val angle = index * PI / 6
        drawCircle(
            c.artCoral,
            14f,
            Offset(50f + 32f * cos(angle).toFloat(), 50f + 32f * sin(angle).toFloat()),
        )
    }
    val fur = lerp(StatusColors.yellow, c.secondaryContainer, 0.3f)
    drawCircle(fur, 10f, Offset(29f, 29f))
    drawCircle(fur, 10f, Offset(71f, 29f))
    drawCircle(c.artCoral, 5f, Offset(29f, 29f))
    drawCircle(c.artCoral, 5f, Offset(71f, 29f))
    drawOval(fur, Offset(21f, 25f), Size(58f, 58f))
    animalEyes(c, 49f, 12f)
    drawOval(c.secondaryContainer, Offset(34f, 57f), Size(32f, 22f))
    animalSmile(c, 59f)
}

private fun DrawScope.fox(c: AppPalette) {
    drawPath(
        Path().apply {
            moveTo(17f, 45f)
            lineTo(17f, 13f)
            quadraticTo(22f, 8f, 41f, 34f)
            close()
            moveTo(59f, 34f)
            quadraticTo(78f, 8f, 83f, 13f)
            lineTo(83f, 45f)
            close()
        },
        c.artCoral,
    )
    drawPath(
        Path().apply {
            moveTo(23f, 34f)
            lineTo(24f, 20f)
            lineTo(34f, 34f)
            close()
            moveTo(66f, 34f)
            lineTo(76f, 20f)
            lineTo(77f, 34f)
            close()
        },
        c.secondaryContainer,
    )
    drawOval(c.artCoral, Offset(13f, 29f), Size(74f, 60f))
    drawPath(
        Path().apply {
            moveTo(12f, 52f)
            quadraticTo(28f, 49f, 50f, 69f)
            quadraticTo(72f, 49f, 88f, 52f)
            quadraticTo(80f, 88f, 50f, 91f)
            quadraticTo(20f, 88f, 12f, 52f)
            close()
        },
        c.secondaryContainer,
    )
    listOf(34f, 66f).forEach { x ->
        drawPath(
            Path().apply {
                moveTo(x - 5f, 59f)
                quadraticTo(x, 50f, x + 5f, 59f)
            },
            c.ink,
            style = Stroke(2.5f, cap = StrokeCap.Round),
        )
    }
    animalSmile(c, 69f)
}

private fun DrawScope.rabbit(c: AppPalette) {
    val fur = Color(0xFFE3C9A0)
    rotate(-12f, Offset(36f, 46f)) {
        drawOval(fur, Offset(27f, 3f), Size(19f, 53f))
        drawOval(c.artCoral.copy(alpha = 0.4f), Offset(33f, 11f), Size(7f, 32f))
    }
    rotate(12f, Offset(64f, 46f)) {
        drawOval(fur, Offset(54f, 3f), Size(19f, 53f))
        drawOval(c.artCoral.copy(alpha = 0.4f), Offset(60f, 11f), Size(7f, 32f))
    }
    drawOval(fur, Offset(17f, 39f), Size(66f, 54f))
    animalEyes(c, 64f)
    animalSmile(c, 74f)
    drawCircle(c.artCoral.copy(alpha = 0.4f), 4f, Offset(28f, 74f))
    drawCircle(c.artCoral.copy(alpha = 0.4f), 4f, Offset(72f, 74f))
}

private fun DrawScope.elephant(c: AppPalette) {
    drawOval(c.artBlue, Offset(4f, 23f), Size(36f, 55f))
    drawOval(c.artBlue, Offset(60f, 23f), Size(36f, 55f))
    drawOval(c.primaryContainer, Offset(12f, 33f), Size(20f, 35f))
    drawOval(c.primaryContainer, Offset(68f, 33f), Size(20f, 35f))
    drawOval(c.artBlue, Offset(25f, 17f), Size(50f, 62f))
    animalEyes(c, 48f, 12f)
    drawPath(
        Path().apply {
            moveTo(50f, 65f)
            lineTo(50f, 80f)
            quadraticTo(50f, 95f, 66f, 87f)
        },
        c.artBlue,
        style = Stroke(14f, cap = StrokeCap.Round),
    )
    drawArc(
        c.primary.copy(alpha = 0.3f),
        190f,
        150f,
        false,
        Offset(46f, 63f),
        Size(9f, 6f),
        style = Stroke(2f),
    )
    drawArc(
        c.primary.copy(alpha = 0.3f),
        190f,
        150f,
        false,
        Offset(46f, 70f),
        Size(9f, 6f),
        style = Stroke(2f),
    )
}

private fun DrawScope.whale(c: AppPalette) {
    drawPath(
        Path().apply {
            moveTo(67f, 60f)
            quadraticTo(76f, 60f, 81f, 45f)
            quadraticTo(67f, 27f, 79f, 29f)
            quadraticTo(84f, 30f, 85f, 38f)
            quadraticTo(95f, 26f, 99f, 34f)
            quadraticTo(98f, 47f, 88f, 49f)
            quadraticTo(91f, 75f, 65f, 80f)
            close()
        },
        c.artBlue,
    )
    drawOval(c.artBlue, Offset(5f, 36f), Size(73f, 51f))
    drawOval(c.primary.copy(alpha = 0.15f), Offset(38f, 69f), Size(21f, 10f))
    drawCircle(c.ink, 3f, Offset(23f, 61f))
    drawArc(
        c.ink,
        10f,
        145f,
        false,
        Offset(22f, 65f),
        Size(19f, 10f),
        style = Stroke(2f, cap = StrokeCap.Round),
    )
    drawOval(c.artBlue, Offset(26f, 14f), Size(7f, 13f))
    rotate(-35f, Offset(21f, 21f)) { drawOval(c.artBlue, Offset(16f, 14f), Size(6f, 12f)) }
    rotate(35f, Offset(39f, 20f)) { drawOval(c.artBlue, Offset(36f, 13f), Size(6f, 13f)) }
}

private fun DrawScope.animalHouse(c: AppPalette) {
    drawRoundRect(c.secondaryContainer, Offset(21f, 40f), Size(58f, 46f), CornerRadius(6f))
    drawPath(
        Path().apply {
            moveTo(12f, 44f)
            lineTo(50f, 13f)
            lineTo(88f, 44f)
        },
        c.secondary,
        style = Stroke(8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawRoundRect(c.secondary, Offset(42f, 67f), Size(16f, 19f), CornerRadius(6f))
    drawOval(c.artCoral, Offset(44f, 49f), Size(12f, 10f))
    listOf(Offset(40f, 46f), Offset(47f, 41f), Offset(55f, 41f), Offset(62f, 46f)).forEach {
        drawCircle(c.artCoral, 3f, it)
    }
    listOf(12f, 88f).forEach { x ->
        drawLine(c.accent, Offset(x, 88f), Offset(x, 72f), 2f)
        drawOval(c.artGreen, Offset(x - 8f, 73f), Size(10f, 6f))
        drawOval(c.artGreen, Offset(x, 78f), Size(10f, 6f))
    }
}

private fun DrawScope.penguin(c: AppPalette) {
    val feathers = Color(0xFF74756C)
    val belly = Color(0xFFFFF7E5)
    drawPath(
        Path().apply {
            moveTo(26f, 45f)
            quadraticTo(10f, 51f, 9f, 70f)
            quadraticTo(17f, 76f, 29f, 64f)
            close()
            moveTo(74f, 45f)
            quadraticTo(90f, 51f, 91f, 70f)
            quadraticTo(83f, 76f, 71f, 64f)
            close()
        },
        feathers,
    )
    drawOval(c.artCoral, Offset(29f, 85f), Size(18f, 9f))
    drawOval(c.artCoral, Offset(53f, 85f), Size(18f, 9f))
    drawOval(feathers, Offset(22f, 12f), Size(56f, 79f))
    drawOval(belly, Offset(30f, 29f), Size(25f, 36f))
    drawOval(belly, Offset(45f, 29f), Size(25f, 36f))
    drawOval(belly, Offset(31f, 49f), Size(38f, 38f))
    animalEyes(c, 47f, 10f)
    drawOval(StatusColors.yellow, Offset(43f, 54f), Size(14f, 9f))
    translate(40f, 65f) {
        scale(0.2f, 0.2f, Offset.Zero) {
            drawGardenMotif(IllustrationRole.AFFECTION, c.copy(secondary = c.artCoral))
        }
    }
}
