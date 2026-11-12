package com.techressive.hex.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.techressive.hex.metadata.Track
import com.techressive.hex.state.HexSkin
import com.techressive.hex.ui.theme.HexColors

@Composable
fun EditTagDialog(
    track: Track,
    activeSkin: HexSkin,
    onDismiss: () -> Unit,
    onSave: (
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        newGenre: String,
        newYear: Int?,
        newTrackNumber: Int?,
        newArtworkUri: Uri?,
        applyToAlbum: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf(track.title) }
    var artist by remember { mutableStateOf(track.artist) }
    var album by remember { mutableStateOf(track.album) }
    var genre by remember { mutableStateOf(track.genre) }
    var yearStr by remember { mutableStateOf(track.year?.toString() ?: "") }
    var trackNumStr by remember { mutableStateOf(track.trackNumber?.toString() ?: "1") }
    var selectedArtworkUri by remember { mutableStateOf<Uri?>(track.artworkUri) }
    var applyArtworkToAlbum by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedArtworkUri = uri
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(activeSkin.surfaceColor)
                .border(1.dp, activeSkin.cardBorderColor, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Dialog Title
                Text(
                    text = "Edit Track & Artwork",
                    color = HexColors.PureWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = activeSkin.cardBorderColor)
                Spacer(modifier = Modifier.height(14.dp))

                // Artwork Picker Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(activeSkin.bgOled)
                            .border(1.dp, activeSkin.cardBorderColor, RoundedCornerShape(12.dp))
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedArtworkUri != null) {
                            AsyncImage(
                                model = selectedArtworkUri,
                                contentDescription = "Cover preview",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Add Artwork",
                                tint = HexColors.Lavender,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = HexColors.CobaltBlue.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (selectedArtworkUri != null) "Change Cover" else "Choose Cover",
                                color = HexColors.Lavender,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (selectedArtworkUri != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .clickable { selectedArtworkUri = null }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Cover",
                                    tint = HexColors.SlateGray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Remove Cover",
                                    color = HexColors.SlateGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Checkbox: Apply artwork to all tracks in this album
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { applyArtworkToAlbum = !applyArtworkToAlbum }
                ) {
                    Checkbox(
                        checked = applyArtworkToAlbum,
                        onCheckedChange = { applyArtworkToAlbum = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = HexColors.CobaltBlue,
                            uncheckedColor = HexColors.SlateGray,
                            checkmarkColor = HexColors.PureBlack
                        )
                    )
                    Text(
                        text = "Apply artwork to all tracks in this album",
                        color = HexColors.PureWhite,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input Fields
                TagTextField(label = "Title", value = title, onValueChange = { title = it }, activeSkin = activeSkin)
                Spacer(modifier = Modifier.height(8.dp))
                TagTextField(label = "Artist", value = artist, onValueChange = { artist = it }, activeSkin = activeSkin)
                Spacer(modifier = Modifier.height(8.dp))
                TagTextField(label = "Album", value = album, onValueChange = { album = it }, activeSkin = activeSkin)
                Spacer(modifier = Modifier.height(8.dp))
                TagTextField(label = "Genre", value = genre, onValueChange = { genre = it }, activeSkin = activeSkin)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TagTextField(
                        label = "Year",
                        value = yearStr,
                        onValueChange = { yearStr = it },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                        activeSkin = activeSkin
                    )
                    TagTextField(
                        label = "Track #",
                        value = trackNumStr,
                        onValueChange = { trackNumStr = it },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                        activeSkin = activeSkin
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = activeSkin.cardBorderColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = HexColors.PureWhite)
                    }

                    Button(
                        onClick = {
                            val yearInt = yearStr.trim().toIntOrNull()
                            val trackNumInt = trackNumStr.trim().toIntOrNull()
                            onSave(
                                title,
                                artist,
                                album,
                                genre,
                                yearInt,
                                trackNumInt,
                                selectedArtworkUri,
                                applyArtworkToAlbum
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HexColors.CobaltBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", color = HexColors.PureBlack, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TagTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    activeSkin: HexSkin,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = HexColors.CobaltBlue,
            unfocusedBorderColor = activeSkin.cardBorderColor,
            focusedLabelColor = HexColors.CobaltBlue,
            unfocusedLabelColor = HexColors.SlateGray,
            focusedTextColor = HexColors.PureWhite,
            unfocusedTextColor = HexColors.PureWhite,
            cursorColor = HexColors.CobaltBlue
        ),
        shape = RoundedCornerShape(10.dp)
    )
}
