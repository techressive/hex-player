import Foundation

public struct LyricsFetchResult: Sendable {
    public let syncedLyrics: [LyricLine]?
    public let plainLyrics: String?
    public let isInstrumental: Bool

    public init(syncedLyrics: [LyricLine]?, plainLyrics: String?, isInstrumental: Bool = false) {
        self.syncedLyrics = syncedLyrics
        self.plainLyrics = plainLyrics
        self.isInstrumental = isInstrumental
    }
}

public final class LyricsService: Sendable {
    public static let shared = LyricsService()

    private let session: URLSession

    public init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 6.0
        config.timeoutIntervalForResource = 12.0
        self.session = URLSession(configuration: config)
    }

    /// Parses standard and extended LRC format strings into structured LyricLine array
    public func parseLRC(_ lrcString: String) -> [LyricLine] {
        var lines: [LyricLine] = []
        let rawLines = lrcString.components(separatedBy: .newlines)

        let pattern = "\\[(\\d{2,}):(\\d{2})(?:\\.(\\d{2,3}))?\\]"
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return [] }

        for rawLine in rawLines {
            let nsString = rawLine as NSString
            let matches = regex.matches(in: rawLine, range: NSRange(location: 0, length: nsString.length))
            guard !matches.isEmpty else { continue }

            let lastMatch = matches.last!
            let textStartIndex = lastMatch.range.location + lastMatch.range.length
            var text = ""
            if textStartIndex < nsString.length {
                text = nsString.substring(from: textStartIndex).trimmingCharacters(in: .whitespaces)
            }

            for match in matches {
                guard match.numberOfRanges >= 3 else { continue }
                let minStr = nsString.substring(with: match.range(at: 1))
                let secStr = nsString.substring(with: match.range(at: 2))
                var ms = 0.0
                if match.numberOfRanges >= 4 && match.range(at: 3).location != NSNotFound {
                    let msStr = nsString.substring(with: match.range(at: 3))
                    let divisor = pow(10.0, Double(msStr.count))
                    ms = (Double(msStr) ?? 0.0) / divisor
                }

                let minutes = Double(minStr) ?? 0.0
                let seconds = Double(secStr) ?? 0.0
                let totalTime = (minutes * 60.0) + seconds + ms

                lines.append(LyricLine(time: totalTime, text: text))
            }
        }

        return lines.sorted { $0.time < $1.time }
    }

    /// Determines the currently active line index based on playback time
    public func activeLineIndex(in lyrics: [LyricLine], at time: TimeInterval) -> Int? {
        guard !lyrics.isEmpty else { return nil }

        var activeIdx: Int? = nil
        for (index, line) in lyrics.enumerated() {
            if time >= line.time {
                activeIdx = index
            } else {
                break
            }
        }
        return activeIdx
    }

    /// Cleans noisy title suffixes commonly found in high-res and remastered tracks
    public func cleanTitle(_ title: String) -> String {
        var cleaned = title
        let patterns = [
            "\\[.*?\\]",
            "\\(.*?remaster.*?\\)",
            "\\(.*?live.*?\\)",
            "\\(.*?deluxe.*?\\)",
            "\\(.*?bonus.*?\\)",
            "- \\d{4} Remaster",
            "- Remastered",
            "- Single Version",
            "- Album Version"
        ]
        for pat in patterns {
            if let regex = try? NSRegularExpression(pattern: pat, options: .caseInsensitive) {
                cleaned = regex.stringByReplacingMatches(
                    in: cleaned,
                    range: NSRange(location: 0, length: (cleaned as NSString).length),
                    withTemplate: ""
                )
            }
        }
        return cleaned.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// Convenience fetch for a Track
    public func fetchLyrics(track: Track) async -> LyricsFetchResult? {
        await fetchOnlineLyrics(
            title: track.title,
            artist: track.artist,
            album: track.album,
            duration: track.duration
        )
    }

    /// Asynchronously fetches synced and plain lyrics from LRCLIB API with local disk caching
    public func fetchOnlineLyrics(
        title: String,
        artist: String,
        album: String? = nil,
        duration: TimeInterval? = nil
    ) async -> LyricsFetchResult? {
        let cleanT = cleanTitle(title)
        let cacheKey = "\(artist.lowercased())_\(cleanT.lowercased())".filter { $0.isLetter || $0.isNumber }

        // Check local disk cache
        if let cached = loadCachedLyrics(key: cacheKey) {
            return cached
        }

        // Try exact match endpoint: /api/get
        var components = URLComponents(string: "https://lrclib.net/api/get")
        var queryItems: [URLQueryItem] = [
            URLQueryItem(name: "artist_name", value: artist),
            URLQueryItem(name: "track_name", value: cleanT)
        ]
        if let alb = album, !alb.isEmpty {
            queryItems.append(URLQueryItem(name: "album_name", value: alb))
        }
        if let dur = duration, dur > 1.0 {
            queryItems.append(URLQueryItem(name: "duration", value: "\(Int(dur))"))
        }
        components?.queryItems = queryItems

        if let url = components?.url,
           let result = await requestLRCLIB(url: url) {
            saveCachedLyrics(result: result, key: cacheKey)
            return result
        }

        // Fallback: search endpoint /api/search?q=
        var searchComp = URLComponents(string: "https://lrclib.net/api/search")
        searchComp?.queryItems = [
            URLQueryItem(name: "q", value: "\(artist) \(cleanT)")
        ]
        if let searchURL = searchComp?.url,
           let searchResult = await requestLRCLIBSearch(url: searchURL) {
            saveCachedLyrics(result: searchResult, key: cacheKey)
            return searchResult
        }

        return nil
    }

    private func requestLRCLIB(url: URL) async -> LyricsFetchResult? {
        do {
            var request = URLRequest(url: url)
            request.setValue("AuralisAudiophile/1.0", forHTTPHeaderField: "User-Agent")
            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse, http.statusCode == 200 else { return nil }

            guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return nil }
            return parseLRCLIBResponse(json)
        } catch {
            return nil
        }
    }

    private func requestLRCLIBSearch(url: URL) async -> LyricsFetchResult? {
        do {
            var request = URLRequest(url: url)
            request.setValue("AuralisAudiophile/1.0", forHTTPHeaderField: "User-Agent")
            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse, http.statusCode == 200 else { return nil }

            guard let list = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]],
                  let first = list.first else { return nil }
            return parseLRCLIBResponse(first)
        } catch {
            return nil
        }
    }

    private func parseLRCLIBResponse(_ json: [String: Any]) -> LyricsFetchResult {
        let isInstrumental = (json["instrumental"] as? Bool) ?? false
        var synced: [LyricLine]? = nil
        var plain: String? = nil

        if let syncedStr = json["syncedLyrics"] as? String, !syncedStr.isEmpty {
            let parsed = parseLRC(syncedStr)
            if !parsed.isEmpty {
                synced = parsed
            }
        }

        if let plainStr = json["plainLyrics"] as? String, !plainStr.isEmpty {
            plain = plainStr
        }

        return LyricsFetchResult(
            syncedLyrics: synced,
            plainLyrics: plain,
            isInstrumental: isInstrumental
        )
    }

    private var cacheDirectory: URL {
        let url = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
            .appendingPathComponent("Auralis/Lyrics", isDirectory: true)
        try? FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
        return url
    }

    private func loadCachedLyrics(key: String) -> LyricsFetchResult? {
        let fileURL = cacheDirectory.appendingPathComponent("\(key).json")
        guard let data = try? Data(contentsOf: fileURL),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }
        return parseLRCLIBResponse(json)
    }

    private func saveCachedLyrics(result: LyricsFetchResult, key: String) {
        let fileURL = cacheDirectory.appendingPathComponent("\(key).json")
        var dict: [String: Any] = [
            "instrumental": result.isInstrumental
        ]
        if let plain = result.plainLyrics { dict["plainLyrics"] = plain }
        if let synced = result.syncedLyrics {
            let lrcText = synced.map { line in
                let m = Int(line.time) / 60
                let s = Int(line.time) % 60
                let cs = Int((line.time.truncatingRemainder(dividingBy: 1.0)) * 100.0)
                return String(format: "[%02d:%02d.%02d]%@", m, s, cs, line.text)
            }.joined(separator: "\n")
            dict["syncedLyrics"] = lrcText
        }
        if let data = try? JSONSerialization.data(withJSONObject: dict) {
            try? data.write(to: fileURL, options: .atomic)
        }
    }
}
