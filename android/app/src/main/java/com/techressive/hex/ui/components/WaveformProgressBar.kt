package com.techressive.hex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techressive.hex.ui.theme.HexColors

@Composable
fun WaveformProgressBar(
    currentPosition: Double,
    duration: Double,
    onSeek: (Double) -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color = HexColors.CobaltBlue,
    secondaryColor: Color = HexColors.Lavender
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    val safeDuration = if (duration > 0) duration else 1.0
    val displayPos = if (isDragging) dragPosition.toDouble() else currentPosition.coerceIn(0.0, safeDuration)
    val remaining = (safeDuration - displayPos).coerceAtLeast(0.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = (displayPos / safeDuration).toFloat().coerceIn(0f, 1f),
            onValueChange = { fraction ->
                isDragging = true
                dragPosition = (fraction * safeDuration).toFloat()
            },
            onValueChangeFinished = {
                isDragging = false
                onSeek(dragPosition.toDouble())
            },
            colors = SliderDefaults.colors(
                thumbColor = secondaryColor,
                activeTrackColor = primaryColor,
                inactiveTrackColor = HexColors.SlateGray.copy(alpha = 0.25f)
            ),
            modifier = Modifier.fillMaxWidth().height(24.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(displayPos),
                color = HexColors.SlateGray,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Text(
                text = "-${formatDuration(remaining)}",
                color = HexColors.SlateGray,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

private fun formatDuration(seconds: Double): String {
    val totalSec = seconds.toInt().coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
}
