package com.techressive.hex.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techressive.hex.metadata.LyricLine
import com.techressive.hex.ui.theme.HexColors

@Composable
fun SyncdLyricsWidget(
    lyrics: List<LyricLine>,
    currentPosition: Double,
    onSeekTo: (Double) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = HexColors.CobaltBlue
) {
    if (lyrics.isEmpty()) return

    // Find active line index
    val activeIndex = remember(lyrics, currentPosition) {
        val idx = lyrics.indexOfLast { it.timestamp <= currentPosition }
        if (idx >= 0) idx else 0
    }

    val prevLine = if (activeIndex > 0) lyrics[activeIndex - 1] else null
    val currentLine = lyrics.getOrNull(activeIndex)
    val nextLine = if (activeIndex < lyrics.size - 1) lyrics[activeIndex + 1] else null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HexColors.DarkGray.copy(alpha = 0.5f))
            .border(1.dp, HexColors.LightSlate.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Previous Line
            Text(
                text = prevLine?.text ?: " ",
                color = HexColors.SlateGray.copy(alpha = 0.45f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (prevLine != null) Modifier.clickable { onSeekTo(prevLine.timestamp) } else Modifier)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Current Line (Highlighted & Centered)
            Text(
                text = currentLine?.text ?: " ",
                color = accentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (currentLine != null) Modifier.clickable { onSeekTo(currentLine.timestamp) } else Modifier)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Next Line
            Text(
                text = nextLine?.text ?: " ",
                color = HexColors.SlateGray.copy(alpha = 0.55f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (nextLine != null) Modifier.clickable { onSeekTo(nextLine.timestamp) } else Modifier)
            )
        }
    }
}
