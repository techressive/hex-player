import SwiftUI
import AppKit

public struct AuralisApp: App {
    @StateObject private var appState = AppState.shared
    @StateObject private var audioController = AudioEngineController.shared
    @StateObject private var deviceManager = AudioDeviceManager.shared

    public init() {
        NSWindow.allowsAutomaticWindowTabbing = false
    }

    public var body: some Scene {
        WindowGroup {
            MainContentView()
                .environmentObject(appState)
                .environmentObject(audioController)
                .environmentObject(deviceManager)
                .frame(minWidth: 980, maxWidth: .infinity, minHeight: 600, maxHeight: .infinity)
                .background(appState.themeCanvas)
                .preferredColorScheme(appState.selectedSkin.isLight ? .light : .dark)
                .sheet(isPresented: $appState.showEqualizerSheet) {
                    EqualizerModalView()
                }
                .background(WindowAccessor { window in
                    window.tabbingMode = .disallowed
                    window.minSize = NSSize(width: 980, height: 600)
                    window.maxSize = NSSize(width: CGFloat.greatestFiniteMagnitude, height: CGFloat.greatestFiniteMagnitude)
                })
        }
        .windowStyle(HiddenTitleBarWindowStyle())
        .commands {
            CommandGroup(replacing: .appSettings) {
                Button("Settings...") {
                    appState.selectedSidebarItem = .settings
                }
                .keyboardShortcut(",", modifiers: [.command])
            }

            CommandMenu("Playback") {
                Button("Play / Pause") {
                    audioController.togglePlayPause()
                }
                .keyboardShortcut(.space, modifiers: [])

                Button("Next Track") {
                    appState.playNext()
                }
                .keyboardShortcut(.rightArrow, modifiers: [.command])

                Button("Previous Track") {
                    appState.playPrevious()
                }
                .keyboardShortcut(.leftArrow, modifiers: [.command])

                Button("Seek Forward 5s") {
                    audioController.seek(to: audioController.currentTime + 5.0)
                }
                .keyboardShortcut(.rightArrow, modifiers: [])

                Button("Seek Backward 5s") {
                    audioController.seek(to: audioController.currentTime - 5.0)
                }
                .keyboardShortcut(.leftArrow, modifiers: [])
            }

            CommandMenu("Audio Engine") {
                Button("Audiophile Equalizer") {
                    appState.selectedSidebarItem = .equalizer
                }
                .keyboardShortcut("e", modifiers: [.command])

                Button("Now Playing") {
                    appState.selectedSidebarItem = .nowPlaying
                }
                .keyboardShortcut("0", modifiers: [.command])

                Button("Preferences & Output Settings") {
                    appState.selectedSidebarItem = .settings
                }
            }
        }
    }
}

public final class SidebarViewState: ObservableObject {
    @Published public var isShowingNewPlaylistAlert: Bool = false
    @Published public var newPlaylistName: String = ""
    public init() {}
}

public struct SidebarView: View {
    @ObservedObject var state = AppState.shared
    @ObservedObject var audioController = AudioEngineController.shared
    @ObservedObject var deviceManager = AudioDeviceManager.shared
    @StateObject private var ui = SidebarViewState()

    public init() {}

    public var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Window traffic light spacing & HEX Branding with Top-Left Back Button
            HStack(spacing: 8) {
                if state.canGoBack {
                    Button(action: {
                        withAnimation(.easeInOut(duration: 0.15)) {
                            state.goBack()
                        }
                    }) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(state.activeAccentColor)
                            .padding(6)
                            .background(state.themeCardSurface)
                            .clipShape(Circle())
                            .overlay(
                                Circle().stroke(state.themeCardBorder, lineWidth: 1)
                            )
                    }
                    .buttonStyle(PlainButtonStyle())
                    .help("Go Back")
                }

