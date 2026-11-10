import SwiftUI

public final class LyricsViewState: ObservableObject {
    @Published public var autoScrollEnabled: Bool = true
    @Published public var isSearchingOnline: Bool = false
    @Published public var searchStatusMessage: String? = nil
    @Published public var showManualSearch: Bool = false
    @Published public var manualTitle: String = ""
    @Published public var manualArtist: String = ""
    public init() {}
}

public struct LyricsView: View {
    @ObservedObject var state = AppState.shared
    @ObservedObject var audioController = AudioEngineController.shared
    @StateObject private var ui = LyricsViewState()

    public init() {}

    public var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text("SYNCHRONIZED LYRICS")
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .foregroundColor(AuralisTheme.textSecondary)

                if ui.isSearchingOnline {
                    HStack(spacing: 6) {
                        ProgressView()
                            .controlSize(.small)
                        Text("Fetching from LRCLIB...")
                            .font(.system(size: 10, design: .monospaced))
                            .foregroundColor(AuralisTheme.gold)
                    }
                    .padding(.leading, 8)
                }

                Spacer()

                Button(action: {
                    if let track = audioController.currentTrack {
                        ui.manualTitle = track.title
                        ui.manualArtist = track.artist
                    }
                    ui.showManualSearch.toggle()
                }) {
                    HStack(spacing: 4) {
                        Image(systemName: "magnifyingglass")
                        Text("Search Online")
                    }
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(AuralisTheme.textMuted)
                }
                .buttonStyle(PlainButtonStyle())
                .padding(.trailing, 10)

