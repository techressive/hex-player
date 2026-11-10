import SwiftUI

public enum HexLogoStyle {
    case gradient
    case monochromeWhite
    case monochromeBlack
}

/// Precise geometric 3-pillar vector rendition of the official HEX hexagon logo
public struct HexLogoShape: Shape {
    public func path(in rect: CGRect) -> Path {
        var path = Path()
        let w = rect.width
        let h = rect.height

        // Total bounding box proportions for the 3 pillars
        let colWidth = w * 0.25
        let gap = w * 0.125

        // Left Pillar
        let leftX = 0.0
        var leftPillar = Path()
        leftPillar.move(to: CGPoint(x: leftX, y: h * 0.38))
        leftPillar.addLine(to: CGPoint(x: leftX + colWidth, y: h * 0.22))
        leftPillar.addLine(to: CGPoint(x: leftX + colWidth, y: h * 0.78))
        leftPillar.addLine(to: CGPoint(x: leftX, y: h * 0.62))
        leftPillar.closeSubpath()
        path.addPath(leftPillar)

        // Middle Pillar (Elevated peak)
        let midX = leftX + colWidth + gap
        var midPillar = Path()
        midPillar.move(to: CGPoint(x: midX, y: h * 0.16))
        midPillar.addLine(to: CGPoint(x: midX + colWidth, y: 0))
        midPillar.addLine(to: CGPoint(x: midX + colWidth, y: h * 0.72))
        midPillar.addLine(to: CGPoint(x: midX, y: h * 0.88))
        midPillar.closeSubpath()
        path.addPath(midPillar)

        // Right Pillar
        let rightX = midX + colWidth + gap
        var rightPillar = Path()
        rightPillar.move(to: CGPoint(x: rightX, y: h * 0.28))
        rightPillar.addLine(to: CGPoint(x: rightX + colWidth, y: h * 0.44))
        rightPillar.addLine(to: CGPoint(x: rightX + colWidth, y: h))
        rightPillar.addLine(to: CGPoint(x: rightX, y: h * 0.84))
        rightPillar.closeSubpath()
        path.addPath(rightPillar)

        return path
    }
}

public struct HexLogoView: View {
    public let size: CGFloat
    public let style: HexLogoStyle

    public init(size: CGFloat = 24, style: HexLogoStyle = .gradient) {
        self.size = size
        self.style = style
    }

    public var body: some View {
        Group {
            switch style {
            case .gradient:
                HexLogoShape()
                    .fill(
                        LinearGradient(
                            colors: [
                                Color(red: 0x5B/255.0, green: 0x7C/255.0, blue: 0xFA/255.0), // Cobalt Blue #5B7CFA
                                Color(red: 0xA9/255.0, green: 0x9B/255.0, blue: 0xFF/255.0)  // Lavender #A99BFF
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
            case .monochromeWhite:
                HexLogoShape()
                    .fill(Color.white)
            case .monochromeBlack:
                HexLogoShape()
                    .fill(Color.black)
            }
        }
        .frame(width: size, height: size * 1.05)
    }
}

public struct HexBrandHeaderView: View {
    public let size: CGFloat
    public let showWordmark: Bool

    public init(size: CGFloat = 22, showWordmark: Bool = true) {
        self.size = size
        self.showWordmark = showWordmark
    }

    public var body: some View {
        HStack(spacing: 8) {
            HexLogoView(size: size, style: .gradient)

            if showWordmark {
                Text("hex")
                    .font(.hexHeading(size: size * 0.95, weight: .bold))
                    .foregroundColor(AuralisTheme.textPrimary)
                    .tracking(0.5)
            }
        }
    }
}
