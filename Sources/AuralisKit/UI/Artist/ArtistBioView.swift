import SwiftUI
import AppKit

public final class ArtistBioViewState: ObservableObject {
    @Published public var profile: ArtistProfile? = nil
    @Published public var isLoading: Bool = true
    public init() {}
}

public struct ArtistBioView: View {
    public let artistName: String
    @ObservedObject var state = AppState.shared
    @ObservedObject var audioController = AudioEngineController.shared
    @StateObject private var ui = ArtistBioViewState()

    public init(artistName: String) {
        self.artistName = artistName
    }

    private var artistTracks: [Track] {
        state.tracks.filter {
            $0.artist.caseInsensitiveCompare(artistName) == .orderedSame ||
            $0.artist.localizedCaseInsensitiveContains(artistName)
        }
    }

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                // Top-Left Back Button (Always available on Artist page)
                HStack {
                    Button(action: {
                        if state.canGoBack {
                            state.goBack()
                        } else {
                            state.selectedSidebarItem = .artists
                            state.selectedArtistTarget = nil
                        }
                    }) {
                        HStack(spacing: 6) {
                            Image(systemName: "chevron.left")
                            Text("Back")
                        }
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(state.activeAccentColor)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(state.themeCardSurface)
                        .cornerRadius(6)
                        .overlay(
                            RoundedRectangle(cornerRadius: 6)
                                .stroke(state.themeCardBorder, lineWidth: 1)
                        )
                    }
                    .buttonStyle(PlainButtonStyle())

                    Spacer()
                }
                .padding(.horizontal, 32)
                .padding(.top, 16)

