import SwiftUI
import AppKit
import UniformTypeIdentifiers

public enum LibrarySection: String, CaseIterable, Identifiable {
    case tracks = "Tracks"
    case albums = "Albums"
    case artists = "Artists"
    case genres = "Genres"
    case recentlyAdded = "Recently Added"

    public var id: String { rawValue }

    public var iconName: String {
        switch self {
        case .tracks: return "music.note"
        case .albums: return "square.stack.fill"
        case .artists: return "person.2.fill"
        case .genres: return "guitars.fill"
        case .recentlyAdded: return "clock.fill"
        }
    }
}

public final class LibraryViewState: ObservableObject {
    @Published public var hoveredTrackId: UUID? = nil
    @Published public var selectedSection: LibrarySection = .tracks
    @Published public var selectedAlbum: AlbumGroup? = nil
    @Published public var selectedArtist: ArtistGroup? = nil
    public init() {}
}

public struct LibraryView: View {
    @ObservedObject var state = AppState.shared
    @ObservedObject var audioController = AudioEngineController.shared
    @StateObject private var ui = LibraryViewState()

    private let formatFilters = ["All", "Hi\u{2011}Res", "FLAC", "DSD", "WAV", "ALAC"]

    public init(initialSection: LibrarySection = .tracks) {
        _ui = StateObject(wrappedValue: {
            let s = LibraryViewState()
            s.selectedSection = initialSection
            return s
        }())
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Top Toolbar: Section Tabs, Search & Action Buttons
            VStack(spacing: 10) {
                HStack(spacing: 12) {
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

                    // Structure Section Switcher: [Tracks | Albums | Artists | Genres | Recently Added]
                    HStack(spacing: 4) {
                        ForEach(LibrarySection.allCases) { section in
                            let isSelected = (ui.selectedSection == section)
                            Button(action: {
                                withAnimation(.easeInOut(duration: 0.15)) {
                                    ui.selectedSection = section
                                    ui.selectedAlbum = nil
                                    ui.selectedArtist = nil
                                    state.selectedArtistTarget = nil
                                }
                            }) {
                                HStack(spacing: 6) {
                                    Image(systemName: section.iconName)
                                        .font(.system(size: 11))
                                    Text(section.rawValue)
                                        .font(.system(size: 12, weight: isSelected ? .bold : .medium))
                                }
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(isSelected ? state.activeAccentColor : state.themeCardSurface)
                                .foregroundColor(isSelected ? .black : AuralisTheme.textSecondary)
                                .cornerRadius(6)
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(PlainButtonStyle())
                            .contentShape(Rectangle())
                        }
                    }

                    Spacer()

                    // Add Music Folder Button
                    Button(action: { openFolderPicker() }) {
                        HStack(spacing: 6) {
                            Image(systemName: "folder.badge.plus")
                            Text("Add Folder...")
                        }
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(state.activeAccentColor)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(state.themeCardSurface)
                        .cornerRadius(6)
                        .overlay(
                            RoundedRectangle(cornerRadius: 6)
                                .stroke(state.themeCardBorder, lineWidth: 1)
                        )
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(PlainButtonStyle())
                    .contentShape(Rectangle())

                    // Add Audio Files Button
                    Button(action: { openFilePicker() }) {
                        HStack(spacing: 6) {
                            Image(systemName: "doc.badge.plus")
                            Text("Add Files...")
                        }
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(AuralisTheme.textSecondary)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .pitchBlackCard()
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(PlainButtonStyle())
                    .contentShape(Rectangle())
                }

                // Search & Format Filters Bar
                HStack(spacing: 12) {
                    // Search Bar
                    HStack(spacing: 8) {
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 12))
                            .foregroundColor(AuralisTheme.textMuted)

                        TextField("Search tracks, artists, albums...", text: $state.searchQuery)
                            .textFieldStyle(PlainTextFieldStyle())
                            .font(.system(size: 12))
                            .foregroundColor(AuralisTheme.textPrimary)

                        if !state.searchQuery.isEmpty {
                            Button(action: { state.searchQuery = "" }) {
                                Image(systemName: "xmark.circle.fill")
                                    .font(.system(size: 11))
                                    .foregroundColor(AuralisTheme.textMuted)
                            }
                            .buttonStyle(PlainButtonStyle())
                        }
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(AuralisTheme.cardSurface)
                    .cornerRadius(6)
                    .overlay(
                        RoundedRectangle(cornerRadius: 6)
                            .stroke(AuralisTheme.cardBorder, lineWidth: 1)
                    )
                    .frame(maxWidth: 320)

                    // Format Chips
                    HStack(spacing: 4) {
                        ForEach(formatFilters, id: \.self) { filter in
                            let isSelected = (state.selectedFormatFilter == filter)
                            Button(action: {
                                withAnimation(.easeInOut(duration: 0.15)) {
                                    state.selectedFormatFilter = filter
                                }
                            }) {
                                Text(filter)
                                    .font(.system(size: 10, weight: .bold, design: .monospaced))
                                    .lineLimit(1)
                                    .fixedSize(horizontal: true, vertical: false)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(isSelected ? state.activeAccentColor : state.themeCardSurface)
                                    .foregroundColor(isSelected ? .black : AuralisTheme.textSecondary)
                                    .cornerRadius(4)
                                    .contentShape(Rectangle())
                            }
                            .buttonStyle(PlainButtonStyle())
                            .contentShape(Rectangle())
                        }
                    }

                    Spacer()

                    // Syncing spinner or total count
                    if state.isSyncing {
                        HStack(spacing: 6) {
                            ProgressView().controlSize(.small)
                            Text(state.syncProgressMessage)
                                .font(.system(size: 10, design: .monospaced))
                                .foregroundColor(state.activeAccentColor)
                        }
                    } else {
                        Text("\(state.filteredTracks.count) tracks")
                            .font(.system(size: 11, design: .monospaced))
                            .foregroundColor(AuralisTheme.textMuted)
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(state.themeDeepCanvas)
            .overlay(
                Rectangle().frame(height: 1).foregroundColor(state.themeCardBorder),
                alignment: .bottom
            )

            // Main Library View Content
            if state.tracks.isEmpty {
                emptyLibraryView
            } else if let artistTarget = state.selectedArtistTarget {
                ArtistBioView(artistName: artistTarget)
            } else if let album = ui.selectedAlbum {
                albumDetailView(album: album)
            } else if let artist = ui.selectedArtist {
                artistDetailView(artist: artist)
            } else {
                switch ui.selectedSection {
                case .tracks:
                    tracksTableView(tracks: state.filteredTracks)
                case .albums:
                    albumsGridView
                case .artists:
                    artistsGridView
                case .genres:
                    genresGridView
                case .recentlyAdded:
                    tracksTableView(tracks: state.recentlyAddedTracks)
                }
            }
        }
        .background(state.themeCanvas)
        .onDrop(of: [.fileURL], isTargeted: nil) { providers in
            Task {
                var urls: [URL] = []
                for provider in providers {
                    if let item = try? await provider.loadItem(forTypeIdentifier: UTType.fileURL.identifier),
                       let data = item as? Data,
                       let url = URL(dataRepresentation: data, relativeTo: nil) {
                        urls.append(url)
                    }
                }
                if !urls.isEmpty {
                    await state.importURLs(urls)
                }
            }
            return true
        }
        .onAppear {
            if let target = state.selectedAlbumTarget {
                ui.selectedSection = .albums
                ui.selectedAlbum = target
                state.selectedAlbumTarget = nil
            }
        }
        .onChange(of: state.selectedAlbumTarget) { target in
            if let target = target {
                ui.selectedSection = .albums
                ui.selectedAlbum = target
                state.selectedAlbumTarget = nil
            }
        }
    }

    // MARK: - Empty State View

    private var emptyLibraryView: some View {
        VStack(spacing: 16) {
            Spacer()

            Image(systemName: "music.note.house.fill")
                .font(.system(size: 56))
                .foregroundColor(AuralisTheme.gold.opacity(0.8))

            Text("Your Library is Empty")
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(AuralisTheme.textPrimary)

            Text("Select your music folder or drop high-resolution audio files (FLAC, WAV, DSD, ALAC) to begin listening in bit-perfect fidelity.")
                .font(.system(size: 13))
                .foregroundColor(AuralisTheme.textMuted)
                .multilineTextAlignment(.center)
                .frame(maxWidth: 420)

            HStack(spacing: 12) {
                Button(action: { openFolderPicker() }) {
                    HStack(spacing: 8) {
                        Image(systemName: "folder.fill")
                        Text("Add Music Folder...")
                    }
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(.black)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .background(AuralisTheme.gold)
                    .cornerRadius(8)
                }
                .buttonStyle(PlainButtonStyle())

                Button(action: { openFilePicker() }) {
                    HStack(spacing: 8) {
                        Image(systemName: "doc.fill")
                        Text("Add Audio Files...")
                    }
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(AuralisTheme.textPrimary)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .pitchBlackCard()
                }
                .buttonStyle(PlainButtonStyle())
            }
            .padding(.top, 8)

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Tracks Table View (Responsive Table Layout)

    private func tracksTableView(tracks: [Track]) -> some View {
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
            }
            .font(.system(size: 10, weight: .bold, design: .monospaced))
            .foregroundColor(AuralisTheme.textMuted)
            .padding(.horizontal, 20)
            .padding(.vertical, 8)
            .background(AuralisTheme.cardSurface)

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
                                        .foregroundColor(AuralisTheme.gold)
                                } else {
                                    Text("\(index + 1)")
                                        .font(.system(size: 11, design: .monospaced))
                                        .foregroundColor(isCurrent ? AuralisTheme.gold : AuralisTheme.textMuted)
                                }
                            }
                            .frame(width: 32, alignment: .center)

                            // Title
                            HStack(spacing: 8) {
                                Text(track.title)
                                    .font(.system(size: 13, weight: isCurrent ? .bold : .medium))
                                    .foregroundColor(isCurrent ? AuralisTheme.gold : AuralisTheme.textPrimary)
                                    .lineLimit(1)

                                if track.isHiRes {
                                    Text("Hi\u{2011}Res")
                                        .font(.system(size: 8, weight: .black, design: .monospaced))
                                        .foregroundColor(AuralisTheme.gold)
                                        .lineLimit(1)
                                        .fixedSize(horizontal: true, vertical: false)
                                        .padding(.horizontal, 4)
                                        .padding(.vertical, 1)
                                        .background(AuralisTheme.gold.opacity(0.12))
                                        .cornerRadius(3)
                                        .layoutPriority(1)
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)

                            // Artist
                            Text(track.artist)
                                .font(.system(size: 12))
                                .foregroundColor(AuralisTheme.textSecondary)
                                .lineLimit(1)
                                .frame(maxWidth: .infinity, alignment: .leading)

                            // Album
                            Text(track.album)
                                .font(.system(size: 12))
                                .foregroundColor(AuralisTheme.textMuted)
                                .lineLimit(1)
                                .frame(maxWidth: .infinity, alignment: .leading)

                            // Format
                            Text(track.codec.rawValue)
                                .font(.system(size: 10, weight: .bold, design: .monospaced))
                                .foregroundColor(codecBadgeColor(track.codec))
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(codecBadgeColor(track.codec).opacity(0.12))
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
                        }
                        .padding(.horizontal, 20)
                        .padding(.vertical, 8)
                        .background(isCurrent ? AuralisTheme.cardSurfaceHover : (isHovered ? AuralisTheme.cardSurface : Color.white.opacity(0.0001)))
                        .cornerRadius(6)
                        .contentShape(Rectangle())
                        .onHover { hovering in
                            ui.hoveredTrackId = hovering ? track.id : nil
                        }
                        .onTapGesture(count: 2) {
                            state.play(track: track)
                            state.selectedTab = .nowPlaying
                        }
                    }
                }
                .padding(.vertical, 4)
            }
        }
    }

    // MARK: - Albums Grid View

    private var albumsGridView: some View {
        ScrollView {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 180, maximum: 240), spacing: 20)], spacing: 20) {
                ForEach(state.albums) { album in
                    Button(action: {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            ui.selectedAlbum = album
                        }
                    }) {
                        VStack(alignment: .leading, spacing: 8) {
                            // Cover art
                            ZStack {
                                if let img = album.artwork {
                                    Image(nsImage: img)
                                        .resizable()
                                        .aspectRatio(contentMode: .fill)
                                } else {
                                    ProceduralVinylArt(title: album.title, artist: album.artist, size: 180)
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .aspectRatio(1.0, contentMode: .fit)
                            .cornerRadius(10)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(AuralisTheme.cardBorder, lineWidth: 1)
                            )
                            .overlay(
                                Group {
                                    if album.artwork == nil {
                                        Button(action: {
                                            if let first = album.tracks.first {
                                                Task {
                                                    await ArtworkFetchService.shared.fetchAndSaveArtwork(for: first)
                                                }
                                            }
                                        }) {
                                            HStack(spacing: 4) {
                                                Image(systemName: "arrow.down.circle.fill")
                                                Text("Fetch artwork")
                                            }
                                            .font(.system(size: 9, weight: .bold))
                                            .foregroundColor(.white)
                                            .padding(.horizontal, 8)
                                            .padding(.vertical, 4)
                                            .background(Color.black.opacity(0.85))
                                            .cornerRadius(6)
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 6)
                                                    .stroke(AuralisTheme.cobaltBlue, lineWidth: 1)
                                            )
                                        }
                                        .buttonStyle(PlainButtonStyle())
                                        .padding(8)
                                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
                                    }
                                }
                            )

                            // Metadata
                            VStack(alignment: .leading, spacing: 2) {
                                Text(album.title)
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(AuralisTheme.textPrimary)
                                    .lineLimit(1)

                                Text(album.artist)
                                    .font(.system(size: 12))
                                    .foregroundColor(AuralisTheme.textSecondary)
                                    .lineLimit(1)

                                HStack {
                                    Text("\(album.tracks.count) tracks")
                                        .font(.system(size: 11, design: .monospaced))
                                        .foregroundColor(AuralisTheme.textMuted)

                                    Spacer()

                                    if album.isHiRes {
                                        Text("Hi\u{2011}Res")
                                            .font(.system(size: 8, weight: .black, design: .monospaced))
                                            .foregroundColor(AuralisTheme.gold)
                                            .lineLimit(1)
                                            .fixedSize(horizontal: true, vertical: false)
                                    }
                                }
                            }
                        }
                        .padding(10)
                        .background(AuralisTheme.cardSurface)
                        .cornerRadius(12)
                    }
                    .buttonStyle(PlainButtonStyle())
                }
            }
            .padding(24)
        }
    }

    // MARK: - Artists Grid View

    private var artistsGridView: some View {
        ScrollView {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 180, maximum: 220), spacing: 20)], spacing: 20) {
                ForEach(state.artists) { artist in
                    Button(action: {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            ui.selectedArtist = artist
                        }
                    }) {
                        VStack(spacing: 10) {
                            // Circular Artist Portrait
                            ZStack {
                                if let img = artist.sampleArtwork {
                                    Image(nsImage: img)
                                        .resizable()
                                        .aspectRatio(contentMode: .fill)
                                } else {
                                    Circle()
                                        .fill(AuralisTheme.cardSurfaceHover)
                                        .overlay(
                                            Image(systemName: "person.crop.circle.fill")
                                                .font(.system(size: 48))
                                                .foregroundColor(AuralisTheme.gold)
                                        )
                                }
                            }
                            .frame(width: 120, height: 120)
                            .clipShape(Circle())
                            .overlay(Circle().stroke(AuralisTheme.cardBorder, lineWidth: 1))

                            VStack(spacing: 2) {
                                Text(artist.name)
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(AuralisTheme.textPrimary)
                                    .lineLimit(1)

                                Text("\(artist.albumCount) albums • \(artist.trackCount) songs")
                                    .font(.system(size: 11, design: .monospaced))
                                    .foregroundColor(AuralisTheme.textMuted)
                            }
                        }
                        .padding(16)
                        .frame(maxWidth: .infinity)
                        .background(AuralisTheme.cardSurface)
                        .cornerRadius(12)
                    }
                    .buttonStyle(PlainButtonStyle())
                }
            }
            .padding(24)
        }
    }

    // MARK: - Genres Grid View

    private var genresGridView: some View {
        ScrollView {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 180, maximum: 240), spacing: 16)], spacing: 16) {
                ForEach(state.genres) { genre in
                    Button(action: {
                        state.searchQuery = genre.name
                        ui.selectedSection = .tracks
                    }) {
                        VStack(alignment: .leading, spacing: 10) {
                            HStack {
                                Image(systemName: "music.quarternote.3")
                                    .font(.system(size: 18))
                                    .foregroundColor(AuralisTheme.gold)
                                Spacer()
                                Text("\(genre.trackCount)")
                                    .font(.system(size: 12, weight: .bold, design: .monospaced))
                                    .foregroundColor(AuralisTheme.textSecondary)
                            }

                            Text(genre.name)
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(AuralisTheme.textPrimary)
                                .lineLimit(1)
                        }
                        .padding(18)
                        .background(AuralisTheme.cardSurface)
                        .cornerRadius(10)
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(AuralisTheme.cardBorder, lineWidth: 1)
                        )
                    }
                    .buttonStyle(PlainButtonStyle())
                }
            }
            .padding(24)
        }
    }

    // MARK: - Album Detail View

    private func albumDetailView(album: AlbumGroup) -> some View {
        VStack(spacing: 0) {
            // Album Header
            HStack(spacing: 20) {
                Button(action: { ui.selectedAlbum = nil }) {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                        Text("Albums")
                    }
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(AuralisTheme.gold)
                }
                .buttonStyle(PlainButtonStyle())

                Spacer()

                Button(action: {
                    if let first = album.tracks.first {
                        state.play(track: first)
                        state.selectedTab = .nowPlaying
                    }
                }) {
                    HStack(spacing: 6) {
                        Image(systemName: "play.fill")
                        Text("Play Album")
                    }
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(.black)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 6)
                    .background(AuralisTheme.gold)
                    .cornerRadius(6)
                }
                .buttonStyle(PlainButtonStyle())
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 12)
            .background(AuralisTheme.deepCanvas)

            // Album Header Strip
            HStack(spacing: 20) {
                if let img = album.artwork {
                    Image(nsImage: img)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: 100, height: 100)
                        .cornerRadius(8)
                } else {
                    VStack(spacing: 6) {
                        ProceduralVinylArt(title: album.title, artist: album.artist, size: 90)
                            .frame(width: 90, height: 90)
                            .cornerRadius(8)

                        Button(action: {
                            if let first = album.tracks.first {
                                Task {
                                    await ArtworkFetchService.shared.fetchAndSaveArtwork(for: first)
                                }
                            }
                        }) {
                            HStack(spacing: 4) {
                                Image(systemName: "arrow.down.circle.fill")
                                Text("Fetch artwork")
                            }
                            .font(.system(size: 9, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 3)
                            .background(AuralisTheme.cobaltBlue)
                            .cornerRadius(4)
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text(album.title)
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(AuralisTheme.textPrimary)

                    Text(album.artist)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(AuralisTheme.gold)

                    HStack(spacing: 8) {
                        if let year = album.year {
                            Text("\(year)")
                                .font(.system(size: 11, design: .monospaced))
                                .foregroundColor(AuralisTheme.textMuted)
                        }
                        Text("• \(album.tracks.count) tracks")
                            .font(.system(size: 11, design: .monospaced))
                            .foregroundColor(AuralisTheme.textMuted)

                        if album.isHiRes {
                            Text("• LOSSLESS HI\u{2011}RES")
                                .font(.system(size: 9, weight: .bold, design: .monospaced))
                                .foregroundColor(AuralisTheme.gold)
                                .lineLimit(1)
                                .fixedSize(horizontal: true, vertical: false)
                        }
                    }
                }
                Spacer()
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 16)

            tracksTableView(tracks: album.tracks)
        }
    }

    // MARK: - Artist Detail View (Artist Bio with Master Narrative and Library Songs)

    private func artistDetailView(artist: ArtistGroup) -> some View {
        VStack(spacing: 0) {
            HStack {
                Button(action: {
                    ui.selectedArtist = nil
                    state.selectedArtistTarget = nil
                }) {
                    HStack(spacing: 6) {
                        Image(systemName: "chevron.left")
                        Text("All Artists")
                    }
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(state.activeAccentColor)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
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
            .padding(.horizontal, 24)
            .padding(.vertical, 10)
            .background(state.themeDeepCanvas)

            ArtistBioView(artistName: artist.name)
        }
    }

    // MARK: - Helpers & Pickers

    private func openFolderPicker() {
        let panel = NSOpenPanel()
        panel.canChooseFiles = false
        panel.canChooseDirectories = true
        panel.allowsMultipleSelection = true
        panel.prompt = "Add Library Folder"
        panel.message = "Select a music folder to automatically index its songs and subfolders."

        if panel.runModal() == .OK {
            for url in panel.urls {
                state.addLibraryFolder(url: url)
            }
        }
    }

    private func openFilePicker() {
        let panel = NSOpenPanel()
        panel.canChooseFiles = true
        panel.canChooseDirectories = false
        panel.allowsMultipleSelection = true
        panel.allowedContentTypes = [
            UTType.audio,
            UTType(filenameExtension: "flac") ?? .audio,
            UTType(filenameExtension: "wav") ?? .audio,
            UTType(filenameExtension: "dsf") ?? .audio,
            UTType(filenameExtension: "dff") ?? .audio,
            UTType(filenameExtension: "alac") ?? .audio,
            UTType(filenameExtension: "aiff") ?? .audio,
            UTType(filenameExtension: "mp3") ?? .audio,
            UTType(filenameExtension: "ogg") ?? .audio,
            UTType(filenameExtension: "opus") ?? .audio
        ]

        if panel.runModal() == .OK {
            Task {
                await state.importURLs(panel.urls)
            }
        }
    }

    private func codecBadgeColor(_ codec: AudioCodec) -> Color {
        switch codec {
        case .dsd: return AuralisTheme.emeraldDSD
        case .flac, .alac: return AuralisTheme.gold
        case .wav, .aiff: return AuralisTheme.cyanHiRes
        default: return AuralisTheme.textMuted
        }
    }

    private func formatDuration(_ duration: TimeInterval) -> String {
        let secs = Int(duration)
        let m = secs / 60
        let s = secs % 60
        return String(format: "%d:%02d", m, s)
    }
}
