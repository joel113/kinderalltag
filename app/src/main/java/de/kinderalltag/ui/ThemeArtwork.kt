package de.kinderalltag.ui

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import kotlin.math.*

internal enum class ThemeArtwork {
    GARDEN,
    RAINBOW,
    DINOSAUR,
    PUZZLE,
    BLOCKS,
    CAR,
    HOUSE,
    HEART,
    STAR,
    ROCKET,
    PLANET,
    SATELLITE,
    MOON,
    STATION,
    ORBIT_HEART,
    LION,
    FOX,
    RABBIT,
    ELEPHANT,
    WHALE,
    ANIMAL_HOUSE,
    PENGUIN,
    BUS,
    TRACTOR,
    EXCAVATOR,
    DUMP_TRUCK,
    GARAGE,
    HEART_CAR,
    TREE,
    FLOWER,
    WATERFALL,
    BUTTERFLY,
    MOUNTAINS,
    NATURE_COTTAGE,
    BLOSSOM_HEART,
    COW,
    PIG,
    COMBINE,
    BALER,
    BARN,
    WHEAT,
    UNICORN,
    FAIRY,
    PRINCESS,
    ELF,
    MAGIC_WAND,
    CASTLE,
    ENCHANTED_HEART,
}

internal fun artworkFor(theme: AppThemeId, role: IllustrationRole): ThemeArtwork =
    when (theme) {
        AppThemeId.GARDEN -> ThemeArtwork.GARDEN
        AppThemeId.PLAY ->
            when (role) {
                IllustrationRole.JOY -> ThemeArtwork.RAINBOW
                IllustrationRole.FRIEND -> ThemeArtwork.DINOSAUR
                IllustrationRole.HELPER -> ThemeArtwork.PUZZLE
                IllustrationRole.BRAND -> ThemeArtwork.BLOCKS
                IllustrationRole.ACTIVITY -> ThemeArtwork.CAR
                IllustrationRole.HOUSEHOLD -> ThemeArtwork.HOUSE
                IllustrationRole.AFFECTION -> ThemeArtwork.HEART
            }
        AppThemeId.SPACE ->
            when (role) {
                IllustrationRole.JOY -> ThemeArtwork.STAR
                IllustrationRole.FRIEND -> ThemeArtwork.SATELLITE
                IllustrationRole.HELPER -> ThemeArtwork.ROCKET
                IllustrationRole.BRAND -> ThemeArtwork.PLANET
                IllustrationRole.ACTIVITY -> ThemeArtwork.MOON
                IllustrationRole.HOUSEHOLD -> ThemeArtwork.STATION
                IllustrationRole.AFFECTION -> ThemeArtwork.ORBIT_HEART
            }
        AppThemeId.ANIMALS ->
            when (role) {
                IllustrationRole.JOY -> ThemeArtwork.LION
                IllustrationRole.FRIEND -> ThemeArtwork.RABBIT
                IllustrationRole.HELPER -> ThemeArtwork.ELEPHANT
                IllustrationRole.BRAND -> ThemeArtwork.FOX
                IllustrationRole.ACTIVITY -> ThemeArtwork.WHALE
                IllustrationRole.HOUSEHOLD -> ThemeArtwork.ANIMAL_HOUSE
                IllustrationRole.AFFECTION -> ThemeArtwork.PENGUIN
            }
        AppThemeId.MAGIC ->
            when (role) {
                IllustrationRole.JOY -> ThemeArtwork.FAIRY
                IllustrationRole.FRIEND -> ThemeArtwork.ELF
                IllustrationRole.HELPER -> ThemeArtwork.MAGIC_WAND
                IllustrationRole.BRAND -> ThemeArtwork.UNICORN
                IllustrationRole.ACTIVITY -> ThemeArtwork.PRINCESS
                IllustrationRole.HOUSEHOLD -> ThemeArtwork.CASTLE
                IllustrationRole.AFFECTION -> ThemeArtwork.ENCHANTED_HEART
            }
        AppThemeId.FARM ->
            when (role) {
                IllustrationRole.JOY -> ThemeArtwork.COW
                IllustrationRole.FRIEND -> ThemeArtwork.PIG
                IllustrationRole.HELPER -> ThemeArtwork.COMBINE
                IllustrationRole.BRAND -> ThemeArtwork.TRACTOR
                IllustrationRole.ACTIVITY -> ThemeArtwork.BALER
                IllustrationRole.HOUSEHOLD -> ThemeArtwork.BARN
                IllustrationRole.AFFECTION -> ThemeArtwork.WHEAT
            }
        AppThemeId.NATURE ->
            when (role) {
                IllustrationRole.JOY -> ThemeArtwork.FLOWER
                IllustrationRole.FRIEND -> ThemeArtwork.BUTTERFLY
                IllustrationRole.HELPER -> ThemeArtwork.WATERFALL
                IllustrationRole.BRAND -> ThemeArtwork.TREE
                IllustrationRole.ACTIVITY -> ThemeArtwork.MOUNTAINS
                IllustrationRole.HOUSEHOLD -> ThemeArtwork.NATURE_COTTAGE
                IllustrationRole.AFFECTION -> ThemeArtwork.BLOSSOM_HEART
            }
        AppThemeId.VEHICLES ->
            when (role) {
                IllustrationRole.JOY -> ThemeArtwork.CAR
                IllustrationRole.FRIEND -> ThemeArtwork.BUS
                IllustrationRole.HELPER -> ThemeArtwork.EXCAVATOR
                IllustrationRole.BRAND -> ThemeArtwork.TRACTOR
                IllustrationRole.ACTIVITY -> ThemeArtwork.DUMP_TRUCK
                IllustrationRole.HOUSEHOLD -> ThemeArtwork.GARAGE
                IllustrationRole.AFFECTION -> ThemeArtwork.HEART_CAR
            }
    }

