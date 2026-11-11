@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.techressive.hex.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

enum class LibraryTab(val label: String) {
    ALL_TRACKS("All Tracks"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    RECENT("Recently Added"),
    FAVORITES("Favorites"),
    PLAYLISTS("Playlists")
}

@Composable
fun LibraryScreen(
    appState: AppState,
    modifier: Modifier = Modifier
) {
    val tracks by appState.tracks.collectAsState()
    val favorites by appState.favorites.collectAsState()
    val recentTracks by appState.recentTracks.collectAsState()
    val currentTrack by appState.currentTrack.collectAsState()
    val isScanning by appState.isScanning.collectAsState()
    val primaryAccent by appState.accentColorPrimary.collectAsState()
    val isArtistSeparationEnabled by appState.isArtistSeparationEnabled.collectAsState()
    val artistSeparators by appState.artistSeparators.collectAsState()
    val activeSkin by appState.activeSkin.collectAsState()

    val selectedTabIndex by appState.selectedLibraryTabIndex.collectAsState()
    val trackSortOrder by appState.trackSortOrder.collectAsState()
    val themeMode by appState.themeMode.collectAsState()
    val tabs = LibraryTab.values()

    var showSortMenu by remember { androidx.compose.runtime.mutableStateOf(false) }

    val sortedTracks = remember(tracks, trackSortOrder) {
        appState.getSortedTracks(tracks)
    }

    // SAF Directory Picker Launcher
    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { appState.importDirectoryUri(it) }
    }

