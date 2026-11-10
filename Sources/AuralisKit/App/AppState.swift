import Foundation
import SwiftUI
import AppKit
import AVFoundation

public enum NavigationTab: String, CaseIterable, Identifiable, Sendable {
    case nowPlaying = "Now Playing"
    case library = "Library"
    case equalizer = "Equalizer"
    case settings = "Settings"

    public var id: String { rawValue }

    public var iconName: String {
        switch self {
        case .nowPlaying: return "play.circle.fill"
        case .library: return "music.note.list"
        case .equalizer: return "slider.vertical.3"
        case .settings: return "gearshape.fill"
        }
    }
}

public struct Playlist: Identifiable, Codable, Hashable, Sendable {
    public let id: UUID
    public var name: String
    public var trackIds: [UUID]
    public let createdAt: Date

    public init(id: UUID = UUID(), name: String, trackIds: [UUID] = [], createdAt: Date = Date()) {
        self.id = id
        self.name = name
        self.trackIds = trackIds
        self.createdAt = createdAt
    }
}

public enum SidebarItem: Hashable, Identifiable, Sendable {
    case nowPlaying
    case recent
    case favorites
    case playlist(id: UUID)
    case albums
    case artists
    case genres
    case allTracks
    case equalizer
    case settings

    public var id: String {
        switch self {
        case .nowPlaying: return "nowPlaying"
        case .recent: return "recent"
        case .favorites: return "favorites"
        case .playlist(let id): return "playlist_\(id.uuidString)"
        case .albums: return "albums"
        case .artists: return "artists"
        case .genres: return "genres"
        case .allTracks: return "allTracks"
        case .equalizer: return "equalizer"
        case .settings: return "settings"
        }
    }

    public var title: String {
        switch self {
        case .nowPlaying: return "Now Playing"
        case .recent: return "Recent"
        case .favorites: return "Favorites"
        case .playlist: return "Playlist"
        case .albums: return "Albums"
        case .artists: return "Artists"
        case .genres: return "Genres"
        case .allTracks: return "All Library"
        case .equalizer: return "Equalizer"
        case .settings: return "Settings"
        }
    }

    public var iconName: String {
        switch self {
        case .nowPlaying: return "play.circle.fill"
        case .recent: return "clock.fill"
        case .favorites: return "heart.fill"
        case .playlist: return "music.note.list"
        case .albums: return "square.stack.fill"
        case .artists: return "person.2.fill"
        case .genres: return "guitars.fill"
        case .allTracks: return "music.note"
        case .equalizer: return "slider.vertical.3"
        case .settings: return "gearshape.fill"
        }
    }
}

public struct AlbumGroup: Identifiable, Hashable {
    public var id: String { "\(artist.lowercased())_\(title.lowercased())" }
    public let title: String
    public let artist: String
    public let year: Int?
    public let artwork: NSImage?
    public let tracks: [Track]
    public var isHiRes: Bool { tracks.contains(where: { $0.isHiRes }) }
}

public struct ArtistGroup: Identifiable, Hashable {
    public var id: String { name.lowercased() }
    public let name: String
    public let albumCount: Int
    public let trackCount: Int
    public let tracks: [Track]
    public let sampleArtwork: NSImage?
}

public struct GenreGroup: Identifiable, Hashable {
    public var id: String { name.lowercased() }
    public let name: String
    public let trackCount: Int
    public let tracks: [Track]
}

public enum RepeatMode: String, CaseIterable, Sendable {
    case off = "Off"
    case all = "All"
    case one = "One"

    public var iconName: String {
        switch self {
        case .off: return "repeat"
        case .all: return "repeat"
        case .one: return "repeat.1"
        }
    }
}

@MainActor
public final class AppState: ObservableObject {
    public static let shared = AppState()

    @Published public var selectedSidebarItem: SidebarItem = .nowPlaying
    @Published public var hoveredSidebarItem: SidebarItem? = nil

    // Navigation History Stack
    @Published public var navigationHistory: [SidebarItem] = []

