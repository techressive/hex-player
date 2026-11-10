import Foundation
import Accelerate
import AVFoundation

public final class SpectrumAnalyzer: ObservableObject, @unchecked Sendable {
    public static let shared = SpectrumAnalyzer()

    // 64 logarithmic frequency bands normalized 0.0 ... 1.0 for UI visualizer
    @Published public private(set) var bands: [Float] = Array(repeating: 0.0, count: 64)
    
    private let fftSize: Int = 1024
    private let log2n: vDSP_Length
    private var fftSetup: FFTSetup?
    private var hannWindow: [Float]
    
    // Internal decay buffers for smooth analog VU-style needle decay
    private var smoothedBands: [Float]
    private var lastDispatchTime: CFAbsoluteTime = 0
    private let lock = NSLock()

    public init() {
        self.log2n = vDSP_Length(log2(Double(fftSize)))
        self.fftSetup = vDSP_create_fftsetup(log2n, FFTRadix(kFFTRadix2))
        
        var window = [Float](repeating: 0, count: fftSize)
        vDSP_hann_window(&window, vDSP_Length(fftSize), Int32(vDSP_HANN_NORM))
        self.hannWindow = window
        self.smoothedBands = Array(repeating: 0.0, count: 64)
    }

    deinit {
        if let setup = fftSetup {
            vDSP_destroy_fftsetup(setup)
        }
    }

    /// Process an incoming audio buffer from AVAudioNode tap
    public func process(buffer: AVAudioPCMBuffer) {
        guard AudioEngineController.shared.isPlaying else {
            // When stopped or paused, zero out bands once and sleep
            lock.lock()
            let needsClear = !smoothedBands.allSatisfy({ $0 < 0.001 })
            if needsClear {
                smoothedBands = Array(repeating: 0.0, count: 64)
            }
            lock.unlock()

            if needsClear {
                DispatchQueue.main.async { [weak self] in
                    self?.bands = Array(repeating: 0.0, count: 64)
                }
            }
            return
        }

        let now = CFAbsoluteTimeGetCurrent()
        guard now - lastDispatchTime >= 0.033 else { return } // 30 FPS throttle
        lastDispatchTime = now

        guard let channelData = buffer.floatChannelData?[0],
              let fftSetup = self.fftSetup else { return }

        let frameCount = Int(buffer.frameLength)
        guard frameCount >= fftSize else { return }

        // Window the incoming audio samples
        var windowed = [Float](repeating: 0, count: fftSize)
        vDSP_vmul(channelData, 1, hannWindow, 1, &windowed, 1, vDSP_Length(fftSize))

        // Prepare complex split vector with explicit buffer pointers
        var realp = [Float](repeating: 0, count: fftSize / 2)
        var imagp = [Float](repeating: 0, count: fftSize / 2)
        var magnitudes = [Float](repeating: 0, count: fftSize / 2)

        realp.withUnsafeMutableBufferPointer { rPtr in
            imagp.withUnsafeMutableBufferPointer { iPtr in
                var split = DSPSplitComplex(realp: rPtr.baseAddress!, imagp: iPtr.baseAddress!)

                windowed.withUnsafeBufferPointer { wPtr in
                    wPtr.baseAddress!.withMemoryRebound(to: DSPComplex.self, capacity: fftSize / 2) { complexPtr in
                        vDSP_ctoz(complexPtr, 2, &split, 1, vDSP_Length(fftSize / 2))
                    }
                }

                vDSP_fft_zrip(fftSetup, &split, 1, log2n, FFTDirection(FFT_FORWARD))
                vDSP_zvmags(&split, 1, &magnitudes, 1, vDSP_Length(fftSize / 2))
            }
        }

        // Downsample into 64 logarithmic frequency bands
        let numBands = 64
        var newBands = [Float](repeating: 0.0, count: numBands)
        let sampleRate = Float(buffer.format.sampleRate)
        let binWidth = sampleRate / Float(fftSize)

        for i in 0..<numBands {
            // Logarithmic mapping between 20 Hz and 20 kHz
            let lowFreq = 20.0 * pow(1000.0, Float(i) / Float(numBands))
            let highFreq = 20.0 * pow(1000.0, Float(i + 1) / Float(numBands))
            
            let lowBin = max(0, min(fftSize / 2 - 1, Int(lowFreq / binWidth)))
            let highBin = max(lowBin + 1, min(fftSize / 2, Int(highFreq / binWidth)))

            var bandEnergy: Float = 0.0
            for bin in lowBin..<highBin {
                bandEnergy += magnitudes[bin]
            }
            bandEnergy /= Float(max(1, highBin - lowBin))

            // Convert to dB scale with threshold
            let db = 10.0 * log10(max(1e-6, bandEnergy))
            // Map -70 dB ... 0 dB to 0.0 ... 1.0
            let normalized = max(0.0, min(1.0, (db + 70.0) / 70.0))
            newBands[i] = normalized
        }

        lock.lock()
        for i in 0..<numBands {
            if newBands[i] > smoothedBands[i] {
                // Instant rise (peak attack)
                smoothedBands[i] = newBands[i]
            } else {
                // Smooth exponential release decay (analog ballistics)
                smoothedBands[i] = smoothedBands[i] * 0.88 + newBands[i] * 0.12
            }
        }
        let finalBands = smoothedBands
        lock.unlock()

        DispatchQueue.main.async { [weak self] in
            self?.bands = finalBands
        }
    }
}