                HexBrandHeaderView(size: 20, showWordmark: true)

                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.top, 28)
            .padding(.bottom, 16)

            ScrollView(.vertical, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 18) {
                    // SECTION 1: QUICK ACCESS
                    VStack(alignment: .leading, spacing: 4) {
                        sectionHeader("QUICK ACCESS")

                        sidebarRow(
                            item: .nowPlaying,
                            icon: "play.circle.fill",
                            title: "Now Playing",
                            badge: audioController.isPlaying ? "LIVE" : nil
                        )

                        sidebarRow(
                            item: .recent,
                            icon: "clock.arrow.circlepath",
                            title: "Recent"
                        )

                        sidebarRow(
                            item: .favorites,
                            icon: "heart.fill",
                            title: "Favorites",
                            badge: state.favoriteTracks.isEmpty ? nil : "\(state.favoriteTracks.count)"
                        )
                    }

                    // SECTION 2: PLAYLISTS
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            sectionHeader("PLAYLISTS")
                            Spacer()
                            Button(action: {
                                ui.isShowingNewPlaylistAlert = true
                            }) {
                                Image(systemName: "plus")
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundColor(state.activeAccentColor)
                                    .padding(4)
                                    .background(state.themeCardSurface)
                                    .cornerRadius(4)
                            }
                            .buttonStyle(PlainButtonStyle())
                            .help("Create New Playlist")
                        }
                        .padding(.trailing, 14)

                        if state.playlists.isEmpty {
                            Text("No playlists yet")
                                .font(.system(size: 11))
                                .foregroundColor(AuralisTheme.textMuted)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 4)
                        } else {
                            ForEach(state.playlists) { pl in
                                sidebarRow(
                                    item: .playlist(id: pl.id),
                                    icon: "music.note.list",
                                    title: pl.name,
                                    badge: "\(pl.trackIds.count)"
                                )
                                .contextMenu {
                                    Button("Play Playlist") {
                                        let tracks = state.tracks(for: pl)
                                        if let first = tracks.first {
                                            state.play(track: first)
                                            state.clearQueue()
                                            for t in tracks.dropFirst() {
                                                state.addToQueue(track: t)
                                            }
                                        }
                                    }
                                    Divider()
                                    Button("Delete Playlist", role: .destructive) {
                                        state.deletePlaylist(id: pl.id)
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 3: LIBRARY
                    VStack(alignment: .leading, spacing: 4) {
                        sectionHeader("LIBRARY")

                        sidebarRow(item: .albums, icon: "square.stack.fill", title: "Albums")
                        sidebarRow(item: .artists, icon: "person.2.fill", title: "Artists")
                        sidebarRow(item: .genres, icon: "guitars.fill", title: "Genres")
                        sidebarRow(item: .allTracks, icon: "music.note.list", title: "All Library", badge: state.tracks.isEmpty ? nil : "\(state.tracks.count)")
                    }

                    // SECTION 4: AUDIO & PREFERENCES
                    VStack(alignment: .leading, spacing: 4) {
                        sectionHeader("AUDIO & PREFERENCES")

                        sidebarRow(
                            item: .equalizer,
                            icon: "slider.vertical.3",
                            title: "Equalizer",
                            hasActiveIndicator: ParametricEQEngine.shared.isEnabled
                        )

                        sidebarRow(item: .settings, icon: "gearshape.fill", title: "Settings")
                    }
                }
                .padding(.bottom, 12)
            }

            Spacer(minLength: 8)

            // Bottom Sidebar Status Card (Engine state & active output)
            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 6) {
                    Circle()
                        .fill(AuralisTheme.emeraldDSD)
                        .frame(width: 6, height: 6)
                    Text(deviceManager.selectedDevice?.name.uppercased() ?? "CORE AUDIO 64-BIT")
                        .font(.system(size: 9, weight: .bold, design: .monospaced))
                        .foregroundColor(AuralisTheme.emeraldDSD)
                        .lineLimit(1)
                }

                if let current = audioController.currentTrack {
                    Text("\(current.codec.rawValue) • \(current.formattedSampleRate)")
                        .font(.system(size: 10, weight: .medium, design: .monospaced))
                        .foregroundColor(AuralisTheme.textMuted)
                } else {
                    Text("Bit-Perfect 64-bit Engine")
                        .font(.system(size: 10, weight: .medium, design: .monospaced))
                        .foregroundColor(AuralisTheme.textMuted)
                }
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(state.themeCardSurface)
            .cornerRadius(8)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(state.themeCardBorder, lineWidth: 1)
            )
            .padding(.horizontal, 12)
            .padding(.bottom, 16)
        }
        .frame(width: 220)
        .background(state.themeDeepCanvas)
        .overlay(
            Rectangle().frame(width: 1).foregroundColor(state.themeCardBorder),
            alignment: .trailing
        )
        .alert("Create New Playlist", isPresented: $ui.isShowingNewPlaylistAlert) {
            TextField("Playlist Name", text: $ui.newPlaylistName)
            Button("Create") {
                let trimmed = ui.newPlaylistName.trimmingCharacters(in: .whitespacesAndNewlines)
                if !trimmed.isEmpty {
                    let pl = state.createPlaylist(name: trimmed)
                    state.selectedSidebarItem = .playlist(id: pl.id)
                }
                ui.newPlaylistName = ""
            }
            Button("Cancel", role: .cancel) {
                ui.newPlaylistName = ""
            }
        }
    }

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.system(size: 10, weight: .bold, design: .monospaced))
            .foregroundColor(AuralisTheme.textMuted)
            .padding(.horizontal, 14)
            .padding(.top, 4)
            .padding(.bottom, 2)
    }

    private func sidebarRow(
        item: SidebarItem,
        icon: String,
        title: String,
        badge: String? = nil,
        hasActiveIndicator: Bool = false
    ) -> some View {
        let isSelected = (state.selectedSidebarItem == item)
        let isHovered = (state.hoveredSidebarItem == item)

        return Button(action: {
            withAnimation(.easeInOut(duration: 0.15)) {
                state.navigateTo(item: item)
                if item == .artists {
                    state.selectedArtistTarget = nil
                }
            }
        }) {
            HStack(spacing: 10) {
                Image(systemName: icon)
                    .font(.system(size: 13))
                    .frame(width: 18)
                    .foregroundColor(isSelected ? state.activeAccentColor : (isHovered ? state.themeTextPrimary : AuralisTheme.textMuted))

                Text(title)
                    .font(.system(size: 12.5, weight: isSelected ? .semibold : .medium))
                    .foregroundColor(isSelected ? state.themeTextPrimary : (isHovered ? state.themeTextPrimary : AuralisTheme.textSecondary))
                    .lineLimit(1)

                Spacer(minLength: 0)

                if hasActiveIndicator {
                    Circle()
                        .fill(AuralisTheme.emeraldDSD)
                        .frame(width: 6, height: 6)
                } else if let b = badge {
                    Text(b)
                        .font(.system(size: 9, weight: .bold, design: .monospaced))
                        .padding(.horizontal, 5)
                        .padding(.vertical, 2)
                        .background(isSelected ? state.activeAccentColor.opacity(0.2) : state.themeCardSurface)
                        .foregroundColor(isSelected ? state.activeAccentColor : AuralisTheme.textMuted)
                        .cornerRadius(4)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 12)
            .padding(.vertical, 7)
            .background(isSelected ? state.themeCardSurfaceHover : (isHovered ? state.themeCardSurface : Color.clear))
            .cornerRadius(6)
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle())
        .contentShape(Rectangle())
        .padding(.horizontal, 8)
        .onHover { hovering in
            state.hoveredSidebarItem = hovering ? item : nil
        }
    }
}

