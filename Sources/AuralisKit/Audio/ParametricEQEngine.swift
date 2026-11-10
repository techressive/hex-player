import Foundation
import AVFoundation

public enum EQMode: String, CaseIterable, Identifiable, Sendable {
    case simple = "Simple Graphic EQ"
    case expert = "Expert Parametric EQ"

    public var id: String { rawValue }
}

public struct EQPreset: Identifiable, Codable, Equatable, Sendable {
    public let id: String
    public let name: String
    public let description: String
    public let bands: [EQBandConfig]
    public let isCustom: Bool

    public init(id: String = UUID().uuidString, name: String, description: String, bands: [EQBandConfig], isCustom: Bool = false) {
        self.id = id
        self.name = name
        self.description = description
        self.bands = bands
        self.isCustom = isCustom
    }
}

public final class ParametricEQEngine: ObservableObject, @unchecked Sendable {
    public static let shared = ParametricEQEngine()

    @Published public var isEnabled: Bool = true {
        didSet { applyToAudioUnit() }
    }
    
    @Published public var mode: EQMode = .simple
    
    // Expert bands (10 parametric bands)
    @Published public var expertBands: [EQBandConfig] = [] {
        didSet { applyToAudioUnit() }
    }
    
    // Simple 7-band sliders (frequency fixed, gain adjustable)
    @Published public var simpleBands: [EQBandConfig] = [] {
        didSet { applyToAudioUnit() }
    }

    // User-created custom presets
    @Published public var userPresets: [EQPreset] = []

    public let eqNode: AVAudioUnitEQ
    public let responseCalculator = BiquadResponseCalculator()

    private let userPresetsStorageKey = "HEX_UserEQPresets_v1"

    public init() {
        // Create an AVAudioUnitEQ with 12 available hardware filter bands
        self.eqNode = AVAudioUnitEQ(numberOfBands: 12)
        
        setupDefaultBands()
        loadUserPresets()
        applyToAudioUnit()
    }

    public func saveCurrentAsPreset(name: String) {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        let activeBands = (mode == .simple) ? simpleBands : expertBands
        let newPreset = EQPreset(
            name: trimmed,
            description: "Custom user preset (\(mode == .simple ? "7-Band Graphic" : "10-Band Parametric"))",
            bands: activeBands,
            isCustom: true
        )
        userPresets.append(newPreset)
        persistUserPresets()
    }

    public func deleteUserPreset(id: String) {
        userPresets.removeAll(where: { $0.id == id })
        persistUserPresets()
    }

    private func loadUserPresets() {
        if let data = UserDefaults.standard.data(forKey: userPresetsStorageKey),
           let saved = try? JSONDecoder().decode([EQPreset].self, from: data) {
            self.userPresets = saved
        }
    }

    private func persistUserPresets() {
        if let data = try? JSONEncoder().encode(userPresets) {
            UserDefaults.standard.set(data, forKey: userPresetsStorageKey)
        }
    }

