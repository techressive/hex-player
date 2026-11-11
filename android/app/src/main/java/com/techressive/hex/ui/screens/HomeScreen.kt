package com.techressive.hex.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
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
import com.techressive.hex.ui.theme.HexBrandHeader
import com.techressive.hex.ui.theme.HexColors

@Composable
fun HomeScreen(
    appState: AppState,
    modifier: Modifier = Modifier
) {
    val tracks by appState.tracks.collectAsState()
    val recentTracks by appState.recentTracks.collectAsState()
    val favorites by appState.favorites.collectAsState()
    val playCounts by appState.playCounts.collectAsState()
    val activeSkin by appState.activeSkin.collectAsState()
    val themeMode by appState.themeMode.collectAsState()
    val primaryAccent by appState.accentColorPrimary.collectAsState()
    val secondaryAccent by appState.accentColorSecondary.collectAsState()
    val isScanning by appState.isScanning.collectAsState()

    val screenBg = if (themeMode == com.techressive.hex.state.ThemeMode.LIGHT) Color(0xFFF6F8FC) else activeSkin.bgOled

    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { appState.importDirectoryUri(it) }
    }

    val scrollState = rememberScrollState()

    // 1. Top 5 recently played tracks
    val topRecent = recentTracks.take(5)

    // 1b. Recently added tracks (newest tracks in library)
    val recentlyAdded = remember(tracks) { tracks.takeLast(10).reversed() }

    // 2. Top plays sorted by playCount
    val topPlays = tracks
        .filter { (playCounts[it.id] ?: 0) > 0 }
        .sortedByDescending { playCounts[it.id] ?: 0 }
        .take(10)
        .ifEmpty { tracks.take(5) }

    // 3. Favorites
    val favoriteTracks = tracks.filter { favorites.contains(it.id) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HexBrandHeader(size = 28.dp, showWordmark = true)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tracks.isEmpty()) {
                        IconButton(
                            onClick = { dirPickerLauncher.launch(null) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(activeSkin.surfaceColor)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "Add Folder",
                                tint = HexColors.Lavender,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    IconButton(
                        onClick = { appState.navigateTo(Screen.Settings) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(activeSkin.surfaceColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = HexColors.SlateGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (isScanning) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = HexColors.CobaltBlue,
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Adding audio files & extracting embedded artworks...",
                        color = HexColors.SlateGray,
                        fontSize = 11.sp
                    )
                }
            }

            // Section 1: RECENTLY PLAYED (Top Cover Art Carousel)
            if (topRecent.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                SectionHeader(title = "RECENTLY PLAYED", subtitle = "Last 5 played tracks")

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(topRecent, key = { it.id }) { track ->
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable { appState.playTrack(track) }
                        ) {
                            AlbumCoverView(
                                track = track,
                                modifier = Modifier.size(120.dp),
                                cornerRadius = 14.dp,
                                showFetchButton = false
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = track.title,
                                color = HexColors.PureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                color = HexColors.SlateGray,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Section 1b: RECENTLY ADDED (Newest Tracks in Library)
            if (recentlyAdded.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                SectionHeader(title = "RECENTLY ADDED", subtitle = "Newest tracks in your library")

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(recentlyAdded, key = { "home_added_${it.id}" }) { track ->
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable { appState.playTrack(track) }
                        ) {
                            AlbumCoverView(
                                track = track,
                                modifier = Modifier.size(120.dp),
                                cornerRadius = 14.dp,
                                showFetchButton = false
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = track.title,
                                color = HexColors.PureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                color = HexColors.SlateGray,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Section 2: TOP PLAYS (Most Played Songs)
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader(title = "TOP PLAYS", subtitle = "Your most frequent tracks")

            if (topPlays.isEmpty()) {
                EmptySectionHint("No plays recorded yet. Start playing your music!")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    topPlays.take(5).forEachIndexed { index, track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(activeSkin.surfaceColor)
                                .border(1.dp, activeSkin.cardBorderColor, RoundedCornerShape(12.dp))
                                .clickable { appState.playTrack(track) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = if (index == 0) primaryAccent else HexColors.SlateGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(22.dp)
                            )
                            AlbumCoverView(
                                track = track,
                                modifier = Modifier.size(42.dp),
                                cornerRadius = 8.dp,
                                showFetchButton = false
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    color = HexColors.PureWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    color = HexColors.SlateGray,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val count = playCounts[track.id] ?: 0
                            if (count > 0) {
                                Text(
                                    text = "$count plays",
                                    color = HexColors.Lavender,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: YOUR FAVORITES
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader(title = "YOUR FAVORITES", subtitle = "${favoriteTracks.size} loved songs")

            if (favoriteTracks.isEmpty()) {
                EmptySectionHint("No favorite tracks yet. Tap heart icon on any track to add.")
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(favoriteTracks.take(8), key = { it.id }) { track ->
                        Column(
                            modifier = Modifier
                                .width(100.dp)
                                .clickable { appState.playTrack(track) }
                        ) {
                            AlbumCoverView(
                                track = track,
                                modifier = Modifier.size(100.dp),
                                cornerRadius = 12.dp,
                                showFetchButton = false
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = track.title,
                                color = HexColors.PureWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Section 4: LIBRARY SHORTCUTS
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader(title = "EXPLORE LIBRARY", subtitle = "${tracks.size} tracks total")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LibraryNavCard(
                    title = "All Tracks",
                    count = "${tracks.size}",
                    icon = Icons.Default.MusicNote,
                    modifier = Modifier.weight(1f),
                    activeSkin = activeSkin,
                    accentColor = HexColors.CobaltBlue,
                    onClick = { appState.navigateTo(Screen.Library) }
                )
                LibraryNavCard(
                    title = "Artists",
                    count = "${appState.getSeparatedArtists().size}",
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f),
                    activeSkin = activeSkin,
                    accentColor = HexColors.Lavender,
                    onClick = { appState.navigateTo(Screen.Library) }
                )
                LibraryNavCard(
                    title = "Albums",
                    count = "${tracks.groupBy { it.album }.size}",
                    icon = Icons.Default.Album,
                    modifier = Modifier.weight(1f),
                    activeSkin = activeSkin,
                    accentColor = Color(0xFF64DFDF),
                    onClick = { appState.navigateTo(Screen.Library) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            text = title,
            color = HexColors.Lavender,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = subtitle,
            color = HexColors.SlateGray,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun EmptySectionHint(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = HexColors.SlateGray,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun LibraryNavCard(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    activeSkin: com.techressive.hex.state.HexSkin,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(activeSkin.surfaceColor)
            .border(1.dp, activeSkin.cardBorderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = HexColors.PureWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = count,
                color = HexColors.SlateGray,
                fontSize = 10.sp
            )
        }
    }
}
