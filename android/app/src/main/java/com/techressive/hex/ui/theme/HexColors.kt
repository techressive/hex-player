package com.techressive.hex.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object HexColors {
    // Official HEX Brand Identity Palette
    val CobaltBlue = Color(0xFF5B7CFA)
    val Lavender = Color(0xFFA99BFF)
    val SlateGray = Color(0xFFA6A9B0)
    val OledBlack = Color(0xFF050505)
    val PureBlack = Color(0xFF000000)

    // Audiophile & Surface Palette
    val CardSurface = Color(0xFF111114)
    val CardSurfaceHover = Color(0xFF1A1A1F)
    val CardBorder = Color(0xFF222228)
    val DeepCanvas = Color(0xFF0A0A0C)

    val TextPrimary = Color(0xFFEDEDED)
    val TextSecondary = Color(0xFFA6A9B0)
    val TextMuted = Color(0xFF71717A)

    val PureWhite = Color(0xFFFFFFFF)
    val OffWhite = Color(0xFFE2E8F0)
    val DarkGray = Color(0xFF1E1E24)
    val LightSlate = Color(0xFFCBD5E1)

    val EmeraldDSD = Color(0xFF00E676)
    val GoldMaster = Color(0xFFFFD700)
    val CyanHiRes = Color(0xFF64D2FF)

    // Official 3-Pillar Hexagon Gradient
    val BrandGradient = Brush.linearGradient(
        colors = listOf(CobaltBlue, Lavender)
    )

    val ReverseBrandGradient = Brush.linearGradient(
        colors = listOf(Lavender, CobaltBlue)
    )
}
