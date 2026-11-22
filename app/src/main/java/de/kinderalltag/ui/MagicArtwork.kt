package de.kinderalltag.ui

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*

internal fun DrawScope.drawMagicMotif(art: ThemeArtwork, c: AppPalette) {
    when (art) {
        ThemeArtwork.UNICORN -> unicorn(c)
        ThemeArtwork.FAIRY -> fairy(c)
        ThemeArtwork.PRINCESS -> princess(c)
        ThemeArtwork.ELF -> elf(c)
        ThemeArtwork.MAGIC_WAND -> magicWand(c)
        ThemeArtwork.CASTLE -> castle(c)
        ThemeArtwork.ENCHANTED_HEART -> {
            scale(0.73f, 0.73f, Offset(50f, 50f)) {
                drawGardenMotif(IllustrationRole.AFFECTION, c.copy(secondary = c.artCoral))
            }
            sparkle(Offset(17f, 29f), 10f)
            sparkle(Offset(80f, 66f), 8f)
            sparkle(Offset(71f, 16f), 5f)
        }
        else -> error("Not a magic illustration: $art")
    }
}

private val MagicSkin = Color(0xFFF3D4BD)

private fun DrawScope.sparkle(center: Offset, radius: Float) {
    drawPath(
        Path().apply {
            moveTo(center.x, center.y - radius)
            quadraticTo(
                center.x + radius * 0.2f,
                center.y - radius * 0.2f,
                center.x + radius,
                center.y,
            )
            quadraticTo(
                center.x + radius * 0.2f,
                center.y + radius * 0.2f,
                center.x,
                center.y + radius,
            )
            quadraticTo(
                center.x - radius * 0.2f,
                center.y + radius * 0.2f,
                center.x - radius,
                center.y,
            )
            quadraticTo(
                center.x - radius * 0.2f,
                center.y - radius * 0.2f,
                center.x,
                center.y - radius,
            )
            close()
        },
        StatusColors.yellow,
    )
}

internal fun DrawScope.drawMagicCrown(c: AppPalette) {
    drawPath(
        Path().apply {
            moveTo(10f, 12f)
            lineTo(30f, 28f)
            lineTo(50f, 0f)
            lineTo(70f, 28f)
            lineTo(90f, 12f)
            lineTo(80f, 48f)
            lineTo(20f, 48f)
            close()
        },
        StatusColors.yellow,
    )
    drawRoundRect(c.artCoral, Offset(19f, 40f), Size(62f, 8f), CornerRadius(3f))
    drawCircle(c.secondary, 5f, Offset(50f, 29f))
}

