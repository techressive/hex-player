package com.techressive.hex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techressive.hex.metadata.Track
import com.techressive.hex.state.AppState
import com.techressive.hex.state.Screen
import com.techressive.hex.ui.components.AlbumCoverView
import com.techressive.hex.ui.theme.HexColors

@Composable
fun AlbumDetailScreen(
    albumName: String,
    artistName: String,
    appState: AppState,
    modifier: Modifier = Modifier
) {
    val allTracks by appState.tracks.collectAsState()
    val currentTrack by appState.currentTrack.collectAsState()
    val activeSkin by appState.activeSkin.collectAsState()
    val primaryAccent by appState.accentColorPrimary.collectAsState()

    val albumTracks = remember(allTracks, albumName) {
        allTracks.filter { it.album.equals(albumName, ignoreCase = true) }
            .sortedBy { it.trackNumber ?: 0 }
    }

    val firstTrack = albumTracks.firstOrNull()
    val totalDurationSec = albumTracks.sumOf { it.durationMs } / 1000
    val totalMin = totalDurationSec / 60

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(activeSkin.bgOled)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { appState.goBack() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(activeSkin.surfaceColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = HexColors.PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "Album",
                    color = HexColors.PureWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Album Header Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumCoverView(
                    track = firstTrack,
                    modifier = Modifier.size(110.dp),
                    cornerRadius = 14.dp,
                    showFetchButton = false
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = albumName,
                        color = HexColors.PureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = artistName,
                        color = HexColors.Lavender,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            appState.navigateTo(Screen.ArtistBio(artistName))
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${albumTracks.size} tracks • $totalMin min",
                        color = HexColors.SlateGray,
                        fontSize = 12.sp
                    )
                }
            }

            // Action Buttons: Play All & Shuffle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (albumTracks.isNotEmpty()) {
                            appState.playTrack(albumTracks.first())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HexColors.CobaltBlue),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play All",
                        tint = HexColors.PureBlack,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play All", color = HexColors.PureBlack, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (albumTracks.isNotEmpty()) {
                            val shuffled = albumTracks.shuffled()
                            appState.playTrack(shuffled.first())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeSkin.surfaceColor),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, activeSkin.cardBorderColor, RoundedCornerShape(24.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = HexColors.Lavender,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle", color = HexColors.PureWhite, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Track List
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(albumTracks, key = { _, track -> track.id }) { index, track ->
                    val isPlayingThis = track.id == currentTrack?.id
                    val durSec = track.durationMs / 1000
                    val durStr = String.format("%d:%02d", durSec / 60, durSec % 60)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isPlayingThis) HexColors.CobaltBlue.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { appState.playTrack(track) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${track.trackNumber ?: (index + 1)}",
                            color = if (isPlayingThis) primaryAccent else HexColors.SlateGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(28.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = if (isPlayingThis) primaryAccent else HexColors.PureWhite,
                                fontSize = 14.sp,
                                fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (track.isHiRes) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Hi-Res • ${track.sampleRate.toInt()}Hz",
                                    color = HexColors.Lavender,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Text(
                            text = durStr,
                            color = HexColors.SlateGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
