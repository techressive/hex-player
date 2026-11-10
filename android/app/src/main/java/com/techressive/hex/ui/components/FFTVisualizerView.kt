package com.techressive.hex.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.techressive.hex.audio.SpectrumAnalyzer
import com.techressive.hex.ui.theme.HexColors

@Composable
fun FFTVisualizerView(
    spectrumAnalyzer: SpectrumAnalyzer,
    modifier: Modifier = Modifier,
    height: Dp = 32.dp,
    barSpacing: Dp = 2.dp,
    primaryColor: Color = HexColors.CobaltBlue,
    secondaryColor: Color = HexColors.Lavender
) {
    val bands by spectrumAnalyzer.bands.collectAsState()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val totalBands = bands.size.coerceAtLeast(1)
        val spacingPx = barSpacing.toPx()
        val totalSpacing = spacingPx * (totalBands - 1)
        val barWidth = ((size.width - totalSpacing) / totalBands).coerceAtLeast(1f)
        val canvasHeight = size.height

        val brush = Brush.verticalGradient(
            colors = listOf(secondaryColor, primaryColor),
            startY = 0f,
            endY = canvasHeight
        )

        for (i in 0 until totalBands) {
            val magnitude = bands[i].coerceIn(0f, 1f)
            val barHeight = (canvasHeight * magnitude).coerceAtLeast(2f)
            val x = i * (barWidth + spacingPx)
            val y = canvasHeight - barHeight

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
