import SwiftUI

public struct SimpleEQView: View {
    @ObservedObject var eqEngine = ParametricEQEngine.shared

    public init() {}

    public var body: some View {
        VStack(spacing: 20) {
            // Quick preset bar
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(eqEngine.presets) { preset in
                        Button(action: {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                eqEngine.loadPreset(preset)
                            }
                        }) {
                            Text(preset.name)
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundColor(AuralisTheme.textPrimary)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(AuralisTheme.cardSurface)
                                .cornerRadius(8)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8)
                                        .stroke(AuralisTheme.cardBorder, lineWidth: 1)
                                )
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
                .padding(.horizontal, 4)
            }

            // 7-Band Graphic Vertical Sliders
            HStack(spacing: 16) {
                ForEach(0..<eqEngine.simpleBands.count, id: \.self) { idx in
                    let band = eqEngine.simpleBands[idx]
                    VStack(spacing: 8) {
                        // Gain readout
                        Text(formatGain(band.gain))
                            .font(.system(size: 10, weight: .bold, design: .monospaced))
                            .foregroundColor(band.gain != 0 ? AuralisTheme.gold : AuralisTheme.textMuted)
                            .frame(height: 16)

                        // Vertical Slider Container
                        GeometryReader { sliderGeo in
                            let totalHeight = sliderGeo.size.height
                            let clampedGain = max(-12.0, min(12.0, band.gain))
                            // Map -12...+12 to fraction 0...1 (0 = bottom, 1 = top)
                            let fraction = (clampedGain + 12.0) / 24.0
                            let knobY = totalHeight * (1.0 - CGFloat(fraction))

                            ZStack(alignment: .top) {
                                // Background track
                                Capsule()
                                    .fill(AuralisTheme.cardBorder)
                                    .frame(width: 4, height: totalHeight)

                                // Center 0dB detent marker
                                Rectangle()
                                    .fill(Color.white.opacity(0.2))
                                    .frame(width: 14, height: 1)
                                    .offset(y: totalHeight * 0.5)

                                // Active fill from center
                                let centerOffset = totalHeight * 0.5
                                let fillHeight = abs(knobY - centerOffset)
                                let fillY = min(knobY, centerOffset)

                                Capsule()
                                    .fill(AuralisTheme.gold.opacity(0.85))
                                    .frame(width: 4, height: fillHeight)
                                    .offset(y: fillY)

                                // Draggable Knob
                                Circle()
                                    .fill(AuralisTheme.gold)
                                    .frame(width: 16, height: 16)
                                    .shadow(color: AuralisTheme.gold.opacity(0.6), radius: 4)
                                    .offset(y: max(0, min(totalHeight - 16, knobY - 8)))
                            }
                            .frame(maxWidth: .infinity)
                            .contentShape(Rectangle())
                            .gesture(
                                DragGesture(minimumDistance: 0)
                                    .onChanged { drag in
                                        let y = drag.location.y
                                        let rawFraction = 1.0 - (y / totalHeight)
                                        let newGain = -12.0 + (rawFraction * 24.0)
                                        // Snap to 0 within threshold
                                        let finalGain = abs(newGain) < 0.35 ? 0.0 : newGain
                                        eqEngine.setSimpleGain(at: idx, gain: Double(finalGain))
                                    }
                            )
                        }
                        .frame(height: 160)

                        // Frequency Label
                        Text(band.name)
                            .font(.system(size: 11, weight: .semibold, design: .monospaced))
                            .foregroundColor(AuralisTheme.textSecondary)

                        // Double click / tap to reset band
                        Button(action: {
                            eqEngine.setSimpleGain(at: idx, gain: 0.0)
                        }) {
                            Text("RESET")
                                .font(.system(size: 8, weight: .bold, design: .monospaced))
                                .foregroundColor(AuralisTheme.textMuted)
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .pitchBlackCard()
                }
            }

            // Bottom controls
            HStack {
                Button(action: {
                    withAnimation {
                        eqEngine.resetToFlat()
                    }
                }) {
                    HStack(spacing: 6) {
                        Image(systemName: "arrow.counterclockwise")
                        Text("Reset All to Flat (0 dB)")
                    }
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(AuralisTheme.textSecondary)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .pitchBlackCard()
                }
                .buttonStyle(PlainButtonStyle())

                Spacer()

                Text("7-Band Audiophile Direct Filter Graph")
                    .font(.system(size: 11, weight: .medium, design: .monospaced))
                    .foregroundColor(AuralisTheme.textMuted)
            }
        }
        .padding(16)
    }

    private func formatGain(_ gain: Double) -> String {
        if abs(gain) < 0.05 { return "0 dB" }
        return String(format: "%+.1f dB", gain)
    }
}