internal fun DrawScope.drawThemeMotif(theme: AppThemeDefinition, role: IllustrationRole) {
    val c = theme.colors
    when (artworkFor(theme.id, role)) {
        ThemeArtwork.GARDEN -> drawGardenMotif(role, c)
        ThemeArtwork.UNICORN,
        ThemeArtwork.FAIRY,
        ThemeArtwork.PRINCESS,
        ThemeArtwork.ELF,
        ThemeArtwork.MAGIC_WAND,
        ThemeArtwork.CASTLE,
        ThemeArtwork.ENCHANTED_HEART -> drawMagicMotif(artworkFor(theme.id, role), c)
        ThemeArtwork.COW,
        ThemeArtwork.PIG,
        ThemeArtwork.COMBINE,
        ThemeArtwork.BALER,
        ThemeArtwork.BARN,
        ThemeArtwork.WHEAT -> drawFarmMotif(artworkFor(theme.id, role), c)
        ThemeArtwork.TREE,
        ThemeArtwork.FLOWER,
        ThemeArtwork.WATERFALL,
        ThemeArtwork.BUTTERFLY,
        ThemeArtwork.MOUNTAINS,
        ThemeArtwork.NATURE_COTTAGE,
        ThemeArtwork.BLOSSOM_HEART -> drawNatureMotif(artworkFor(theme.id, role), c)
        ThemeArtwork.BUS,
        ThemeArtwork.TRACTOR,
        ThemeArtwork.EXCAVATOR,
        ThemeArtwork.DUMP_TRUCK,
        ThemeArtwork.GARAGE,
        ThemeArtwork.HEART_CAR -> drawVehicleMotif(artworkFor(theme.id, role), c)
        ThemeArtwork.LION,
        ThemeArtwork.FOX,
        ThemeArtwork.RABBIT,
        ThemeArtwork.ELEPHANT,
        ThemeArtwork.WHALE,
        ThemeArtwork.ANIMAL_HOUSE,
        ThemeArtwork.PENGUIN -> drawAnimalMotif(artworkFor(theme.id, role), c)
        ThemeArtwork.STAR -> star(c)
        ThemeArtwork.RAINBOW -> rainbow(c)
        ThemeArtwork.DINOSAUR -> dinosaur(c)
        ThemeArtwork.PUZZLE -> puzzle(c)
        ThemeArtwork.BLOCKS -> blocks(c)
        ThemeArtwork.CAR -> drawCar(c)
        ThemeArtwork.HOUSE -> playHouse(c)
        ThemeArtwork.HEART ->
            drawGardenMotif(IllustrationRole.AFFECTION, c.copy(secondary = c.artCoral))
        ThemeArtwork.ROCKET -> rocket(c)
        ThemeArtwork.PLANET -> planet(c)
        ThemeArtwork.SATELLITE -> satellite(c)
        ThemeArtwork.MOON -> moon(c)
        ThemeArtwork.STATION -> station(c)
        ThemeArtwork.ORBIT_HEART -> {
            rotate(-24f, Offset(50f, 50f)) {
                drawOval(c.artBlue, Offset(2f, 27f), Size(96f, 48f), style = Stroke(4f))
            }
            scale(0.65f, 0.65f, Offset(50f, 50f)) {
                drawGardenMotif(IllustrationRole.AFFECTION, c.copy(secondary = c.artCoral))
            }
            drawCircle(StatusColors.yellow, 5f, Offset(12f, 62f))
            drawCircle(StatusColors.yellow, 3f, Offset(85f, 23f))
        }
    }
}