    public var canGoBack: Bool {
        return !navigationHistory.isEmpty || selectedArtistTarget != nil
    }

    public func navigateTo(item: SidebarItem) {
        if selectedSidebarItem != item {
            navigationHistory.append(selectedSidebarItem)
            if navigationHistory.count > 50 {
                navigationHistory.removeFirst()
            }
            selectedSidebarItem = item
        }
    }

    public func goBack() {
        if selectedArtistTarget != nil {
            selectedArtistTarget = nil
            return
        }
        if let previous = navigationHistory.popLast() {
            selectedSidebarItem = previous
        }
    }

    public var selectedTab: NavigationTab {
        get {
            switch selectedSidebarItem {
            case .nowPlaying: return .nowPlaying
            case .equalizer: return .equalizer
            case .settings: return .settings
            default: return .library
            }
        }
        set {
            switch newValue {
            case .nowPlaying: selectedSidebarItem = .nowPlaying
            case .equalizer: selectedSidebarItem = .equalizer
            case .settings: selectedSidebarItem = .settings
            case .library: selectedSidebarItem = .allTracks
            }
        }
    }

    @Published public var hoveredSidebarTab: NavigationTab? = nil
    @Published public var tracks: [Track] = []
    @Published public var playlists: [Playlist] = []
    @Published public var selectedFormatFilter: String = "All"
    @Published public var searchQuery: String = ""
    @Published public var showEqualizerSheet: Bool = false
    @Published public var isFocusMode: Bool = false

    // Appearance & Skins
    @Published public var selectedSkin: AppSkin = .pitchBlack
    @Published public var selectedAccentColor: AppAccentColor = .gold

    // Dynamic Theme Accessors
    public var themeCanvas: Color { selectedSkin.canvasColor }
    public var themeDeepCanvas: Color { selectedSkin.deepCanvasColor }
    public var themeCardSurface: Color { selectedSkin.cardSurfaceColor }
    public var themeCardSurfaceHover: Color { selectedSkin.cardSurfaceHoverColor }
    public var themeCardBorder: Color { selectedSkin.cardBorderColor }
    public var themeTextPrimary: Color { selectedSkin.textPrimaryColor }
    public var themeTextSecondary: Color { selectedSkin.textSecondaryColor }
    public var themeTextMuted: Color { selectedSkin.textMutedColor }

    /// Dynamic active accent color:
    /// When on Now Playing, button & tab accents reflect the album artwork palette!
    /// Otherwise, reflects the user's selected accent color in Settings.
    public var activeAccentColor: Color {
        if selectedSidebarItem == .nowPlaying {
            return currentPalette.primary
        }
        return selectedAccentColor.color
    }

    // Playback Enhancements
    @Published public var repeatMode: RepeatMode = .off
    @Published public var isShuffleEnabled: Bool = false
    @Published public var queue: [Track] = []
    @Published public var history: [Track] = []
    @Published public var likedTrackIds: Set<UUID> = []
    @Published public var currentPalette: ArtworkColorPalette = .fallback
    @Published public var selectedAlbumTarget: AlbumGroup? = nil
    @Published public var selectedArtistTarget: String? = nil

    // Library Folder Auto-Sync State
    @Published public var watchedFolders: [URL] = []
    @Published public var isSyncing: Bool = false
    @Published public var syncProgressMessage: String = ""
    @Published public var autoSyncOnLaunch: Bool = true
    @Published public var scanSubfoldersRecursively: Bool = true

    public let audioController = AudioEngineController.shared
    public let eqEngine = ParametricEQEngine.shared

    private let userDefaultsWatchedKey = "AuralisWatchedFolders_v1"
    private let userDefaultsAutoSyncKey = "AuralisAutoSyncOnLaunch_v1"
    private let userDefaultsRecursiveKey = "AuralisRecursiveScan_v1"
    private let userDefaultsLikedKey = "AuralisLikedTracks_v1"
    private let userDefaultsPlaylistsKey = "AuralisPlaylists_v1"
    private let userDefaultsSkinKey = "AuralisSkin_v1"
    private let userDefaultsAccentKey = "AuralisAccent_v1"

