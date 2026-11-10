import SwiftUI
import AppKit

public final class DetachedCoverWindowState: ObservableObject {
    @Published public var isHovered: Bool = false
    public init() {}
}

public struct DetachedCoverWindowView: View {
    @ObservedObject var audioController = AudioEngineController.shared
    @ObservedObject var appState = AppState.shared
    @StateObject private var ui = DetachedCoverWindowState()

    public init() {}

    public var body: some View {
        GeometryReader { geo in
            ZStack {
                // 1. Edge-to-edge Album Artwork
                if let track = audioController.currentTrack, let artwork = track.artwork {
                    Image(nsImage: artwork)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: geo.size.width, height: geo.size.height)
                        .clipped()
                } else if let track = audioController.currentTrack {
                    ProceduralVinylArt(title: track.title, artist: track.artist, size: max(geo.size.width, geo.size.height))
                        .frame(width: geo.size.width, height: geo.size.height)
                } else {
                    ZStack {
                        AuralisTheme.deepCanvas
                        VStack(spacing: 12) {
                            Image(systemName: "music.note")
                                .font(.system(size: 48))
                                .foregroundColor(AuralisTheme.gold)
                            Text("No Track Playing")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(AuralisTheme.textMuted)
                        }
                    }
                    .frame(width: geo.size.width, height: geo.size.height)
                }

                // 2. Interactive Controls Overlay (Fades in on hover with 65% opacity dark glass)
                VStack(spacing: 0) {
                    // Top header: Close / Title / Artist
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 2) {
                            if let track = audioController.currentTrack {
                                Text(track.title)
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(.white)
                                    .lineLimit(1)
                                    .shadow(color: .black, radius: 4)

                                Text(track.artist)
                                    .font(.system(size: 12, weight: .medium))
                                    .foregroundColor(AuralisTheme.gold)
                                    .lineLimit(1)
                                    .shadow(color: .black, radius: 4)
                            }
                        }

                        Spacer()

                        Button(action: {
                            DetachedCoverWindowManager.shared.close()
                        }) {
                            Image(systemName: "xmark.circle.fill")
                                .font(.system(size: 16))
                                .foregroundColor(.white.opacity(0.8))
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 16)

                    Spacer()

                    // Middle: Scrubber & Format
                    if let track = audioController.currentTrack {
                        VStack(spacing: 4) {
                            WaveformProgressBar(
                                currentTime: audioController.currentTime,
                                duration: audioController.duration,
                                onSeek: { audioController.seek(to: $0) }
                            )
                            .frame(height: 28)

                            HStack {
                                Text(formatTime(audioController.currentTime))
                                    .font(.system(size: 9, design: .monospaced))
                                    .foregroundColor(AuralisTheme.textSecondary)

                                Spacer()

                                Text("\(track.codec.rawValue) • \(track.formattedSampleRate)")
                                    .font(.system(size: 9, weight: .bold, design: .monospaced))
                                    .foregroundColor(AuralisTheme.gold)

                                Spacer()

                                Text(formatTime(audioController.duration))
                                    .font(.system(size: 9, design: .monospaced))
                                    .foregroundColor(AuralisTheme.textSecondary)
                            }
                        }
                        .padding(.horizontal, 16)
                    }

                    // Bottom Row: Transport controls & Volume
                    HStack(spacing: 16) {
                        // Previous
                        Button(action: { appState.playPrevious() }) {
                            Image(systemName: "backward.fill")
                                .font(.system(size: 15))
                                .foregroundColor(.white)
                        }
                        .buttonStyle(PlainButtonStyle())

                        // Play / Pause
                        Button(action: { audioController.togglePlayPause() }) {
                            Circle()
                                .fill(AuralisTheme.gold)
                                .frame(width: 36, height: 36)
                                .overlay(
                                    Image(systemName: audioController.isPlaying ? "pause.fill" : "play.fill")
                                        .font(.system(size: 15, weight: .bold))
                                        .foregroundColor(.black)
                                        .offset(x: audioController.isPlaying ? 0 : 1)
                                )
                        }
                        .buttonStyle(PlainButtonStyle())

                        // Next
                        Button(action: { appState.playNext() }) {
                            Image(systemName: "forward.fill")
                                .font(.system(size: 15))
                                .foregroundColor(.white)
                        }
                        .buttonStyle(PlainButtonStyle())

                        Spacer()

                        // Volume Bar
                        HStack(spacing: 6) {
                            Image(systemName: audioController.volume > 0.5 ? "speaker.wave.3.fill" : (audioController.volume > 0 ? "speaker.wave.1.fill" : "speaker.slash.fill"))
                                .font(.system(size: 11))
                                .foregroundColor(.white.opacity(0.8))

                            Slider(
                                value: Binding(
                                    get: { Double(audioController.volume) },
                                    set: { audioController.volume = Float($0) }
                                ),
                                in: 0.0...1.0
                            )
                            .frame(width: 60)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 16)
                }
                .background(Color.black.opacity(0.68))
                .opacity(ui.isHovered ? 1.0 : 0.0)
                .animation(.easeInOut(duration: 0.2), value: ui.isHovered)
            }
            .contentShape(Rectangle())
            .onHover { hovering in
                ui.isHovered = hovering
            }
        }
        .frame(minWidth: 260, minHeight: 260)
    }

    private func formatTime(_ seconds: TimeInterval) -> String {
        let mins = Int(seconds) / 60
        let secs = Int(seconds) % 60
        return String(format: "%d:%02d", mins, secs)
    }
}

@MainActor
public final class DetachedCoverWindowManager {
    public static let shared = DetachedCoverWindowManager()

    private var windowController: NSWindowController?
    private var window: NSPanel?

    private init() {}

    public func toggleWindow() {
        if let win = window, win.isVisible {
            win.orderOut(nil)
        } else {
            showWindow()
        }
    }

    public func showWindow() {
        if let win = window {
            win.makeKeyAndOrderFront(nil)
            return
        }

        let panel = NSPanel(
            contentRect: NSRect(x: 150, y: 150, width: 360, height: 360),
            styleMask: [.titled, .closable, .resizable, .fullSizeContentView, .nonactivatingPanel],
            backing: .buffered,
            defer: false
        )

        panel.title = "Auralis - Artwork"
        panel.isFloatingPanel = true
        panel.level = .floating
        panel.titleVisibility = .hidden
        panel.titlebarAppearsTransparent = true
        panel.isMovableByWindowBackground = true
        panel.hasShadow = true
        panel.minSize = NSSize(width: 240, height: 240)
        panel.contentAspectRatio = NSSize(width: 1, height: 1)
        panel.backgroundColor = .black

        let hostingView = NSHostingView(rootView: DetachedCoverWindowView())
        panel.contentView = hostingView
        panel.center()

        self.window = panel
        panel.makeKeyAndOrderFront(nil)
    }

    public func close() {
        window?.orderOut(nil)
    }
}
