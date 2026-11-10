import Foundation
import AppKit

@MainActor
public final class ArtworkFetchService: ObservableObject {
    public static let shared = ArtworkFetchService()

    @Published public var fetchingTrackIds: Set<UUID> = []
    @Published public var lastErrorMessage: String? = nil

    private let cacheDirectory: URL

    private init() {
        let appSupport = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
        let dir = appSupport.appendingPathComponent("HEX").appendingPathComponent("Artwork")
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        self.cacheDirectory = dir
    }

    public func isFetching(trackId: UUID) -> Bool {
        fetchingTrackIds.contains(trackId)
    }

    /// Fetches high-resolution artwork from online databases, writes it to disk, and updates memory.
    @discardableResult
    public func fetchAndSaveArtwork(for track: Track) async -> NSImage? {
        fetchingTrackIds.insert(track.id)
        defer {
            fetchingTrackIds.remove(track.id)
        }

        // 1. Search for online artwork URL
        guard let artworkURL = await findArtworkURL(title: track.title, artist: track.artist, album: track.album) else {
            lastErrorMessage = "No artwork found online for \"\(track.title)\""
            return nil
        }

        // 2. Download artwork data
        do {
            var request = URLRequest(url: artworkURL)
            request.timeoutInterval = 15.0
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode),
                  let nsImage = NSImage(data: data) else {
                lastErrorMessage = "Failed to decode artwork image"
                return nil
            }

            // 3. Write artwork to track's local folder as cover.jpg if accessible
            let trackFolder = track.url.deletingLastPathComponent()
            let companionCover = trackFolder.appendingPathComponent("cover.jpg")
            if !FileManager.default.fileExists(atPath: companionCover.path) {
                try? data.write(to: companionCover, options: .atomic)
            }

            // 4. Write artwork to persistent application cache
            let cachedFile = cacheDirectory.appendingPathComponent("\(track.id.uuidString).jpg")
            try? data.write(to: cachedFile, options: .atomic)

            // 5. Update AppState and AudioController in memory
            AppState.shared.updateArtwork(for: track.id, image: nsImage)

            return nsImage
        } catch {
            lastErrorMessage = error.localizedDescription
            return nil
        }
    }

    /// Searches iTunes API first with upscale to 1200x1200bb, then falls back to Deezer.
    private func findArtworkURL(title: String, artist: String, album: String) async -> URL? {
        // Query permutations: title + artist, album + artist
        let queries = [
            "\(artist) \(title)",
            "\(artist) \(album)",
            title
        ]

        for query in queries {
            if let itunesURL = await searchItunes(query: query) {
                return itunesURL
            }
        }

        for query in queries {
            if let deezerURL = await searchDeezer(query: query) {
                return deezerURL
            }
        }

        return nil
    }

    private func searchItunes(query: String) async -> URL? {
        guard let encoded = query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "https://itunes.apple.com/search?term=\(encoded)&media=music&entity=song&limit=1") else {
            return nil
        }

        do {
            var request = URLRequest(url: url)
            request.timeoutInterval = 8.0
            let (data, _) = try await URLSession.shared.data(for: request)
            guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let results = json["results"] as? [[String: Any]],
                  let first = results.first,
                  let rawArtURL = first["artworkUrl100"] as? String else {
                return nil
            }

            // Upgrade 100x100 to 1200x1200 high-resolution master
            let highResString = rawArtURL
                .replacingOccurrences(of: "100x100bb.jpg", with: "1200x1200bb.jpg")
                .replacingOccurrences(of: "100x100bb", with: "1200x1200bb")

            return URL(string: highResString) ?? URL(string: rawArtURL)
        } catch {
            return nil
        }
    }

    private func searchDeezer(query: String) async -> URL? {
        guard let encoded = query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "https://api.deezer.com/search?q=\(encoded)&limit=1") else {
            return nil
        }

        do {
            var request = URLRequest(url: url)
            request.timeoutInterval = 8.0
            let (data, _) = try await URLSession.shared.data(for: request)
            guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let dataList = json["data"] as? [[String: Any]],
                  let first = dataList.first,
                  let albumObj = first["album"] as? [String: Any] else {
                return nil
            }

            if let xl = albumObj["cover_xl"] as? String, let u = URL(string: xl) {
                return u
            }
            if let big = albumObj["cover_big"] as? String, let u = URL(string: big) {
                return u
            }
            if let medium = albumObj["cover_medium"] as? String, let u = URL(string: medium) {
                return u
            }
        } catch {
            return nil
        }
        return nil
    }

    /// Loads cached artwork for a given track id if present on disk
    public func cachedArtwork(for trackId: UUID) -> NSImage? {
        let cachedFile = cacheDirectory.appendingPathComponent("\(trackId.uuidString).jpg")
        if FileManager.default.fileExists(atPath: cachedFile.path),
           let data = try? Data(contentsOf: cachedFile),
           let image = NSImage(data: data) {
            return image
        }
        return nil
    }
}