private fun DrawScope.face(c: AppPalette, center: Offset = Offset(50f, 50f), spacing: Float = 10f) {
    drawCircle(c.ink, 2.5f, center.copy(x = center.x - spacing))
    drawCircle(c.ink, 2.5f, center.copy(x = center.x + spacing))
    drawArc(
        c.ink,
        10f,
        160f,
        false,
        Offset(center.x - 6, center.y + 3),
        Size(12f, 9f),
        style = Stroke(2f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.star(c: AppPalette) {
    val path = Path()
    repeat(10) { index ->
        val radius = if (index % 2 == 0) 41f else 22f
        val angle = -PI / 2 + index * PI / 5
        val x = 50 + cos(angle).toFloat() * radius
        val y = 50 + sin(angle).toFloat() * radius
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, StatusColors.yellow)
    face(c, Offset(50f, 48f), 10f)
    drawCircle(c.artCoral, 4f, Offset(32f, 57f))
    drawCircle(c.artCoral, 4f, Offset(68f, 57f))
}

private fun DrawScope.rainbow(c: AppPalette) {
    val bands = listOf(c.artCoral, StatusColors.yellow, c.artGreen, c.artBlue)
    bands.forEachIndexed { index, color ->
        val inset = 10f + index * 9f
        drawArc(
            color,
            180f,
            180f,
            false,
            Offset(inset, 22f + index * 9),
            Size(100f - 2 * inset, 100f - 2 * inset),
            style = Stroke(10f),
        )
    }
    listOf(Offset(11f, 71f), Offset(22f, 76f), Offset(78f, 76f), Offset(89f, 71f)).forEach {
        drawCircle(c.trackerPaper, 11f, it)
    }
    translate(65f, -3f) { scale(0.3f, 0.3f, Offset.Zero) { star(c) } }
}

private fun DrawScope.puzzle(c: AppPalette) {
    drawRoundRect(c.artGreen, Offset(19f, 25f), Size(61f, 59f), CornerRadius(6f))
    drawCircle(c.artGreen, 12f, Offset(46f, 23f))
    drawCircle(c.artGreen, 12f, Offset(80f, 52f))
    drawCircle(c.background, 11f, Offset(18f, 54f))
    drawCircle(c.background, 11f, Offset(47f, 85f))
    drawCircle(c.trackerPaper.copy(alpha = 0.6f), 4f, Offset(61f, 40f))
}

private fun DrawScope.blocks(c: AppPalette) {
    drawRoundRect(c.artBlue, Offset(14f, 54f), Size(34f, 34f), CornerRadius(5f))
    drawRoundRect(c.artCoral, Offset(52f, 54f), Size(34f, 34f), CornerRadius(5f))
    drawRoundRect(StatusColors.yellow, Offset(33f, 16f), Size(34f, 34f), CornerRadius(5f))
    drawCircle(c.trackerPaper, 8f, Offset(31f, 71f), style = Stroke(3f))
    drawPath(
        Path().apply {
            moveTo(69f, 62f)
            lineTo(59f, 79f)
            lineTo(79f, 79f)
            close()
        },
        c.trackerPaper,
    )
    drawRoundRect(c.trackerPaper, Offset(43f, 26f), Size(14f, 14f), CornerRadius(2f))
}

internal fun DrawScope.drawCar(c: AppPalette) {
    drawPath(
        Path().apply {
            moveTo(22f, 46f)
            quadraticTo(27f, 17f, 45f, 17f)
            lineTo(62f, 17f)
            quadraticTo(75f, 20f, 79f, 47f)
            close()
        },
        c.artBlue,
    )
    drawRoundRect(c.trackerPaper, Offset(32f, 24f), Size(17f, 22f), CornerRadius(6f))
    drawRoundRect(c.trackerPaper, Offset(53f, 24f), Size(15f, 22f), CornerRadius(6f))
    drawRoundRect(c.artBlue, Offset(10f, 42f), Size(82f, 34f), CornerRadius(13f))
    drawRoundRect(StatusColors.yellow, Offset(10f, 49f), Size(9f, 10f), CornerRadius(3f))
    listOf(29f, 73f).forEach {
        drawCircle(c.ink.copy(alpha = 0.75f), 12f, Offset(it, 77f))
        drawCircle(c.artBlue, 5f, Offset(it, 77f))
    }
}

private fun DrawScope.dinosaur(c: AppPalette) {
    drawPath(
        Path().apply {
            moveTo(20f, 70f)
            quadraticTo(2f, 66f, 3f, 45f)
            quadraticTo(12f, 53f, 29f, 51f)
            close()
        },
        c.artGreen,
    )
    drawOval(c.artGreen, Offset(13f, 45f), Size(59f, 35f))
    drawRoundRect(c.artGreen, Offset(56f, 29f), Size(21f, 43f), CornerRadius(10f))
    drawOval(c.artGreen, Offset(56f, 18f), Size(36f, 32f))
    listOf(23f, 46f, 65f).forEach {
        drawRoundRect(c.artGreen, Offset(it, 68f), Size(12f, 21f), CornerRadius(5f))
    }
    repeat(4) { index ->
        val x = 20f + index * 10
        drawPath(
            Path().apply {
                moveTo(x, 48f)
                lineTo(x + 3f, 37f)
                lineTo(x + 9f, 45f)
                close()
            },
            c.artBlue,
        )
    }
    drawCircle(c.ink, 2.5f, Offset(81f, 29f))
    drawCircle(c.artCoral, 3.5f, Offset(84f, 38f))
    drawArc(c.ink, 10f, 160f, false, Offset(71f, 34f), Size(10f, 7f), style = Stroke(1.7f))
}

private fun DrawScope.playHouse(c: AppPalette) {
    drawRoundRect(Color(0xFFF3DFC0), Offset(21f, 42f), Size(58f, 43f), CornerRadius(6f))
    drawPath(
        Path().apply {
            moveTo(10f, 46f)
            lineTo(50f, 12f)
            lineTo(90f, 46f)
        },
        c.artGreen,
        style = Stroke(10f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    translate(31f, 40f) {
        scale(0.38f, 0.38f, Offset.Zero) {
            drawGardenMotif(IllustrationRole.AFFECTION, c.copy(secondary = c.artCoral))
        }
    }
}

private fun DrawScope.rocket(c: AppPalette) {
    drawPath(
        Path().apply {
            moveTo(39f, 72f)
            quadraticTo(31f, 88f, 50f, 99f)
            quadraticTo(69f, 88f, 61f, 72f)
            close()
        },
        StatusColors.yellow,
    )
    drawPath(
        Path().apply {
            moveTo(33f, 48f)
            lineTo(14f, 76f)
            lineTo(36f, 69f)
            close()
            moveTo(67f, 48f)
            lineTo(86f, 76f)
            lineTo(64f, 69f)
            close()
        },
        c.artCoral,
    )
    drawPath(
        Path().apply {
            moveTo(30f, 73f)
            quadraticTo(18f, 30f, 50f, 7f)
            quadraticTo(82f, 30f, 70f, 73f)
            close()
        },
        c.artBlue,
    )
    drawPath(
        Path().apply {
            moveTo(35f, 22f)
            quadraticTo(43f, 11f, 50f, 7f)
            quadraticTo(60f, 13f, 65f, 22f)
            close()
        },
        c.artCoral,
    )
    drawCircle(c.primary, 13f, Offset(50f, 44f))
    drawCircle(c.trackerPaper, 9f, Offset(50f, 44f))
    drawCircle(Color.White, 3f, Offset(47f, 41f))
    drawRoundRect(c.primary, Offset(31f, 68f), Size(38f, 7f), CornerRadius(3f))
}

private fun DrawScope.planet(c: AppPalette) {
    drawCircle(c.artBlue, 29f, Offset(50f, 50f))
    drawCircle(c.primary.copy(alpha = 0.2f), 8f, Offset(42f, 36f))
    drawCircle(c.primary.copy(alpha = 0.16f), 5f, Offset(60f, 62f))
    rotate(-25f, Offset(50f, 50f)) {
        drawOval(StatusColors.yellow, Offset(5f, 33f), Size(90f, 34f), style = Stroke(6f))
        drawArc(c.artBlue, 190f, 160f, false, Offset(22f, 23f), Size(56f, 56f), style = Stroke(7f))
    }
    drawCircle(StatusColors.yellow, 3f, Offset(83f, 14f))
    drawCircle(c.artCoral, 2.5f, Offset(18f, 82f))
}

private fun DrawScope.satellite(c: AppPalette) {
    listOf(5f, 71f).forEach { x ->
        drawRoundRect(c.artBlue, Offset(x, 38f), Size(24f, 29f), CornerRadius(4f))
        drawLine(c.primary, Offset(x + 12f, 40f), Offset(x + 12f, 65f), 1.5f)
        drawLine(c.primary, Offset(x + 2f, 52f), Offset(x + 22f, 52f), 1.5f)
    }
    drawLine(c.primary, Offset(24f, 52f), Offset(76f, 52f), 4f)
    drawRoundRect(StatusColors.yellow, Offset(34f, 30f), Size(32f, 46f), CornerRadius(9f))
    face(c, Offset(50f, 47f), 7f)
    drawLine(c.primary, Offset(50f, 30f), Offset(50f, 14f), 3f)
    drawCircle(c.artCoral, 5f, Offset(50f, 12f))
    drawLine(c.primary, Offset(43f, 77f), Offset(37f, 87f), 3f, cap = StrokeCap.Round)
    drawLine(c.primary, Offset(57f, 77f), Offset(63f, 87f), 3f, cap = StrokeCap.Round)
}

private fun DrawScope.moon(c: AppPalette) {
    drawCircle(StatusColors.yellow, 37f, Offset(49f, 50f))
    drawCircle(c.background, 34f, Offset(66f, 31f))
    drawCircle(c.artCoral.copy(alpha = 0.35f), 5f, Offset(25f, 53f))
    drawCircle(c.artCoral.copy(alpha = 0.3f), 3f, Offset(39f, 74f))
    drawCircle(c.ink, 2f, Offset(35f, 57f))
    drawArc(c.ink, 10f, 130f, false, Offset(38f, 60f), Size(11f, 9f), style = Stroke(2f))
    drawCircle(c.artBlue, 3f, Offset(83f, 17f))
    drawCircle(StatusColors.yellow, 4f, Offset(76f, 76f))
}

private fun DrawScope.station(c: AppPalette) {
    drawArc(c.artBlue, 180f, 180f, true, Offset(25f, 15f), Size(50f, 66f))
    drawArc(c.primary, 180f, 180f, false, Offset(25f, 15f), Size(50f, 66f), style = Stroke(3f))
    drawRoundRect(c.artGreen, Offset(12f, 46f), Size(76f, 31f), CornerRadius(13f))
    listOf(29f, 50f, 71f).forEach { drawCircle(c.trackerPaper, 6f, Offset(it, 60f)) }
    drawLine(c.primary, Offset(31f, 78f), Offset(24f, 91f), 5f, cap = StrokeCap.Round)
    drawLine(c.primary, Offset(69f, 78f), Offset(76f, 91f), 5f, cap = StrokeCap.Round)
    drawCircle(StatusColors.yellow, 3f, Offset(50f, 30f))
}