// MARK: - Main Content View Router

public struct MainContentView: View {
    @ObservedObject var state = AppState.shared
    @ObservedObject var audioController = AudioEngineController.shared
    @ObservedObject var deviceManager = AudioDeviceManager.shared

    public init() {}

    public var body: some View {
        HStack(spacing: 0) {
            // Left Sidebar
            SidebarView()

            // Main Content Area
            VStack(spacing: 0) {
                Group {
                    switch state.selectedSidebarItem {
                    case .nowPlaying:
                        NowPlayingMainView()
                    case .recent:
                        RecentTracksView()
                    case .favorites:
                        FavoritesTracksView()
                    case .playlist(let playlistId):
                        PlaylistDetailView(playlistId: playlistId)
                    case .albums:
                        LibraryView(initialSection: .albums)
                    case .artists:
                        if let target = state.selectedArtistTarget {
                            ArtistBioView(artistName: target)
                        } else {
                            LibraryView(initialSection: .artists)
                        }
                    case .genres:
                        LibraryView(initialSection: .genres)
                    case .allTracks:
                        LibraryView(initialSection: .tracks)
                    case .equalizer:
                        EqualizerModalView()
                    case .settings:
                        SettingsView()
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)

                // Global Persistent Mini Player (visible on tabs other than Now Playing)
                if state.selectedSidebarItem != .nowPlaying {
                    PersistentMiniPlayer()
                }
            }
        }
        .background(state.themeCanvas)
    }
}

// MARK: - Global Persistent Mini Player

public struct PersistentMiniPlayer: View {
    @ObservedObject var state = AppState.shared
    @ObservedObject var audioController = AudioEngineController.shared
    @ObservedObject var deviceManager = AudioDeviceManager.shared

    public init() {}

