import SwiftUI
import AppKit

// MARK: - Reusable Shared Tracks Table View

public final class SharedTracksViewState: ObservableObject {
    @Published public var hoveredTrackId: UUID? = nil
    public init() {}
}

public struct SharedTracksTableView: View {
    public let tracks: [Track]
    public let playlistId: UUID?

    @ObservedObject var state = AppState.shared
    @ObservedObject var audioController = AudioEngineController.shared
    @StateObject private var ui = SharedTracksViewState()

    public init(tracks: [Track], playlistId: UUID? = nil) {
        self.tracks = tracks
        self.playlistId = playlistId
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Header Row
            HStack(spacing: 12) {
                Text("#")
                    .frame(width: 32, alignment: .center)
                Text("TITLE")
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text("ARTIST")
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text("ALBUM")
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text("FORMAT")
                    .frame(width: 80, alignment: .center)
                Text("SAMPLE RATE")
                    .frame(width: 90, alignment: .trailing)
                Text("DURATION")
                    .frame(width: 65, alignment: .trailing)
                Text("")
                    .frame(width: 30, alignment: .center)
            }
            .font(.system(size: 10, weight: .bold, design: .monospaced))
            .foregroundColor(AuralisTheme.textMuted)
            .padding(.horizontal, 20)
            .padding(.vertical, 8)
            .background(state.themeCardSurface)

            // Rows
            ScrollView {
                LazyVStack(spacing: 2) {
                    ForEach(Array(tracks.enumerated()), id: \.element.id) { index, track in
                        let isCurrent = audioController.currentTrack?.id == track.id
                        let isHovered = ui.hoveredTrackId == track.id

                        HStack(spacing: 12) {
                            // Play state or index
                            ZStack {
                                if isCurrent && audioController.isPlaying {
                                    Image(systemName: "waveform")
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundColor(state.activeAccentColor)
                                } else {
                                    Text("\(index + 1)")
                                        .font(.system(size: 11, design: .monospaced))
                                        .foregroundColor(isCurrent ? state.activeAccentColor : AuralisTheme.textMuted)
                                }
                            }
                            .frame(width: 32, alignment: .center)

                            // Title + Hi-Res Badge
                            HStack(spacing: 8) {
                                Text(track.title)
                                    .font(.system(size: 13, weight: isCurrent ? .bold : .medium))
                                    .foregroundColor(isCurrent ? state.activeAccentColor : state.themeTextPrimary)
                                    .lineLimit(1)

                                if track.isHiRes {
                                    Text("Hi\u{2011}Res")
                                        .font(.system(size: 8, weight: .black, design: .monospaced))
                                        .foregroundColor(state.activeAccentColor)
                                        .lineLimit(1)
                                        .fixedSize(horizontal: true, vertical: false)
                                        .padding(.horizontal, 4)
                                        .padding(.vertical, 1)
                                        .background(state.activeAccentColor.opacity(0.15))
                                        .cornerRadius(3)
                                        .layoutPriority(1)
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)

                            // Artist (Clickable)
                            Button(action: {
                                state.navigateToArtist(track.artist)
                            }) {
                                Text(track.artist)
                                    .font(.system(size: 12))
                                    .foregroundColor(AuralisTheme.textSecondary)
                                    .lineLimit(1)
                            }
                            .buttonStyle(PlainButtonStyle())
                            .frame(maxWidth: .infinity, alignment: .leading)

                            // Album (Clickable)
                            Button(action: {
                                state.navigateToAlbum(named: track.album, artist: track.artist)
                            }) {
                                Text(track.album)
                                    .font(.system(size: 12))
                                    .foregroundColor(AuralisTheme.textMuted)
                                    .lineLimit(1)
                            }
                            .buttonStyle(PlainButtonStyle())
                            .frame(maxWidth: .infinity, alignment: .leading)

                            // Format
                            Text(track.codec.rawValue)
                                .font(.system(size: 10, weight: .bold, design: .monospaced))
                                .foregroundColor(state.activeAccentColor)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(state.activeAccentColor.opacity(0.12))
                                .cornerRadius(4)
                                .frame(width: 80, alignment: .center)

                            // Sample Rate
                            Text(track.formattedSampleRate)
                                .font(.system(size: 11, design: .monospaced))
                                .foregroundColor(AuralisTheme.textSecondary)
                                .frame(width: 90, alignment: .trailing)

                            // Duration
                            Text(formatDuration(track.duration))
                                .font(.system(size: 11, design: .monospaced))
                                .foregroundColor(AuralisTheme.textMuted)
                                .frame(width: 65, alignment: .trailing)

                            // Context Actions: Like & Options Menu
                            HStack(spacing: 6) {
                                Button(action: {
                                    state.toggleLike(track: track)
                                }) {
                                    Image(systemName: state.isLiked(track: track) ? "heart.fill" : "heart")
                                        .font(.system(size: 11))
                                        .foregroundColor(state.isLiked(track: track) ? state.activeAccentColor : AuralisTheme.textMuted)
                                }
                                .buttonStyle(PlainButtonStyle())

                                if let plId = playlistId {
                                    Button(action: {
                                        state.removeTrackFromPlaylist(trackId: track.id, playlistId: plId)
                                    }) {
                                        Image(systemName: "minus.circle")
                                            .font(.system(size: 11))
                                            .foregroundColor(AuralisTheme.textMuted)
                                    }
                                    .buttonStyle(PlainButtonStyle())
                                    .help("Remove from Playlist")
                                }
                            }
                            .frame(width: 30, alignment: .center)
                        }
                        .padding(.horizontal, 20)
                        .padding(.vertical, 8)
                        .background(isCurrent ? state.themeCardSurfaceHover : (isHovered ? state.themeCardSurface : Color.white.opacity(0.0001)))
                        .cornerRadius(6)
                        .contentShape(Rectangle())
                        .onHover { hovering in
                            ui.hoveredTrackId = hovering ? track.id : nil
                        }
                        .onTapGesture(count: 2) {
                            state.play(track: track)
                            state.selectedSidebarItem = .nowPlaying
                        }
                    }
                }
                .padding(.vertical, 4)
            }
        }
    }

