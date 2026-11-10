import SwiftUI

public final class ExpertEQState: ObservableObject {
    @Published public var selectedBandIndex: Int = 1
    @Published public var hoveredBandIndex: Int? = nil
    public init() {}
}

public struct ExpertParametricEQView: View {
    @ObservedObject var eqEngine = ParametricEQEngine.shared
    @ObservedObject var analyzer = SpectrumAnalyzer.shared
    @StateObject private var ui = ExpertEQState()

    private let minFreq: Double = 20.0
    private let maxFreq: Double = 20000.0
    private let minDb: Double = -24.0
    private let maxDb: Double = 24.0

    public init() {}

    public var body: some View {
        VStack(spacing: 16) {
            // Interactive Curve Canvas
            GeometryReader { geo in
                let w = geo.size.width
                let h = geo.size.height

                ZStack {
                    // Deep black canvas background
                    AuralisTheme.cardSurface

                    // Frequency & dB Gridlines
                    CurveGrid(w: w, h: h, minFreq: minFreq, maxFreq: maxFreq, minDb: minDb, maxDb: maxDb)

                    // Real-Time FFT Spectrum Glow in Background
                    FFTSpectrumBackground(bands: analyzer.bands, w: w, h: h)

                    // Mathematical Biquad Response Curve
                    ResponseCurvePath(
                        bands: eqEngine.expertBands,
                        calculator: eqEngine.responseCalculator,
                        w: w,
                        h: h,
                        minFreq: minFreq,
                        maxFreq: maxFreq,
                        minDb: minDb,
                        maxDb: maxDb
                    )
                    .stroke(
                        LinearGradient(
                            colors: [AuralisTheme.cyanHiRes, AuralisTheme.gold],
                            startPoint: .leading,
                            endPoint: .trailing
                        ),
                        style: StrokeStyle(lineWidth: 2.5, lineCap: .round, lineJoin: .round)
                    )
                    .shadow(color: AuralisTheme.gold.opacity(0.4), radius: 6)

                    // Draggable Node Handles
                    ForEach(0..<eqEngine.expertBands.count, id: \.self) { idx in
                        let band = eqEngine.expertBands[idx]
                        let pt = pointFor(band: band, in: CGSize(width: w, height: h))
                        let isSelected = ui.selectedBandIndex == idx

                        ZStack {
                            Circle()
                                .fill(isSelected ? AuralisTheme.gold : AuralisTheme.cardSurface)
                                .frame(width: isSelected ? 22 : 18, height: isSelected ? 22 : 18)
                                .overlay(
                                    Circle()
                                        .stroke(
                                            isSelected ? Color.white : (band.isEnabled ? AuralisTheme.gold : AuralisTheme.textMuted),
                                            lineWidth: isSelected ? 2.5 : 1.5
                                        )
                                )
                                .shadow(color: AuralisTheme.gold.opacity(isSelected ? 0.8 : 0.2), radius: isSelected ? 8 : 4)

                            Text("\(idx + 1)")
                                .font(.system(size: 9, weight: .bold, design: .monospaced))
                                .foregroundColor(isSelected ? .black : (band.isEnabled ? AuralisTheme.textPrimary : AuralisTheme.textMuted))
                        }
                        .position(pt)
                        .contentShape(Circle().size(width: 30, height: 30))
                        .gesture(
                            DragGesture()
                                .onChanged { value in
                                    ui.selectedBandIndex = idx
                                    let newFreq = freqFor(x: value.location.x, width: w)
                                    let newGain = gainFor(y: value.location.y, height: h)
                                    
                                    var updated = eqEngine.expertBands[idx]
                                    updated.frequency = newFreq
                                    // For cut filters, gain is locked to 0
                                    if updated.type != .lowPass && updated.type != .highPass {
                                        updated.gain = newGain
                                    }
                                    eqEngine.expertBands[idx] = updated
                                }
                        )
                        .onTapGesture {
                            ui.selectedBandIndex = idx
                        }
                    }
                }
                .cornerRadius(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(AuralisTheme.cardBorder, lineWidth: 1)
                )
            }
            .frame(height: 240)

            // Fine Inspection Parameter Controls for Selected Band
            if ui.selectedBandIndex >= 0 && ui.selectedBandIndex < eqEngine.expertBands.count {
                let currentBand = eqEngine.expertBands[ui.selectedBandIndex]
                
                HStack(spacing: 16) {
                    // Band selector pills
                    VStack(alignment: .leading, spacing: 6) {
                        Text("ACTIVE BAND")
                            .font(.system(size: 9, weight: .bold, design: .monospaced))
                            .foregroundColor(AuralisTheme.textMuted)

                        HStack(spacing: 4) {
                            ForEach(0..<eqEngine.expertBands.count, id: \.self) { idx in
                                Button(action: { ui.selectedBandIndex = idx }) {
                                    Text("\(idx + 1)")
                                        .font(.system(size: 10, weight: .bold, design: .monospaced))
                                        .frame(width: 24, height: 24)
                                        .background(ui.selectedBandIndex == idx ? AuralisTheme.gold : AuralisTheme.cardSurface)
                                        .foregroundColor(ui.selectedBandIndex == idx ? .black : AuralisTheme.textSecondary)
                                        .cornerRadius(4)
                                }
                                .buttonStyle(PlainButtonStyle())
                            }
                        }
                    }

                    Divider()
                        .frame(height: 36)
                        .background(AuralisTheme.cardBorder)

                    // Filter Type Picker
                    VStack(alignment: .leading, spacing: 4) {
                        Text("FILTER TYPE")
                            .font(.system(size: 9, weight: .bold, design: .monospaced))
                            .foregroundColor(AuralisTheme.textMuted)

                        Picker("", selection: Binding(
                            get: { currentBand.type },
                            set: { newType in
                                var b = currentBand
                                b.type = newType
                                eqEngine.expertBands[ui.selectedBandIndex] = b
                            }
                        )) {
                            ForEach(FilterType.allCases) { type in
                                Text(type.rawValue).tag(type)
                            }
                        }
                        .pickerStyle(MenuPickerStyle())
                        .frame(width: 140)
                    }

                    // Frequency Slider
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text("FREQ")
                                .font(.system(size: 9, weight: .bold, design: .monospaced))
                                .foregroundColor(AuralisTheme.textMuted)
                            Spacer()
                            Text(formatFrequency(currentBand.frequency))
                                .font(.system(size: 11, weight: .bold, design: .monospaced))
                                .foregroundColor(AuralisTheme.gold)
                        }

                        Slider(
                            value: Binding(
                                get: { log10(max(minFreq, min(maxFreq, currentBand.frequency))) },
                                set: { logVal in
                                    var b = currentBand
                                    b.frequency = pow(10.0, logVal)
                                    eqEngine.expertBands[ui.selectedBandIndex] = b
                                }
                            ),
                            in: log10(minFreq)...log10(maxFreq)
                        )
                        .frame(width: 120)
                    }

                    // Gain Slider
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text("GAIN")
                                .font(.system(size: 9, weight: .bold, design: .monospaced))
                                .foregroundColor(AuralisTheme.textMuted)
                            Spacer()
                            Text(String(format: "%+.1f dB", currentBand.gain))
                                .font(.system(size: 11, weight: .bold, design: .monospaced))
                                .foregroundColor(currentBand.gain != 0 ? AuralisTheme.gold : AuralisTheme.textSecondary)
                        }

                        Slider(
                            value: Binding(
                                get: { currentBand.gain },
                                set: { newGain in
                                    var b = currentBand
                                    b.gain = newGain
                                    eqEngine.expertBands[ui.selectedBandIndex] = b
                                }
                            ),
                            in: -24.0...24.0
                        )
                        .frame(width: 110)
                        .disabled(currentBand.type == .lowPass || currentBand.type == .highPass)
                    }

