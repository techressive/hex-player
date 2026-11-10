import SwiftUI
import AppKit

public final class NowPlayingViewState: ObservableObject {
    @Published public var showQueuePopover: Bool = false
    @Published public var showHistoryPopover: Bool = false
    @Published public var showPlaylistMenu: Bool = false
    @Published public var showFullLyricsSheet: Bool = false
    @Published public var isCoverHovered: Bool = false
    public init() {}
}

public struct NowPlayingMainView: View {
    @ObservedObject var state = AppState.shared
    @ObservedObject var audioController = AudioEngineController.shared
    @ObservedObject var eqEngine = ParametricEQEngine.shared
    @ObservedObject var deviceManager = AudioDeviceManager.shared
    @StateObject private var ui = NowPlayingViewState()

    private var artworkAccent: Color {
        state.currentPalette.primary
    }

    public init() {}

    public var body: some View {
        GeometryReader { geo in
            ZStack {
                // 1. Dynamic Background Gradient matching Cover Art Palette
                dynamicArtworkBackground

                // Top-Left Back Button (visible whenever navigation history exists)
                if state.canGoBack {
                    VStack {
                        HStack {
                            Button(action: {
                                withAnimation(.easeInOut(duration: 0.15)) {
                                    state.goBack()
                                }
                            }) {
                                HStack(spacing: 6) {
                                    Image(systemName: "chevron.left")
                                    Text("Back")
                                }
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(.white.opacity(0.85))
                                .padding(.horizontal, 10)
                                .padding(.vertical, 5)
                                .background(Color.black.opacity(0.6))
                                .cornerRadius(6)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 6)
                                        .stroke(Color.white.opacity(0.15), lineWidth: 1)
                                )
                            }
                            .buttonStyle(PlainButtonStyle())
                            .help("Go Back")

                            Spacer()
                        }
                        .padding(.top, 16)
                        .padding(.leading, 24)

                        Spacer()
                    }
                    .zIndex(10)
                }

                // 2. Main Content
                HStack(spacing: 36) {
                    Spacer(minLength: 10)

                    // Left Column: Artwork centerpiece with subtle ambient glow
                    VStack {
                        Spacer()
                        artworkCenterpiece(availableH: geo.size.height)
                        Spacer()
                    }

                    // Right Column: Typography, Actions, Visualizer, Scrubber, Transport & Volume
                    VStack(alignment: .leading, spacing: 14) {
                        Spacer()

                        // Track Metadata (Title, Artist, Album)
                        trackMetadataSection

                        // Action Buttons Bar: Like, Add to Playlist, Lyrics, Full Screen, Output Device
                        actionButtonsBar

                        // Compact FFT Spectrum Visualizer (height: 22)
                        FFTVisualizerView(height: 22, barSpacing: 2)
                            .padding(.vertical, 2)

                        // 3-Line Synchronized Lyrics Widget
                        threeLineLyricsWidget

                        // Waveform Scrubber
                        WaveformProgressBar(
                            currentTime: audioController.currentTime,
                            duration: audioController.duration,
                            onSeek: { audioController.seek(to: $0) }
                        )

                        // Transport Controls: Shuffle, Prev, Play/Pause, Next, Repeat
                        transportControlsRow

                        // Volume Bar
                        volumeBarSection

                        Spacer()
                    }
                    .frame(maxWidth: 480)

                    Spacer(minLength: 10)
                }
                .padding(.horizontal, 28)
                .padding(.vertical, 20)
            }
            .sheet(isPresented: $ui.showFullLyricsSheet) {
                LyricsView()
            }
        }
    }

    // MARK: - Dynamic Background Gradient

    private var dynamicArtworkBackground: some View {
        ZStack {
            AuralisTheme.pitchBlack

            // Radial gradient anchored from cover art position
            RadialGradient(
                gradient: Gradient(colors: [
                    state.currentPalette.primary.opacity(0.35),
                    state.currentPalette.secondary.opacity(0.18),
                    state.currentPalette.ambient.opacity(0.08),
                    Color.clear
                ]),
                center: .topLeading,
                startRadius: 80,
                endRadius: 750
            )

            // Linear gradient for bottom fade
            LinearGradient(
                colors: [
                    Color.clear,
                    AuralisTheme.pitchBlack.opacity(0.7)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
        }
        .animation(.easeInOut(duration: 0.6), value: state.currentPalette.primary)
    }

    // MARK: - Artwork Centerpiece (Click opens Detached Cover Window)

    private func artworkCenterpiece(availableH: CGFloat) -> some View {
        let coverSize = min(460.0, max(240.0, availableH * 0.60))

        return Button(action: {
            DetachedCoverWindowManager.shared.toggleWindow()
        }) {
            ZStack(alignment: .bottomTrailing) {
                if let track = audioController.currentTrack {
                    AlbumCoverView(
                        image: track.artwork,
                        title: track.title,
                        artist: track.artist,
                        size: coverSize,
                        showAmbientGlow: true,
                        onFetchArtwork: {
                            Task {
                                await ArtworkFetchService.shared.fetchAndSaveArtwork(for: track)
                            }
                        },
                        isFetchingArtwork: ArtworkFetchService.shared.isFetching(trackId: track.id)
                    )
                } else {
                    AlbumCoverView(
                        image: nil,
                        title: "No Track Playing",
                        artist: "Select or drop audio files",
                        size: coverSize,
                        showAmbientGlow: false
                    )
                }

                // Expand badge on hover
                if ui.isCoverHovered {
                    HStack(spacing: 4) {
                        Image(systemName: "arrow.up.left.and.arrow.down.right")
                            .font(.system(size: 10, weight: .bold))
                        Text("Detached Window")
                            .font(.system(size: 10, weight: .bold, design: .monospaced))
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.black.opacity(0.75))
                    .cornerRadius(6)
                    .padding(10)
                    .transition(.opacity)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle())
        .onHover { hovering in
            withAnimation(.easeInOut(duration: 0.15)) {
                ui.isCoverHovered = hovering
            }
        }
    }

    // MARK: - Track Metadata (Title, Artist, Clickable Album)

    private var trackMetadataSection: some View {
        VStack(alignment: .leading, spacing: 5) {
            if let track = audioController.currentTrack {
                Text(track.title)
                    .font(.system(size: 26, weight: .bold))
                    .foregroundColor(AuralisTheme.textPrimary)
                    .lineLimit(2)

                HStack(spacing: 6) {
                    // Artist (Click -> Artist Bio)
                    Button(action: {
                        state.navigateToArtist(track.artist)
                    }) {
                        Text(track.artist)
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(artworkAccent)
                    }
                    .buttonStyle(PlainButtonStyle())

                    Text("•")
                        .foregroundColor(AuralisTheme.textMuted)

                    // Album (Click -> Library Albums & Inspect Album)
                    Button(action: {
                        state.navigateToAlbum(named: track.album, artist: track.artist)
                    }) {
                        Text(track.album)
                            .font(.system(size: 15))
                            .foregroundColor(AuralisTheme.textSecondary)
                            .lineLimit(1)
                    }
                    .buttonStyle(PlainButtonStyle())
                }

                HStack(spacing: 10) {
                    AudioFormatBadge(track: track)

                    if track.artwork == nil {
                        Button(action: {
                            Task {
                                await ArtworkFetchService.shared.fetchAndSaveArtwork(for: track)
                            }
                        }) {
                            HStack(spacing: 5) {
                                if ArtworkFetchService.shared.isFetching(trackId: track.id) {
                                    ProgressView().controlSize(.small)
                                    Text("Fetching...")
                                } else {
                                    Image(systemName: "arrow.down.circle.fill")
                                    Text("Fetch artwork")
                                }
                            }
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(AuralisTheme.cobaltBlue)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(AuralisTheme.cardSurface)
                            .cornerRadius(6)
                            .overlay(
                                RoundedRectangle(cornerRadius: 6)
                                    .stroke(AuralisTheme.cobaltBlue.opacity(0.4), lineWidth: 1)
                            )
                        }
                        .buttonStyle(PlainButtonStyle())
                        .help("Fetch high-resolution album artwork from online databases and save to file")
                    }
                }
                .padding(.top, 2)
            } else {
                Text("Auralis Audiophile Studio")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(AuralisTheme.textPrimary)

                Text("Bit-Perfect 64-bit Audio Architecture")
                    .font(.system(size: 14))
                    .foregroundColor(AuralisTheme.textMuted)

                AudioFormatBadge(track: nil)
                    .padding(.top, 2)
            }
        }
    }

    // MARK: - Action Buttons Bar (Like, Playlist, Lyrics, Full Screen, Output)

    private var actionButtonsBar: some View {
        HStack(spacing: 12) {
            if let track = audioController.currentTrack {
                // Like Button (Heart toggle)
                Button(action: { state.toggleLike(track: track) }) {
                    Image(systemName: state.isLiked(track: track) ? "heart.fill" : "heart")
                        .font(.system(size: 14))
                        .foregroundColor(state.isLiked(track: track) ? artworkAccent : AuralisTheme.textMuted)
                        .padding(7)
                        .pitchBlackCard()
                }
                .buttonStyle(PlainButtonStyle())
                .help(state.isLiked(track: track) ? "Remove from Favorites" : "Mark as Favorite")

                // Add to Playlist / Queue Menu
                Menu {
                    Button("Add to Up Next Queue") {
                        state.addToQueue(track: track)
                    }
                    Button("Add to Favorites") {
                        if !state.isLiked(track: track) {
                            state.toggleLike(track: track)
                        }
                    }
                    if !state.playlists.isEmpty {
                        Divider()
                        ForEach(state.playlists) { pl in
                            Button("Add to \(pl.name)") {
                                state.addTrackToPlaylist(playlistId: pl.id, trackId: track.id)
                            }
                        }
                    }
                } label: {
                    Image(systemName: "text.badge.plus")
                        .font(.system(size: 14))
                        .foregroundColor(AuralisTheme.textMuted)
                        .padding(7)
                        .pitchBlackCard()
                }
                .menuStyle(BorderlessButtonMenuStyle())
                .help("Add to Playlist / Queue")

                // Lyrics Shortcut (Opens full lyrics sheet)
                Button(action: { ui.showFullLyricsSheet = true }) {
                    Image(systemName: "quote.bubble.fill")
                        .font(.system(size: 14))
                        .foregroundColor(artworkAccent)
                        .padding(7)
                        .pitchBlackCard()
                }
                .buttonStyle(PlainButtonStyle())
                .help("Open Full Lyrics")

                // Full Screen Player Button
                Button(action: {
                    NSApplication.shared.keyWindow?.toggleFullScreen(nil)
                }) {
                    Image(systemName: "arrow.up.left.and.arrow.down.right")
                        .font(.system(size: 13))
                        .foregroundColor(AuralisTheme.textMuted)
                        .padding(7)
                        .pitchBlackCard()
                }
                .buttonStyle(PlainButtonStyle())
                .help("Toggle Full Screen")
            }

            Spacer()

            // Output Device Selector Pill
            outputDevicePill
        }
    }

    // MARK: - 3-Line Synchronized Lyrics Widget

    private var threeLineLyricsWidget: some View {
        Group {
            if let track = audioController.currentTrack {
                let lyrics = track.syncedLyrics
                let activeIdx = LyricsService.shared.activeLineIndex(in: lyrics, at: audioController.currentTime)

                VStack(alignment: .leading, spacing: 5) {
                    if !lyrics.isEmpty {
                        // 1. Previous Line
                        if let cur = activeIdx, cur > 0, cur - 1 < lyrics.count {
                            let prevLine = lyrics[cur - 1]
                            Button(action: { audioController.seek(to: prevLine.time) }) {
                                Text(prevLine.text.isEmpty ? "•••" : prevLine.text)
                                    .font(.system(size: 12))
                                    .foregroundColor(AuralisTheme.textMuted.opacity(0.65))
                                    .lineLimit(1)
                            }
                            .buttonStyle(PlainButtonStyle())
                        } else {
                            Text(" ")
                                .font(.system(size: 12))
                        }

                        // 2. Current Active Line (Glowing with artwork accent)
                        if let cur = activeIdx, cur < lyrics.count {
                            let curLine = lyrics[cur]
                            Button(action: { audioController.seek(to: curLine.time) }) {
                                HStack(spacing: 7) {
                                    Circle()
                                        .fill(artworkAccent)
                                        .frame(width: 6, height: 6)
                                        .shadow(color: artworkAccent.opacity(0.8), radius: 4)

                                    Text(curLine.text.isEmpty ? "•••" : curLine.text)
                                        .font(.system(size: 14, weight: .bold))
                                        .foregroundColor(artworkAccent)
                                        .lineLimit(1)
                                }
                            }
                            .buttonStyle(PlainButtonStyle())
                        } else {
                            Text(lyrics.first?.text ?? "...")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(AuralisTheme.textSecondary)
                                .lineLimit(1)
                        }

                        // 3. Upcoming Line
                        if let cur = activeIdx, cur + 1 < lyrics.count {
                            let nextLine = lyrics[cur + 1]
                            Button(action: { audioController.seek(to: nextLine.time) }) {
                                Text(nextLine.text.isEmpty ? "•••" : nextLine.text)
                                    .font(.system(size: 12))
                                    .foregroundColor(AuralisTheme.textMuted.opacity(0.65))
                                    .lineLimit(1)
                            }
                            .buttonStyle(PlainButtonStyle())
                        } else if activeIdx == nil && lyrics.count > 1 {
                            Text(lyrics[1].text)
                                .font(.system(size: 12))
                                .foregroundColor(AuralisTheme.textMuted.opacity(0.65))
                                .lineLimit(1)
                        } else {
                            Text(" ")
                                .font(.system(size: 12))
                        }
                    } else if let plain = track.plainLyrics, !plain.isEmpty {
                        let lines = plain.components(separatedBy: .newlines).filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
                        ForEach(Array(lines.prefix(3)), id: \.self) { line in
                            Text(line)
                                .font(.system(size: 12))
                                .foregroundColor(AuralisTheme.textSecondary)
                                .lineLimit(1)
                        }
                    } else {
                        HStack(spacing: 8) {
                            Image(systemName: "music.mic")
                                .font(.system(size: 12))
                                .foregroundColor(artworkAccent.opacity(0.8))
                            Text("Synced lyrics • Click to load or search")
                                .font(.system(size: 11))
                                .foregroundColor(AuralisTheme.textMuted)
                            Spacer()
                            Button("Open") {
                                ui.showFullLyricsSheet = true
                            }
                            .font(.system(size: 10, weight: .semibold))
                            .foregroundColor(artworkAccent)
                        }
                        .padding(.vertical, 4)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(AuralisTheme.cardSurface.opacity(0.65))
                .cornerRadius(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(artworkAccent.opacity(0.2), lineWidth: 1)
                )
                .onAppear {
                    autoFetchLyricsIfNeeded(for: track)
                }
                .onChange(of: track.id) { _ in
                    autoFetchLyricsIfNeeded(for: track)
                }
            }
        }
    }

    private func autoFetchLyricsIfNeeded(for track: Track) {
        guard track.syncedLyrics.isEmpty && track.plainLyrics == nil else { return }
        Task {
            if let result = try? await LyricsService.shared.fetchLyrics(track: track) {
                await MainActor.run {
                    if let synced = result.syncedLyrics, !synced.isEmpty {
                        state.updateLyricsForCurrentTrack(synced: synced, plain: result.plainLyrics)
                    } else if let plain = result.plainLyrics {
                        state.updateLyricsForCurrentTrack(synced: [], plain: plain)
                    }
                }
            }
        }
    }

    // MARK: - Transport Controls Row (Symmetrically Centered Play/Pause)

    private var transportControlsRow: some View {
        HStack(spacing: 0) {
            // Left auxiliary controls: Queue & History (Fixed width to ensure exact centering)
            HStack(spacing: 10) {
                // Queue Popover
                Button(action: { ui.showQueuePopover.toggle() }) {
                    HStack(spacing: 4) {
                        Image(systemName: "list.bullet")
                            .font(.system(size: 13))
                        if !state.queue.isEmpty {
                            Text("\(state.queue.count)")
                                .font(.system(size: 10, weight: .bold, design: .monospaced))
                        }
                    }
                    .foregroundColor(!state.queue.isEmpty ? artworkAccent : AuralisTheme.textMuted)
                    .padding(6)
                    .pitchBlackCard()
                }
                .buttonStyle(PlainButtonStyle())
                .help("Up Next Queue")
                .popover(isPresented: $ui.showQueuePopover, arrowEdge: .bottom) {
                    queuePopoverContent
                }

                // History Popover
                Button(action: { ui.showHistoryPopover.toggle() }) {
                    Image(systemName: "clock.arrow.circlepath")
                        .font(.system(size: 13))
                        .foregroundColor(AuralisTheme.textMuted)
                        .padding(6)
                        .pitchBlackCard()
                }
                .buttonStyle(PlainButtonStyle())
                .help("Playback History")
                .popover(isPresented: $ui.showHistoryPopover, arrowEdge: .bottom) {
                    historyPopoverContent
                }
            }
            .frame(width: 80, alignment: .leading)

            Spacer()

            // Centered Main Transport Controls Cluster
            HStack(spacing: 20) {
                // Shuffle
                Button(action: { state.toggleShuffle() }) {
                    Image(systemName: "shuffle")
                        .font(.system(size: 15))
                        .foregroundColor(state.isShuffleEnabled ? artworkAccent : AuralisTheme.textMuted)
                }
                .buttonStyle(PlainButtonStyle())
                .help("Shuffle: \(state.isShuffleEnabled ? "On" : "Off")")

                // Previous
                Button(action: { state.playPrevious() }) {
                    Image(systemName: "backward.fill")
                        .font(.system(size: 19))
                        .foregroundColor(AuralisTheme.textPrimary)
                }
                .buttonStyle(PlainButtonStyle())

                // Play / Pause (Mathematical Center)
                Button(action: { audioController.togglePlayPause() }) {
                    ZStack {
                        Circle()
                            .fill(artworkAccent)
                            .frame(width: 50, height: 50)
                            .shadow(color: artworkAccent.opacity(0.45), radius: 10)

                        Image(systemName: audioController.isPlaying ? "pause.fill" : "play.fill")
                            .font(.system(size: 21, weight: .bold))
                            .foregroundColor(.black)
                            .offset(x: audioController.isPlaying ? 0 : 2)
                    }
                }
                .buttonStyle(PlainButtonStyle())

                // Next
                Button(action: { state.playNext() }) {
                    Image(systemName: "forward.fill")
                        .font(.system(size: 19))
                        .foregroundColor(AuralisTheme.textPrimary)
                }
                .buttonStyle(PlainButtonStyle())

                // Repeat
                Button(action: { state.cycleRepeat() }) {
                    Image(systemName: state.repeatMode.iconName)
                        .font(.system(size: 15))
                        .foregroundColor(state.repeatMode != .off ? artworkAccent : AuralisTheme.textMuted)
                }
                .buttonStyle(PlainButtonStyle())
                .help("Repeat: \(state.repeatMode.rawValue)")
            }

            Spacer()

            // Right auxiliary controls: DSP Equalizer (Equal 80pt frame width to balance left side)
            HStack(spacing: 10) {
                Spacer()
                Button(action: { state.showEqualizerSheet = true }) {
                    Image(systemName: "slider.vertical.3")
                        .font(.system(size: 13))
                        .foregroundColor(eqEngine.isEnabled ? artworkAccent : AuralisTheme.textMuted)
                        .padding(6)
                        .pitchBlackCard()
                }
                .buttonStyle(PlainButtonStyle())
                .help("Audiophile DSP Equalizer")
            }
            .frame(width: 80, alignment: .trailing)
        }
    }

    // MARK: - Dedicated Volume Bar

    private var volumeBarSection: some View {
        HStack(spacing: 10) {
            Button(action: {
                if audioController.volume > 0 {
                    audioController.volume = 0
                } else {
                    audioController.volume = 0.8
                }
            }) {
                Image(systemName: audioController.volume > 0.5 ? "speaker.wave.3.fill" : (audioController.volume > 0 ? "speaker.wave.1.fill" : "speaker.slash.fill"))
                    .font(.system(size: 12))
                    .foregroundColor(AuralisTheme.textSecondary)
                    .frame(width: 16)
            }
            .buttonStyle(PlainButtonStyle())

            Slider(
                value: Binding(
                    get: { Double(audioController.volume) },
                    set: { audioController.volume = Float($0) }
                ),
                in: 0.0...1.0
            )

            Text("\(Int(audioController.volume * 100))%")
                .font(.system(size: 10, weight: .medium, design: .monospaced))
                .foregroundColor(AuralisTheme.textMuted)
                .frame(width: 32, alignment: .trailing)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(AuralisTheme.cardSurface)
        .cornerRadius(6)
    }

    // MARK: - Queue Popover

    private var queuePopoverContent: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("UP NEXT QUEUE")
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .foregroundColor(artworkAccent)

                Spacer()

                if !state.queue.isEmpty {
                    Button("Clear") { state.clearQueue() }
                        .font(.system(size: 10))
                        .foregroundColor(Color.red.opacity(0.8))
                }
            }

            if state.queue.isEmpty {
                Text("No tracks in queue. Tracks play automatically from your library or structured album.")
                    .font(.system(size: 11))
                    .foregroundColor(AuralisTheme.textMuted)
                    .frame(width: 260)
                    .padding(.vertical, 8)
            } else {
                ScrollView {
                    VStack(spacing: 4) {
                        ForEach(Array(state.queue.enumerated()), id: \.element.id) { idx, item in
                            HStack(spacing: 8) {
                                Text("\(idx + 1)")
                                    .font(.system(size: 10, design: .monospaced))
                                    .foregroundColor(AuralisTheme.textMuted)
                                    .frame(width: 16)

                                VStack(alignment: .leading, spacing: 1) {
                                    Text(item.title)
                                        .font(.system(size: 12, weight: .medium))
                                        .foregroundColor(AuralisTheme.textPrimary)
                                        .lineLimit(1)
                                    Text(item.artist)
                                        .font(.system(size: 10))
                                        .foregroundColor(AuralisTheme.textMuted)
                                        .lineLimit(1)
                                }

                                Spacer()

                                Button(action: { state.removeFromQueue(at: idx) }) {
                                    Image(systemName: "xmark")
                                        .font(.system(size: 9))
                                        .foregroundColor(AuralisTheme.textMuted)
                                }
                                .buttonStyle(PlainButtonStyle())
                            }
                            .padding(.vertical, 4)
                        }
                    }
                }
                .frame(width: 280, height: 220)
            }
        }
        .padding(14)
        .background(AuralisTheme.deepCanvas)
    }

    // MARK: - History Popover

    private var historyPopoverContent: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("PLAY HISTORY")
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .foregroundColor(artworkAccent)

                Spacer()

                if !state.history.isEmpty {
                    Button("Clear") { state.clearHistory() }
                        .font(.system(size: 10))
                        .foregroundColor(Color.red.opacity(0.8))
                }
            }

            if state.history.isEmpty {
                Text("No recently played tracks.")
                    .font(.system(size: 11))
                    .foregroundColor(AuralisTheme.textMuted)
                    .frame(width: 240)
                    .padding(.vertical, 8)
            } else {
                ScrollView {
                    VStack(spacing: 4) {
                        ForEach(Array(state.history.reversed().prefix(25).enumerated()), id: \.element.id) { _, item in
                            Button(action: { state.play(track: item) }) {
                                HStack(spacing: 8) {
                                    VStack(alignment: .leading, spacing: 1) {
                                        Text(item.title)
                                            .font(.system(size: 12, weight: .medium))
                                            .foregroundColor(AuralisTheme.textPrimary)
                                            .lineLimit(1)
                                        Text(item.artist)
                                            .font(.system(size: 10))
                                            .foregroundColor(AuralisTheme.textMuted)
                                            .lineLimit(1)
                                    }
                                    Spacer()
                                    Text(item.codec.rawValue)
                                        .font(.system(size: 8, weight: .bold, design: .monospaced))
                                        .foregroundColor(artworkAccent)
                                }
                                .padding(.vertical, 3)
                            }
                            .buttonStyle(PlainButtonStyle())
                        }
                    }
                }
                .frame(width: 280, height: 220)
            }
        }
        .padding(14)
        .background(AuralisTheme.deepCanvas)
    }

    // MARK: - Output Device Selection Pill Menu

    private var outputDevicePill: some View {
        Menu {
            ForEach(deviceManager.availableDevices) { device in
                Button(action: {
                    audioController.setOutputDevice(device)
                }) {
                    HStack {
                        Text(device.name)
                        if deviceManager.selectedDevice?.id == device.id {
                            Image(systemName: "checkmark")
                        }
                    }
                }
            }
            Divider()
            Button("Audio Settings...") {
                state.selectedSidebarItem = .settings
            }
        } label: {
            HStack(spacing: 6) {
                Image(systemName: deviceManager.selectedDevice?.iconName ?? "speaker.wave.2.fill")
                    .font(.system(size: 10))
                    .foregroundColor(artworkAccent)

                Text(deviceManager.selectedDevice?.name ?? "Select Output")
                    .font(.system(size: 11, weight: .semibold, design: .monospaced))
                    .foregroundColor(AuralisTheme.textSecondary)
                    .lineLimit(1)

                Image(systemName: "chevron.up.chevron.down")
                    .font(.system(size: 8))
                    .foregroundColor(AuralisTheme.textMuted)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(AuralisTheme.cardSurface)
            .cornerRadius(6)
            .overlay(
                RoundedRectangle(cornerRadius: 6)
                    .stroke(AuralisTheme.cardBorder, lineWidth: 1)
            )
        }
        .menuStyle(BorderlessButtonMenuStyle())
    }
}
