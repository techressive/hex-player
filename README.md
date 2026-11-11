# <p align="center"><img src="android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="96" height="96" alt="HEX Logo" /><br>HEX — Audiophile Music Player</p>
# <p align="center"><img src="[https://github.com/techressive/hex-player/blob/main/Sources/Hex%20Music%20App%20Showcase%20Large.jpeg]" width="1280" height="720" alt="HEX Logo"
<p align="center">
  <b>A bit-perfect, ultra-low latency, OLED-native Hi-Res audio player engineered with modern Jetpack Compose for Android.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Architecture-ARM64--v8a-0091BD?style=for-the-badge&logo=arm&logoColor=white" alt="ARM64" />
  <img src="https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge" alt="License" />
</p>

---

## 🌟 Highlights & Features

### 🎧 Bit-Perfect Audiophile Sound Engine
- **32-Bit Floating Point Pipeline**: Native 32-bit float audio processing preserving dynamic range and fine harmonic details.
- **Hi-Res Audio Detection**: Real-time detection and visual badges for FLAC, ALAC, WAV, DSD, and high-sample-rate tracks (up to 24-bit / 192kHz).
- **Hardware Equalizer Integration**: Direct binding to Android's hardware `android.media.audiofx.Equalizer` session with support for 7-band standard mode, 10-band expert tuning, bass boost, and audiophile sound presets.
- **Pure Gapless Playback**: Continuous playback with instantaneous track transitions.

### 🎤 Precision Synced Lyrics
- **Ultra-Fast 50ms Polling Loop**: Lyric line tracking operates at 20Hz refresh rates to prevent audio-lyric drift.
- **100ms Perceptual Anticipation Lead**: Lyric lines illuminate precisely in sync with vocal attacks for a studio karaoke feel.
- **Smart LRCLIB Integration**: Automatic fetching of synchronized `.lrc` lyrics from LRCLIB with fuzzy search fallback.
- **Local Companion `.lrc` Auto-Detection**: Seamlessly indexes and reads companion `.lrc` files located alongside your audio files.
- **Dedicated Fullscreen Lyrics Screen**:
  - Top 1/3 features high-resolution album artwork with artist backdrop.
  - Bottom 2/3 renders smooth scrolling, interactive lyrics that slide seamlessly under the artwork.
  - Tap any lyric line to jump directly to that timestamp.

### 🖤 Pure OLED Dark Aesthetic
- **True OLED Black (`#000000`)**: Maximizes battery life on AMOLED/OLED displays with rich contrast.
- **Neon Accents**: Signature Electric Cobalt (`#5B7CFA`) and Soft Lavender (`#A99BFF`) styling.
- **Multiple Color Themes**: Choose between Cyberpunk, Emerald, Sunset, OLED Dark, and Light mode.
- **Fluid Gestures**:
  - Swipe down anywhere on Now Playing or Lyrics to effortlessly dismiss back to your library.
  - Smooth 60/120fps hardware-accelerated horizontal swiping across library sections.
  - Smooth shrink-back animations.

### 📚 Intelligent Library Management
- **Smart Multi-Artist Separation**: Automatically parses collaborative artist tags (e.g., `Artist A, Artist B`, `feat.`) into individual clickable artist links to explore each creator's discography and biography.
- **Recursive SAF Music Scanner**: Fast Storage Access Framework directory scanning that preserves nested folder hierarchies.
- **Fast Metadata & Artwork Extraction**: Instant extraction of embedded ID3, Vorbis, and MP4 cover art, with on-demand fallback to Last.fm and MusicBrainz.
- **Dynamic Organization**:
  - **All Tracks**: Sort by title, artist, album, date added, duration, or bitrate.
  - **Artists & Albums**: Grouped collections with track count and artwork.
  - **Recently Added**: Quick access to your newest additions.
  - **Favorites & Playlists**: Curate custom playlists with one-tap favorites.
- **Home Dashboard**: Live playback statistics, Recent Plays carousel, Recently Added carousel, and Top Plays.

---

## 📱 Screenshots

| Home Screen | Now Playing | Fullscreen Lyrics |
|:---:|:---:|:---:|
| *(Add Screenshot)* | *(Add Screenshot)* | *(Add Screenshot)* |

| Library Tabs | Equalizer | Artist Detail |
|:---:|:---:|:---:|
| *(Add Screenshot)* | *(Add Screenshot)* | *(Add Screenshot)* |

---

## 🛠 Tech Stack & Architecture

- **Language**: Kotlin 1.9+
- **UI Framework**: Jetpack Compose with Material 3
- **Audio Engine**: AndroidX Media3 / ExoPlayer + `android.media.audiofx` Hardware Equalizer
- **State Management**: Unidirectional Data Flow with Kotlin Coroutines & `StateFlow`
- **Networking**: OkHttp 4 for lyrics and metadata APIs
- **Target ABI**: `arm64-v8a` (strictly optimized for modern 64-bit Android devices)
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)

---

## 🚀 Building From Source

### Prerequisites
1. **Android Studio Hedgehog (2023.1.1)** or newer.
2. **JDK 17** (configured in Android Studio under Settings > Build Tools > Gradle).
3. **Android SDK 34** and Build Tools.

### Clone and Open
```bash
git clone https://github.com/techressive/hex-player.git
cd hex-player
```

1. Open Android Studio.
2. Select **Open** and choose the `android` folder inside the cloned project:
   ```
   hex-player/android
   ```
3. Allow Gradle to sync.
4. Connect your Android device (or launch an ARM64 emulator).
5. Click **Run 'app'** (`Shift + F10`) or build the APK via:
   ```bash
   cd android
   ./gradlew assembleDebug
   ```
   The built APK will be located at:
   ```
   android/app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📦 Releases

Download the latest pre-compiled APK directly from the [Releases](https://github.com/techressive/hex-player/releases) tab.

1. Download `HEX-vX.X.X.apk` to your Android device.
2. Open the file and allow installation from unknown sources if prompted.
3. Grant audio/storage permissions and select your music folder to start listening!

---

## 🤝 Contributing

Contributions, feature suggestions, and bug reports are warmly welcome!

1. Fork the repository.
2. Create your feature branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'feat: Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