                    // Q Factor Slider
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text("Q FACTOR")
                                .font(.system(size: 9, weight: .bold, design: .monospaced))
                                .foregroundColor(AuralisTheme.textMuted)
                            Spacer()
                            Text(String(format: "%.2f", currentBand.q))
                                .font(.system(size: 11, weight: .bold, design: .monospaced))
                                .foregroundColor(AuralisTheme.textSecondary)
                        }

                        Slider(
                            value: Binding(
                                get: { currentBand.q },
                                set: { newQ in
                                    var b = currentBand
                                    b.q = newQ
                                    eqEngine.expertBands[ui.selectedBandIndex] = b
                                }
                            ),
                            in: 0.1...15.0
                        )
                        .frame(width: 100)
                    }

                    // Enable / Bypass Toggle
                    Button(action: {
                        var b = currentBand
                        b.isEnabled.toggle()
                        eqEngine.expertBands[ui.selectedBandIndex] = b
                    }) {
                        Image(systemName: currentBand.isEnabled ? "power.circle.fill" : "power.circle")
                            .font(.system(size: 20))
                            .foregroundColor(currentBand.isEnabled ? AuralisTheme.gold : AuralisTheme.textMuted)
                    }
                    .buttonStyle(PlainButtonStyle())
                }
                .padding(12)
                .pitchBlackCard()
            }
        }
        .padding(16)
    }

    private func pointFor(band: EQBandConfig, in size: CGSize) -> CGPoint {
        let xFrac = (log10(max(minFreq, min(maxFreq, band.frequency))) - log10(minFreq)) / (log10(maxFreq) - log10(minFreq))
        let clampedGain = max(minDb, min(maxDb, band.gain))
        let yFrac = 1.0 - ((clampedGain - minDb) / (maxDb - minDb))
        return CGPoint(x: size.width * CGFloat(xFrac), y: size.height * CGFloat(yFrac))
    }

    private func freqFor(x: CGFloat, width: CGFloat) -> Double {
        let frac = max(0.0, min(1.0, Double(x / width)))
        let logMin = log10(minFreq)
        let logMax = log10(maxFreq)
        return pow(10.0, logMin + frac * (logMax - logMin))
    }

    private func gainFor(y: CGFloat, height: CGFloat) -> Double {
        let frac = max(0.0, min(1.0, Double(y / height)))
        let rawGain = maxDb - (frac * (maxDb - minDb))
        return Double(round(rawGain * 10.0) / 10.0)
    }

    private func formatFrequency(_ freq: Double) -> String {
        if freq >= 1000 {
            return String(format: "%.1f kHz", freq / 1000.0)
        }
        return "\(Int(freq)) Hz"
    }
}

