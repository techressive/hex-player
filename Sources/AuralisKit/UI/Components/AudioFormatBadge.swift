import SwiftUI

public struct AudioFormatBadge: View {
    public let track: Track?

    public init(track: Track?) {
        self.track = track
    }

    public var body: some View {
        if let track = track {
            HStack(spacing: 8) {
                // Codec pill
                HStack(spacing: 5) {
                    Circle()
                        .fill(codecColor(for: track.codec))
                        .frame(width: 6, height: 6)
                    
                    Text(track.codec.rawValue)
                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                        .foregroundColor(AuralisTheme.textPrimary)
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(AuralisTheme.cardSurface)
                .cornerRadius(6)
                .overlay(
                    RoundedRectangle(cornerRadius: 6)
                        .stroke(codecColor(for: track.codec).opacity(0.6), lineWidth: 1)
                )

                // Technical bit-depth & sample rate
                HStack(spacing: 6) {
                    Text("\(track.formattedBitDepth) / \(track.formattedSampleRate)")
                        .font(.system(size: 11, weight: .semibold, design: .monospaced))
                        .foregroundColor(AuralisTheme.textSecondary)

                    if track.isHiRes {
                        Text("HI\u{2011}RES")
                            .font(.system(size: 9, weight: .black, design: .monospaced))
                            .foregroundColor(AuralisTheme.gold)
                            .lineLimit(1)
                            .fixedSize(horizontal: true, vertical: false)
                            .padding(.horizontal, 5)
                            .padding(.vertical, 2)
                            .background(AuralisTheme.gold.opacity(0.12))
                            .cornerRadius(4)
                    }
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(AuralisTheme.cardSurface)
                .cornerRadius(6)
                .overlay(
                    RoundedRectangle(cornerRadius: 6)
                        .stroke(AuralisTheme.cardBorder, lineWidth: 1)
                )
            }
        } else {
            HStack(spacing: 6) {
                Text("BIT-PERFECT AUDIO ENGINE")
                    .font(.system(size: 10, weight: .bold, design: .monospaced))
                    .foregroundColor(AuralisTheme.textMuted)
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(AuralisTheme.cardSurface)
            .cornerRadius(6)
            .overlay(
                RoundedRectangle(cornerRadius: 6)
                    .stroke(AuralisTheme.cardBorder, lineWidth: 1)
            )
        }
    }

    private func codecColor(for codec: AudioCodec) -> Color {
        switch codec {
        case .dsd: return AuralisTheme.emeraldDSD
        case .flac, .alac: return AuralisTheme.gold
        case .wav, .aiff: return AuralisTheme.cyanHiRes
        default: return AuralisTheme.textMuted
        }
    }
}
