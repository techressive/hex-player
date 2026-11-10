package com.techressive.hex.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techressive.hex.audio.EQMode
import com.techressive.hex.audio.ParametricEQEngine
import com.techressive.hex.ui.theme.HexColors
import com.techressive.hex.ui.theme.LocalHexSkin

@Composable
fun EqualizerModalSheet(
    eqEngine: ParametricEQEngine,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skin = LocalHexSkin.current
    val isEnabled by eqEngine.isEnabled.collectAsState()
    val mode by eqEngine.mode.collectAsState()
    val activePreset by eqEngine.activePresetName.collectAsState()
    val bands by eqEngine.bands.collectAsState()
    val presets by eqEngine.presets.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    val verticalScroll = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(skin.bgOled)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .verticalScroll(verticalScroll)
        ) {
            // Header with top-left back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(skin.surfaceColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = HexColors.PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Parametric Equalizer",
                        color = HexColors.PureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Master Toggle
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { eqEngine.setEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = HexColors.PureBlack,
                        checkedTrackColor = HexColors.CobaltBlue,
                        uncheckedThumbColor = HexColors.OffWhite,
                        uncheckedTrackColor = HexColors.DarkGray
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Selector: Simple (7-Band) vs Expert (10-Band)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(HexColors.DarkGray)
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (mode == EQMode.SIMPLE) HexColors.CobaltBlue else Color.Transparent)
                        .clickable { eqEngine.setMode(EQMode.SIMPLE) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Simple (7-Band)",
                        color = if (mode == EQMode.SIMPLE) HexColors.PureWhite else HexColors.SlateGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (mode == EQMode.EXPERT) HexColors.CobaltBlue else Color.Transparent)
                        .clickable { eqEngine.setMode(EQMode.EXPERT) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Expert (10-Band)",
                        color = if (mode == EQMode.EXPERT) HexColors.PureWhite else HexColors.SlateGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real-Time Frequency Response Curve Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(skin.surfaceColor)
                    .border(1.dp, skin.cardBorderColor, RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val midY = h / 2f

                    // Center 0 dB reference line
                    drawLine(
                        color = HexColors.SlateGray.copy(alpha = 0.3f),
                        start = Offset(0f, midY),
                        end = Offset(w, midY),
                        strokeWidth = 1f
                    )

                    if (isEnabled && bands.isNotEmpty()) {
                        val path = Path()
                        val steps = 60
                        val evalFreqs = DoubleArray(steps + 1) { idx ->
                            val fraction = idx / steps.toDouble()
                            20.0 * Math.pow(1000.0, fraction)
                        }
                        val curve = com.techressive.hex.audio.BiquadFilter.calculateMagnitudeCurve(bands, evalFreqs)
                        for (i in 0..steps) {
                            val fraction = i / steps.toFloat()
                            val clampedDb = curve[i].coerceIn(-15.0, 15.0)
                            val y = midY - (clampedDb / 15.0 * (midY - 8f)).toFloat()
                            val x = fraction * w
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            brush = Brush.horizontalGradient(listOf(HexColors.CobaltBlue, HexColors.Lavender)),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets Horizontal Bar + "Save Preset"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Save Preset" button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(HexColors.CobaltBlue.copy(alpha = 0.2f))
                        .border(1.dp, HexColors.CobaltBlue, RoundedCornerShape(16.dp))
                        .clickable { showSaveDialog = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Save Preset",
                            tint = HexColors.Lavender,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save Preset",
                            color = HexColors.Lavender,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Presets list
                presets.forEach { p ->
                    val isSelected = p.name == activePreset
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) HexColors.CobaltBlue else HexColors.DarkGray)
                            .clickable { eqEngine.applyPreset(p) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = p.name,
                            color = if (isSelected) HexColors.PureWhite else HexColors.OffWhite,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )

                        // If custom preset, allow deletion
                        if (p.isCustom) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Preset",
                                tint = HexColors.SlateGray,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { eqEngine.deleteCustomPreset(p.name) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Spacer(modifier = Modifier.height(20.dp))

            // Band Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (mode == EQMode.SIMPLE) "7-Band Graphic Sliders" else "10-Band Parametric Cells",
                    color = HexColors.OffWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (mode == EQMode.SIMPLE) "Fixed Frequency & Q" else "Full Gain, Freq & Q Control",
                    color = HexColors.Lavender,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (mode == EQMode.SIMPLE) {
                // Simple 7-Band Graphic Sliders
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bands.forEachIndexed { index, band ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(HexColors.DarkGray.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = band.label,
                                color = HexColors.Lavender,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(52.dp)
                            )

                            Slider(
                                value = band.gainDb.toFloat(),
                                onValueChange = { eqEngine.setBandGain(index, it) },
                                valueRange = -12f..12f,
                                colors = SliderDefaults.colors(
                                    thumbColor = HexColors.CobaltBlue,
                                    activeTrackColor = HexColors.CobaltBlue,
                                    inactiveTrackColor = skin.surfaceColor
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = String.format(java.util.Locale.US, "%+.1f dB", band.gainDb),
                                color = HexColors.OffWhite,
                                fontSize = 11.sp,
                                modifier = Modifier.width(52.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                    }
                }
            } else {
                // Expert 10-Band Parametric Controls
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    bands.forEachIndexed { index, band ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(skin.surfaceColor.copy(alpha = 0.85f))
                                .border(1.dp, skin.cardBorderColor, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${index + 1}. ${band.name}",
                                        color = HexColors.PureWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(HexColors.CobaltBlue.copy(alpha = 0.25f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = band.type.name.replace("_", " "),
                                            color = HexColors.Lavender,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Text(
                                    text = String.format(java.util.Locale.US, "%+.1f dB", band.gainDb),
                                    color = if (band.gainDb != 0.0) HexColors.CobaltBlue else HexColors.OffWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Gain Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gain",
                                    color = HexColors.SlateGray,
                                    fontSize = 11.sp,
                                    modifier = Modifier.width(40.dp)
                                )
                                Slider(
                                    value = band.gainDb.toFloat(),
                                    onValueChange = { eqEngine.setBandGain(index, it) },
                                    valueRange = -12f..12f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = HexColors.CobaltBlue,
                                        activeTrackColor = HexColors.CobaltBlue,
                                        inactiveTrackColor = HexColors.DarkGray
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Frequency & Q readouts / sliders
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Freq: ${band.frequency.toInt()} Hz",
                                    color = HexColors.SlateGray,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "Q: %.2f", band.q),
                                    color = HexColors.SlateGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Save Custom Preset Dialog
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                containerColor = HexColors.DarkGray,
                title = {
                    Text(text = "Save EQ Preset", color = HexColors.PureWhite, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text(
                            text = "Enter a name for your custom equalizer tuning:",
                            color = HexColors.OffWhite,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPresetName,
                            onValueChange = { newPresetName = it },
                            singleLine = true,
                            placeholder = { Text("e.g. Bass Head, Warm Vocal", color = HexColors.SlateGray) }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPresetName.isNotBlank()) {
                                eqEngine.saveCurrentAsPreset(newPresetName.trim())
                                newPresetName = ""
                                showSaveDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HexColors.CobaltBlue)
                    ) {
                        Text("Save", color = HexColors.PureWhite)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel", color = HexColors.SlateGray)
                    }
                }
            )
        }
    }
}
