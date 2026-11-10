import Foundation
import AVFoundation

public enum DecoderError: LocalizedError {
    case unsupportedFormat(String)
    case ffmpegMissing
    case conversionFailed(String)

    public var errorDescription: String? {
        switch self {
        case .unsupportedFormat(let ext):
            return "Audio format '.\(ext)' is unsupported."
        case .ffmpegMissing:
            return "FFmpeg is required for DSD/OGG/Opus decoding but was not found."
        case .conversionFailed(let msg):
            return "Lossless decoding failed: \(msg)"
        }
    }
}

public final class UniversalDecoder: Sendable {
    public static let shared = UniversalDecoder()

    private let ffmpegPaths = [
        "/opt/homebrew/bin/ffmpeg",
        "/usr/local/bin/ffmpeg",
        "/usr/bin/ffmpeg"
    ]

    public var ffmpegBinary: String? {
        for path in ffmpegPaths {
            if FileManager.default.isExecutableFile(atPath: path) {
                return path
            }
        }
        return nil
    }

    /// Prepares an audio file for playback with AVAudioEngine.
    /// If natively supported by CoreAudio/AVAudioFile, returns the original URL.
    /// If exotic (DSD DSF/DFF, OGG, OPUS, APE), losslessly decodes via FFmpeg to a pristine 24-bit/32-bit float AIFF/WAV cache.
    public func prepareFileForPlayback(url: URL) throws -> (playbackURL: URL, isTemporary: Bool) {
        let ext = url.pathExtension.lowercased()
        
        // Formats natively supported by macOS AVAudioFile:
        let nativeExtensions = ["flac", "wav", "wave", "aiff", "aif", "alac", "m4a", "mp3", "aac", "caf"]
        if nativeExtensions.contains(ext) {
            do {
                _ = try AVAudioFile(forReading: url)
                return (url, false)
            } catch {
                // If native reader failed unexpectedly, fall through to FFmpeg fallback
            }
        }

        // Needs FFmpeg transcoding/decoding
        guard let ffmpeg = ffmpegBinary else {
            throw DecoderError.unsupportedFormat(ext)
        }

        let tempDir = FileManager.default.temporaryDirectory.appendingPathComponent("AuralisDecoded", isDirectory: true)
        try? FileManager.default.createDirectory(at: tempDir, withIntermediateDirectories: true)

        let targetFilename = "\(url.deletingPathExtension().lastPathComponent)_\(abs(url.path.hashValue)).wav"
        let outputURL = tempDir.appendingPathComponent(targetFilename)

        if FileManager.default.fileExists(atPath: outputURL.path) {
            return (outputURL, true)
        }

        // Run FFmpeg to decode DSD or exotic codec to uncompressed 24-bit PCM WAV (high-end preservation)
        let process = Process()
        process.executableURL = URL(fileURLWithPath: ffmpeg)
        process.arguments = [
            "-v", "error",
            "-i", url.path,
            "-c:a", "pcm_s24le", // 24-bit uncompressed studio PCM
            "-y",
            outputURL.path
        ]

        let pipe = Pipe()
        process.standardError = pipe
        try process.run()
        process.waitUntilExit()

        guard process.terminationStatus == 0 else {
            let data = pipe.fileHandleForReading.readDataToEndOfFile()
            let errStr = String(data: data, encoding: .utf8) ?? "Unknown FFmpeg error"
            throw DecoderError.conversionFailed(errStr)
        }

        return (outputURL, true)
    }
}