// Background Grid with Freq & dB Labels
private struct CurveGrid: View {
    let w: CGFloat
    let h: CGFloat
    let minFreq: Double
    let maxFreq: Double
    let minDb: Double
    let maxDb: Double

    private let freqMarkers: [Double] = [50, 100, 200, 500, 1000, 2000, 5000, 10000]
    private let dbMarkers: [Double] = [18, 12, 6, 0, -6, -12, -18]

    var body: some View {
        ZStack {
            // Horizontal dB gridlines
            ForEach(dbMarkers, id: \.self) { db in
                let yFrac = 1.0 - ((db - minDb) / (maxDb - minDb))
                let y = h * CGFloat(yFrac)

                Path { path in
                    path.move(to: CGPoint(x: 0, y: y))
                    path.addLine(to: CGPoint(x: w, y: y))
                }
                .stroke(
                    db == 0 ? Color.white.opacity(0.2) : Color.white.opacity(0.04),
                    style: StrokeStyle(lineWidth: db == 0 ? 1.0 : 0.5, dash: db == 0 ? [] : [4, 4])
                )

                Text(String(format: "%+d dB", Int(db)))
                    .font(.system(size: 8, weight: .medium, design: .monospaced))
                    .foregroundColor(db == 0 ? AuralisTheme.textSecondary : AuralisTheme.textMuted)
                    .position(x: 24, y: y - 7)
            }

            // Vertical Frequency gridlines
            ForEach(freqMarkers, id: \.self) { freq in
                let xFrac = (log10(freq) - log10(minFreq)) / (log10(maxFreq) - log10(minFreq))
                let x = w * CGFloat(xFrac)

                Path { path in
                    path.move(to: CGPoint(x: x, y: 0))
                    path.addLine(to: CGPoint(x: x, y: h))
                }
                .stroke(Color.white.opacity(0.04), style: StrokeStyle(lineWidth: 0.5, dash: [4, 4]))

                Text(freq >= 1000 ? "\(Int(freq / 1000))k" : "\(Int(freq))")
                    .font(.system(size: 8, weight: .medium, design: .monospaced))
                    .foregroundColor(AuralisTheme.textMuted)
                    .position(x: x, y: h - 8)
            }
        }
    }
}

// Live FFT Spectrum Behind the EQ Curve
private struct FFTSpectrumBackground: View {
    let bands: [Float]
    let w: CGFloat
    let h: CGFloat

    var body: some View {
        GeometryReader { _ in
            Path { path in
                let count = bands.count
                guard count > 0 else { return }

                path.move(to: CGPoint(x: 0, y: h))
                for i in 0..<count {
                    let x = w * (CGFloat(i) / CGFloat(count - 1))
                    let energy = CGFloat(bands[i])
                    let y = h - (energy * h * 0.75)
                    path.addLine(to: CGPoint(x: x, y: y))
                }
                path.addLine(to: CGPoint(x: w, y: h))
                path.closeSubpath()
            }
            .fill(
                LinearGradient(
                    colors: [
                        AuralisTheme.gold.opacity(0.18),
                        AuralisTheme.cyanHiRes.opacity(0.05),
                        Color.clear
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
        }
    }
}

// Calculates and Draws Biquad Transfer Function Response Path
private struct ResponseCurvePath: Shape {
    let bands: [EQBandConfig]
    let calculator: BiquadResponseCalculator
    let w: CGFloat
    let h: CGFloat
    let minFreq: Double
    let maxFreq: Double
    let minDb: Double
    let maxDb: Double

    func path(in rect: CGRect) -> Path {
        var path = Path()
        let steps = 140

        for i in 0...steps {
            let frac = Double(i) / Double(steps)
            let logMin = log10(minFreq)
            let logMax = log10(maxFreq)
            let freq = pow(10.0, logMin + frac * (logMax - logMin))

            let db = calculator.totalMagnitudeDb(for: bands, at: freq)
            let clampedDb = max(minDb, min(maxDb, db))

            let x = rect.width * CGFloat(frac)
            let yFrac = 1.0 - ((clampedDb - minDb) / (maxDb - minDb))
            let y = rect.height * CGFloat(yFrac)

            if i == 0 {
                path.move(to: CGPoint(x: x, y: y))
            } else {
                path.addLine(to: CGPoint(x: x, y: y))
            }
        }

        return path
    }
}