    private func formatDuration(_ seconds: Double) -> String {
        guard seconds.isFinite && !seconds.isNaN && seconds >= 0 else { return "0:00" }
        let mins = Int(seconds) / 60
        let secs = Int(seconds) % 60
        return String(format: "%d:%02d", mins, secs)
    }
}

// MARK: - Favorites Tracks View

public struct FavoritesTracksView: View {
    @ObservedObject var state = AppState.shared
    @ObservedObject var audioController = AudioEngineController.shared

    public init() {}

    public var body: some View {
        VStack(spacing: 0) {
            // Header Banner
            HStack(spacing: 20) {
                if state.canGoBack {
                    Button(action: {
                        withAnimation(.easeInOut(duration: 0.15)) {
                            state.goBack()
                        }
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: "chevron.left")
                            Text("Back")
                        }
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(state.activeAccentColor)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 5)
                        .background(state.themeCardSurface)
                        .cornerRadius(6)
                        .overlay(
                            RoundedRectangle(cornerRadius: 6)
                                .stroke(state.themeCardBorder, lineWidth: 1)
                        )
                    }
                    .buttonStyle(PlainButtonStyle())
                    .help("Go Back")
                }

                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(state.activeAccentColor.opacity(0.18))
                        .frame(width: 80, height: 80)
                    Image(systemName: "heart.fill")
                        .font(.system(size: 36))
                        .foregroundColor(state.activeAccentColor)
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text("QUICK ACCESS")
                        .font(.system(size: 10, weight: .bold, design: .monospaced))
                        .foregroundColor(state.activeAccentColor)

                    Text("Favorites")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(state.themeTextPrimary)

                    Text("\(state.favoriteTracks.count) liked tracks")
                        .font(.system(size: 12, design: .monospaced))
                        .foregroundColor(AuralisTheme.textMuted)
                }

                Spacer()

