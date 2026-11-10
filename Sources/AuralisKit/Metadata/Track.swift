import Foundation
import AppKit

public enum AudioCodec: String, Codable, CaseIterable, Sendable {
    case flac = "FLAC"
    case wav = "WAV"
    case alac = "ALAC"
    case aiff = "AIFF"
    case dsd = "DSD"
    case mp3 = "MP3"
    case aac = "AAC"
    case ogg = "OGG"
    case opus = "OPUS"
    case unknown = "AUDIO"

    public var isLossless: Bool {
        switch self {
        case .flac, .wav, .alac, .aiff, .dsd: return true
        default: return false
        }
    }

    public var badgeColorHex: String {
        switch self {
        case .dsd: return "#FFD700" // Pure Gold
        case .flac, .alac: return "#E5A93C" // Warm Studio Amber
        case .wav, .aiff: return "#64D2FF" // Studio Cyan
        default: return "#A1A1AA" // Zinc
        }
    }
}

public struct LyricLine: Identifiable, Equatable, Hashable, Sendable {
    public let id = UUID()
    public let time: TimeInterval
    public let text: String

    public init(time: TimeInterval, text: String) {
        self.time = time
        self.text = text
    }
}

public struct Track: Identifiable, Hashable, @unchecked Sendable {
    public let id: UUID
    public var url: URL
    public var title: String
    public var artist: String
    public var album: String
    public var genre: String
    public var year: Int?
    public var duration: TimeInterval
    public var trackNumber: Int?
    
    // Audiophile specifications
    public var sampleRate: Double     // e.g. 192000.0 (192 kHz)
    public var bitDepth: Int          // e.g. 24-bit
    public var channels: Int          // 2 for stereo
    public var bitRate: Int           // e.g. 9216000 bps
    public var codec: AudioCodec
    
    // Rich media
    public var artwork: NSImage?
    public var syncedLyrics: [LyricLine]
    public var plainLyrics: String?
    public var artistBio: String?
    public var artistImage: NSImage?
    
    public init(
        id: UUID = UUID(),
        url: URL,
        title: String,
        artist: String,
        album: String,
        genre: String = "High-Fidelity",
        year: Int? = 2024,
        duration: TimeInterval,
        trackNumber: Int? = 1,
        sampleRate: Double = 96000.0,
        bitDepth: Int = 24,
        channels: Int = 2,
        bitRate: Int = 4608000,
        codec: AudioCodec = .flac,
        artwork: NSImage? = nil,
        syncedLyrics: [LyricLine] = [],
        plainLyrics: String? = nil,
        artistBio: String? = nil,
        artistImage: NSImage? = nil
    ) {
        self.id = id
        self.url = url
        self.title = title
        self.artist = artist
        self.album = album
        self.genre = genre
        self.year = year
        self.duration = duration
        self.trackNumber = trackNumber
        self.sampleRate = sampleRate
        self.bitDepth = bitDepth
        self.channels = channels
        self.bitRate = bitRate
        self.codec = codec
        self.artwork = artwork
        self.syncedLyrics = syncedLyrics
        self.plainLyrics = plainLyrics
        self.artistBio = artistBio
        self.artistImage = artistImage
    }

    public var formattedSampleRate: String {
        if sampleRate >= 1_000_000 {
            return String(format: "%.1f MHz", sampleRate / 1_000_000.0)
        } else if sampleRate >= 1000 {
            let khz = sampleRate / 1000.0
            return khz.truncatingRemainder(dividingBy: 1) == 0 ? "\(Int(khz)) kHz" : String(format: "%.1f kHz", khz)
        } else {
            return "\(Int(sampleRate)) Hz"
        }
    }

    public var formattedBitDepth: String {
        if codec == .dsd {
            return "1-bit Direct Stream"
        }
        return "\(bitDepth)-bit"
    }

    public var formattedBitRate: String {
        if bitRate >= 1000 {
            return "\(bitRate / 1000) kbps"
        }
        return "\(bitRate) bps"
    }

    public var audiophileBadgeText: String {
        return "\(codec.rawValue) • \(formattedBitDepth) / \(formattedSampleRate)"
    }
    
    public var isHiRes: Bool {
        return (sampleRate >= 88200 || bitDepth >= 24) || codec == .dsd
    }

    public static func == (lhs: Track, rhs: Track) -> Bool {
        lhs.id == rhs.id
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}