    public var body: some View {
        HStack(spacing: 0) {
            // LEFT COLUMN: Track Info (Cover, Title, Artist, Like)
            HStack(spacing: 12) {
                if let track = audioController.currentTrack {
                    Button(action: { state.selectedSidebarItem = .nowPlaying }) {
                        AlbumCoverView(
                            image: track.artwork,
                            title: track.title,
                            artist: track.artist,
                            size: 44,
                            showAmbientGlow: false
                        )
                    }
                    .buttonStyle(PlainButtonStyle())
                    .help("Open Now Playing")

                    VStack(alignment: .leading, spacing: 2) {
                        Text(track.title)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(state.themeTextPrimary)
                            .lineLimit(1)

                        HStack(spacing: 6) {
                            Button(action: {
                                state.navigateToArtist(track.artist)
                            }) {
                                Text(track.artist)
                                    .font(.system(size: 11))
                                    .foregroundColor(state.activeAccentColor)
                                    .lineLimit(1)
                            }
                            .buttonStyle(PlainButtonStyle())

                            if track.isHiRes {
                                Text("Hi\u{2011}Res")
                                    .font(.system(size: 8, weight: .black, design: .monospaced))
                                    .foregroundColor(state.activeAccentColor)
                                    .lineLimit(1)
                                    .fixedSize(horizontal: true, vertical: false)
                                    .padding(.horizontal, 4)
                                    .padding(.vertical, 1)
                                    .background(state.activeAccentColor.opacity(0.12))
                                    .cornerRadius(3)
                            }
                        }
                    }
                    .frame(maxWidth: 190, alignment: .leading)

                    // Like Button
                    Button(action: { state.toggleLike(track: track) }) {
                        Image(systemName: state.likedTrackIds.contains(track.id) ? "heart.fill" : "heart")
                            .font(.system(size: 13))
                            .foregroundColor(state.likedTrackIds.contains(track.id) ? state.activeAccentColor : AuralisTheme.textMuted)
                    }
                    .buttonStyle(PlainButtonStyle())
                    .help(state.likedTrackIds.contains(track.id) ? "Unlike" : "Like")
                } else {
                    HStack(spacing: 10) {
                        Image(systemName: "music.note")
                            .font(.system(size: 16))
                            .foregroundColor(AuralisTheme.textMuted)
                            .frame(width: 44, height: 44)
                            .background(state.themeCardSurface)
                            .cornerRadius(6)

                        VStack(alignment: .leading, spacing: 2) {
                            Text("No Track Playing")
                                .font(.system(size: 12, weight: .medium))
                                .foregroundColor(AuralisTheme.textSecondary)

                            Text("Select a track from Library")
                                .font(.system(size: 10, design: .monospaced))
                                .foregroundColor(AuralisTheme.textMuted)
                        }
                    }
                }
            }
            .frame(width: 270, alignment: .leading)

            Spacer(minLength: 16)

            // CENTER COLUMN: Playback controls + Scrubber with live timestamps
            VStack(spacing: 5) {
                // Controls Row
                HStack(spacing: 18) {
                    // Shuffle
                    Button(action: { state.toggleShuffle() }) {
                        Image(systemName: "shuffle")
                            .font(.system(size: 12, weight: state.isShuffleEnabled ? .bold : .regular))
                            .foregroundColor(state.isShuffleEnabled ? state.activeAccentColor : AuralisTheme.textMuted)
                    }
                    .buttonStyle(PlainButtonStyle())
                    .help(state.isShuffleEnabled ? "Shuffle Enabled" : "Shuffle Disabled")

                    // Previous Track
                    Button(action: { state.playPrevious() }) {
                        Image(systemName: "backward.fill")
                            .font(.system(size: 13))
                            .foregroundColor(state.themeTextPrimary)
                    }
                    .buttonStyle(PlainButtonStyle())
                    .keyboardShortcut(.leftArrow, modifiers: [.command])

                    // Play/Pause
                    Button(action: { audioController.togglePlayPause() }) {
                        Circle()
                            .fill(state.activeAccentColor)
                            .frame(width: 32, height: 32)
                            .overlay(
                                Image(systemName: audioController.isPlaying ? "pause.fill" : "play.fill")
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(.black)
                                    .offset(x: audioController.isPlaying ? 0 : 1)
                            )
                    }
                    .buttonStyle(PlainButtonStyle())

                    // Next Track
                    Button(action: { state.playNext() }) {
                        Image(systemName: "forward.fill")
                            .font(.system(size: 13))
                            .foregroundColor(state.themeTextPrimary)
                    }
                    .buttonStyle(PlainButtonStyle())
                    .keyboardShortcut(.rightArrow, modifiers: [.command])

                    // Repeat Mode
                    Button(action: { state.cycleRepeat() }) {
                        Image(systemName: state.repeatMode.iconName)
                            .font(.system(size: 12, weight: state.repeatMode != .off ? .bold : .regular))
                            .foregroundColor(state.repeatMode != .off ? state.activeAccentColor : AuralisTheme.textMuted)
                    }
                    .buttonStyle(PlainButtonStyle())
                    .help("Repeat: \(state.repeatMode.rawValue)")
                }

                // Progress Bar with Timestamps
                HStack(spacing: 8) {
                    Text(formatTime(audioController.currentTime))
                        .font(.system(size: 10, design: .monospaced))
                        .foregroundColor(AuralisTheme.textMuted)
                        .frame(width: 40, alignment: .trailing)

                    WaveformProgressBar(
                        currentTime: audioController.currentTime,
                        duration: audioController.duration,
                        onSeek: { audioController.seek(to: $0) }
                    )
                    .frame(maxWidth: .infinity)

                    Text(formatTime(audioController.duration))
                        .font(.system(size: 10, design: .monospaced))
                        .foregroundColor(AuralisTheme.textMuted)
                        .frame(width: 40, alignment: .leading)
                }
            }
            .frame(maxWidth: 480)

            Spacer(minLength: 16)

            // RIGHT COLUMN: Quick Tools, Output & Volume
            HStack(spacing: 12) {
                // Equalizer Quick Button
                Button(action: { state.showEqualizerSheet.toggle() }) {
                    Image(systemName: "slider.vertical.3")
                        .font(.system(size: 12))
                        .foregroundColor(ParametricEQEngine.shared.isEnabled ? state.activeAccentColor : AuralisTheme.textMuted)
                }
                .buttonStyle(PlainButtonStyle())
                .help("Equalizer (Cmd+E)")

                // Output Device Menu
                Menu {
                    ForEach(deviceManager.availableDevices) { dev in
                        Button(action: { audioController.setOutputDevice(dev) }) {
                            HStack {
                                Text(dev.name)
                                if deviceManager.selectedDevice?.id == dev.id {
                                    Image(systemName: "checkmark")
                                }
                            }
                        }
                    }
                    Divider()
                    Button("Audio Output Settings...") {
                        state.selectedSidebarItem = .settings
                    }
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: deviceManager.selectedDevice?.iconName ?? "speaker.wave.2.fill")
                            .font(.system(size: 10))
                        Text(deviceManager.selectedDevice?.name ?? "Output")
                            .font(.system(size: 10, weight: .medium))
                            .lineLimit(1)
                            .truncationMode(.tail)
                    }
                    .foregroundColor(state.activeAccentColor)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 3)
                    .background(state.themeCardSurface)
                    .cornerRadius(4)
                }
                .menuStyle(BorderlessButtonMenuStyle())
                .frame(maxWidth: 110)