private fun DrawScope.magicFace(c: AppPalette, center: Offset) {
    listOf(-7f, 7f).forEach { x -> drawCircle(c.ink, 1.8f, Offset(center.x + x, center.y)) }
    drawCircle(c.artCoral, 3f, Offset(center.x - 11f, center.y + 5f))
    drawCircle(c.artCoral, 3f, Offset(center.x + 11f, center.y + 5f))
    drawArc(
        c.secondary,
        10f,
        160f,
        false,
        Offset(center.x - 4f, center.y + 3f),
        Size(8f, 6f),
        style = Stroke(1.5f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.unicorn(c: AppPalette) {
    // A side profile with a flowing three-color mane and a striped golden horn.
    drawPath(
        Path().apply {
            moveTo(48f, 26f)
            cubicTo(79f, 16f, 89f, 47f, 80f, 66f)
            quadraticTo(76f, 80f, 89f, 91f)
            lineTo(47f, 91f)
            close()
        },
        c.artBlue,
    )
    drawPath(
        Path().apply {
            moveTo(59f, 32f)
            cubicTo(83f, 32f, 65f, 73f, 79f, 88f)
        },
        c.artCoral,
        style = Stroke(9f, cap = StrokeCap.Round),
    )
    drawPath(
        Path().apply {
            moveTo(63f, 39f)
            quadraticTo(71f, 60f, 65f, 85f)
        },
        c.artGreen,
        style = Stroke(5f, cap = StrokeCap.Round),
    )
    drawPath(
        Path().apply {
            moveTo(29f, 35f)
            lineTo(29f, 5f)
            lineTo(43f, 33f)
            close()
        },
        StatusColors.yellow,
    )
    drawLine(c.accent, Offset(30f, 19f), Offset(36f, 22f), 1.5f)
    drawPath(
        Path().apply {
            moveTo(28f, 34f)
            quadraticTo(36f, 23f, 52f, 29f)
            lineTo(58f, 13f)
            quadraticTo(74f, 24f, 64f, 40f)
            quadraticTo(65f, 67f, 59f, 90f)
            lineTo(31f, 90f)
            lineTo(35f, 63f)
            quadraticTo(9f, 71f, 9f, 54f)
            close()
        },
        c.primaryContainer,
    )
    drawLine(c.artCoral, Offset(58f, 23f), Offset(58f, 34f), 4f, cap = StrokeCap.Round)
    drawArc(
        c.ink,
        5f,
        165f,
        false,
        Offset(34f, 40f),
        Size(15f, 10f),
        style = Stroke(2f, cap = StrokeCap.Round),
    )
    drawCircle(c.artCoral, 4f, Offset(36f, 56f))
    drawCircle(c.primary, 1.8f, Offset(17f, 55f))
    sparkle(Offset(82f, 18f), 7f)
}

private fun DrawScope.fairy(c: AppPalette) {
    rotate(-25f, Offset(35f, 48f)) { drawOval(c.artBlue, Offset(13f, 27f), Size(24f, 42f)) }
    rotate(25f, Offset(65f, 48f)) { drawOval(c.artBlue, Offset(63f, 27f), Size(24f, 42f)) }
    drawOval(c.artGreen, Offset(22f, 50f), Size(22f, 25f))
    drawOval(c.artGreen, Offset(56f, 50f), Size(22f, 25f))
    drawLine(c.primary, Offset(43f, 76f), Offset(40f, 91f), 4f, cap = StrokeCap.Round)
    drawLine(c.primary, Offset(57f, 76f), Offset(60f, 91f), 4f, cap = StrokeCap.Round)
    drawPath(
        Path().apply {
            moveTo(47f, 48f)
            lineTo(32f, 78f)
            quadraticTo(50f, 86f, 68f, 78f)
            lineTo(53f, 48f)
            close()
        },
        c.artCoral,
    )
    drawLine(MagicSkin, Offset(42f, 53f), Offset(27f, 62f), 4f, cap = StrokeCap.Round)
    drawLine(MagicSkin, Offset(58f, 53f), Offset(74f, 46f), 4f, cap = StrokeCap.Round)
    drawCircle(StatusColors.yellow, 17f, Offset(50f, 31f))
    drawCircle(StatusColors.yellow, 8f, Offset(50f, 13f))
    drawCircle(MagicSkin, 14f, Offset(50f, 34f))
    magicFace(c, Offset(50f, 33f))
    drawLine(c.primary, Offset(73f, 53f), Offset(81f, 31f), 2f, cap = StrokeCap.Round)
    sparkle(Offset(82f, 28f), 8f)
}

private fun DrawScope.princess(c: AppPalette) {
    drawRoundRect(StatusColors.yellow, Offset(28f, 22f), Size(44f, 47f), CornerRadius(19f))
    drawPath(
        Path().apply {
            moveTo(42f, 54f)
            lineTo(17f, 89f)
            quadraticTo(50f, 98f, 83f, 89f)
            lineTo(58f, 54f)
            close()
        },
        c.artCoral,
    )
    drawPath(
        Path().apply {
            moveTo(50f, 59f)
            lineTo(37f, 92f)
            lineTo(63f, 92f)
            close()
        },
        c.secondaryContainer,
    )
    drawLine(MagicSkin, Offset(40f, 57f), Offset(28f, 67f), 5f, cap = StrokeCap.Round)
    drawLine(MagicSkin, Offset(60f, 57f), Offset(72f, 67f), 5f, cap = StrokeCap.Round)
    drawCircle(MagicSkin, 16f, Offset(50f, 40f))
    magicFace(c, Offset(50f, 39f))
    translate(25f, 4f) { scale(0.5f, 0.5f, Offset.Zero) { drawMagicCrown(c) } }
    drawCircle(c.primary, 3f, Offset(50f, 61f))
    sparkle(Offset(15f, 32f), 6f)
    sparkle(Offset(84f, 45f), 5f)
}

private fun DrawScope.elf(c: AppPalette) {
    drawLine(c.primary, Offset(42f, 74f), Offset(38f, 91f), 6f, cap = StrokeCap.Round)
    drawLine(c.primary, Offset(58f, 74f), Offset(62f, 91f), 6f, cap = StrokeCap.Round)
    drawRoundRect(c.artGreen, Offset(32f, 48f), Size(36f, 32f), CornerRadius(9f))
    drawLine(MagicSkin, Offset(35f, 54f), Offset(24f, 68f), 5f, cap = StrokeCap.Round)
    drawLine(MagicSkin, Offset(65f, 54f), Offset(76f, 68f), 5f, cap = StrokeCap.Round)
    drawPath(
        Path().apply {
            moveTo(36f, 31f)
            lineTo(17f, 23f)
            quadraticTo(18f, 45f, 37f, 45f)
            close()
        },
        MagicSkin,
    )
    drawPath(
        Path().apply {
            moveTo(64f, 31f)
            lineTo(83f, 23f)
            quadraticTo(82f, 45f, 63f, 45f)
            close()
        },
        MagicSkin,
    )
    drawCircle(MagicSkin, 17f, Offset(50f, 36f))
    magicFace(c, Offset(50f, 36f))
    drawPath(
        Path().apply {
            moveTo(29f, 26f)
            quadraticTo(34f, 2f, 73f, 8f)
            quadraticTo(60f, 12f, 69f, 27f)
            close()
        },
        c.artGreen,
    )
    drawLine(c.primary, Offset(31f, 27f), Offset(69f, 27f), 4f, cap = StrokeCap.Round)
    drawLine(c.primary, Offset(34f, 68f), Offset(66f, 68f), 4f)
    drawRoundRect(StatusColors.yellow, Offset(46f, 64f), Size(8f, 8f), CornerRadius(2f))
    sparkle(Offset(88f, 55f), 6f)
}

private fun DrawScope.magicWand(c: AppPalette) {
    drawLine(c.primary, Offset(24f, 85f), Offset(65f, 38f), 7f, cap = StrokeCap.Round)
    drawLine(c.artCoral, Offset(28f, 81f), Offset(36f, 72f), 7f, cap = StrokeCap.Round)
    sparkle(Offset(65f, 34f), 23f)
    drawCircle(c.artCoral, 5f, Offset(65f, 34f))
    sparkle(Offset(25f, 29f), 8f)
    sparkle(Offset(83f, 70f), 7f)
    drawCircle(c.artBlue, 3f, Offset(46f, 13f))
    drawCircle(c.artGreen, 3f, Offset(60f, 83f))
}

private fun DrawScope.castle(c: AppPalette) {
    drawRoundRect(c.artBlue, Offset(28f, 48f), Size(44f, 40f), CornerRadius(3f))
    listOf(12f, 65f).forEach { x ->
        drawRoundRect(c.primaryContainer, Offset(x, 35f), Size(23f, 53f), CornerRadius(3f))
        drawPath(
            Path().apply {
                moveTo(x - 5f, 37f)
                lineTo(x + 11.5f, 13f)
                lineTo(x + 28f, 37f)
                close()
            },
            c.artCoral,
        )
        drawRoundRect(c.primary, Offset(x + 7f, 46f), Size(9f, 15f), CornerRadius(4.5f))
    }
    drawRoundRect(c.primary, Offset(41f, 65f), Size(18f, 23f), CornerRadius(9f, 9f))
    drawLine(c.primary, Offset(50f, 46f), Offset(50f, 10f), 2f)
    drawPath(
        Path().apply {
            moveTo(51f, 10f)
            lineTo(71f, 16f)
            lineTo(51f, 23f)
            close()
        },
        c.artGreen,
    )
    listOf(32f, 46f, 60f).forEach { x -> drawRect(c.artBlue, Offset(x, 41f), Size(8f, 10f)) }
    sparkle(Offset(12f, 13f), 6f)
    sparkle(Offset(88f, 14f), 5f)
}
