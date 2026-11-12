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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.window.Dialog
import com.techressive.hex.metadata.Track
import com.techressive.hex.state.HexSkin
import com.techressive.hex.ui.theme.HexColors

@Composable
fun AddToPlaylistDialog(
    track: Track,
    playlists: Map<String, List<Track>>,
    activeSkin: HexSkin,
    onDismiss: () -> Unit,
    onSelectPlaylist: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit
) {
    var newPlaylistName by remember { mutableStateOf("") }
    var isCreatingNew by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(activeSkin.surfaceColor)
                .border(1.dp, activeSkin.cardBorderColor, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Add to Playlist",
                        color = HexColors.PureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { isCreatingNew = !isCreatingNew },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(HexColors.CobaltBlue.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Playlist",
                            tint = HexColors.CobaltBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = activeSkin.cardBorderColor)
                Spacer(modifier = Modifier.height(12.dp))

                // If user toggles creating new playlist
                if (isCreatingNew || playlists.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(activeSkin.bgOled)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "New Playlist",
                            color = HexColors.Lavender,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            placeholder = { Text("Playlist name...", color = HexColors.SlateGray, fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HexColors.CobaltBlue,
                                unfocusedBorderColor = activeSkin.cardBorderColor,
                                focusedTextColor = HexColors.PureWhite,
                                unfocusedTextColor = HexColors.PureWhite
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (newPlaylistName.isNotBlank()) {
                                    onCreatePlaylist(newPlaylistName.trim())
                                    onDismiss()
                                }
                            },
                            enabled = newPlaylistName.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = HexColors.CobaltBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Create & Add", color = HexColors.PureBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (playlists.isNotEmpty()) {
                    Text(
                        text = "EXISTING PLAYLISTS",
                        color = HexColors.SlateGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        items(playlists.keys.toList()) { name ->
                            val count = playlists[name]?.size ?: 0
                            val alreadyIn = playlists[name]?.any { it.id == track.id } == true

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onSelectPlaylist(name)
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = name,
                                    tint = if (alreadyIn) HexColors.CobaltBlue else HexColors.SlateGray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        color = HexColors.PureWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (alreadyIn) "Already in playlist • $count tracks" else "$count tracks",
                                        color = if (alreadyIn) HexColors.Lavender else HexColors.SlateGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = activeSkin.cardBorderColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = HexColors.PureWhite)
                }
            }
        }
    }
}