    val screenBg = if (themeMode == com.techressive.hex.state.ThemeMode.LIGHT) Color(0xFFF6F8FC) else activeSkin.bgOled

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        // Top Bar with Universal Top-Left Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        tint = HexColors.OffWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Library",
                    color = HexColors.PureWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Completely hide Add Folder button/icon once tracks have been added
            if (tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(HexColors.CobaltBlue.copy(alpha = 0.2f))
                        .border(1.dp, HexColors.CobaltBlue.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                        .clickable { dirPickerLauncher.launch(null) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "Import Folder",
                            tint = HexColors.Lavender,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Folder",
                            color = HexColors.Lavender,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (isScanning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = HexColors.CobaltBlue,
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scanning audio files recursively...",
                    color = HexColors.SlateGray,
                    fontSize = 12.sp
                )
            }
        }

        val pagerState = androidx.compose.foundation.pager.rememberPagerState(
            initialPage = selectedTabIndex.coerceIn(0, tabs.size - 1),
            pageCount = { tabs.size }
        )
        val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

        // Sync appState tab index when pager settles on a new page after user swipe
        androidx.compose.runtime.LaunchedEffect(pagerState.settledPage) {
            if (selectedTabIndex != pagerState.settledPage) {
                appState.setSelectedLibraryTabIndex(pagerState.settledPage)
            }
        }

        // Sync pager when selectedTabIndex changes programmatically (e.g. from tab click or external navigation)
        androidx.compose.runtime.LaunchedEffect(selectedTabIndex) {
            if (!pagerState.isScrollInProgress && pagerState.currentPage != selectedTabIndex) {
                pagerState.animateScrollToPage(
                    page = selectedTabIndex,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 320,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                )
            }
        }

        // Section Tabs & Sort Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = activeSkin.bgOled,
                contentColor = HexColors.PureWhite,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = primaryAccent,
                        height = 2.dp
                    )
                },
                edgePadding = 16.dp,
                modifier = Modifier.weight(1f)
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            appState.setSelectedLibraryTabIndex(index)
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    page = index,
                                    animationSpec = androidx.compose.animation.core.tween(
                                        durationMillis = 320,
                                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                                    )
                                )
                            }
                        },
                        text = {
                            Text(
                                text = tab.label,
                                color = if (pagerState.currentPage == index) primaryAccent else HexColors.SlateGray,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Sort button for songs/library
            Box(modifier = Modifier.padding(end = 12.dp)) {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(activeSkin.surfaceColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Sort",
                        tint = HexColors.Lavender,
                        modifier = Modifier.size(16.dp)
                    )
                }

                androidx.compose.material3.DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(activeSkin.surfaceColor)
                ) {
                    com.techressive.hex.state.TrackSortOrder.values().forEach { order ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = {
                                Text(
                                    text = order.displayName,
                                    color = if (order == trackSortOrder) primaryAccent else HexColors.PureWhite,
                                    fontWeight = if (order == trackSortOrder) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            onClick = {
                                appState.setTrackSortOrder(order)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Swipeable Content with HorizontalPager
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (tabs[page]) {
                LibraryTab.ALL_TRACKS -> TrackListView(
                    tracks = sortedTracks,
                    currentTrack = currentTrack,
                    favorites = favorites,
                    onPlay = { appState.playTrack(it) },
                    onToggleFavorite = { appState.toggleFavorite(it) },
                    onFetchArtwork = { appState.fetchArtworkForTrack(it) },
                    onArtistClick = { appState.navigateTo(Screen.ArtistBio(it)) }
                )
                LibraryTab.ARTISTS -> {
                    val artistGroups = remember(tracks, isArtistSeparationEnabled, artistSeparators) {
                        appState.getSeparatedArtists()
                    }
                    if (artistGroups.isEmpty()) {
                        if (tracks.isEmpty()) {
                            EmptyLibraryPlaceholder { dirPickerLauncher.launch(null) }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No artists found in library.",
                                    color = HexColors.SlateGray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(artistGroups.keys.toList().sorted()) { artist ->
                                val artistPhoto = com.techressive.hex.metadata.ArtistBioService.getCachedArtistPhoto(artist)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { appState.navigateTo(Screen.ArtistBio(artist)) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(activeSkin.surfaceColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!artistPhoto.isNullOrBlank()) {
                                            coil.compose.AsyncImage(
                                                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                                    .data(artistPhoto)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = artist,
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = artist,
                                                tint = HexColors.CobaltBlue,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = artist,
                                            color = HexColors.PureWhite,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${artistGroups[artist]?.size ?: 0} tracks",
                                            color = HexColors.SlateGray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                LibraryTab.ALBUMS -> {
                    val albumGroups = tracks.groupBy { it.album }
                    if (albumGroups.isEmpty()) {
                        if (tracks.isEmpty()) {
                            EmptyLibraryPlaceholder { dirPickerLauncher.launch(null) }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No albums found in library.",
                                    color = HexColors.SlateGray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(albumGroups.keys.toList().sorted()) { album ->
                                val albumTracks = albumGroups[album] ?: emptyList()
                                val firstTrack = albumTracks.firstOrNull()
                                val artist = firstTrack?.artist ?: "Unknown Artist"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            appState.navigateTo(Screen.AlbumDetail(albumName = album, artist = artist))
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AlbumCoverView(
                                        track = firstTrack,
                                        modifier = Modifier.size(52.dp),
                                        cornerRadius = 10.dp,
                                        showFetchButton = false
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = album,
                                            color = HexColors.PureWhite,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$artist • ${albumTracks.size} tracks",
                                            color = HexColors.SlateGray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                LibraryTab.RECENT -> {
                    val recentlyAdded = remember(tracks) { tracks.takeLast(50).reversed() }
                    TrackListView(
                        tracks = recentlyAdded,
                        currentTrack = currentTrack,
                        favorites = favorites,
                        emptyMessage = "No tracks added yet.",
                        onPlay = { appState.playTrack(it) },
                        onToggleFavorite = { appState.toggleFavorite(it) },
                        onFetchArtwork = { appState.fetchArtworkForTrack(it) },
                        onArtistClick = { appState.navigateTo(Screen.ArtistBio(it)) }
                    )
                }
                LibraryTab.FAVORITES -> {
                    val favTracks = sortedTracks.filter { favorites.contains(it.id) }
                    TrackListView(
                        tracks = favTracks,
                        currentTrack = currentTrack,
                        favorites = favorites,
                        emptyMessage = "No favorite songs yet. Tap the heart icon on any song.",
                        onPlay = { appState.playTrack(it) },
                        onToggleFavorite = { appState.toggleFavorite(it) },
                        onFetchArtwork = { appState.fetchArtworkForTrack(it) },
                        onArtistClick = { appState.navigateTo(Screen.ArtistBio(it)) }
                    )
                }
                LibraryTab.PLAYLISTS -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Playlists will appear here.",
                            color = HexColors.SlateGray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackListView(
    tracks: List<Track>,
    currentTrack: Track?,
    favorites: Set<String>,
    emptyMessage: String = "Your library is empty. Add a music folder to start listening.",
    onPlay: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onFetchArtwork: (Track) -> Unit,
    onArtistClick: (String) -> Unit
) {
    if (tracks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                color = HexColors.SlateGray,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(tracks, key = { it.id }) { track ->
                val isPlayingThis = track.id == currentTrack?.id
                val isFav = favorites.contains(track.id)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isPlayingThis) HexColors.CobaltBlue.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onPlay(track) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumCoverView(
                        track = track,
                        modifier = Modifier.size(44.dp),
                        cornerRadius = 8.dp,
                        showFetchButton = false
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            color = if (isPlayingThis) HexColors.CobaltBlue else HexColors.PureWhite,
                            fontSize = 14.sp,
                            fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = track.artist,
                                color = HexColors.SlateGray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { onArtistClick(track.artist) }
                            )
                            if (track.isHiRes) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(HexColors.CobaltBlue.copy(alpha = 0.25f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "Hi-Res",
                                        color = HexColors.Lavender,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Fetch artwork button if track has no artwork
                    if (track.artworkUri == null || track.artworkUri.toString().isBlank()) {
                        IconButton(
                            onClick = { onFetchArtwork(track) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Fetch artwork",
                                tint = HexColors.Lavender.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Favorite button
                    IconButton(
                        onClick = { onToggleFavorite(track) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFav) Color(0xFFFF4D6D) else HexColors.SlateGray.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyLibraryPlaceholder(onAddFolder: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Your library has no tracks yet.",
                color = HexColors.SlateGray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(HexColors.CobaltBlue)
                    .clickable { onAddFolder() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Select Music Directory",
                    color = HexColors.PureWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