    private func setupDefaultBands() {
        // Simple 7-Band Graphic Musical EQ (Sub-Bass to Air)
        self.simpleBands = [
            EQBandConfig(name: "32 Hz", type: .lowShelf, frequency: 32.0, gain: 0.0, q: 0.7),
            EQBandConfig(name: "64 Hz", type: .peaking, frequency: 64.0, gain: 0.0, q: 1.0),
            EQBandConfig(name: "250 Hz", type: .peaking, frequency: 250.0, gain: 0.0, q: 1.0),
            EQBandConfig(name: "1 kHz", type: .peaking, frequency: 1000.0, gain: 0.0, q: 1.0),
            EQBandConfig(name: "4 kHz", type: .peaking, frequency: 4000.0, gain: 0.0, q: 1.0),
            EQBandConfig(name: "8 kHz", type: .peaking, frequency: 8000.0, gain: 0.0, q: 1.0),
            EQBandConfig(name: "16 kHz", type: .highShelf, frequency: 16000.0, gain: 0.0, q: 0.7)
        ]

        // Expert 10-Band Parametric EQ
        self.expertBands = [
            EQBandConfig(name: "Sub Cut", type: .highPass, frequency: 20.0, gain: 0.0, q: 0.71),
            EQBandConfig(name: "Low Shelf", type: .lowShelf, frequency: 80.0, gain: 0.0, q: 0.71),
            EQBandConfig(name: "Bass", type: .peaking, frequency: 160.0, gain: 0.0, q: 1.2),
            EQBandConfig(name: "Low Mid", type: .peaking, frequency: 400.0, gain: 0.0, q: 1.4),
            EQBandConfig(name: "Mid", type: .peaking, frequency: 1000.0, gain: 0.0, q: 1.4),
            EQBandConfig(name: "Upper Mid", type: .peaking, frequency: 2500.0, gain: 0.0, q: 1.5),
            EQBandConfig(name: "Presence", type: .peaking, frequency: 5000.0, gain: 0.0, q: 1.5),
            EQBandConfig(name: "Brilliance", type: .peaking, frequency: 8500.0, gain: 0.0, q: 1.8),
            EQBandConfig(name: "High Shelf", type: .highShelf, frequency: 12000.0, gain: 0.0, q: 0.71),
            EQBandConfig(name: "Air Cut", type: .lowPass, frequency: 20000.0, gain: 0.0, q: 0.71)
        ]
    }

    /// Synchronizes current software EQ bands to the hardware AVAudioUnitEQ DSP node
    public func applyToAudioUnit() {
        guard isEnabled else {
            eqNode.bypass = true
            return
        }
        eqNode.bypass = false

        let activeBands = (mode == .simple) ? simpleBands : expertBands
        let count = min(activeBands.count, eqNode.bands.count)

        for i in 0..<count {
            let config = activeBands[i]
            let hwBand = eqNode.bands[i]

            hwBand.bypass = !config.isEnabled
            hwBand.frequency = Float(config.frequency)
            hwBand.gain = Float(config.gain)
            hwBand.bandwidth = Float(1.0 / max(0.05, config.q))

            switch config.type {
            case .peaking:
                hwBand.filterType = .parametric
            case .lowShelf:
                hwBand.filterType = .lowShelf
            case .highShelf:
                hwBand.filterType = .highShelf
            case .lowPass:
                hwBand.filterType = .lowPass
            case .highPass:
                hwBand.filterType = .highPass
            case .notch:
                hwBand.filterType = .bandStop
            case .bandPass:
                hwBand.filterType = .bandPass
            }
        }

        // Bypass any remaining unused hardware bands
        if count < eqNode.bands.count {
            for j in count..<eqNode.bands.count {
                eqNode.bands[j].bypass = true
            }
        }
    }

    public func setSimpleGain(at index: Int, gain: Double) {
        guard index >= 0 && index < simpleBands.count else { return }
        simpleBands[index].gain = max(-24.0, min(24.0, gain))
        applyToAudioUnit()
    }

    public func resetToFlat() {
        for i in 0..<simpleBands.count {
            simpleBands[i].gain = 0.0
        }
        for i in 0..<expertBands.count {
            expertBands[i].gain = 0.0
        }
        applyToAudioUnit()
    }

    public func loadPreset(_ preset: EQPreset) {
        if mode == .simple {
            // Map preset gains to simple bands if compatible, or switch to expert
            if preset.bands.count == simpleBands.count {
                for (idx, b) in preset.bands.enumerated() {
                    simpleBands[idx].gain = b.gain
                }
            } else {
                expertBands = preset.bands
                mode = .expert
            }
        } else {
            expertBands = preset.bands
        }
        applyToAudioUnit()
    }