                if !state.favoriteTracks.isEmpty {
                    HStack(spacing: 12) {
                        Button(action: {
                            if let first = state.favoriteTracks.first {
                                state.play(track: first)
                                state.clearQueue()
                                for t in state.favoriteTracks.dropFirst() {
                                    state.addToQueue(track: t)
                                }
                            }
                        }) {
                            HStack(spacing: 6) {
                                Image(systemName: "play.fill")
                                Text("Play All")
                            }
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.black)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(state.activeAccentColor)
                            .cornerRadius(6)
                        }
                        .buttonStyle(PlainButtonStyle())

                        Button(action: {
                            let shuffled = state.favoriteTracks.shuffled()
                            if let first = shuffled.first {
                                state.play(track: first)
                                state.clearQueue()
                                for t in shuffled.dropFirst() {
                                    state.addToQueue(track: t)
                                }
                            }
                        }) {
                            HStack(spacing: 6) {
                                Image(systemName: "shuffle")
                                Text("Shuffle")
                            }
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(state.themeTextPrimary)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(state.themeCardSurface)
                            .cornerRadius(6)
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
            }
            .padding(.horizontal, 28)
            .padding(.vertical, 20)
            .background(state.themeDeepCanvas)
            .overlay(
                Rectangle().frame(height: 1).foregroundColor(state.themeCardBorder),
                alignment: .bottom
            )

            // Content
            if state.favoriteTracks.isEmpty {
                VStack(spacing: 14) {
                    Spacer()
                    Image(systemName: "heart")
                        .font(.system(size: 48))
                        .foregroundColor(AuralisTheme.textMuted.opacity(0.5))
                    Text("No Favorites Yet")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(state.themeTextPrimary)
                    Text("Click the heart icon on any playing track or library row to save your favorite songs here.")
                        .font(.system(size: 13))
                        .foregroundColor(AuralisTheme.textMuted)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: 380)
                    Spacer()
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                SharedTracksTableView(tracks: state.favoriteTracks)
            }
        }
        .background(state.themeCanvas)
    }
}

// MARK: - Recent Tracks View

public struct RecentTracksView: View {
    @ObservedObject var state = AppState.shared
    @ObservedObject var audioController = AudioEngineController.shared

    public init() {}

    public var body: some View {
        VStack(spacing: 0) {
            // Header Banner
            HStack(spacing: 20) {
                if state.canGoBack {
                    Button(action: {
                        withAnimation(.easeInOut(duration: 0.15)) {
                            state.goBack()
                        }
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: "chevron.left")
                            Text("Back")
                        }
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(state.activeAccentColor)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 5)
                        .background(state.themeCardSurface)
                        .cornerRadius(6)
                        .overlay(
                            RoundedRectangle(cornerRadius: 6)
                                .stroke(state.themeCardBorder, lineWidth: 1)
                        )
                    }
                    .buttonStyle(PlainButtonStyle())
                    .help("Go Back")
                }

                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(state.activeAccentColor.opacity(0.18))
                        .frame(width: 80, height: 80)
                    Image(systemName: "clock.arrow.circlepath")
                        .font(.system(size: 36))
                        .foregroundColor(state.activeAccentColor)
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text("QUICK ACCESS")
                        .font(.system(size: 10, weight: .bold, design: .monospaced))
                        .foregroundColor(state.activeAccentColor)

                    Text("Recent")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(state.themeTextPrimary)

                    Text("\(state.recentTracks.count) recently played & imported tracks")
                        .font(.system(size: 12, design: .monospaced))
                        .foregroundColor(AuralisTheme.textMuted)
                }

                Spacer()

                if !state.recentTracks.isEmpty {
                    HStack(spacing: 12) {
                        Button(action: {
                            if let first = state.recentTracks.first {
                                state.play(track: first)
                                state.clearQueue()
                                for t in state.recentTracks.dropFirst() {
                                    state.addToQueue(track: t)
                                }
                            }
                        }) {
                            HStack(spacing: 6) {
                                Image(systemName: "play.fill")
                                Text("Play All")
                            }
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.black)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(state.activeAccentColor)
                            .cornerRadius(6)
                        }
                        .buttonStyle(PlainButtonStyle())

                        if !state.history.isEmpty {
                            Button(action: { state.clearHistory() }) {
                                HStack(spacing: 4) {
                                    Image(systemName: "trash")
                                    Text("Clear History")
                                }
                                .font(.system(size: 11))
                                .foregroundColor(Color.red.opacity(0.85))
                                .padding(.horizontal, 10)
                                .padding(.vertical, 8)
                                .background(state.themeCardSurface)
                                .cornerRadius(6)
                            }
                            .buttonStyle(PlainButtonStyle())
                        }
                    }
                }
            }
            .padding(.horizontal, 28)
            .padding(.vertical, 20)
            .background(state.themeDeepCanvas)
            .overlay(
                Rectangle().frame(height: 1).foregroundColor(state.themeCardBorder),
                alignment: .bottom
            )

            // Content
            if state.recentTracks.isEmpty {
                VStack(spacing: 14) {
                    Spacer()
                    Image(systemName: "clock")
                        .font(.system(size: 48))
                        .foregroundColor(AuralisTheme.textMuted.opacity(0.5))
                    Text("No Recent Tracks")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(state.themeTextPrimary)
                    Text("Your playback history and recently imported tracks will appear here.")
                        .font(.system(size: 13))
                        .foregroundColor(AuralisTheme.textMuted)
                    Spacer()
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                SharedTracksTableView(tracks: state.recentTracks)
            }
        }
        .background(state.themeCanvas)
    }
}

