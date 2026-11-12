@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.techressive.hex.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.techressive.hex.metadata.Track
import com.techressive.hex.state.AppState
import com.techressive.hex.state.Screen
import com.techressive.hex.ui.components.AddToPlaylistDialog
import com.techressive.hex.ui.components.AlbumCoverView
import com.techressive.hex.ui.components.EditTagDialog
import com.techressive.hex.ui.components.TrackDetailsDialog
import com.techressive.hex.ui.components.TrackOptionsSheet
import com.techressive.hex.ui.theme.HexColors

@Composable
fun ArtistBioScreen(
    artistName: String,
    appState: AppState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val artistDetails by appState.artistDetails.collectAsState()
    val allTracks by appState.tracks.collectAsState()
    val currentTrack by appState.currentTrack.collectAsState()
    val favorites by appState.favorites.collectAsState()
    val playlists by appState.playlists.collectAsState()
    val isArtistSeparationEnabled by appState.isArtistSeparationEnabled.collectAsState()
    val artistSeparators by appState.artistSeparators.collectAsState()
    val activeSkin by appState.activeSkin.collectAsState()

    val artistTracks = remember(allTracks, artistName, isArtistSeparationEnabled, artistSeparators) {
        allTracks.filter { track ->
            appState.splitArtists(track.artist).any { it.equals(artistName, ignoreCase = true) }
        }
    }
    val scrollState = rememberScrollState()

    // Custom Artist Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            appState.updateArtistArtwork(artistName, uri)
        }
    }

    // Modal Sheet & Dialog States
    var selectedTrackForOptions by remember { mutableStateOf<Track?>(null) }
    var selectedTrackForDetails by remember { mutableStateOf<Track?>(null) }
    var selectedTrackForEditTags by remember { mutableStateOf<Track?>(null) }
    var selectedTrackForPlaylist by remember { mutableStateOf<Track?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(activeSkin.bgOled)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header with backdrop cover photo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                if (!artistDetails?.coverPhotoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(artistDetails!!.coverPhotoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Cover Backdrop",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(HexColors.CobaltBlue.copy(alpha = 0.4f), activeSkin.surfaceColor)
                                )
                            )
                    )
                }

                // Dark gradient overlay on backdrop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    activeSkin.bgOled
                                )
                            )
                        )
                )

                // Universal Top-Left Back Button
                IconButton(
                    onClick = { appState.goBack() },
                    modifier = Modifier
                        .padding(start = 16.dp, top = 16.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(activeSkin.surfaceColor.copy(alpha = 0.85f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = HexColors.PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Artist Profile Row: 1:1 Square Photo + Name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-40).dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // 1:1 Square Photo with click to change photo
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .aspectRatio(1f)
                        .shadow(16.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(activeSkin.surfaceColor)
                        .border(2.dp, HexColors.CobaltBlue, RoundedCornerShape(16.dp))
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (!artistDetails?.squarePhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(artistDetails!!.squarePhotoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = artistName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = artistName,
                            tint = HexColors.SlateGray,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    // Edit badge overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(HexColors.PureBlack.copy(alpha = 0.75f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change Artist Photo",
                            tint = HexColors.Lavender,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = artistName,
                        color = HexColors.PureWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${artistTracks.size} songs in library",
                        color = HexColors.Lavender,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tap photo to change image",
                        color = HexColors.CobaltBlue,
                        fontSize = 10.sp,
                        modifier = Modifier.clickable { photoPickerLauncher.launch("image/*") }
                    )
                }
            }

            // Section: STATS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "STATS",
                    color = HexColors.Lavender,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Spotify Monthly Listeners Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(activeSkin.surfaceColor)
                            .border(1.dp, activeSkin.cardBorderColor, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = "Spotify Monthly Listeners",
                                color = HexColors.SlateGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = artistDetails?.spotifyMonthlyListeners ?: "—",
                                color = Color(0xFF1DB954), // Spotify Green
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Instagram Followers Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(activeSkin.surfaceColor)
                            .border(1.dp, activeSkin.cardBorderColor, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = "Instagram Followers",
                                color = HexColors.SlateGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = artistDetails?.instagramFollowers ?: "—",
                                color = Color(0xFFE1306C), // Instagram Pink/Magenta
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Biography (Strictly titled "Biography")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Biography",
                    color = HexColors.PureWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = artistDetails?.biography ?: "Loading artist biography...",
                    color = HexColors.OffWhite.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Songs from this artist in the user's library with Long Press Context Menu
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Songs in Library (${artistTracks.size})",
                    color = HexColors.PureWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (artistTracks.isEmpty()) {
                    Text(
                        text = "No songs by $artistName found in your current library.",
                        color = HexColors.SlateGray,
                        fontSize = 13.sp
                    )
                } else {
                    artistTracks.forEach { track ->
                        val isPlayingThis = track.id == currentTrack?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPlayingThis) HexColors.CobaltBlue.copy(alpha = 0.15f) else Color.Transparent)
                                .combinedClickable(
                                    onClick = { appState.playTrack(track) },
                                    onLongClick = { selectedTrackForOptions = track }
                                )
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AlbumCoverView(
                                track = track,
                                modifier = Modifier.size(40.dp),
                                cornerRadius = 6.dp,
                                showFetchButton = false
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    color = if (isPlayingThis) HexColors.CobaltBlue else HexColors.PureWhite,
                                    fontSize = 14.sp,
                                    fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.album,
                                    color = HexColors.SlateGray,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (track.isHiRes) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(HexColors.CobaltBlue.copy(alpha = 0.25f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Hi-Res",
                                        color = HexColors.Lavender,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Modal Bottom Sheet & Dialogs
        if (selectedTrackForOptions != null) {
            val t = selectedTrackForOptions!!
            TrackOptionsSheet(
                track = t,
                isFavorite = favorites.contains(t.id),
                activeSkin = activeSkin,
                onDismiss = { selectedTrackForOptions = null },
                onViewDetails = { selectedTrackForDetails = t },
                onEditTags = { selectedTrackForEditTags = t },
                onAddToPlaylist = { selectedTrackForPlaylist = t },
                onGoToArtist = { artist -> appState.navigateTo(Screen.ArtistBio(artist)) },
                onGoToAlbum = { album, artist -> appState.navigateTo(Screen.AlbumDetail(album, artist)) },
                onToggleFavorite = { appState.toggleFavorite(t) }
            )
        }

        if (selectedTrackForDetails != null) {
            TrackDetailsDialog(
                track = selectedTrackForDetails!!,
                activeSkin = activeSkin,
                onDismiss = { selectedTrackForDetails = null }
            )
        }

        if (selectedTrackForEditTags != null) {
            val t = selectedTrackForEditTags!!
            EditTagDialog(
                track = t,
                activeSkin = activeSkin,
                onDismiss = { selectedTrackForEditTags = null },
                onSave = { newTitle, newArtist, newAlbum, newGenre, newYear, newTrackNumber, newArtworkUri, applyToAlbum ->
                    appState.updateTrackMetadata(
                        track = t,
                        newTitle = newTitle,
                        newArtist = newArtist,
                        newAlbum = newAlbum,
                        newGenre = newGenre,
                        newYear = newYear,
                        newTrackNumber = newTrackNumber,
                        newArtworkUri = newArtworkUri,
                        applyArtworkToAlbum = applyToAlbum
                    )
                }
            )
        }

        if (selectedTrackForPlaylist != null) {
            val t = selectedTrackForPlaylist!!
            AddToPlaylistDialog(
                track = t,
                playlists = playlists,
                activeSkin = activeSkin,
                onDismiss = { selectedTrackForPlaylist = null },
                onSelectPlaylist = { plName -> appState.addTrackToPlaylist(plName, t) },
                onCreatePlaylist = { plName -> appState.createPlaylist(plName, t) }
            )
        }
    }
}
