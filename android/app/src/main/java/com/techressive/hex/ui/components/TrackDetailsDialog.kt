package com.techressive.hex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun TrackDetailsDialog(
    track: Track,
    activeSkin: HexSkin,
    onDismiss: () -> Unit
) {
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
                // Header with Hi-Res badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Track Details",
                        color = HexColors.PureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (track.isHiRes) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(HexColors.CobaltBlue.copy(alpha = 0.25f))
                                .border(1.dp, HexColors.CobaltBlue, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "HI-RES AUDIO",
                                color = HexColors.Lavender,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = activeSkin.cardBorderColor)
                Spacer(modifier = Modifier.height(12.dp))

                // Detail Items
                DetailItem(label = "Title", value = track.title)
                DetailItem(label = "Artist", value = track.artist)
                DetailItem(label = "Album", value = track.album)
                DetailItem(label = "Genre", value = track.genre)
                DetailItem(label = "Track #", value = "${track.trackNumber ?: "—"}")
                DetailItem(label = "Year", value = "${track.year ?: "—"}")

                val durSec = track.durationMs / 1000
                val durStr = String.format("%d:%02d (%d sec)", durSec / 60, durSec % 60, durSec)
                DetailItem(label = "Duration", value = durStr)

                DetailItem(label = "Audio Codec", value = track.codec.displayName)
                DetailItem(label = "Sample Rate", value = track.formattedSampleRate)
                DetailItem(label = "Bit Depth", value = track.formattedBitDepth)

                val kbps = if (track.bitrate > 0) "${track.bitrate / 1000} kbps" else "Variable / Native"
                DetailItem(label = "Bitrate", value = kbps)
                DetailItem(label = "Channels", value = if (track.channels == 2) "Stereo 2.0" else "${track.channels} Channels")

                DetailItem(
                    label = "File Path",
                    value = track.path,
                    isMonospace = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = HexColors.CobaltBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", color = HexColors.PureBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    isMonospace: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = HexColors.SlateGray,
            fontSize = 12.sp,
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            color = HexColors.PureWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (isMonospace) androidx.compose.ui.text.font.FontFamily.Monospace else null,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
