import Foundation
import AVFoundation

public final class AudioEngineController: ObservableObject, @unchecked Sendable {
    public static let shared = AudioEngineController()

    @Published public private(set) var isPlaying: Bool = false
    @Published public private(set) var currentTime: TimeInterval = 0.0
    @Published public private(set) var duration: TimeInterval = 0.0
    @Published public var volume: Float = 1.0 {
        didSet {
            playerNode.volume = max(0.0, min(1.0, volume))
        }
    }
    @Published public private(set) var currentTrack: Track?
    @Published public private(set) var engineError: String?

    private let engine = AVAudioEngine()
    private let playerNode = AVAudioPlayerNode()
    private let eqEngine = ParametricEQEngine.shared
    private let spectrumAnalyzer = SpectrumAnalyzer.shared
    private let decoder = UniversalDecoder.shared
    public let deviceManager = AudioDeviceManager.shared

    private var audioFile: AVAudioFile?
    private var seekFrameOffset: AVAudioFramePosition = 0
    private var audioSampleRate: Double = 44100.0
    private var totalFrames: AVAudioFramePosition = 0

    private var displayLinkTimer: Timer?
    private let stateLock = NSLock()

    public init() {
        setupEngine()
    }

    private func setupEngine() {
        engine.attach(playerNode)
        engine.attach(eqEngine.eqNode)

        // Wire nodes: playerNode -> AVAudioUnitEQ -> mainMixerNode -> outputNode
        engine.connect(playerNode, to: eqEngine.eqNode, format: nil)
        engine.connect(eqEngine.eqNode, to: engine.mainMixerNode, format: nil)

        // Install FFT analysis tap on mixer node for 60fps spectrum visualizer
        let mixerFormat = engine.mainMixerNode.outputFormat(forBus: 0)
        engine.mainMixerNode.installTap(onBus: 0, bufferSize: 1024, format: mixerFormat) { [weak self] buffer, _ in
            self?.spectrumAnalyzer.process(buffer: buffer)
        }

        do {
            try engine.start()
        } catch {
            print("[AudioEngine] Error starting AVAudioEngine: \(error.localizedDescription)")
            self.engineError = error.localizedDescription
        }

        startProgressTimer()
    }

    public func loadAndPlay(track: Track) {
        stateLock.lock()
        defer { stateLock.unlock() }

        stopInternal()
        self.currentTrack = track

        do {
            let prep = try decoder.prepareFileForPlayback(url: track.url)
            let file = try AVAudioFile(forReading: prep.playbackURL)
            self.audioFile = file
            self.audioSampleRate = file.processingFormat.sampleRate
            self.totalFrames = file.length
            self.duration = Double(totalFrames) / audioSampleRate
            self.seekFrameOffset = 0
            self.currentTime = 0.0

            scheduleAndPlay(fromFrame: 0)
            self.engineError = nil
        } catch {
            print("[AudioEngine] Failed to load track: \(error)")
            DispatchQueue.main.async {
                self.engineError = error.localizedDescription
            }
        }
    }

    public func loadCurrentTrackMetadata(_ track: Track) {
        stateLock.lock()
        defer { stateLock.unlock() }
        if self.currentTrack?.id == track.id {
            DispatchQueue.main.async {
                self.currentTrack = track
            }
        }
    }

    private func scheduleAndPlay(fromFrame: AVAudioFramePosition) {
        guard let file = audioFile else { return }

        if !engine.isRunning {
            try? engine.start()
        }

        playerNode.stop()

        let remainingFrames = max(0, totalFrames - fromFrame)
        guard remainingFrames > 0 else { return }

        seekFrameOffset = fromFrame
        playerNode.scheduleSegment(
            file,
            startingFrame: fromFrame,
            frameCount: AVAudioFrameCount(remainingFrames),
            at: nil
        ) { [weak self] in
            guard let self = self else { return }
            DispatchQueue.main.async {
                if self.isPlaying && self.currentTime >= self.duration - 0.5 {
                    self.isPlaying = false
                    NotificationCenter.default.post(name: .trackDidFinishPlaying, object: nil)
                }
            }
        }

        playerNode.play()
        DispatchQueue.main.async {
            self.isPlaying = true
        }
    }

    public func togglePlayPause() {
        guard audioFile != nil else { return }
        if isPlaying {
            pause()
        } else {
            resume()
        }
    }

    public func pause() {
        guard isPlaying else { return }
        playerNode.pause()
        isPlaying = false
        displayLinkTimer?.invalidate()
        displayLinkTimer = nil
    }

    public func resume() {
        guard !isPlaying, audioFile != nil else { return }
        if !engine.isRunning {
            try? engine.start()
        }
        playerNode.play()
        isPlaying = true
        startProgressTimer()
    }

    public func setOutputDevice(_ device: AudioOutputDevice) {
        stateLock.lock()
        defer { stateLock.unlock() }

        let wasPlaying = isPlaying
        if wasPlaying {
            playerNode.pause()
        }
        engine.pause()
        deviceManager.selectDevice(device, on: engine)
        do {
            try engine.start()
            if wasPlaying {
                playerNode.play()
            }
        } catch {
            print("[AudioEngine] Error restarting engine after device change: \(error)")
        }
    }

    public func stop() {
        stateLock.lock()
        defer { stateLock.unlock() }
        stopInternal()
    }

    private func stopInternal() {
        playerNode.stop()
        isPlaying = false
        currentTime = 0.0
        seekFrameOffset = 0
        displayLinkTimer?.invalidate()
        displayLinkTimer = nil
    }

    public func seek(to time: TimeInterval) {
        stateLock.lock()
        defer { stateLock.unlock() }

        guard audioFile != nil, duration > 0 else { return }
        let clampedTime = max(0.0, min(duration, time))
        let targetFrame = AVAudioFramePosition(clampedTime * audioSampleRate)

        let wasPlaying = isPlaying
        playerNode.stop()
        scheduleAndPlay(fromFrame: targetFrame)

        if !wasPlaying {
            playerNode.pause()
            DispatchQueue.main.async {
                self.isPlaying = false
            }
            displayLinkTimer?.invalidate()
            displayLinkTimer = nil
        }
        self.currentTime = clampedTime
    }

    private func startProgressTimer() {
        displayLinkTimer?.invalidate()
        displayLinkTimer = Timer.scheduledTimer(withTimeInterval: 0.08, repeats: true) { [weak self] _ in
            guard let self = self, self.isPlaying,
                  let nodeTime = self.playerNode.lastRenderTime,
                  nodeTime.isSampleTimeValid,
                  let playerTime = self.playerNode.playerTime(forNodeTime: nodeTime) else { return }

            let currentFrame = self.seekFrameOffset + playerTime.sampleTime
            let computedTime = Double(currentFrame) / self.audioSampleRate
            let clamped = max(0.0, min(self.duration, computedTime))

            if abs(clamped - self.currentTime) >= 0.06 {
                DispatchQueue.main.async {
                    self.currentTime = clamped
                }
            }
        }
    }
}

public extension Notification.Name {
    static let trackDidFinishPlaying = Notification.Name("AuralisTrackDidFinishPlaying")
}
