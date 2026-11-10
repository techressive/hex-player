package com.techressive.hex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.techressive.hex.metadata.ArtistBioService
import com.techressive.hex.state.AppState
import com.techressive.hex.ui.components.AlbumCoverView
import com.techressive.hex.ui.theme.HexColors

@Composable
fun FullScreenLyricsScreen(
    appState: AppState,
    modifier: Modifier = Modifier
) {
    val currentTrack by appState.currentTrack.collectAsState()
    val currentPosition by appState.audioPlayer.currentPosition.collectAsState()
    val lyrics by appState.lyrics.collectAsState()
    val primaryAccent by appState.accentColorPrimary.collectAsState()
    val secondaryAccent by appState.accentColorSecondary.collectAsState()
    val activeSkin by appState.activeSkin.collectAsState()

    val context = LocalContext.current
    val listState = rememberLazyListState()

    val artistPhoto = remember(currentTrack) {
        currentTrack?.let { ArtistBioService.getCachedArtistPhoto(it.artist) }
    }

    val activeIndex = remember(lyrics, currentPosition) {
        val idx = lyrics.indexOfLast { it.timestamp <= currentPosition }
        if (idx >= 0) idx else 0
    }

    // Auto-scroll with animation to current line
    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty() && activeIndex in lyrics.indices) {
            val target = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 40f && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                    appState.goBack()
                    return available
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 30f) {
                    appState.goBack()
                    return available
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HexColors.PureBlack)
            .nestedScroll(nestedScrollConnection)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 38f) {
                        appState.goBack()
                    }
                }
            }
    ) {
        // 1. Background Artist Photo with dark immersive overlay for legibility
        if (!artistPhoto.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artistPhoto)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Heavy dark gradient overlay ensuring all text is crystal clear
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.88f),
                            Color.Black.copy(alpha = 0.94f),
                            Color.Black.copy(alpha = 0.98f)
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Top 1/3: Header Bar + Prominent Album Artwork with solid backdrop & zIndex(10f)
            // Any lyrics scrolling up cleanly pass BEHIND this elevated top section!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.36f)
                    .zIndex(10f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black,
                                Color.Black.copy(alpha = 0.98f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            ) {
                // Header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack?.title ?: "Now Playing",
                            color = HexColors.PureWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentTrack?.artist ?: "",
                            color = secondaryAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = { appState.goBack() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = HexColors.PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // High-Resolution Album Artwork
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AlbumCoverView(
                        track = currentTrack,
                        modifier = Modifier.fillMaxHeight(),
                        cornerRadius = 16.dp,
                        showFetchButton = false
                    )
                }
            }

            // Bottom 2/3: Dedicated Synced Lyrics View with tap-to-seek
            // Clipped to its bounds so lyrics slide completely behind the artwork
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.64f)
                    .clipToBounds()
            ) {
                if (lyrics.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No synchronized lyrics found for this track.\nCompatible with embedded tags and companion .lrc files.",
                            color = HexColors.SlateGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(lyrics, key = { _, line -> line.id }) { index, line ->
                            val isCurrent = index == activeIndex
                            val isPast = index < activeIndex

                            Text(
                                text = line.text,
                                color = when {
                                    isCurrent -> HexColors.PureWhite
                                    isPast -> HexColors.SlateGray.copy(alpha = 0.65f)
                                    else -> HexColors.SlateGray.copy(alpha = 0.35f)
                                },
                                fontSize = if (isCurrent) 22.sp else 16.sp,
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                lineHeight = if (isCurrent) 32.sp else 24.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) primaryAccent.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        appState.audioPlayer.seekTo(line.timestamp)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