    private var currentIndex: Int? {
        guard let current = audioController.currentTrack else { return nil }
        return tracks.firstIndex(where: { $0.id == current.id })
    }

    public init() {
        self.autoSyncOnLaunch = UserDefaults.standard.object(forKey: userDefaultsAutoSyncKey) as? Bool ?? true
        self.scanSubfoldersRecursively = UserDefaults.standard.object(forKey: userDefaultsRecursiveKey) as? Bool ?? true

        // Load saved folder paths from UserDefaults
        if let savedPaths = UserDefaults.standard.stringArray(forKey: userDefaultsWatchedKey) {
            self.watchedFolders = savedPaths.map { URL(fileURLWithPath: $0) }
        }

        // Load liked tracks
        if let liked = UserDefaults.standard.stringArray(forKey: userDefaultsLikedKey) {
            self.likedTrackIds = Set(liked.compactMap { UUID(uuidString: $0) })
        }

        // Load saved skin
        if let savedSkin = UserDefaults.standard.string(forKey: userDefaultsSkinKey),
           let skin = AppSkin(rawValue: savedSkin) {
            self.selectedSkin = skin
        }

        // Load saved accent color
        if let savedAccent = UserDefaults.standard.string(forKey: userDefaultsAccentKey),
           let accent = AppAccentColor(rawValue: savedAccent) {
            self.selectedAccentColor = accent
        }

        // Load saved playlists
        if let data = UserDefaults.standard.data(forKey: userDefaultsPlaylistsKey),
           let savedPlaylists = try? JSONDecoder().decode([Playlist].self, from: data) {
            self.playlists = savedPlaylists
        }

        NotificationCenter.default.addObserver(
            forName: .trackDidFinishPlaying,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in
                self?.playNext()
            }
        }

        // Fast cold-start: load cached library metadata from disk
        loadCachedLibrary()

