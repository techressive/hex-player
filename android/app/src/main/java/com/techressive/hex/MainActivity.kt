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
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.techressive.hex.state.AppState
import com.techressive.hex.state.Screen
import com.techressive.hex.ui.components.FirstLaunchWelcomeScreen
import com.techressive.hex.ui.components.HexSplashScreen
import com.techressive.hex.ui.components.MiniPlayerBar
import com.techressive.hex.ui.screens.ArtistBioScreen
import com.techressive.hex.ui.screens.EqualizerModalSheet
import com.techressive.hex.ui.screens.LibraryScreen
import com.techressive.hex.ui.screens.NowPlayingScreen
import com.techressive.hex.ui.screens.SettingsScreen
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.techressive.hex.ui.theme.HexColors
import com.techressive.hex.ui.theme.HexLogo
import com.techressive.hex.ui.theme.HexTheme

class MainActivity : ComponentActivity() {

    companion object {
        @Volatile
        private var retainedAppState: AppState? = null

        fun getOrCreateAppState(context: android.content.Context): AppState {
            return retainedAppState ?: synchronized(this) {
                retainedAppState ?: AppState(context.applicationContext).also { retainedAppState = it }
            }
        }
    }

    private lateinit var appState: AppState

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val perms = mutableListOf<String>()
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                perms.add(android.Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (perms.isNotEmpty()) {
                requestPermissions(perms.toTypedArray(), 101)
            }
        } else {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 101)
            }
        }

        setContent {
            appState = remember { getOrCreateAppState(applicationContext) }

            val currentScreen by appState.currentScreen.collectAsState()
            val currentTrack by appState.currentTrack.collectAsState()
            val tracks by appState.tracks.collectAsState()
            val hasCompletedOnboarding by appState.hasCompletedOnboarding.collectAsState()
            val canGoBack by appState.canGoBack.collectAsState()
            val isEqVisible by appState.isEqSheetVisible.collectAsState()
            val isDrawerOpen by appState.isDrawerOpen.collectAsState()
            val themeMode by appState.themeMode.collectAsState()
            val activeSkin by appState.activeSkin.collectAsState()

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

            // Synchronize AppState isDrawerOpen with Material 3 DrawerState
            LaunchedEffect(isDrawerOpen) {
                if (isDrawerOpen && drawerState.isClosed) {
                    drawerState.open()
                } else if (!isDrawerOpen && drawerState.isOpen) {
                    drawerState.close()
                }
            }

            LaunchedEffect(drawerState.isOpen) {
                if (drawerState.isOpen != isDrawerOpen) {
                    if (drawerState.isOpen) appState.openDrawer() else appState.closeDrawer()
                }
            }

            var showSplash by remember { mutableStateOf(true) }

            // System Back button listener connects to Drawer, Equalizer, and AppState back navigation
            BackHandler(enabled = (isDrawerOpen || isEqVisible || canGoBack) && !showSplash) {
                if (isDrawerOpen) {
                    appState.closeDrawer()
                } else if (isEqVisible) {
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
                    } else if (!hasCompletedOnboarding && tracks.isEmpty()) {
                        FirstLaunchWelcomeScreen(
                            activeSkin = activeSkin,
                            onFolderSelected = { uri -> appState.importDirectoryUri(uri) },
                            onSkip = { appState.setCompletedOnboarding(true) }
                        )
                    } else {
                        val isImmersive = currentScreen is Screen.NowPlaying || currentScreen is Screen.FullScreenLyrics

                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            gesturesEnabled = !isImmersive,
                            drawerContent = {
                                ModalDrawerSheet(
                                    drawerContainerColor = activeSkin.bgOled,
                                    drawerContentColor = HexColors.PureWhite,
                                    modifier = Modifier.width(300.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                    ) {
                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Brand Header in Sidebar
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        ) {
                                            HexLogo(size = 38.dp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "HEX",
                                                    color = HexColors.PureWhite,
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = "Audiophile Hi-Res Player",
                                                    color = HexColors.Lavender,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(20.dp))
                                        HorizontalDivider(color = activeSkin.cardBorderColor)
                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Navigation Items
                                        NavigationDrawerItem(
                                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                            label = { Text("Home", fontWeight = FontWeight.SemiBold) },
                                            selected = currentScreen is Screen.Home,
                                            onClick = {
                                                appState.closeDrawer()
                                                appState.navigateTo(Screen.Home)
                                            },
                                            colors = NavigationDrawerItemDefaults.colors(
                                                selectedContainerColor = HexColors.CobaltBlue.copy(alpha = 0.2f),
                                                selectedTextColor = HexColors.CobaltBlue,
                                                selectedIconColor = HexColors.CobaltBlue,
                                                unselectedTextColor = HexColors.PureWhite,
                                                unselectedIconColor = HexColors.SlateGray
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        NavigationDrawerItem(
                                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                                            label = { Text("Library", fontWeight = FontWeight.SemiBold) },
                                            badge = {
                                                if (tracks.isNotEmpty()) {
                                                    Text(
                                                        text = "${tracks.size}",
                                                        color = HexColors.SlateGray,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            },
                                            selected = currentScreen is Screen.Library,
                                            onClick = {
                                                appState.closeDrawer()
                                                appState.navigateTo(Screen.Library)
                                            },
                                            colors = NavigationDrawerItemDefaults.colors(
                                                selectedContainerColor = HexColors.CobaltBlue.copy(alpha = 0.2f),
                                                selectedTextColor = HexColors.CobaltBlue,
                                                selectedIconColor = HexColors.CobaltBlue,
                                                unselectedTextColor = HexColors.PureWhite,
                                                unselectedIconColor = HexColors.SlateGray
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        NavigationDrawerItem(
                                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Now Playing") },
                                            label = { Text("Now Playing", fontWeight = FontWeight.SemiBold) },
                                            badge = {
                                                if (currentTrack != null) {
                                                    Text(
                                                        text = currentTrack?.title ?: "",
                                                        color = HexColors.CobaltBlue,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.width(100.dp)
                                                    )
                                                }
                                            },
                                            selected = currentScreen is Screen.NowPlaying || currentScreen is Screen.FullScreenLyrics,
                                            onClick = {
                                                appState.closeDrawer()
                                                appState.navigateTo(Screen.NowPlaying)
                                            },
                                            colors = NavigationDrawerItemDefaults.colors(
                                                selectedContainerColor = HexColors.CobaltBlue.copy(alpha = 0.2f),
                                                selectedTextColor = HexColors.CobaltBlue,
                                                selectedIconColor = HexColors.CobaltBlue,
                                                unselectedTextColor = HexColors.PureWhite,
                                                unselectedIconColor = HexColors.SlateGray
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        NavigationDrawerItem(
                                            icon = { Icon(Icons.Default.GraphicEq, contentDescription = "Equalizer") },
                                            label = { Text("Equalizer", fontWeight = FontWeight.SemiBold) },
                                            badge = {
                                                Text(
                                                    text = "DSP",
                                                    color = HexColors.Lavender,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            selected = isEqVisible,
                                            onClick = {
                                                appState.closeDrawer()
                                                appState.showEqualizer(true)
                                            },
                                            colors = NavigationDrawerItemDefaults.colors(
                                                selectedContainerColor = HexColors.CobaltBlue.copy(alpha = 0.2f),
                                                selectedTextColor = HexColors.CobaltBlue,
                                                selectedIconColor = HexColors.CobaltBlue,
                                                unselectedTextColor = HexColors.PureWhite,
                                                unselectedIconColor = HexColors.SlateGray
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        NavigationDrawerItem(
                                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                            label = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                                            selected = currentScreen is Screen.Settings,
                                            onClick = {
                                                appState.closeDrawer()
                                                appState.navigateTo(Screen.Settings)
                                            },
                                            colors = NavigationDrawerItemDefaults.colors(
                                                selectedContainerColor = HexColors.CobaltBlue.copy(alpha = 0.2f),
                                                selectedTextColor = HexColors.CobaltBlue,
                                                selectedIconColor = HexColors.CobaltBlue,
                                                unselectedTextColor = HexColors.PureWhite,
                                                unselectedIconColor = HexColors.SlateGray
                                            )
                                        )

                                        Spacer(modifier = Modifier.weight(1f))

                                        // Footer in Sidebar
                                        HorizontalDivider(color = activeSkin.cardBorderColor)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                                            Text(
                                                text = appState.audioPlayer.activeEngineBadge,
                                                color = HexColors.SlateGray,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "HEX v1.0.0 • Pure Bit-Perfect",
                                                color = HexColors.SlateGray.copy(alpha = 0.6f),
                                                fontSize = 10.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(activeSkin.bgOled)
                                    .safeDrawingPadding()
                            ) {
                                val hasMiniPlayer = !isImmersive && currentTrack != null

                                AnimatedContent(
                                    targetState = currentScreen,
                                    transitionSpec = {
                                        val fluidEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)
                                        if (targetState is Screen.NowPlaying || targetState is Screen.FullScreenLyrics) {
                                            (androidx.compose.animation.slideInVertically(
                                                animationSpec = tween(360, easing = fluidEase),
                                                initialOffsetY = { height -> height }
                                            ) + fadeIn(animationSpec = tween(360, easing = fluidEase)))
                                                .togetherWith(
                                                    androidx.compose.animation.slideOutVertically(
                                                        animationSpec = tween(300, easing = fluidEase),
                                                        targetOffsetY = { height -> height }
                                                    ) + fadeOut(animationSpec = tween(300, easing = fluidEase))
                                                )
                                        } else {
                                            (androidx.compose.animation.scaleIn(
                                                initialScale = 0.94f,
                                                animationSpec = tween(280, easing = fluidEase)
                                            ) + fadeIn(animationSpec = tween(280, easing = fluidEase)))
                                                .togetherWith(
                                                    androidx.compose.animation.scaleOut(
                                                        targetScale = 0.94f,
                                                        animationSpec = tween(260, easing = fluidEase)
                                                    ) + fadeOut(animationSpec = tween(260, easing = fluidEase))
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
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::appState.isInitialized && isFinishing) {
            appState.audioPlayer.release()
        }
    }
}
