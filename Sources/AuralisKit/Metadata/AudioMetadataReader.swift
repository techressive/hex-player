import Foundation
import AVFoundation
import AppKit

public final class AudioMetadataReader: Sendable {
    public static let shared = AudioMetadataReader()

    public func readMetadata(for url: URL) async -> Track {
        let asset = AVURLAsset(url: url)
        let filename = url.deletingPathExtension().lastPathComponent
        let ext = url.pathExtension.lowercased()

        var title = filename
        var artist = "Unknown Artist"
        var album = "Unknown Album"
        var genre = "Audiophile Master"
        var year: Int? = nil
        var trackNumber: Int? = 1
        var artwork: NSImage? = nil
        var embeddedLyrics: String? = nil

        var duration: TimeInterval = 0.0
        var sampleRate: Double = 44100.0
        var bitDepth: Int = 16
        var channels: Int = 2
        var bitRate: Int = 1411200
        let codec: AudioCodec = codecFromExtension(ext)

        do {
            let dur = try await asset.load(.duration)
            duration = CMTimeGetSeconds(dur)

            let metadata = try await asset.load(.commonMetadata)
            for item in metadata {
                guard let commonKey = item.commonKey else { continue }
                switch commonKey {
                case .commonKeyTitle:
                    if let val = try? await item.load(.stringValue) { title = val }
                case .commonKeyArtist:
                    if let val = try? await item.load(.stringValue) { artist = val }
                case .commonKeyAlbumName:
                    if let val = try? await item.load(.stringValue) { album = val }
                case .commonKeyArtwork:
                    if let data = try? await item.load(.dataValue) {
                        artwork = NSImage(data: data)
                    }
                case .commonKeyCreationDate:
                    if let str = try? await item.load(.stringValue), let y = Int(str.prefix(4)) {
                        year = y
                    }
                default:
                    break
                }
            }

            // Check format streams
            let tracks = try await asset.loadTracks(withMediaType: .audio)
            if let audioTrack = tracks.first {
                let descriptions = try await audioTrack.load(.formatDescriptions)
                if let firstDesc = descriptions.first {
                    if let basic = CMAudioFormatDescriptionGetStreamBasicDescription(firstDesc) {
                        sampleRate = basic.pointee.mSampleRate
                        channels = Int(basic.pointee.mChannelsPerFrame)
                        let bits = Int(basic.pointee.mBitsPerChannel)
                        if bits > 0 { bitDepth = bits }
                        if sampleRate > 0 && bitDepth > 0 && channels > 0 {
                            bitRate = Int(sampleRate) * bitDepth * channels
                        }
                    }
                }
            }
        } catch {
            // Fallback for files where AVAsset commonMetadata has limited fields
        }

        // Check for companion cover image file in the same directory if not embedded
        if artwork == nil {
            let folder = url.deletingLastPathComponent()
            let candidates = ["cover.jpg", "cover.png", "folder.jpg", "album.jpg", "Front.jpg"]
            for cand in candidates {
                let imgURL = folder.appendingPathComponent(cand)
                if FileManager.default.fileExists(atPath: imgURL.path),
                   let data = try? Data(contentsOf: imgURL),
                   let img = NSImage(data: data) {
                    artwork = img
                    break
                }
            }
        }

        // Check for companion .lrc file in the same directory
        let lrcURL = url.deletingPathExtension().appendingPathExtension("lrc")
        var syncedLyrics: [LyricLine] = []
        if FileManager.default.fileExists(atPath: lrcURL.path),
           let lrcContent = try? String(contentsOf: lrcURL, encoding: .utf8) {
            syncedLyrics = LyricsService.shared.parseLRC(lrcContent)
        }

        return Track(
            url: url,
            title: title,
            artist: artist,
            album: album,
            genre: genre,
            year: year,
            duration: max(1.0, duration),
            trackNumber: trackNumber,
            sampleRate: sampleRate,
            bitDepth: bitDepth,
            channels: channels,
            bitRate: bitRate,
            codec: codec,
            artwork: artwork,
            syncedLyrics: syncedLyrics,
            plainLyrics: embeddedLyrics
        )
    }

    private func codecFromExtension(_ ext: String) -> AudioCodec {
        switch ext {
        case "flac": return .flac
        case "wav", "wave": return .wav
        case "alac": return .alac
        case "aiff", "aif": return .aiff
        case "dsf", "dff", "dsd": return .dsd
        case "mp3": return .mp3
        case "aac", "m4a": return .aac
        case "ogg": return .ogg
        case "opus": return .opus
        default: return .unknown
        }
    }
}