                // Volume slider
                HStack(spacing: 4) {
                    Button(action: {
                        if audioController.volume > 0 {
                            audioController.volume = 0
                        } else {
                            audioController.volume = 0.8
                        }
                    }) {
                        Image(systemName: audioController.volume == 0 ? "speaker.slash.fill" : (audioController.volume < 0.4 ? "speaker.wave.1.fill" : "speaker.wave.2.fill"))
                            .font(.system(size: 11))
                            .foregroundColor(AuralisTheme.textMuted)
                    }
                    .buttonStyle(PlainButtonStyle())

                    Slider(
                        value: Binding(
                            get: { Double(audioController.volume) },
                            set: { audioController.volume = Float($0) }
                        ),
                        in: 0.0...1.0
                    )
                    .frame(width: 70)
                }

                // Expand to Now Playing
                Button(action: { state.selectedSidebarItem = .nowPlaying }) {
                    Image(systemName: "arrow.up.left.and.arrow.down.right")
                        .font(.system(size: 11))
                        .foregroundColor(AuralisTheme.textMuted)
                }
                .buttonStyle(PlainButtonStyle())
                .help("Expand Now Playing")
            }
            .frame(width: 270, alignment: .trailing)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 10)
        .frame(height: 68)
        .background(state.themeDeepCanvas)
        .overlay(
            Rectangle().frame(height: 1).foregroundColor(state.themeCardBorder),
            alignment: .top
        )
    }

    private func formatTime(_ seconds: Double) -> String {
        guard !seconds.isNaN && !seconds.isInfinite && seconds >= 0 else { return "0:00" }
        let total = Int(seconds)
        let m = total / 60
        let s = total % 60
        return String(format: "%d:%02d", m, s)
    }
}

// MARK: - Window Accessor

struct WindowAccessor: NSViewRepresentable {
    let callback: (NSWindow) -> Void

    func makeNSView(context: Context) -> NSView {
        let view = NSView()
        DispatchQueue.main.async {
            if let window = view.window {
                callback(window)
            }
        }
        return view
    }

    func updateNSView(_ nsView: NSView, context: Context) {}
}
