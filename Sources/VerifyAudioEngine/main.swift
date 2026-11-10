import Foundation
import AuralisKit

print("=== Running Auralis Audiophile Verification Suite ===")

// 1. Test Biquad Peaking Magnitude
let calc = BiquadResponseCalculator(sampleRate: 48000.0)
let band = EQBandConfig(
    type: .peaking,
    frequency: 1000.0,
    gain: 6.0,
    q: 1.0,
    isEnabled: true
)
let magAtPeak = calc.magnitudeDb(for: band, at: 1000.0)
assert(abs(magAtPeak - 6.0) < 0.2, "Peaking magnitude should match gain (+6.0 dB)")
let magFar = calc.magnitudeDb(for: band, at: 100.0)
assert(abs(magFar) < 0.5, "Far magnitude should be ~0 dB")
print("✅ Test 1 Passed: Biquad Peaking Filter response matches RBJ transfer function (+6.0dB at 1kHz, 0.0dB at 100Hz)")

// 2. Test High-Pass Filter Attenuation
let hpBand = EQBandConfig(
    type: .highPass,
    frequency: 500.0,
    gain: 0.0,
    q: 0.707,
    isEnabled: true
)
let magSub = calc.magnitudeDb(for: hpBand, at: 50.0)
assert(magSub < -20.0, "Sub-bass should be attenuated by 500Hz high pass filter")
let magHigh = calc.magnitudeDb(for: hpBand, at: 5000.0)
assert(abs(magHigh) < 0.5, "Frequencies above cutoff should pass unattenuated")
print("✅ Test 2 Passed: High-Pass Filter achieves \(String(format: "%.1f", magSub)) dB attenuation at 50Hz")

// 3. Test Synchronized LRC Lyrics Parser
let lrcContent = """
[00:00.00]Intro
[00:05.50]First line of high-res lyrics
[00:15.25]Second line
[00:30.00]Final outro
"""
let lines = LyricsService.shared.parseLRC(lrcContent)
assert(lines.count == 4, "Should parse 4 lines")
assert(lines[1].text == "First line of high-res lyrics", "Text should match")
assert(abs(lines[1].time - 5.5) < 0.01, "Timestamp should match 5.5s")
let idxAt6 = LyricsService.shared.activeLineIndex(in: lines, at: 6.0)
assert(idxAt6 == 1, "Active line at 6.0s should be line 1")
print("✅ Test 3 Passed: Synchronized LRC parser parsed 4 timestamps and accurately matched active playback time")

// 4. Test LRCLIB Title Cleaner for Online Search
let noisyTitle = "Time (2011 Remastered) [192kHz/24-bit]"
let cleaned = LyricsService.shared.cleanTitle(noisyTitle)
assert(cleaned == "Time", "Title cleaner should strip remaster tags: got '\(cleaned)'")
print("✅ Test 4 Passed: LRCLIB title cleaner successfully normalized '\(noisyTitle)' -> '\(cleaned)'")

// 5. Test Audio Codec Lossless Flags & Badges
assert(AudioCodec.flac.isLossless, "FLAC is lossless")
assert(AudioCodec.wav.isLossless, "WAV is lossless")
assert(AudioCodec.alac.isLossless, "ALAC is lossless")
assert(AudioCodec.dsd.isLossless, "DSD is lossless")
assert(!AudioCodec.mp3.isLossless, "MP3 is lossy")
assert(!AudioCodec.aac.isLossless, "AAC is lossy")
print("✅ Test 5 Passed: Audio Codec classifications (FLAC, WAV, ALAC, DSD) properly identified as lossless")

