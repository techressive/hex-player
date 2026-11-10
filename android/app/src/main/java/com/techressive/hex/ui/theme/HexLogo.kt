package com.techressive.hex.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class HexLogoStyle {
    GRADIENT,
    MONOCHROME_WHITE,
    MONOCHROME_BLACK
}

@Composable
fun HexLogo(
    size: Dp = 24.dp,
    style: HexLogoStyle = HexLogoStyle.GRADIENT,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Pillar 1: Left vertical pillar
        val path1 = Path().apply {
            moveTo(w * 0.22f, h * 0.25f)
            lineTo(w * 0.36f, h * 0.17f)
            lineTo(w * 0.36f, h * 0.83f)
            lineTo(w * 0.22f, h * 0.75f)
            close()
        }

        // Pillar 2: Center-Right pillar
        val path2 = Path().apply {
            moveTo(w * 0.43f, h * 0.35f)
            lineTo(w * 0.57f, h * 0.27f)
            lineTo(w * 0.57f, h * 0.93f)
            lineTo(w * 0.43f, h * 0.85f)
            close()
        }

        // Pillar 3: Right / Header pillar
        val path3 = Path().apply {
            moveTo(w * 0.64f, h * 0.15f)
            lineTo(w * 0.78f, h * 0.07f)
            lineTo(w * 0.78f, h * 0.73f)
            lineTo(w * 0.64f, h * 0.65f)
            close()
        }

        when (style) {
            HexLogoStyle.GRADIENT -> {
                drawPath(path1, brush = HexColors.BrandGradient)
                drawPath(path2, brush = HexColors.BrandGradient)
                drawPath(path3, brush = HexColors.ReverseBrandGradient)
            }
            HexLogoStyle.MONOCHROME_WHITE -> {
                drawPath(path1, color = Color.White)
                drawPath(path2, color = Color.White)
                drawPath(path3, color = Color.White)
            }
            HexLogoStyle.MONOCHROME_BLACK -> {
                drawPath(path1, color = Color.Black)
                drawPath(path2, color = Color.Black)
                drawPath(path3, color = Color.Black)
            }
        }
    }
}

@Composable
fun HexBrandHeader(
    size: Dp = 22.dp,
    showWordmark: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HexLogo(size = size, style = HexLogoStyle.GRADIENT)
        if (showWordmark) {
            Text(
                text = "hex",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.95f).sp,
                color = HexColors.TextPrimary,
                letterSpacing = 0.5.sp
            )
        }
    }
}
