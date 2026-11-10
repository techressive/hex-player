package com.techressive.hex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techressive.hex.metadata.Track
import com.techressive.hex.state.AppState
import com.techressive.hex.state.Screen
import com.techressive.hex.ui.theme.HexColors

@Composable
fun MiniPlayerBar(
    appState: AppState,
    modifier: Modifier = Modifier
) {
    val currentTrack by appState.currentTrack.collectAsState()
    val isPlaying by appState.audioPlayer.isPlaying.collectAsState()
    val currentPos by appState.audioPlayer.currentPosition.collectAsState()
    val duration by appState.audioPlayer.duration.collectAsState()
    val primaryAccent by appState.accentColorPrimary.collectAsState()
    val activeSkin by appState.activeSkin.collectAsState()

    val track = currentTrack ?: return

    val progress = if (duration > 0.0) (currentPos / duration).toFloat().coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(activeSkin.surfaceColor)
            .border(1.dp, activeSkin.cardBorderColor, RoundedCornerShape(16.dp))
            .clickable { appState.navigateTo(Screen.NowPlaying) }
    ) {
        Column {
            // Thin playback progress bar at the top
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = HexColors.CobaltBlue,
                trackColor = Color.Transparent
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art
                AlbumCoverView(
                    track = track,
                    modifier = Modifier.size(44.dp),
                    cornerRadius = 8.dp,
                    showFetchButton = false,
                    onClick = { appState.navigateTo(Screen.NowPlaying) }
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Artist
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = track.title,
                        color = HexColors.PureWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        color = HexColors.SlateGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Play / Pause button
                IconButton(
                    onClick = { appState.audioPlayer.togglePlayPause() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(HexColors.CobaltBlue)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = HexColors.PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Skip Next button
                IconButton(
                    onClick = { appState.skipNext() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip Next",
                        tint = HexColors.OffWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
