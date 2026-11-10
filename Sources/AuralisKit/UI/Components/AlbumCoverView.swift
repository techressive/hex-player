import SwiftUI
import AppKit

public final class AlbumCoverState: ObservableObject {
    @Published public var isHovered: Bool = false
    public init() {}
}

public struct AlbumCoverView: View {
    public let image: NSImage?
    public let title: String
    public let artist: String
    public let size: CGFloat
    public let showAmbientGlow: Bool
    public let onFetchArtwork: (() -> Void)?
    public let isFetchingArtwork: Bool

    @StateObject private var ui = AlbumCoverState()

    public init(
        image: NSImage?,
        title: String,
        artist: String,
        size: CGFloat = 460,
        showAmbientGlow: Bool = true,
        onFetchArtwork: (() -> Void)? = nil,
        isFetchingArtwork: Bool = false
    ) {
        self.image = image
        self.title = title
        self.artist = artist
        self.size = size
        self.showAmbientGlow = showAmbientGlow
        self.onFetchArtwork = onFetchArtwork
        self.isFetchingArtwork = isFetchingArtwork
    }

    public var body: some View {
        ZStack {
            // Ambient soft lighting glow reflecting against pitch black
            if showAmbientGlow {
                if let nsImage = image {
                    Image(nsImage: nsImage)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: size * 0.9, height: size * 0.9)
                        .blur(radius: 40)
                        .opacity(ui.isHovered ? 0.45 : 0.28)
                        .animation(.easeInOut(duration: 0.4), value: ui.isHovered)
                } else {
                    Circle()
                        .fill(
                            RadialGradient(
                                colors: [AuralisTheme.gold.opacity(0.2), Color.clear],
                                center: .center,
                                startRadius: 20,
                                endRadius: size * 0.6
                            )
                        )
                        .frame(width: size, height: size)
                }
            }

            // High-Resolution Cover Frame
            Group {
                if let nsImage = image {
                    Image(nsImage: nsImage)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: size, height: size)
                        .clipped()
                } else {
                    // Procedural Audiophile Vinyl Master Graphic
                    ProceduralVinylArt(title: title, artist: artist, size: size)
                }
            }
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(
                        LinearGradient(
                            colors: [
                                Color.white.opacity(ui.isHovered ? 0.25 : 0.12),
                                Color.white.opacity(0.02),
                                Color.black.opacity(0.8)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1.5
                    )
            )
            .shadow(color: Color.black.opacity(0.9), radius: 30, x: 0, y: 15)
            .overlay(
                Group {
                    if image == nil, let fetchAction = onFetchArtwork {
                        Button(action: fetchAction) {
                            HStack(spacing: 5) {
                                if isFetchingArtwork {
                                    ProgressView()
                                        .controlSize(.small)
                                    Text("Fetching...")
                                } else {
                                    Image(systemName: "arrow.down.circle.fill")
                                    Text("Fetch artwork")
                                }
                            }
                            .font(.system(size: max(9, min(12, size * 0.038)), weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, max(8, min(14, size * 0.045)))
                            .padding(.vertical, max(5, min(8, size * 0.025)))
                            .background(Color.black.opacity(0.85))
                            .cornerRadius(12)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(AuralisTheme.cobaltBlue, lineWidth: 1)
                            )
                            .shadow(color: Color.black.opacity(0.7), radius: 6)
                        }
                        .buttonStyle(PlainButtonStyle())
                        .padding(max(8, min(16, size * 0.04)))
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
                    }
                }
            )
            .scaleEffect(ui.isHovered ? 1.015 : 1.0)
            .animation(.spring(response: 0.35, dampingFraction: 0.8), value: ui.isHovered)
            .onHover { hovering in
                ui.isHovered = hovering
            }
        }
        .frame(width: size, height: size)
    }
}

public struct ProceduralVinylArt: View {
    let title: String
    let artist: String
    let size: CGFloat

    public var body: some View {
        ZStack {
            // Deep obsidian record canvas
            AuralisTheme.cardSurface

            // Microgrooves
            ForEach([0.35, 0.45, 0.55, 0.65, 0.75, 0.85], id: \.self) { fraction in
                Circle()
                    .stroke(Color.white.opacity(0.04), lineWidth: 1)
                    .frame(width: size * fraction, height: size * fraction)
            }

            // Center gold label
            Circle()
                .fill(
                    RadialGradient(
                        colors: [
                            AuralisTheme.gold.opacity(0.85),
                            AuralisTheme.gold.opacity(0.65),
                            Color(red: 0.5, green: 0.35, blue: 0.1)
                        ],
                        center: .center,
                        startRadius: 10,
                        endRadius: size * 0.18
                    )
                )
                .frame(width: size * 0.36, height: size * 0.36)
                .overlay(
                    Circle()
                        .stroke(Color.black.opacity(0.4), lineWidth: 2)
                )

            // Center spindle hole
            Circle()
                .fill(AuralisTheme.pitchBlack)
                .frame(width: size * 0.06, height: size * 0.06)
                .overlay(
                    Circle()
                        .stroke(Color.white.opacity(0.3), lineWidth: 1)
                )

            // Stylized gold typography
            VStack(spacing: 2) {
                Text(title)
                    .font(.system(size: size * 0.026, weight: .bold, design: .serif))
                    .foregroundColor(.black)
                    .lineLimit(1)
                
                Text(artist)
                    .font(.system(size: size * 0.02, weight: .medium, design: .serif))
                    .foregroundColor(Color.black.opacity(0.8))
                    .lineLimit(1)
            }
            .offset(y: -size * 0.09)
        }
        .frame(width: size, height: size)
    }
}