    // Built-in Audiophile Presets
    public var presets: [EQPreset] {
        return [
            EQPreset(
                id: "flat",
                name: "Audiophile Flat / Bypass",
                description: "Bit-perfect uncolored frequency response straight from master audio.",
                bands: simpleBands.map { var b = $0; b.gain = 0.0; return b }
            ),
            EQPreset(
                id: "harman",
                name: "Harman Target Curve",
                description: "Industry-standard reference curve with controlled bass warmth and crystal vocal clarity.",
                bands: [
                    EQBandConfig(name: "32 Hz", type: .lowShelf, frequency: 32.0, gain: 4.5, q: 0.7),
                    EQBandConfig(name: "64 Hz", type: .peaking, frequency: 64.0, gain: 3.2, q: 1.0),
                    EQBandConfig(name: "250 Hz", type: .peaking, frequency: 250.0, gain: 0.8, q: 1.0),
                    EQBandConfig(name: "1 kHz", type: .peaking, frequency: 1000.0, gain: 0.0, q: 1.0),
                    EQBandConfig(name: "4 kHz", type: .peaking, frequency: 4000.0, gain: 2.5, q: 1.0),
                    EQBandConfig(name: "8 kHz", type: .peaking, frequency: 8000.0, gain: 1.0, q: 1.0),
                    EQBandConfig(name: "16 kHz", type: .highShelf, frequency: 16000.0, gain: 0.5, q: 0.7)
                ]
            ),
            EQPreset(
                id: "warm_tube",
                name: "Analog Tube & Vinyl",
                description: "Rich second-harmonic low-end warmth with gentle silky roll-off on aggressive high frequencies.",
                bands: [
                    EQBandConfig(name: "32 Hz", type: .lowShelf, frequency: 32.0, gain: 2.0, q: 0.7),
                    EQBandConfig(name: "64 Hz", type: .peaking, frequency: 64.0, gain: 3.0, q: 1.0),
                    EQBandConfig(name: "250 Hz", type: .peaking, frequency: 250.0, gain: 1.5, q: 1.0),
                    EQBandConfig(name: "1 kHz", type: .peaking, frequency: 1000.0, gain: 0.0, q: 1.0),
                    EQBandConfig(name: "4 kHz", type: .peaking, frequency: 4000.0, gain: -0.5, q: 1.0),
                    EQBandConfig(name: "8 kHz", type: .peaking, frequency: 8000.0, gain: -1.5, q: 1.0),
                    EQBandConfig(name: "16 kHz", type: .highShelf, frequency: 16000.0, gain: -2.5, q: 0.7)
                ]
            ),
            EQPreset(
                id: "vocal_intimacy",
                name: "Vocal Intimacy & Clarity",
                description: "Brings lead singer front and center, scooping muddiness and accentuating breath detail.",
                bands: [
                    EQBandConfig(name: "32 Hz", type: .lowShelf, frequency: 32.0, gain: -1.0, q: 0.7),
                    EQBandConfig(name: "64 Hz", type: .peaking, frequency: 64.0, gain: -1.0, q: 1.0),
                    EQBandConfig(name: "250 Hz", type: .peaking, frequency: 250.0, gain: -1.5, q: 1.0),
                    EQBandConfig(name: "1 kHz", type: .peaking, frequency: 1000.0, gain: 1.5, q: 1.0),
                    EQBandConfig(name: "4 kHz", type: .peaking, frequency: 4000.0, gain: 3.5, q: 1.0),
                    EQBandConfig(name: "8 kHz", type: .peaking, frequency: 8000.0, gain: 2.0, q: 1.0),
                    EQBandConfig(name: "16 kHz", type: .highShelf, frequency: 16000.0, gain: 1.0, q: 0.7)
                ]
            ),
            EQPreset(
                id: "hd650_correction",
                name: "Sennheiser HD600/HD650 Correction",
                description: "AutoEQ compensation for open-back dynamic headphone sub-bass roll-off and veil.",
                bands: [
                    EQBandConfig(name: "Sub Lift", type: .lowShelf, frequency: 60.0, gain: 5.0, q: 0.71),
                    EQBandConfig(name: "Mid Bass", type: .peaking, frequency: 180.0, gain: -1.5, q: 1.4),
                    EQBandConfig(name: "Mids", type: .peaking, frequency: 1200.0, gain: 0.0, q: 1.0),
                    EQBandConfig(name: "Veil Lift", type: .peaking, frequency: 3200.0, gain: 2.0, q: 1.8),
                    EQBandConfig(name: "Treble Air", type: .highShelf, frequency: 10000.0, gain: 2.5, q: 0.71)
                ]
            )
        ]
    }
}
