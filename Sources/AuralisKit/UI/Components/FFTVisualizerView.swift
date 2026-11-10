import SwiftUI

public struct FFTVisualizerView: View {
    @ObservedObject private var analyzer = SpectrumAnalyzer.shared
    public let height: CGFloat
    public let barSpacing: CGFloat

    public init(height: CGFloat = 40, barSpacing: CGFloat = 2) {
        self.height = height
        self.barSpacing = barSpacing
    }

    public var body: some View {
        GeometryReader { geo in
            let count = analyzer.bands.count
            let totalSpacing = barSpacing * CGFloat(count - 1)
            let barWidth = max(2.0, (geo.size.width - totalSpacing) / CGFloat(count))

            HStack(alignment: .bottom, spacing: barSpacing) {
                ForEach(0..<count, id: \.self) { idx in
                    let value = CGFloat(analyzer.bands[idx])
                    let barHeight = max(2.0, value * height)

                    RoundedRectangle(cornerRadius: 1.5)
                        .fill(
                            LinearGradient(
                                colors: [
                                    AuralisTheme.cyanHiRes.opacity(0.8),
                                    AuralisTheme.gold
                                ],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .frame(width: barWidth, height: barHeight)
                        .opacity(0.35 + Double(value) * 0.65)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: height, alignment: .bottom)
        }
        .frame(height: height)
    }
}
