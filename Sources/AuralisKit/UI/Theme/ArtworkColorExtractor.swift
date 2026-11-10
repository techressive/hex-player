import Foundation
import SwiftUI
import AppKit

public struct ArtworkColorPalette: Sendable {
    public let primary: Color
    public let secondary: Color
    public let ambient: Color

    public static let fallback = ArtworkColorPalette(
        primary: Color(red: 0.15, green: 0.12, blue: 0.08),
        secondary: Color(red: 0.08, green: 0.08, blue: 0.12),
        ambient: Color(red: 0.04, green: 0.04, blue: 0.05)
    )
}

public enum ArtworkColorExtractor {
    /// Extracts a rich, low-luminance color palette from an NSImage for subtle OLED background glow
    public static func extractPalette(from image: NSImage?) -> ArtworkColorPalette {
        guard let image = image,
              let cgImage = image.cgImage(forProposedRect: nil, context: nil, hints: nil) else {
            return .fallback
        }

        // Downsample to a 16x16 thumbnail for instantaneous extraction (<1ms)
        let width = 16
        let height = 16
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        var rawData = [UInt8](repeating: 0, count: width * height * 4)
        guard let context = CGContext(
            data: &rawData,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: width * 4,
            space: colorSpace,
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        ) else {
            return .fallback
        }

        context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))

        var totalR: Double = 0
        var totalG: Double = 0
        var totalB: Double = 0
        var pixelCount: Double = 0

        var vibrantR: Double = 0
        var vibrantG: Double = 0
        var vibrantB: Double = 0
        var maxSat: Double = 0

        var secondaryR: Double = 0
        var secondaryG: Double = 0
        var secondaryB: Double = 0
        var maxBrightness: Double = 0

        for y in 0..<height {
            for x in 0..<width {
                let offset = (y * width + x) * 4
                let r = Double(rawData[offset]) / 255.0
                let g = Double(rawData[offset + 1]) / 255.0
                let b = Double(rawData[offset + 2]) / 255.0
                let a = Double(rawData[offset + 3]) / 255.0

                if a < 0.5 { continue }

                totalR += r
                totalG += g
                totalB += b
                pixelCount += 1.0

                // Calculate saturation & brightness
                let maxC = max(r, max(g, b))
                let minC = min(r, min(g, b))
                let delta = maxC - minC
                let sat = maxC == 0 ? 0 : delta / maxC
                let bri = maxC

                // Vibrant color: highest saturation
                if sat > maxSat && bri > 0.15 && bri < 0.95 {
                    maxSat = sat
                    vibrantR = r
                    vibrantG = g
                    vibrantB = b
                }

                // Secondary color: highest brightness that is not too close to vibrant
                if bri > maxBrightness && sat > 0.1 {
                    maxBrightness = bri
                    secondaryR = r
                    secondaryG = g
                    secondaryB = b
                }
            }
        }

        if pixelCount == 0 { return .fallback }

        let avgR = totalR / pixelCount
        let avgG = totalG / pixelCount
        let avgB = totalB / pixelCount

        // Use vibrant color or average if saturation was negligible
        let prR = maxSat > 0.15 ? vibrantR : avgR
        let prG = maxSat > 0.15 ? vibrantG : avgG
        let prB = maxSat > 0.15 ? vibrantB : avgB

        let secR = secondaryR > 0 ? secondaryR : (avgR * 0.7)
        let secG = secondaryG > 0 ? secondaryG : (avgG * 0.7)
        let secB = secondaryB > 0 ? secondaryB : (avgB * 0.7)

        return ArtworkColorPalette(
            primary: Color(red: prR, green: prG, blue: prB),
            secondary: Color(red: secR, green: secG, blue: secB),
            ambient: Color(red: avgR * 0.4, green: avgG * 0.4, blue: avgB * 0.4)
        )
    }
}