        // Background Auto-sync on startup if enabled and watched folders exist
        if autoSyncOnLaunch && !watchedFolders.isEmpty {
            Task {
                await self.rescanAllFolders()
            }
        }
    }

    // MARK: - Appearance & Playlist Management

    public func setSkin(_ skin: AppSkin) {
        selectedSkin = skin
        UserDefaults.standard.set(skin.rawValue, forKey: userDefaultsSkinKey)
    }

    public func setAccentColor(_ accent: AppAccentColor) {
        selectedAccentColor = accent
        UserDefaults.standard.set(accent.rawValue, forKey: userDefaultsAccentKey)
    }

    @discardableResult
    public func createPlaylist(name: String) -> Playlist {
        let pl = Playlist(name: name)
        playlists.append(pl)
        savePlaylists()
        return pl
    }

    public func deletePlaylist(id: UUID) {
        playlists.removeAll(where: { $0.id == id })
        savePlaylists()
        if case .playlist(let curId) = selectedSidebarItem, curId == id {
            selectedSidebarItem = .allTracks
        }
    }

    public func addTrackToPlaylist(trackId: UUID, playlistId: UUID) {
        guard let idx = playlists.firstIndex(where: { $0.id == playlistId }) else { return }
        if !playlists[idx].trackIds.contains(trackId) {
            playlists[idx].trackIds.append(trackId)
            savePlaylists()
        }
    }

    public func addTrackToPlaylist(playlistId: UUID, trackId: UUID) {
        addTrackToPlaylist(trackId: trackId, playlistId: playlistId)
    }

    public func removeTrackFromPlaylist(trackId: UUID, playlistId: UUID) {
        guard let idx = playlists.firstIndex(where: { $0.id == playlistId }) else { return }
        playlists[idx].trackIds.removeAll(where: { $0 == trackId })
        savePlaylists()
    }

    public func playlist(with id: UUID) -> Playlist? {
        playlists.first(where: { $0.id == id })
    }

    public func tracks(for playlist: Playlist) -> [Track] {
        playlist.trackIds.compactMap { id in
            tracks.first(where: { $0.id == id })
        }
    }

    private func savePlaylists() {
        if let encoded = try? JSONEncoder().encode(playlists) {
            UserDefaults.standard.set(encoded, forKey: userDefaultsPlaylistsKey)
        }
    }

    public var favoriteTracks: [Track] {
        tracks.filter { likedTrackIds.contains($0.id) }
    }

    public var recentTracks: [Track] {
        var result: [Track] = history
        for t in tracks {
            if !result.contains(where: { $0.id == t.id }) {
                result.append(t)
            }
        }
        return result
    }

    // MARK: - Filtered Collections & Structured Groups

    public var filteredTracks: [Track] {
        tracks.filter { track in
            let matchesSearch = searchQuery.isEmpty ||
                track.title.localizedCaseInsensitiveContains(searchQuery) ||
                track.artist.localizedCaseInsensitiveContains(searchQuery) ||
                track.album.localizedCaseInsensitiveContains(searchQuery)

            let matchesFormat: Bool
            switch selectedFormatFilter {
            case "All": matchesFormat = true
            case "Hi-Res", "Hi\u{2011}Res": matchesFormat = track.isHiRes
            case "FLAC": matchesFormat = track.codec == .flac
            case "WAV": matchesFormat = track.codec == .wav
            case "DSD": matchesFormat = track.codec == .dsd
            case "ALAC": matchesFormat = track.codec == .alac
            default: matchesFormat = true
            }

            return matchesSearch && matchesFormat
        }
    }

    /// Structured Albums Grouping
    public var albums: [AlbumGroup] {
        let grouped = Dictionary(grouping: filteredTracks) { track in
            "\(track.artist.lowercased())__\(track.album.lowercased())"
        }

        return grouped.values.map { albumTracks in
            let first = albumTracks.first!
            let sorted = albumTracks.sorted { ($0.trackNumber ?? 0) < ($1.trackNumber ?? 0) }
            return AlbumGroup(
                title: first.album,
                artist: first.artist,
                year: first.year,
                artwork: first.artwork,
                tracks: sorted
            )
        }.sorted { $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending }
    }

    /// Structured Artists Grouping
    public var artists: [ArtistGroup] {
        let grouped = Dictionary(grouping: filteredTracks) { $0.artist.lowercased() }

        return grouped.values.map { artistTracks in
            let first = artistTracks.first!
            let distinctAlbums = Set(artistTracks.map { $0.album.lowercased() }).count
            let artwork = artistTracks.first(where: { $0.artwork != nil })?.artwork
            return ArtistGroup(
                name: first.artist,
                albumCount: max(1, distinctAlbums),
                trackCount: artistTracks.count,
                tracks: artistTracks,
                sampleArtwork: artwork
            )
        }.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    /// Structured Genres Grouping
    public var genres: [GenreGroup] {
        let grouped = Dictionary(grouping: filteredTracks) { $0.genre.lowercased() }

        return grouped.values.map { genreTracks in
            let name = genreTracks.first!.genre
            return GenreGroup(
                name: name.isEmpty ? "Unclassified" : name,
                trackCount: genreTracks.count,
                tracks: genreTracks
            )
        }.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    /// Recently Added (Newest to oldest)
    public var recentlyAddedTracks: [Track] {
        Array(filteredTracks.reversed())
    }

    // MARK: - Playback Coordination

    public func play(track: Track) {
        audioController.loadAndPlay(track: track)
        currentPalette = ArtworkColorExtractor.extractPalette(from: track.artwork)

        // Track History
        if history.last?.id != track.id {
            history.append(track)
            if history.count > 60 { history.removeFirst() }
        }
    }

    public func playNext() {
        guard !tracks.isEmpty else { return }

        // 1. Check Repeat One
        if repeatMode == .one {
            audioController.seek(to: 0.0)
            audioController.resume()
            return
        }

        // 2. Play next in user Queue if present
        if !queue.isEmpty {
            let nextTrack = queue.removeFirst()
            play(track: nextTrack)
            return
        }

        // 3. Shuffle Mode
        if isShuffleEnabled {
            let available = tracks.filter { $0.id != audioController.currentTrack?.id }
            if let randomTrack = available.randomElement() ?? tracks.randomElement() {
                play(track: randomTrack)
                return
            }
        }

        // 4. Sequential playback
        if let idx = currentIndex, idx + 1 < tracks.count {
            play(track: tracks[idx + 1])
        } else if repeatMode == .all, let first = tracks.first {
            play(track: first)
        } else if let first = tracks.first {
            play(track: first)
        }
    }

    public func playPrevious() {
        guard !tracks.isEmpty else { return }

        // If played more than 3 seconds, replay track from beginning
        if audioController.currentTime > 3.0 {
            audioController.seek(to: 0.0)
            return
        }

        if let idx = currentIndex, idx > 0 {
            play(track: tracks[idx - 1])
        } else if let last = tracks.last {
            play(track: last)
        }
    }

    public func toggleShuffle() {
        isShuffleEnabled.toggle()
    }

    public func cycleRepeat() {
        switch repeatMode {
        case .off: repeatMode = .all
        case .all: repeatMode = .one
        case .one: repeatMode = .off
        }
    }

    public func toggleLike(track: Track) {
        toggleLike(track.id)
    }

    public func toggleLike(_ trackId: UUID) {
        if likedTrackIds.contains(trackId) {
            likedTrackIds.remove(trackId)
        } else {
            likedTrackIds.insert(trackId)
        }
        UserDefaults.standard.set(likedTrackIds.map { $0.uuidString }, forKey: userDefaultsLikedKey)
    }

    public func isLiked(track: Track) -> Bool {
        likedTrackIds.contains(track.id)
    }

    public func addToQueue(track: Track) {
        queue.append(track)
    }

    public func removeFromQueue(at index: Int) {
        guard index >= 0 && index < queue.count else { return }
        queue.remove(at: index)
    }

    public func clearQueue() {
        queue.removeAll()
    }

    public func clearHistory() {
        history.removeAll()
    }

    public func navigateToAlbum(named albumTitle: String, artist: String) {
        if selectedSidebarItem != .albums {
            navigationHistory.append(selectedSidebarItem)
        }
        let match = albums.first {
            $0.title.caseInsensitiveCompare(albumTitle) == .orderedSame &&
            $0.artist.caseInsensitiveCompare(artist) == .orderedSame
        } ?? albums.first {
            $0.title.caseInsensitiveCompare(albumTitle) == .orderedSame
        }

        if let match = match {
            selectedAlbumTarget = match
            selectedSidebarItem = .albums
        }
    }

    public func navigateToArtist(_ artistName: String) {
        if selectedSidebarItem != .artists {
            navigationHistory.append(selectedSidebarItem)
        }
        selectedArtistTarget = artistName
        selectedSidebarItem = .artists
    }

    public func updateArtwork(for trackId: UUID, image: NSImage) {
        if let idx = tracks.firstIndex(where: { $0.id == trackId }) {
            tracks[idx].artwork = image
            if audioController.currentTrack?.id == trackId {
                audioController.loadCurrentTrackMetadata(tracks[idx])
                currentPalette = ArtworkColorExtractor.extractPalette(from: image)
            }
        }
        saveLibraryCache()
    }

    public func updateLyricsForCurrentTrack(synced: [LyricLine], plain: String?) {
        guard let current = audioController.currentTrack,
              let idx = tracks.firstIndex(where: { $0.id == current.id }) else { return }

        var updated = current
        updated.syncedLyrics = synced
        updated.plainLyrics = plain
        tracks[idx] = updated

        if audioController.currentTrack?.id == current.id {
            audioController.loadCurrentTrackMetadata(updated)
        }
    }

    // MARK: - Library Folder Management & Recursive Import

    public func addLibraryFolder(url: URL) {
        if !watchedFolders.contains(where: { $0.path == url.path }) {
            watchedFolders.append(url)
            saveWatchedFolders()
        }
        Task {
            await rescanFolder(url)
        }
    }

    public func removeLibraryFolder(url: URL) {
        watchedFolders.removeAll { $0.path == url.path }
        saveWatchedFolders()

        // Prune tracks belonging to this folder path
        let folderPath = url.path
        tracks.removeAll { $0.url.path.hasPrefix(folderPath) }
        saveLibraryCache()
    }

    public func clearLibrary() {
        audioController.stop()
        tracks.removeAll()
        saveLibraryCache()
    }

    public func setAutoSync(_ enabled: Bool) {
        autoSyncOnLaunch = enabled
        UserDefaults.standard.set(enabled, forKey: userDefaultsAutoSyncKey)
    }

    public func setRecursiveScan(_ enabled: Bool) {
        scanSubfoldersRecursively = enabled
        UserDefaults.standard.set(enabled, forKey: userDefaultsRecursiveKey)
    }

    public func rescanAllFolders() async {
        guard !watchedFolders.isEmpty else { return }
        isSyncing = true
        syncProgressMessage = "Syncing music folders..."

        var accumulated: [Track] = []
        var existingPaths = Set(tracks.map { $0.url.path })

        for folder in watchedFolders {
            syncProgressMessage = "Scanning \(folder.lastPathComponent)..."
            let found = await scanDirectory(folder, recursive: scanSubfoldersRecursively)
            for t in found {
                if !existingPaths.contains(t.url.path) {
                    accumulated.append(t)
                    existingPaths.insert(t.url.path)
                }
            }
        }

        // Check for removed files from existing tracks
        self.tracks.removeAll { !FileManager.default.fileExists(atPath: $0.url.path) }
        self.tracks.append(contentsOf: accumulated)

        isSyncing = false
        syncProgressMessage = ""
        saveLibraryCache()
    }

    private func rescanFolder(_ folderURL: URL) async {
        isSyncing = true
        syncProgressMessage = "Scanning \(folderURL.lastPathComponent)..."

        let found = await scanDirectory(folderURL, recursive: scanSubfoldersRecursively)
        var existingPaths = Set(tracks.map { $0.url.path })
        var newTracks: [Track] = []

        for t in found {
            if !existingPaths.contains(t.url.path) {
                newTracks.append(t)
                existingPaths.insert(t.url.path)
            }
        }

        self.tracks.append(contentsOf: newTracks)
        isSyncing = false
        syncProgressMessage = ""
        saveLibraryCache()
    }

    public func importURLs(_ urls: [URL]) async {
        isSyncing = true
        syncProgressMessage = "Importing files..."
        var newTracks: [Track] = []
        var existingPaths = Set(tracks.map { $0.url.path })

        for url in urls {
            var isDir: ObjCBool = false
            if FileManager.default.fileExists(atPath: url.path, isDirectory: &isDir) {
                if isDir.boolValue {
                    let dirTracks = await scanDirectory(url, recursive: scanSubfoldersRecursively)
                    for t in dirTracks where !existingPaths.contains(t.url.path) {
                        newTracks.append(t)
                        existingPaths.insert(t.url.path)
                    }
                } else {
                    if !existingPaths.contains(url.path) {
                        let track = await AudioMetadataReader.shared.readMetadata(for: url)
                        newTracks.append(track)
                        existingPaths.insert(url.path)
                    }
                }
            }
        }

        self.tracks.append(contentsOf: newTracks)
        isSyncing = false
        syncProgressMessage = ""
        saveLibraryCache()

        if audioController.currentTrack == nil, let first = self.tracks.first {
            play(track: first)
            audioController.pause()
        }
    }

    /// Recursively scans directory at arbitrary depth for audio files
    private func scanDirectory(_ dirURL: URL, recursive: Bool) async -> [Track] {
        var found: [Track] = []
        let allowed = ["flac", "wav", "wave", "aiff", "aif", "alac", "m4a", "mp3", "aac", "dsf", "dff", "ogg", "opus"]

        let options: FileManager.DirectoryEnumerationOptions = recursive ? [.skipsHiddenFiles] : [.skipsHiddenFiles, .skipsSubdirectoryDescendants]
        guard let enumerator = FileManager.default.enumerator(
            at: dirURL,
            includingPropertiesForKeys: [.isRegularFileKey],
            options: options
        ) else { return [] }

        let fileURLs: [URL] = (enumerator.allObjects as? [URL])?.filter {
            allowed.contains($0.pathExtension.lowercased())
        } ?? []

        for fileURL in fileURLs {
            let track = await AudioMetadataReader.shared.readMetadata(for: fileURL)
            found.append(track)
        }

        return found
    }

    private func saveWatchedFolders() {
        let paths = watchedFolders.map { $0.path }
        UserDefaults.standard.set(paths, forKey: userDefaultsWatchedKey)
    }

    // MARK: - Fast Library Disk Cache

    private var appSupportDir: URL {
        let url = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
            .appendingPathComponent("Auralis", isDirectory: true)
        try? FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
        return url
    }

    private func saveLibraryCache() {
        let fileURL = appSupportDir.appendingPathComponent("library_cache.json")
        let simplified = tracks.map { track -> [String: Any] in
            var dict: [String: Any] = [
                "path": track.url.path,
                "title": track.title,
                "artist": track.artist,
                "album": track.album,
                "genre": track.genre,
                "duration": track.duration,
                "sampleRate": track.sampleRate,
                "bitDepth": track.bitDepth,
                "channels": track.channels,
                "bitRate": track.bitRate,
                "codec": track.codec.rawValue
            ]
            if let y = track.year { dict["year"] = y }
            if let num = track.trackNumber { dict["trackNumber"] = num }
            if let plain = track.plainLyrics { dict["plainLyrics"] = plain }
            return dict
        }

        if let data = try? JSONSerialization.data(withJSONObject: simplified, options: []) {
            try? data.write(to: fileURL, options: .atomic)
        }
    }

    private func loadCachedLibrary() {
        let fileURL = appSupportDir.appendingPathComponent("library_cache.json")
        guard let data = try? Data(contentsOf: fileURL),
              let list = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            return
        }

        var loaded: [Track] = []
        for item in list {
            guard let path = item["path"] as? String,
                  FileManager.default.fileExists(atPath: path) else { continue }

            let url = URL(fileURLWithPath: path)
            let title = (item["title"] as? String) ?? url.deletingPathExtension().lastPathComponent
            let artist = (item["artist"] as? String) ?? "Unknown Artist"
            let album = (item["album"] as? String) ?? "Unknown Album"
            let genre = (item["genre"] as? String) ?? "Audiophile Master"
            let duration = (item["duration"] as? Double) ?? 0.0
            let sampleRate = (item["sampleRate"] as? Double) ?? 44100.0
            let bitDepth = (item["bitDepth"] as? Int) ?? 16
            let channels = (item["channels"] as? Int) ?? 2
            let bitRate = (item["bitRate"] as? Int) ?? 1411200
            let codecRaw = (item["codec"] as? String) ?? "AUDIO"
            let codec = AudioCodec(rawValue: codecRaw) ?? .wav
            let year = item["year"] as? Int
            let trackNumber = item["trackNumber"] as? Int
            let plainLyrics = item["plainLyrics"] as? String

            let track = Track(
                url: url,
                title: title,
                artist: artist,
                album: album,
                genre: genre,
                year: year,
                duration: duration,
                trackNumber: trackNumber,
                sampleRate: sampleRate,
                bitDepth: bitDepth,
                channels: channels,
                bitRate: bitRate,
                codec: codec,
                artwork: nil, // Loaded on-demand
                syncedLyrics: [],
                plainLyrics: plainLyrics
            )
            loaded.append(track)
        }

        self.tracks = loaded
    }
}
