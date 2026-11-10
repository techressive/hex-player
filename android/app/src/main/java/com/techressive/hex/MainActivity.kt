package com.techressive.hex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.techressive.hex.state.AppState
import com.techressive.hex.state.Screen
import com.techressive.hex.ui.components.HexSplashScreen
import com.techressive.hex.ui.components.MiniPlayerBar
import com.techressive.hex.ui.screens.ArtistBioScreen
import com.techressive.hex.ui.screens.EqualizerModalSheet
import com.techressive.hex.ui.screens.LibraryScreen
import com.techressive.hex.ui.screens.NowPlayingScreen
import com.techressive.hex.ui.screens.SettingsScreen
import com.techressive.hex.ui.theme.HexColors
import com.techressive.hex.ui.theme.HexTheme

class MainActivity : ComponentActivity() {

    private lateinit var appState: AppState

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val scope = rememberCoroutineScope()
            appState = remember { AppState(applicationContext, scope) }

            val currentScreen by appState.currentScreen.collectAsState()
            val currentTrack by appState.currentTrack.collectAsState()
            val canGoBack by appState.canGoBack.collectAsState()
            val isEqVisible by appState.isEqSheetVisible.collectAsState()
            val themeMode by appState.themeMode.collectAsState()
            val activeSkin by appState.activeSkin.collectAsState()

            var showSplash by remember { mutableStateOf(true) }

            // System Back button listener connects to AppState back navigation
            BackHandler(enabled = (canGoBack || isEqVisible) && !showSplash) {
                if (isEqVisible) {
                    appState.showEqualizer(false)
                } else if (canGoBack) {
                    appState.goBack()
                }
            }

            HexTheme(mode = themeMode, skin = activeSkin) {
                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(400),
                    label = "SplashCrossfade"
                ) { isSplash ->
                    if (isSplash) {
                        HexSplashScreen(onFinish = { showSplash = false })
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(activeSkin.bgOled)
                                .safeDrawingPadding()
                        ) {
                            val isImmersive = currentScreen is Screen.NowPlaying || currentScreen is Screen.FullScreenLyrics
                            val hasMiniPlayer = !isImmersive && currentTrack != null

                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = {
                                    if (targetState is Screen.NowPlaying || targetState is Screen.FullScreenLyrics) {
                                        // Smooth vertical slide up popup animation from bottom
                                        (androidx.compose.animation.slideInVertically(
                                            animationSpec = tween(340),
                                            initialOffsetY = { height -> height }
                                        ) + fadeIn(animationSpec = tween(340)))
                                            .togetherWith(
                                                androidx.compose.animation.slideOutVertically(
                                                    animationSpec = tween(280),
                                                    targetOffsetY = { height -> height }
                                                ) + fadeOut(animationSpec = tween(280))
                                            )
                                    } else {
                                        // Smooth shrink/scale fade transition when navigating back or switching screens
                                        (androidx.compose.animation.scaleIn(
                                            initialScale = 0.93f,
                                            animationSpec = tween(260)
                                        ) + fadeIn(animationSpec = tween(260)))
                                            .togetherWith(
                                                androidx.compose.animation.scaleOut(
                                                    targetScale = 0.93f,
                                                    animationSpec = tween(240)
                                                ) + fadeOut(animationSpec = tween(240))
                                            )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = if (hasMiniPlayer) 74.dp else 0.dp),
                                label = "ScreenTransition"
                            ) { screen ->
                                when (screen) {
                                    is Screen.Home -> com.techressive.hex.ui.screens.HomeScreen(appState = appState)
                                    is Screen.Library -> LibraryScreen(appState = appState)
                                    is Screen.NowPlaying -> NowPlayingScreen(appState = appState)
                                    is Screen.FullScreenLyrics -> com.techressive.hex.ui.screens.FullScreenLyricsScreen(appState = appState)
                                    is Screen.AlbumDetail -> com.techressive.hex.ui.screens.AlbumDetailScreen(
                                        albumName = screen.albumName,
                                        artistName = screen.artist,
                                        appState = appState
                                    )
                                    is Screen.ArtistBio -> ArtistBioScreen(
                                        artistName = screen.artistName,
                                        appState = appState
                                    )
                                    is Screen.Settings -> SettingsScreen(appState = appState)
                                }
                            }

                            // Persistent Mini Player when browsing Home / Library / Artists / Settings
                            if (hasMiniPlayer) {
                                MiniPlayerBar(
                                    appState = appState,
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }

                            // Equalizer Modal Sheet Overlay
                            if (isEqVisible) {
                                EqualizerModalSheet(
                                    eqEngine = appState.eqEngine,
                                    onClose = { appState.showEqualizer(false) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::appState.isInitialized) {
            appState.audioPlayer.release()
        }
    }
}