                // Hero Banner with Backdrop Cover & 1:1 Square Portrait Photo
                ZStack(alignment: .bottomLeading) {
                    // Wide Backdrop Cover Photo
                    if let profile = ui.profile, let coverURL = profile.coverImageURL {
                        AsyncImage(url: coverURL) { phase in
                            switch phase {
                            case .success(let image):
                                image
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .frame(height: 300)
                                    .clipped()
                            default:
                                Color(red: 0.08, green: 0.08, blue: 0.1)
                                    .frame(height: 300)
                            }
                        }
                    } else {
                        LinearGradient(
                            colors: [
                                state.themeCardSurface,
                                state.themeDeepCanvas,
                                state.themeCanvas
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                        .frame(height: 300)
                    }

                    // Backdrop fade gradient
                    LinearGradient(
                        colors: [Color.clear, state.themeCanvas],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(height: 200)

                    // Hero Content with 1:1 Square Photo + Info
                    HStack(alignment: .bottom, spacing: 20) {
                        // 1:1 Square Photo
                        Group {
                            if let profile = ui.profile, let squareURL = profile.squareImageURL {
                                AsyncImage(url: squareURL) { phase in
                                    switch phase {
                                    case .success(let image):
                                        image
                                            .resizable()
                                            .aspectRatio(contentMode: .fill)
                                            .frame(width: 140, height: 140)
                                            .clipped()
                                    default:
                                        ZStack {
                                            state.themeCardSurface
                                            Image(systemName: "person.fill")
                                                .font(.system(size: 48))
                                                .foregroundColor(state.themeTextMuted)
                                        }
                                        .frame(width: 140, height: 140)
                                    }
                                }
                            } else {
                                ZStack {
                                    state.themeCardSurface
                                    Image(systemName: "person.fill")
                                        .font(.system(size: 48))
                                        .foregroundColor(state.themeTextMuted)
                                }
                                .frame(width: 140, height: 140)
                            }
                        }
                        .cornerRadius(16)
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(Color.white.opacity(0.2), lineWidth: 1.5)
                        )
                        .shadow(color: Color.black.opacity(0.8), radius: 16, x: 0, y: 8)

                        // Artist Name & Tags
                        VStack(alignment: .leading, spacing: 8) {
                            HStack(spacing: 8) {
                                Text("ARTIST")
                                    .font(.system(size: 10, weight: .bold, design: .monospaced))
                                    .foregroundColor(state.activeAccentColor)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(state.activeAccentColor.opacity(0.15))
                                    .cornerRadius(4)

                                if let origin = ui.profile?.origin {
                                    Text(origin)
                                        .font(.system(size: 11, weight: .medium))
                                        .foregroundColor(AuralisTheme.textSecondary)
                                }
                            }

                            Text(artistName)
                                .font(Font.hexHeading(size: 36, weight: .bold))
                                .foregroundColor(state.themeTextPrimary)
                                .shadow(color: Color.black, radius: 8)

                            if let genres = ui.profile?.genres, !genres.isEmpty {
                                HStack(spacing: 6) {
                                    ForEach(genres, id: \.self) { genre in
                                        Text(genre)
                                            .font(Font.hexBody(size: 11, weight: .medium))
                                            .foregroundColor(AuralisTheme.textSecondary)
                                            .padding(.horizontal, 8)
                                            .padding(.vertical, 3)
                                            .background(Color.white.opacity(0.08))
                                            .cornerRadius(4)
                                    }
                                }
                            }
                        }

                        Spacer()
                    }
                    .padding(32)
                }
                .frame(height: 300)

                // SECTION: STATS (Spotify Monthly Listeners & Instagram Followers)
                VStack(alignment: .leading, spacing: 12) {
                    Text("STATS")
                        .font(Font.hexHeading(size: 13, weight: .bold))
                        .foregroundColor(state.activeAccentColor)

                    HStack(spacing: 16) {
                        // Spotify Monthly Listeners Card
                        HStack(spacing: 14) {
                            ZStack {
                                Circle()
                                    .fill(Color(red: 0.11, green: 0.72, blue: 0.33).opacity(0.18))
                                    .frame(width: 44, height: 44)
                                Image(systemName: "waveform")
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundColor(Color(red: 0.11, green: 0.72, blue: 0.33))
                            }

                            VStack(alignment: .leading, spacing: 3) {
                                if ui.isLoading {
                                    ProgressView().controlSize(.small)
                                } else {
                                    Text(ui.profile?.spotifyMonthlyListeners ?? "N/A")
                                        .font(Font.hexHeading(size: 14, weight: .bold))
                                        .foregroundColor(state.themeTextPrimary)
                                }
                                Text("Spotify Monthly Listeners")
                                    .font(Font.hexBody(size: 11))
                                    .foregroundColor(AuralisTheme.textMuted)
                            }
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(state.themeCardSurface)
                        .cornerRadius(10)
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(state.themeCardBorder, lineWidth: 1)
                        )

                        // Instagram Followers Card
                        HStack(spacing: 14) {
                            ZStack {
                                Circle()
                                    .fill(Color(red: 0.88, green: 0.25, blue: 0.55).opacity(0.18))
                                    .frame(width: 44, height: 44)
                                Image(systemName: "camera.fill")
                                    .font(.system(size: 18))
                                    .foregroundColor(Color(red: 0.88, green: 0.25, blue: 0.55))
                            }

                            VStack(alignment: .leading, spacing: 3) {
                                if ui.isLoading {
                                    ProgressView().controlSize(.small)
                                } else {
                                    Text(ui.profile?.instagramFollowers ?? "N/A")
                                        .font(Font.hexHeading(size: 14, weight: .bold))
                                        .foregroundColor(state.themeTextPrimary)
                                }
                                Text("Instagram Followers")
                                    .font(Font.hexBody(size: 11))
                                    .foregroundColor(AuralisTheme.textMuted)
                            }
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(state.themeCardSurface)
                        .cornerRadius(10)
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(state.themeCardBorder, lineWidth: 1)
                        )
                    }
                }
                .padding(.horizontal, 32)

                // SECTION: BIOGRAPHY (strictly titled "Biography")
                VStack(alignment: .leading, spacing: 10) {
                    Text("Biography")
                        .font(Font.hexHeading(size: 16, weight: .bold))
                        .foregroundColor(state.themeTextPrimary)

                    if ui.isLoading {
                        HStack(spacing: 8) {
                            ProgressView()
                                .controlSize(.small)
                            Text("Retrieving artist biography...")
                                .font(Font.hexBody(size: 13))
                                .foregroundColor(AuralisTheme.textMuted)
                        }
                        .padding(.vertical, 12)
                    } else if let profile = ui.profile {
                        Text(profile.biography)
                            .font(Font.hexBody(size: 14, weight: .regular))
                            .foregroundColor(state.themeTextPrimary)
                            .lineSpacing(6)
                    }
                }
                .padding(.horizontal, 32)

                // SECTION: TRACKS IN YOUR LIBRARY
                VStack(alignment: .leading, spacing: 14) {
                    HStack {
                        VStack(alignment: .leading, spacing: 3) {
                            Text("SONGS IN YOUR LIBRARY")
                                .font(Font.hexHeading(size: 12, weight: .bold))
                                .foregroundColor(state.activeAccentColor)

                            Text("\(artistTracks.count) tracks found")
                                .font(Font.hexBody(size: 11))
                                .foregroundColor(AuralisTheme.textMuted)
                        }

                        Spacer()

                        if !artistTracks.isEmpty {
                            Button(action: {
                                if let first = artistTracks.first {
                                    state.play(track: first)
                                    state.clearQueue()
                                    for t in artistTracks.dropFirst() {
                                        state.addToQueue(track: t)
                                    }
                                }
                            }) {
                                HStack(spacing: 6) {
                                    Image(systemName: "play.fill")
                                    Text("Play All")
                                }
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 6)
                                .background(state.activeAccentColor)
                                .cornerRadius(6)
                            }
                            .buttonStyle(PlainButtonStyle())
                        }
                    }

                    if artistTracks.isEmpty {
                        HStack(spacing: 8) {
                            Image(systemName: "music.note")
                                .foregroundColor(AuralisTheme.textMuted)
                            Text("No tracks by \(artistName) in your library yet.")
                                .font(Font.hexBody(size: 12))
                                .foregroundColor(AuralisTheme.textMuted)
                        }
                        .padding(.vertical, 12)
                    } else {
                        VStack(spacing: 4) {
                            ForEach(Array(artistTracks.enumerated()), id: \.element.id) { index, track in
                                artistTrackRow(track: track, index: index + 1)
                            }
                        }
                    }
                }
                .padding(.horizontal, 32)

                Spacer().frame(height: 40)
            }
        }
        .background(state.themeCanvas)
        .task(id: artistName) {
            ui.isLoading = true
            ui.profile = await ArtistBioService.shared.fetchProfile(for: artistName)
            ui.isLoading = false
        }
    }

    private func artistTrackRow(track: Track, index: Int) -> some View {
        let isCurrent = audioController.currentTrack?.id == track.id
        return Button(action: {
            state.play(track: track)
        }) {
            HStack(spacing: 12) {
                // Index / Playing wave indicator
                Group {
                    if isCurrent && audioController.isPlaying {
                        Image(systemName: "waveform")
                            .font(.system(size: 12))
                            .foregroundColor(state.activeAccentColor)
                    } else {
                        Text("\(index)")
                            .font(.system(size: 11, design: .monospaced))
                            .foregroundColor(AuralisTheme.textMuted)
                    }
                }
                .frame(width: 24, alignment: .center)

                // Track title & album
                VStack(alignment: .leading, spacing: 2) {
                    Text(track.title)
                        .font(Font.hexBody(size: 13, weight: isCurrent ? .bold : .medium))
                        .foregroundColor(isCurrent ? state.activeAccentColor : state.themeTextPrimary)
                        .lineLimit(1)

                    Text(track.album)
                        .font(Font.hexBody(size: 11))
                        .foregroundColor(AuralisTheme.textMuted)
                        .lineLimit(1)
                }

                Spacer()

                // Audiophile Badge
                Text(track.codec.rawValue)
                    .font(.system(size: 9, weight: .bold, design: .monospaced))
                    .foregroundColor(AuralisTheme.cobaltBlue)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(AuralisTheme.cobaltBlue.opacity(0.12))
                    .cornerRadius(4)

                // Duration
                Text(formatDuration(track.duration))
                    .font(.system(size: 11, design: .monospaced))
                    .foregroundColor(AuralisTheme.textMuted)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(isCurrent ? state.themeCardSurfaceHover : state.themeCardSurface)
            .cornerRadius(8)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(isCurrent ? state.activeAccentColor.opacity(0.3) : state.themeCardBorder, lineWidth: 1)
            )
        }
        .buttonStyle(PlainButtonStyle())
    }

    private func formatDuration(_ duration: TimeInterval) -> String {
        let minutes = Int(duration) / 60
        let seconds = Int(duration) % 60
        return String(format: "%d:%02d", minutes, seconds)
    }
}