// MARK: - Playlist Detail View

public struct PlaylistDetailView: View {
    public let playlistId: UUID
    @ObservedObject var state = AppState.shared
    @ObservedObject var audioController = AudioEngineController.shared

    public init(playlistId: UUID) {
        self.playlistId = playlistId
    }

    private var playlist: Playlist? {
        state.playlist(with: playlistId)
    }

    private var playlistTracks: [Track] {
        guard let pl = playlist else { return [] }
        return state.tracks(for: pl)
    }

    public var body: some View {
        VStack(spacing: 0) {
            if let pl = playlist {
                // Header Banner
                HStack(spacing: 20) {
                    if state.canGoBack {
                        Button(action: {
                            withAnimation(.easeInOut(duration: 0.15)) {
                                state.goBack()
                            }
                        }) {
                            HStack(spacing: 4) {
                                Image(systemName: "chevron.left")
                                Text("Back")
                            }
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundColor(state.activeAccentColor)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 5)
                            .background(state.themeCardSurface)
                            .cornerRadius(6)
                            .overlay(
                                RoundedRectangle(cornerRadius: 6)
                                    .stroke(state.themeCardBorder, lineWidth: 1)
                            )
                        }
                        .buttonStyle(PlainButtonStyle())
                        .help("Go Back")
                    }

                    ZStack {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(state.activeAccentColor.opacity(0.18))
                            .frame(width: 80, height: 80)
                        Image(systemName: "music.note.list")
                            .font(.system(size: 36))
                            .foregroundColor(state.activeAccentColor)
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text("USER PLAYLIST")
                            .font(.system(size: 10, weight: .bold, design: .monospaced))
                            .foregroundColor(state.activeAccentColor)

                        Text(pl.name)
                            .font(.system(size: 28, weight: .bold))
                            .foregroundColor(state.themeTextPrimary)

                        Text("\(playlistTracks.count) tracks")
                            .font(.system(size: 12, design: .monospaced))
                            .foregroundColor(AuralisTheme.textMuted)
                    }

                    Spacer()

                    HStack(spacing: 12) {
                        if !playlistTracks.isEmpty {
                            Button(action: {
                                if let first = playlistTracks.first {
                                    state.play(track: first)
                                    state.clearQueue()
                                    for t in playlistTracks.dropFirst() {
                                        state.addToQueue(track: t)
                                    }
                                }
                            }) {
                                HStack(spacing: 6) {
                                    Image(systemName: "play.fill")
                                    Text("Play All")
                                }
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.black)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(state.activeAccentColor)
                                .cornerRadius(6)
                            }
                            .buttonStyle(PlainButtonStyle())
                        }

                        Button(action: {
                            state.deletePlaylist(id: pl.id)
                        }) {
                            HStack(spacing: 4) {
                                Image(systemName: "trash")
                                Text("Delete")
                            }
                            .font(.system(size: 11))
                            .foregroundColor(Color.red.opacity(0.85))
                            .padding(.horizontal, 10)
                            .padding(.vertical, 8)
                            .background(state.themeCardSurface)
                            .cornerRadius(6)
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
                .padding(.horizontal, 28)
                .padding(.vertical, 20)
                .background(state.themeDeepCanvas)
                .overlay(
                    Rectangle().frame(height: 1).foregroundColor(state.themeCardBorder),
                    alignment: .bottom
                )

                // Content
                if playlistTracks.isEmpty {
                    VStack(spacing: 14) {
                        Spacer()
                        Image(systemName: "music.note.list")
                            .font(.system(size: 48))
                            .foregroundColor(AuralisTheme.textMuted.opacity(0.5))
                        Text("Playlist is Empty")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(state.themeTextPrimary)
                        Text("Add songs to '\(pl.name)' by clicking the + menu on any track.")
                            .font(.system(size: 13))
                            .foregroundColor(AuralisTheme.textMuted)
                        Spacer()
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    SharedTracksTableView(tracks: playlistTracks, playlistId: pl.id)
                }
            } else {
                VStack(spacing: 12) {
                    Spacer()
                    Text("Playlist not found")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(AuralisTheme.textMuted)
                    Button("Go to All Tracks") {
                        state.selectedSidebarItem = .allTracks
                    }
                    .buttonStyle(PlainButtonStyle())
                    .foregroundColor(state.activeAccentColor)
                    Spacer()
                }
            }
        }
        .background(state.themeCanvas)
    }
}
