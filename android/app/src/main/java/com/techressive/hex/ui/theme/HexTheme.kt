package com.techressive.hex.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.techressive.hex.state.HexSkin
import com.techressive.hex.state.ThemeMode

val LocalHexSkin = compositionLocalOf { HexSkin.OLED_BLACK }

@Composable
fun HexTheme(
    mode: ThemeMode = ThemeMode.DARK,
    skin: HexSkin = HexSkin.OLED_BLACK,
    content: @Composable () -> Unit
) {
    val dynamicColorScheme = if (mode == ThemeMode.LIGHT) {
        lightColorScheme(
            primary = HexColors.CobaltBlue,
            secondary = HexColors.Lavender,
            tertiary = HexColors.SlateGray,
            background = Color(0xFFF6F8FC),
            surface = Color(0xFFFFFFFF),
            onPrimary = HexColors.PureWhite,
            onSecondary = HexColors.PureWhite,
            onBackground = Color(0xFF0F141C),
            onSurface = Color(0xFF0F141C)
        )
    } else {
        darkColorScheme(
            primary = HexColors.CobaltBlue,
            secondary = HexColors.Lavender,
            tertiary = HexColors.SlateGray,
            background = skin.bgOled,
            surface = skin.surfaceColor,
            onPrimary = HexColors.PureBlack,
            onSecondary = HexColors.PureBlack,
            onBackground = HexColors.TextPrimary,
            onSurface = HexColors.TextPrimary
        )
    }

    CompositionLocalProvider(LocalHexSkin provides skin) {
        MaterialTheme(
            colorScheme = dynamicColorScheme,
            typography = HexTypography,
            content = content
        )
    }
}

