package com.techressive.hex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techressive.hex.audio.AudioOutputMethod
import com.techressive.hex.audio.AudioSampleFormat
import com.techressive.hex.audio.AudioSampleRate
import com.techressive.hex.state.AppState
import com.techressive.hex.state.HexSkin
import com.techressive.hex.ui.theme.HexBrandHeader
import com.techressive.hex.ui.theme.HexColors

@Composable
fun SettingsScreen(
    appState: AppState,
    modifier: Modifier = Modifier
) {
    val activeSkin by appState.activeSkin.collectAsState()
    val themeMode by appState.themeMode.collectAsState()
    val tracks by appState.tracks.collectAsState()
    val scrollState = rememberScrollState()

    val screenBg = if (themeMode == com.techressive.hex.state.ThemeMode.LIGHT) Color(0xFFF6F8FC) else activeSkin.bgOled

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(scrollState)
        ) {
            // Header with Top-Left Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        tint = HexColors.PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "Preferences",
                    color = HexColors.PureWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Branding card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(activeSkin.surfaceColor)
                    .border(1.dp, activeSkin.cardBorderColor, RoundedCornerShape(16.dp))
                .padding(20.dp)
            ) {
                Column {
                    HexBrandHeader(size = 28.dp, showWordmark = true)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Native ARM64-v8a High-Fidelity Audio Engine",
                        color = HexColors.Lavender,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version 2.4.0 (Build arm64-v8a release)",
                        color = HexColors.SlateGray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Appearance & Themes (Dark/Light + Palette Skins)
            Text(
                text = "APPEARANCE & THEME",
                color = HexColors.Lavender,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Theme Base Mode (Dark vs Light)
            val currentThemeMode by appState.themeMode.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(activeSkin.surfaceColor)
                    .border(1.dp, activeSkin.cardBorderColor, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                com.techressive.hex.state.ThemeMode.values().forEach { mode ->
                    val isModeSelected = mode == currentThemeMode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isModeSelected) HexColors.CobaltBlue else Color.Transparent)
                            .clickable { appState.setThemeMode(mode) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.displayName,
                            color = if (isModeSelected) HexColors.PureBlack else HexColors.OffWhite,
                            fontSize = 13.sp,
                            fontWeight = if (isModeSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "ACCENT COLOR SKIN",
                color = HexColors.SlateGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HexSkin.values().forEach { skin ->
                    val isSelected = skin == activeSkin
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(skin.surfaceColor)
                            .border(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) HexColors.CobaltBlue else skin.cardBorderColor,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { appState.setSkin(skin) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(skin.bgOled)
                                    .border(2.dp, skin.cardBorderColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = skin.displayName,
                                color = HexColors.PureWhite,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = HexColors.CobaltBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Audio Output Engine (Poweramp-style)
            Text(
                text = "AUDIO OUTPUT ENGINE",
                color = HexColors.Lavender,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Configure low-level audio sinks, sample rates, and bit depths for audiophile playback.",
                color = HexColors.SlateGray,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Active Output Method
            val currentMethod by appState.audioPlayer.outputMethod.collectAsState()
            val currentRate by appState.audioPlayer.sampleRate.collectAsState()
            val currentFormat by appState.audioPlayer.sampleFormat.collectAsState()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AudioOutputMethod.values().forEach { method ->
                    val isSelected = method == currentMethod
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) HexColors.CobaltBlue.copy(alpha = 0.18f) else activeSkin.surfaceColor)
                            .border(
                                1.dp,
                                if (isSelected) HexColors.CobaltBlue else activeSkin.cardBorderColor,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                appState.audioPlayer.setAudioOutputConfig(method, currentRate, currentFormat)
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = method.displayName,
                                    color = if (isSelected) HexColors.PureWhite else HexColors.OffWhite,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = method.description,
                                    color = HexColors.SlateGray,
                                    fontSize = 11.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active",
                                    tint = HexColors.CobaltBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Sample Rate
            Text(
                text = "SAMPLE RATE RESAMPLING",
                color = HexColors.SlateGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AudioSampleRate.values().take(3).forEach { rate ->
                    val isSelected = rate == currentRate
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) HexColors.CobaltBlue else activeSkin.surfaceColor)
                            .border(1.dp, if (isSelected) HexColors.CobaltBlue else activeSkin.cardBorderColor, RoundedCornerShape(8.dp))
                            .clickable {
                                appState.audioPlayer.setAudioOutputConfig(currentMethod, rate, currentFormat)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (rate == AudioSampleRate.AUTO) "Auto" else rate.displayName.substringBefore(" "),
                            color = HexColors.PureWhite,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AudioSampleRate.values().drop(3).forEach { rate ->
                    val isSelected = rate == currentRate
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) HexColors.CobaltBlue else activeSkin.surfaceColor)
                            .border(1.dp, if (isSelected) HexColors.CobaltBlue else activeSkin.cardBorderColor, RoundedCornerShape(8.dp))
                            .clickable {
                                appState.audioPlayer.setAudioOutputConfig(currentMethod, rate, currentFormat)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = rate.displayName.substringBefore(" "),
                            color = HexColors.PureWhite,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Sample Format
            Text(
                text = "SAMPLE FORMAT / BIT DEPTH",
                color = HexColors.SlateGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AudioSampleFormat.values().forEach { fmt ->
                    val isSelected = fmt == currentFormat
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) HexColors.CobaltBlue else activeSkin.surfaceColor)
                            .border(1.dp, if (isSelected) HexColors.CobaltBlue else activeSkin.cardBorderColor, RoundedCornerShape(8.dp))
                            .clickable {
                                appState.audioPlayer.setAudioOutputConfig(currentMethod, currentRate, fmt)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (fmt == AudioSampleFormat.AUTO) "Auto" else fmt.displayName.substringBefore(" "),
                            color = HexColors.PureWhite,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Section: Artist Separation
            val isSepEnabled by appState.isArtistSeparationEnabled.collectAsState()
            val activeSeparators by appState.artistSeparators.collectAsState()

            Text(
                text = "ARTIST SEPARATION",
                color = HexColors.Lavender,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Automatically split multi-artist tags into individual artists for cleaner catalogs and dedicated stats.",
                color = HexColors.SlateGray,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Master Switch
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(activeSkin.surfaceColor)
                    .border(1.dp, activeSkin.cardBorderColor, RoundedCornerShape(12.dp))
                    .clickable { appState.toggleArtistSeparation(!isSepEnabled) }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Separate Multiple Artists",
                            color = HexColors.PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isSepEnabled) "Enabled (splits collaborators)" else "Disabled (single combined name)",
                            color = if (isSepEnabled) HexColors.CobaltBlue else HexColors.SlateGray,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = isSepEnabled,
                        onCheckedChange = { appState.toggleArtistSeparation(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = HexColors.PureWhite,
                            checkedTrackColor = HexColors.CobaltBlue,
                            uncheckedThumbColor = HexColors.SlateGray,
                            uncheckedTrackColor = activeSkin.bgOled
                        )
                    )
                }
            }

            if (isSepEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ACTIVE DELIMITERS",
                    color = HexColors.SlateGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val availableDelimiters = listOf(";", "/", ",", "&", " - ", "feat.", "ft.", "featuring")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableDelimiters.take(4).forEach { delim ->
                        val isDelimActive = activeSeparators.contains(delim)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDelimActive) HexColors.CobaltBlue.copy(alpha = 0.3f) else activeSkin.surfaceColor)
                                .border(
                                    1.dp,
                                    if (isDelimActive) HexColors.CobaltBlue else activeSkin.cardBorderColor,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    val updated = activeSeparators.toMutableSet()
                                    if (isDelimActive) updated.remove(delim) else updated.add(delim)
                                    appState.setArtistSeparators(updated)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = delim.trim(),
                                color = if (isDelimActive) HexColors.PureWhite else HexColors.SlateGray,
                                fontSize = 12.sp,
                                fontWeight = if (isDelimActive) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableDelimiters.drop(4).forEach { delim ->
                        val isDelimActive = activeSeparators.contains(delim)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDelimActive) HexColors.CobaltBlue.copy(alpha = 0.3f) else activeSkin.surfaceColor)
                                .border(
                                    1.dp,
                                    if (isDelimActive) HexColors.CobaltBlue else activeSkin.cardBorderColor,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    val updated = activeSeparators.toMutableSet()
                                    if (isDelimActive) updated.remove(delim) else updated.add(delim)
                                    appState.setArtistSeparators(updated)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = delim.trim(),
                                color = if (isDelimActive) HexColors.PureWhite else HexColors.SlateGray,
                                fontSize = 12.sp,
                                fontWeight = if (isDelimActive) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Section: Storage & Cache Management
            Text(
                text = "STORAGE & CACHE",
                color = HexColors.Lavender,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            val cacheSize by appState.cacheSizeMB.collectAsState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(activeSkin.surfaceColor)
                    .border(1.dp, activeSkin.cardBorderColor, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cover Art & Metadata Cache",
                                color = HexColors.PureWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Cached album artworks, artist bios, and lyrics",
                                color = HexColors.SlateGray,
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = cacheSize,
                            color = HexColors.CobaltBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(HexColors.CobaltBlue.copy(alpha = 0.15f))
                            .border(1.dp, HexColors.CobaltBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable {
                                appState.clearAppCache()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Cache",
                                tint = HexColors.PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Clear Cache",
                                color = HexColors.PureWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Section: Audiophile Engine Architecture
            Text(
                text = "DSP & HARDWARE ARCHITECTURE",
                color = HexColors.Lavender,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(activeSkin.surfaceColor)
                    .border(1.dp, activeSkin.cardBorderColor, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoRow(label = "Target Architecture", value = "ARM64-v8a (AArch64)")
                    InfoRow(label = "DSP Acceleration", value = "NEON SIMD / C++17 (-O3)")
                    InfoRow(label = "Active Sink Driver", value = currentMethod.displayName)
                    InfoRow(label = "Equalizer Pipeline", value = "Cascaded RBJ Biquad (10-Band)")
                    InfoRow(label = "FFT Spectrum Analysis", value = "32-Band / 30 FPS Power-Save")
                    InfoRow(label = "Tracks in Library", value = "${tracks.size} tracks")
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = HexColors.SlateGray, fontSize = 12.sp)
        Text(text = value, color = HexColors.OffWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