                Button(action: { ui.autoScrollEnabled.toggle() }) {
                    HStack(spacing: 4) {
                        Image(systemName: ui.autoScrollEnabled ? "arrow.down.circle.fill" : "arrow.down.circle")
                        Text("Auto-Scroll")
                    }
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(ui.autoScrollEnabled ? AuralisTheme.gold : AuralisTheme.textMuted)
                }
                .buttonStyle(PlainButtonStyle())
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 14)
            .background(AuralisTheme.deepCanvas)
            .overlay(
                Rectangle().frame(height: 1).foregroundColor(AuralisTheme.cardBorder),
                alignment: .bottom
            )

            // Manual Search Bar if expanded
            if ui.showManualSearch {
                HStack(spacing: 10) {
                    TextField("Title", text: $ui.manualTitle)
                        .textFieldStyle(PlainTextFieldStyle())
                        .font(.system(size: 12))
                        .padding(6)
                        .background(AuralisTheme.cardSurface)
                        .cornerRadius(6)

                    TextField("Artist", text: $ui.manualArtist)
                        .textFieldStyle(PlainTextFieldStyle())
                        .font(.system(size: 12))
                        .padding(6)
                        .background(AuralisTheme.cardSurface)
                        .cornerRadius(6)

                    Button("Fetch") {
                        Task {
                            await searchLyrics(title: ui.manualTitle, artist: ui.manualArtist)
                        }
                    }
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(.black)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 5)
                    .background(AuralisTheme.gold)
                    .cornerRadius(6)
                    .buttonStyle(PlainButtonStyle())
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 10)
                .background(AuralisTheme.cardSurfaceHover)
            }

            // Content
            if let track = audioController.currentTrack {
                if !track.syncedLyrics.isEmpty {
                    let activeIndex = LyricsService.shared.activeLineIndex(in: track.syncedLyrics, at: audioController.currentTime)

                    ScrollViewReader { proxy in
                        ScrollView(.vertical, showsIndicators: false) {
                            VStack(alignment: .leading, spacing: 22) {
                                Spacer().frame(height: 60)

                                ForEach(0..<track.syncedLyrics.count, id: \.self) { idx in
                                    let line = track.syncedLyrics[idx]
                                    let isActive = activeIndex == idx

                                    Button(action: {
                                        audioController.seek(to: line.time)
                                    }) {
                                        Text(line.text.isEmpty ? "•••" : line.text)
                                            .font(.system(size: isActive ? 24 : 18, weight: isActive ? .bold : .medium, design: .default))
                                            .foregroundColor(isActive ? Color.white : AuralisTheme.textMuted)
                                            .shadow(color: isActive ? AuralisTheme.gold.opacity(0.6) : Color.clear, radius: 10)
                                            .blur(radius: isActive ? 0 : 0.4)
                                            .scaleEffect(isActive ? 1.04 : 1.0, anchor: .leading)
                                            .animation(.spring(response: 0.35, dampingFraction: 0.75), value: isActive)
                                            .padding(.vertical, 4)
                                    }
                                    .buttonStyle(PlainButtonStyle())
                                    .id(idx)
                                }

                                Spacer().frame(height: 120)
                            }
                            .padding(.horizontal, 32)
                        }
                        .onChange(of: activeIndex) { _, newIndex in
                            if ui.autoScrollEnabled, let idx = newIndex {
                                withAnimation(.easeInOut(duration: 0.4)) {
                                    proxy.scrollTo(idx, anchor: .center)
                                }
                            }
                        }
                    }
                } else if let plain = track.plainLyrics, !plain.isEmpty {
                    ScrollView {
                        Text(plain)
                            .font(.system(size: 16, weight: .regular))
                            .foregroundColor(AuralisTheme.textPrimary)
                            .lineSpacing(8)
                            .padding(32)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                } else {
                    VStack(spacing: 12) {
                        Image(systemName: "music.mic")
                            .font(.system(size: 40))
                            .foregroundColor(AuralisTheme.textMuted)

                        Text("No Synchronized Lyrics Found")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(AuralisTheme.textSecondary)

                        if let msg = ui.searchStatusMessage {
                            Text(msg)
                                .font(.system(size: 12, design: .monospaced))
                                .foregroundColor(AuralisTheme.gold)
                        } else {
                            Text("Auralis queries LRCLIB for synced timestamps. You can also place a .lrc companion file next to the audio file.")
                                .font(.system(size: 12))
                                .foregroundColor(AuralisTheme.textMuted)
                                .multilineTextAlignment(.center)
                                .frame(maxWidth: 340)
                        }

                        Button(action: {
                            Task {
                                await searchLyrics(title: track.title, artist: track.artist)
                            }
                        }) {
                            HStack(spacing: 6) {
                                Image(systemName: "arrow.clockwise")
                                Text("Search Online Lyrics Now")
                            }
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(AuralisTheme.gold)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .pitchBlackCard()
                        }
                        .buttonStyle(PlainButtonStyle())
                        .padding(.top, 8)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            } else {
                VStack(spacing: 12) {
                    Image(systemName: "waveform.badge.magnifyingglass")
                        .font(.system(size: 40))
                        .foregroundColor(AuralisTheme.textMuted)

                    Text("No Track Playing")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(AuralisTheme.textSecondary)

                    Text("Select a track in your Library to stream audio and fetch synchronized lyrics.")
                        .font(.system(size: 12))
                        .foregroundColor(AuralisTheme.textMuted)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background(AuralisTheme.pitchBlack)
        .onAppear {
            checkAndFetchLyrics()
        }
        .onChange(of: audioController.currentTrack?.id) { _, _ in
            checkAndFetchLyrics()
        }
    }

    private func checkAndFetchLyrics() {
        guard let track = audioController.currentTrack else { return }
        if track.syncedLyrics.isEmpty && track.plainLyrics == nil {
            Task {
                await searchLyrics(title: track.title, artist: track.artist)
            }
        }
    }

    private func searchLyrics(title: String, artist: String) async {
        ui.isSearchingOnline = true
        ui.searchStatusMessage = nil
        defer { ui.isSearchingOnline = false }

        let result = await LyricsService.shared.fetchOnlineLyrics(
            title: title,
            artist: artist,
            album: audioController.currentTrack?.album,
            duration: audioController.currentTrack?.duration
        )

        guard let res = result else {
            ui.searchStatusMessage = "No lyrics match found on LRCLIB."
            return
        }

        if res.isInstrumental {
            ui.searchStatusMessage = "Track identified as Instrumental."
            return
        }

        if let synced = res.syncedLyrics, !synced.isEmpty {
            state.updateLyricsForCurrentTrack(synced: synced, plain: res.plainLyrics)
        } else if let plain = res.plainLyrics {
            state.updateLyricsForCurrentTrack(synced: [], plain: plain)
        }
    }
}
