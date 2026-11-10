package com.techressive.hex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.techressive.hex.state.AppState
import com.techressive.hex.state.Screen
import com.techressive.hex.ui.components.AlbumCoverView
import com.techressive.hex.ui.components.FFTVisualizerView
import com.techressive.hex.ui.components.SyncdLyricsWidget
import com.techressive.hex.ui.components.WaveformProgressBar
import com.techressive.hex.ui.theme.HexBrandHeader
import com.techressive.hex.ui.theme.HexColors

@Composable
fun NowPlayingScreen(
    appState: AppState,
    modifier: Modifier = Modifier
) {
    val currentTrack by appState.currentTrack.collectAsState()
    val isPlaying by appState.audioPlayer.isPlaying.collectAsState()
    val currentPosition by appState.audioPlayer.currentPosition.collectAsState()
    val duration by appState.audioPlayer.duration.collectAsState()
    val isShuffled by appState.audioPlayer.isShuffled.collectAsState()
    val repeatMode by appState.audioPlayer.repeatMode.collectAsState()
    val volume by appState.audioPlayer.volume.collectAsState()
    val lyrics by appState.lyrics.collectAsState()
    val canGoBack by appState.canGoBack.collectAsState()
    val primaryAccent by appState.accentColorPrimary.collectAsState()
    val secondaryAccent by appState.accentColorSecondary.collectAsState()
    val activeSkin by appState.activeSkin.collectAsState()
    val isFav = currentTrack?.let { appState.isFavorite(it) } ?: false

    val scrollState = rememberScrollState()
    val themeMode by appState.themeMode.collectAsState()

    // Dynamic gradient background derived from cover art, active skin, and themeMode
    val bgBrush = if (themeMode == com.techressive.hex.state.ThemeMode.LIGHT) {
        Brush.verticalGradient(
            colors = listOf(
                primaryAccent.copy(alpha = 0.18f),
                Color(0xFFF6F8FC).copy(alpha = 0.9f),
                Color(0xFFF6F8FC)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                primaryAccent.copy(alpha = 0.28f),
                activeSkin.bgOled.copy(alpha = 0.85f),
                activeSkin.bgOled
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    // Swipe down gesture to go back!
                    if (dragAmount > 38f) {
                        appState.goBack()
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Universal Top-Left Back Button
                if (canGoBack) {
                    IconButton(
                        onClick = { appState.goBack() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(activeSkin.surfaceColor.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = HexColors.OffWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = { appState.navigateTo(Screen.Library) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(activeSkin.surfaceColor.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = "Library",
                            tint = HexColors.OffWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Brand Header
                HexBrandHeader(size = 20.dp)

                // Quick Equalizer & Settings actions
                Row {
                    IconButton(
                        onClick = { appState.showEqualizer(true) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = "Equalizer",
                            tint = primaryAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = { appState.navigateTo(Screen.Settings) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = HexColors.SlateGray,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Centerpiece Album Artwork
            AlbumCoverView(
                track = currentTrack,
                cornerRadius = 20.dp,
                showFetchButton = true,
                onFetchArtwork = {
                    currentTrack?.let { appState.fetchArtworkForTrack(it) }
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Title, Album & Artist info
            Text(
                text = currentTrack?.title ?: "No Track Selected",
                color = HexColors.PureWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Artist link - navigates directly to Biography screen!
            Text(
                text = currentTrack?.artist ?: "Select music in Library",
                color = secondaryAccent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable {
                    currentTrack?.artist?.let {
                        appState.navigateTo(Screen.ArtistBio(it))
                    }
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = currentTrack?.album ?: "",
                color = HexColors.SlateGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Hi-Res Badge in ONE LINE: strictly "Hi-Res" not split
            if (currentTrack?.isHiRes == true) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(HexColors.CobaltBlue.copy(alpha = 0.25f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Hi-Res • ${currentTrack?.formatString()}",
                        color = HexColors.Lavender,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            // Visualizer removed as requested for a cleaner, high-focus playback view

            // Waveform scrubber bar
            WaveformProgressBar(
                currentPosition = currentPosition,
                duration = duration,
                onSeek = { appState.audioPlayer.seekTo(it) },
                primaryColor = primaryAccent,
                secondaryColor = secondaryAccent
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3-Line Interactive Lyrics Widget with FullScreen expansion button
            if (lyrics.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    SyncdLyricsWidget(
                        lyrics = lyrics,
                        currentPosition = currentPosition,
                        onSeekTo = { appState.audioPlayer.seekTo(it) },
                        accentColor = secondaryAccent
                    )
                    // Full Screen lyrics icon button
                    IconButton(
                        onClick = { appState.navigateTo(Screen.FullScreenLyrics) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(HexColors.PureBlack.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = "Full Lyrics",
                            tint = secondaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Transport Controls (Centered Play/Pause with glowing accent)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Shuffle
                IconButton(onClick = { appState.audioPlayer.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffled) primaryAccent else HexColors.SlateGray,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = { appState.skipPrevious() }
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = HexColors.PureWhite,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Centered Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(16.dp, CircleShape, ambientColor = primaryAccent, spotColor = secondaryAccent)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(primaryAccent, secondaryAccent))
                        )
                        .clickable { appState.audioPlayer.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = HexColors.PureBlack,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = { appState.skipNext() }
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = HexColors.PureWhite,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Repeat
                IconButton(onClick = { appState.audioPlayer.toggleRepeat() }) {
                    Icon(
                        imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) primaryAccent else HexColors.SlateGray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Auxiliary Bar (Favorite + Volume Slider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { currentTrack?.let { appState.toggleFavorite(it) } }
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFav) Color(0xFFFF4D6D) else HexColors.SlateGray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Default.VolumeDown,
                    contentDescription = "Volume Down",
                    tint = HexColors.SlateGray,
                    modifier = Modifier.size(18.dp)
                )

                Slider(
                    value = volume,
                    onValueChange = { appState.audioPlayer.setVolume(it) },
                    colors = SliderDefaults.colors(
                        thumbColor = primaryAccent,
                        activeTrackColor = primaryAccent,
                        inactiveTrackColor = HexColors.DarkGray
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                )

                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Volume Up",
                    tint = HexColors.SlateGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
