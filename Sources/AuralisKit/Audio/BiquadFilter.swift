import Foundation

public enum FilterType: String, CaseIterable, Identifiable, Codable, Sendable {
    case peaking = "Bell / Peaking"
    case lowShelf = "Low Shelf"
    case highShelf = "High Shelf"
    case lowPass = "Low Pass (Cut)"
    case highPass = "High Pass (Cut)"
    case notch = "Notch"
    case bandPass = "Band Pass"

    public var id: String { rawValue }
    
    public var shortName: String {
        switch self {
        case .peaking: return "Peak"
        case .lowShelf: return "LShelf"
        case .highShelf: return "HShelf"
        case .lowPass: return "LP"
        case .highPass: return "HP"
        case .notch: return "Notch"
        case .bandPass: return "BP"
        }
    }
}

public struct EQBandConfig: Identifiable, Codable, Equatable, Sendable {
    public let id: UUID
    public var name: String
    public var type: FilterType
    public var frequency: Double // 20 Hz to 22,050 Hz
    public var gain: Double      // -24.0 dB to +24.0 dB
    public var q: Double         // 0.1 to 20.0 (quality factor / bandwidth)
    public var isEnabled: Bool

    public init(
        id: UUID = UUID(),
        name: String = "Band",
        type: FilterType = .peaking,
        frequency: Double = 1000.0,
        gain: Double = 0.0,
        q: Double = 1.0,
        isEnabled: Bool = true
    ) {
        self.id = id
        self.name = name
        self.type = type
        self.frequency = frequency
        self.gain = gain
        self.q = q
        self.isEnabled = isEnabled
    }
}

/// Computes the exact transfer function magnitude using standard RBJ Audio EQ Cookbook equations.
public struct BiquadResponseCalculator: Sendable {
    public let sampleRate: Double

    public init(sampleRate: Double = 48000.0) {
        self.sampleRate = sampleRate
    }

    /// Evaluates magnitude in dB at target frequency for a single filter band
    public func magnitudeDb(for band: EQBandConfig, at freq: Double) -> Double {
        guard band.isEnabled else { return 0.0 }
        
        let f0 = max(10.0, min(sampleRate * 0.499, band.frequency))
        let omega = 2.0 * Double.pi * f0 / sampleRate
        let cosOmega = cos(omega)
        let sinOmega = sin(omega)
        let alpha = sinOmega / (2.0 * max(0.05, band.q))
        let A = pow(10.0, band.gain / 40.0) // sqrt(10^(gain/20))

        var b0 = 1.0, b1 = 0.0, b2 = 0.0
        var a0 = 1.0, a1 = 0.0, a2 = 0.0

        switch band.type {
        case .peaking:
            b0 = 1.0 + alpha * A
            b1 = -2.0 * cosOmega
            b2 = 1.0 - alpha * A
            a0 = 1.0 + alpha / A
            a1 = -2.0 * cosOmega
            a2 = 1.0 - alpha / A

        case .lowShelf:
            let twoSqrtAAlpha = 2.0 * sqrt(A) * alpha
            b0 = A * ((A + 1.0) - (A - 1.0) * cosOmega + twoSqrtAAlpha)
            b1 = 2.0 * A * ((A - 1.0) - (A + 1.0) * cosOmega)
            b2 = A * ((A + 1.0) - (A - 1.0) * cosOmega - twoSqrtAAlpha)
            a0 = (A + 1.0) + (A - 1.0) * cosOmega + twoSqrtAAlpha
            a1 = -2.0 * ((A - 1.0) + (A + 1.0) * cosOmega)
            a2 = (A + 1.0) + (A - 1.0) * cosOmega - twoSqrtAAlpha

        case .highShelf:
            let twoSqrtAAlpha = 2.0 * sqrt(A) * alpha
            b0 = A * ((A + 1.0) + (A - 1.0) * cosOmega + twoSqrtAAlpha)
            b1 = -2.0 * A * ((A - 1.0) + (A + 1.0) * cosOmega)
            b2 = A * ((A + 1.0) + (A - 1.0) * cosOmega - twoSqrtAAlpha)
            a0 = (A + 1.0) - (A - 1.0) * cosOmega + twoSqrtAAlpha
            a1 = 2.0 * ((A - 1.0) - (A + 1.0) * cosOmega)
            a2 = (A + 1.0) - (A - 1.0) * cosOmega - twoSqrtAAlpha

        case .lowPass:
            b0 = (1.0 - cosOmega) / 2.0
            b1 = 1.0 - cosOmega
            b2 = (1.0 - cosOmega) / 2.0
            a0 = 1.0 + alpha
            a1 = -2.0 * cosOmega
            a2 = 1.0 - alpha

        case .highPass:
            b0 = (1.0 + cosOmega) / 2.0
            b1 = -(1.0 + cosOmega)
            b2 = (1.0 + cosOmega) / 2.0
            a0 = 1.0 + alpha
            a1 = -2.0 * cosOmega
            a2 = 1.0 - alpha

        case .notch:
            b0 = 1.0
            b1 = -2.0 * cosOmega
            b2 = 1.0
            a0 = 1.0 + alpha
            a1 = -2.0 * cosOmega
            a2 = 1.0 - alpha

        case .bandPass:
            b0 = alpha
            b1 = 0.0
            b2 = -alpha
            a0 = 1.0 + alpha
            a1 = -2.0 * cosOmega
            a2 = 1.0 - alpha
        }

        // Normalize by a0
        let nb0 = b0 / a0
        let nb1 = b1 / a0
        let nb2 = b2 / a0
        let na1 = a1 / a0
        let na2 = a2 / a0

        // Frequency response at evaluation frequency
        let w = 2.0 * Double.pi * freq / sampleRate
        let phi = 4.0 * pow(sin(w / 2.0), 2.0) // 2*(1 - cos(w))

        // Numerator magnitude squared
        let num = pow(nb0 + nb1 + nb2, 2.0) - (nb0 * nb1 + 4.0 * nb0 * nb2 + nb1 * nb2) * phi + nb0 * nb2 * pow(phi, 2.0)
        // Denominator magnitude squared
        let den = pow(1.0 + na1 + na2, 2.0) - (na1 + 4.0 * na2 + na1 * na2) * phi + na2 * pow(phi, 2.0)

        guard den > 1e-12, num > 1e-12 else { return -60.0 }
        let magSq = max(1e-10, num / den)
        return 10.0 * log10(magSq)
    }

    /// Evaluates total cumulative response across all active bands
    public func totalMagnitudeDb(for bands: [EQBandConfig], at freq: Double) -> Double {
        var totalDb = 0.0
        for band in bands {
            totalDb += magnitudeDb(for: band, at: freq)
        }
        return totalDb
    }
}
