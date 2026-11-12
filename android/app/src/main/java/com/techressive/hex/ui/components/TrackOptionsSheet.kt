package com.techressive.hex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techressive.hex.metadata.Track
import com.techressive.hex.state.HexSkin
import com.techressive.hex.ui.theme.HexColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsSheet(
    track: Track,
    isFavorite: Boolean,
    activeSkin: HexSkin,
    onDismiss: () -> Unit,
    onViewDetails: () -> Unit,
    onEditTags: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onGoToArtist: (String) -> Unit,
    onGoToAlbum: (String, String) -> Unit,
    onToggleFavorite: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = activeSkin.surfaceColor,
        contentColor = HexColors.PureWhite,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(HexColors.SlateGray.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            // Track Info Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumCoverView(
                    track = track,
                    modifier = Modifier.size(54.dp),
                    cornerRadius = 10.dp,
                    showFetchButton = false
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = HexColors.PureWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${track.artist} • ${track.album}",
                        color = HexColors.SlateGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.audiophileBadgeText,
                        color = HexColors.Lavender,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider(color = activeSkin.cardBorderColor)
            Spacer(modifier = Modifier.height(8.dp))

            // Option 1: Track Details
            OptionRow(
                icon = Icons.Default.Info,
                title = "Track Details",
                subtitle = "Codec, bit depth, sample rate & path",
                iconTint = HexColors.CobaltBlue,
                onClick = {
                    onDismiss()
                    onViewDetails()
                }
            )

            // Option 2: Edit Tag & Artwork
            OptionRow(
                icon = Icons.Default.Edit,
                title = "Edit Tag & Artwork",
                subtitle = "Rename title, artist, album & set cover",
                iconTint = HexColors.Lavender,
                onClick = {
                    onDismiss()
                    onEditTags()
                }
            )

            // Option 3: Add to Playlist
            OptionRow(
                icon = Icons.Default.PlaylistAdd,
                title = "Add to Playlist",
                subtitle = "Include in custom listening queues",
                iconTint = Color(0xFF64DFDF),
                onClick = {
                    onDismiss()
                    onAddToPlaylist()
                }
            )

            // Option 4: Go to Artist
            OptionRow(
                icon = Icons.Default.Person,
                title = "Go to Artist",
                subtitle = "View biography and discography",
                iconTint = HexColors.PureWhite,
                onClick = {
                    onDismiss()
                    onGoToArtist(track.artist)
                }
            )

            // Option 5: Go to Album
            OptionRow(
                icon = Icons.Default.Album,
                title = "Go to Album",
                subtitle = "Browse all tracks in this release",
                iconTint = HexColors.PureWhite,
                onClick = {
                    onDismiss()
                    onGoToAlbum(track.album, track.artist)
                }
            )

            // Option 6: Favorite / Unfavorite
            OptionRow(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                title = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                subtitle = if (isFavorite) "Currently in your favorites collection" else "Save to your favorites collection",
                iconTint = if (isFavorite) Color(0xFFFF4D6D) else HexColors.SlateGray,
                onClick = {
                    onToggleFavorite()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = HexColors.PureWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = HexColors.SlateGray,
                fontSize = 11.sp
            )
        }
    }
}
