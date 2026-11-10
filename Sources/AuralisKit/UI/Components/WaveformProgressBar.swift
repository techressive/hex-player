import SwiftUI

public final class WaveformProgressState: ObservableObject {
    @Published public var isDragging: Bool = false
    @Published public var dragProgress: Double = 0.0
    public init() {}
}

public struct WaveformProgressBar: View {
    public let currentTime: TimeInterval
    public let duration: TimeInterval
    public let onSeek: (TimeInterval) -> Void

    @StateObject private var ui = WaveformProgressState()

    public init(currentTime: TimeInterval, duration: TimeInterval, onSeek: @escaping (TimeInterval) -> Void) {
        self.currentTime = currentTime
        self.duration = duration
        self.onSeek = onSeek
    }

    private var progress: Double {
        if ui.isDragging { return ui.dragProgress }
        guard duration > 0 else { return 0.0 }
        return max(0.0, min(1.0, currentTime / duration))
    }

    public var body: some View {
        VStack(spacing: 6) {
            GeometryReader { geo in
                let width = geo.size.width
                
                ZStack(alignment: .leading) {
                    // Track background groove
                    Capsule()
                        .fill(AuralisTheme.cardBorderActive)
                        .frame(height: 4)

                    // Active gold elapsed track
                    Capsule()
                        .fill(
                            LinearGradient(
                                colors: [AuralisTheme.cyanHiRes, AuralisTheme.gold],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .frame(width: max(4, width * CGFloat(progress)), height: 4)

                    // Audiophile knob with glowing aura
                    Circle()
                        .fill(AuralisTheme.gold)
                        .frame(width: 12, height: 12)
                        .shadow(color: AuralisTheme.gold.opacity(0.8), radius: ui.isDragging ? 8 : 4)
                        .offset(x: max(0, min(width - 12, (width * CGFloat(progress)) - 6)))
                }
                .contentShape(Rectangle())
                .gesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged { value in
                            ui.isDragging = true
                            let fraction = max(0.0, min(1.0, Double(value.location.x / width)))
                            ui.dragProgress = fraction
                        }
                        .onEnded { value in
                            let fraction = max(0.0, min(1.0, Double(value.location.x / width)))
                            ui.isDragging = false
                            let targetTime = fraction * duration
                            onSeek(targetTime)
                        }
                )
            }
            .frame(height: 14)

            // Timecode labels
            HStack {
                Text(formatTime(currentTime))
                    .font(.system(size: 11, weight: .medium, design: .monospaced))
                    .foregroundColor(AuralisTheme.textSecondary)

                Spacer()

                Text(duration > 0 ? "-\(formatTime(max(0, duration - currentTime)))" : "--:--")
                    .font(.system(size: 11, weight: .medium, design: .monospaced))
                    .foregroundColor(AuralisTheme.textMuted)
            }
        }
    }

    private func formatTime(_ seconds: TimeInterval) -> String {
        let secs = Int(max(0, seconds))
        let m = secs / 60
        let s = secs % 60
        return String(format: "%02d:%02d", m, s)
    }
}