// 6. Test Track Bit-Depth and Sample Rate formatting
let track = Track(
    url: URL(fileURLWithPath: "/music/test.flac"),
    title: "Kind of Blue",
    artist: "Miles Davis",
    album: "Kind of Blue",
    duration: 320.0,
    sampleRate: 192000.0,
    bitDepth: 24,
    channels: 2,
    bitRate: 9216000,
    codec: .flac
)
assert(track.formattedSampleRate == "192 kHz", "Sample rate should format to 192 kHz")
assert(track.formattedBitDepth == "24-bit", "Bit depth should format to 24-bit")
assert(track.isHiRes == true, "192kHz/24-bit track should be Hi-Res")
print("✅ Test 6 Passed: Track audiophile specifications: \(track.audiophileBadgeText) [Hi-Res: \(track.isHiRes)]")

// 7. Test AudioDeviceManager Output Device Discovery
let deviceManager = AudioDeviceManager.shared
let devices = deviceManager.availableDevices
assert(!devices.isEmpty, "Should discover at least one audio output device on macOS")
print("✅ Test 7 Passed: CoreAudio discovered \(devices.count) audio output device(s): \(devices.map { $0.name }.joined(separator: ", "))")

// 8. Test Clean AppState Initial State & Library Structured Grouping
@MainActor
func testAppState() async {
    let appState = AppState.shared
    // Add sample tracks in memory to verify grouping
    let t1 = Track(url: URL(fileURLWithPath: "/a.flac"), title: "Song A", artist: "Artist 1", album: "Album 1", genre: "Jazz", duration: 180.0, sampleRate: 96000.0, bitDepth: 24, codec: .flac)
    let t2 = Track(url: URL(fileURLWithPath: "/b.flac"), title: "Song B", artist: "Artist 1", album: "Album 1", genre: "Jazz", duration: 200.0, sampleRate: 96000.0, bitDepth: 24, codec: .flac)
    let t3 = Track(url: URL(fileURLWithPath: "/c.wav"), title: "Song C", artist: "Artist 2", album: "Album 2", genre: "Rock", duration: 220.0, sampleRate: 44100.0, bitDepth: 16, codec: .wav)

    appState.tracks = [t1, t2, t3]
    assert(appState.albums.count == 2, "Should group into 2 albums")
    assert(appState.artists.count == 2, "Should group into 2 artists")
    assert(appState.genres.count == 2, "Should group into 2 genres")
    assert(appState.recentlyAddedTracks.count == 3, "Recently added should have 3 tracks")

    // Clear tracks to leave clean
    appState.clearLibrary()
    assert(appState.tracks.isEmpty, "Library clear should leave tracks empty")
    print("✅ Test 8 Passed: Structured library groups (Albums: 2, Artists: 2, Genres: 2) and clean empty state verified")

    // Test 9: Artwork Color Palette Extraction
    let palette = ArtworkColorExtractor.extractPalette(from: nil)
    assert(palette.primary == ArtworkColorPalette.fallback.primary, "Palette should provide fallback primary color")
    print("✅ Test 9 Passed: Dynamic Artwork Color Palette extractor verified")

    // Test 10: Playback Queue, Shuffle & Repeat state transitions
    assert(appState.repeatMode == .off, "Default repeat mode should be off")
    appState.cycleRepeat()
    assert(appState.repeatMode == .all, "Should cycle to Repeat All")
    appState.cycleRepeat()
    assert(appState.repeatMode == .one, "Should cycle to Repeat One")
    appState.cycleRepeat()
    assert(appState.repeatMode == .off, "Should cycle back to Repeat Off")

    assert(!appState.isShuffleEnabled, "Shuffle should be off by default")
    appState.toggleShuffle()
    assert(appState.isShuffleEnabled, "Shuffle should be enabled")
    appState.toggleShuffle()
    assert(!appState.isShuffleEnabled, "Shuffle should be disabled")

    appState.addToQueue(track: t1)
    assert(appState.queue.count == 1, "Queue should contain 1 track")
    appState.clearQueue()
    assert(appState.queue.isEmpty, "Queue should be empty after clear")
    print("✅ Test 10 Passed: Shuffle, Repeat, and Up Next Queue state transitions verified")
}

await testAppState()

print("\n🎉 ALL 10 AUDIOPHILE & SYSTEM TESTS PASSED WITH 100% SUCCESS!")
