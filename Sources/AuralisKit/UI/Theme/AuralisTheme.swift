import SwiftUI

public enum AppSkin: String, CaseIterable, Identifiable, Codable, Sendable {
    case hexOled = "HEX OLED (Official)"
    case pitchBlack = "OLED Pitch Black"
    case studioLight = "Studio Light"
    case vacuumTube = "Vacuum Tube"
    case midnightBlue = "Midnight Blue"
    case slateGraphite = "Slate Minimal"

    public var id: String { rawValue }

    public var subtitle: String {
        switch self {
        case .hexOled: return "#050505 OLED Black with Cobalt & Lavender"
        case .pitchBlack: return "#000000 Pure Black for OLED"
        case .studioLight: return "Clean High-Legibility Light Mode"
        case .vacuumTube: return "Warm Amber Analog Sepia"
        case .midnightBlue: return "Deep Ocean Sapphire Palette"
        case .slateGraphite: return "Monochrome Matte Graphite"
        }
    }

    public var canvasColor: Color {
        switch self {
        case .hexOled: return Color(red: 0x05/255.0, green: 0x05/255.0, blue: 0x05/255.0)
        case .pitchBlack: return Color(red: 0.0, green: 0.0, blue: 0.0)
        case .studioLight: return Color(red: 0.96, green: 0.96, blue: 0.98)
        case .vacuumTube: return Color(red: 0.06, green: 0.04, blue: 0.03)
        case .midnightBlue: return Color(red: 0.03, green: 0.05, blue: 0.09)
        case .slateGraphite: return Color(red: 0.07, green: 0.08, blue: 0.10)
        }
    }

    public var deepCanvasColor: Color {
        switch self {
        case .hexOled: return Color(red: 0x0B/255.0, green: 0x0C/255.0, blue: 0x0E/255.0)
        case .pitchBlack: return Color(red: 0.02, green: 0.02, blue: 0.025)
        case .studioLight: return Color(red: 0.92, green: 0.93, blue: 0.95)
        case .vacuumTube: return Color(red: 0.09, green: 0.06, blue: 0.05)
        case .midnightBlue: return Color(red: 0.05, green: 0.08, blue: 0.13)
        case .slateGraphite: return Color(red: 0.10, green: 0.11, blue: 0.14)
        }
    }

    public var cardSurfaceColor: Color {
        switch self {
        case .hexOled: return Color(red: 0x12/255.0, green: 0x13/255.0, blue: 0x16/255.0)
        case .pitchBlack: return Color(red: 0.05, green: 0.05, blue: 0.06)
        case .studioLight: return Color(red: 1.0, green: 1.0, blue: 1.0)
        case .vacuumTube: return Color(red: 0.13, green: 0.09, blue: 0.07)
        case .midnightBlue: return Color(red: 0.08, green: 0.12, blue: 0.18)
        case .slateGraphite: return Color(red: 0.13, green: 0.14, blue: 0.18)
        }
    }

    public var cardSurfaceHoverColor: Color {
        switch self {
        case .hexOled: return Color(red: 0x1A/255.0, green: 0x1C/255.0, blue: 0x22/255.0)
        case .pitchBlack: return Color(red: 0.08, green: 0.08, blue: 0.09)
        case .studioLight: return Color(red: 0.88, green: 0.90, blue: 0.94)
        case .vacuumTube: return Color(red: 0.18, green: 0.13, blue: 0.10)
        case .midnightBlue: return Color(red: 0.12, green: 0.17, blue: 0.24)
        case .slateGraphite: return Color(red: 0.18, green: 0.20, blue: 0.24)
        }
    }

    public var cardBorderColor: Color {
        switch self {
        case .hexOled: return Color(red: 0x26/255.0, green: 0x28/255.0, blue: 0x30/255.0)
        case .pitchBlack: return Color(red: 0.12, green: 0.12, blue: 0.14)
        case .studioLight: return Color(red: 0.82, green: 0.84, blue: 0.88)
        case .vacuumTube: return Color(red: 0.24, green: 0.17, blue: 0.13)
        case .midnightBlue: return Color(red: 0.15, green: 0.22, blue: 0.32)
        case .slateGraphite: return Color(red: 0.22, green: 0.24, blue: 0.28)
        }
    }

    public var textPrimaryColor: Color {
        switch self {
        case .studioLight: return Color(red: 0.08, green: 0.08, blue: 0.12)
        default: return Color(red: 0.98, green: 0.98, blue: 1.0)
        }
    }

    public var textSecondaryColor: Color {
        switch self {
        case .hexOled: return Color(red: 0xA6/255.0, green: 0xA9/255.0, blue: 0xB0/255.0) // Slate Gray
        case .studioLight: return Color(red: 0.35, green: 0.38, blue: 0.44)
        default: return Color(red: 0.62, green: 0.62, blue: 0.66)
        }
    }

    public var textMutedColor: Color {
        switch self {
        case .hexOled: return Color(red: 0x6E/255.0, green: 0x71/255.0, blue: 0x79/255.0)
        case .studioLight: return Color(red: 0.55, green: 0.58, blue: 0.64)
        default: return Color(red: 0.38, green: 0.38, blue: 0.42)
        }
    }

    public var isLight: Bool {
        self == .studioLight
    }
}

public enum AppAccentColor: String, CaseIterable, Identifiable, Codable, Sendable {
    case cobaltBlue = "Cobalt Blue"
    case lavender = "Lavender"
    case slate = "Slate Gray"
    case gold = "Studio Gold"
    case emerald = "DSD Emerald"
    case cyan = "Master Cyan"
    case amber = "Tube Amber"
    case violet = "Electric Violet"
    case rose = "Rose Gold"
    case crimson = "Crimson Red"

    public var id: String { rawValue }

    public var color: Color {
        switch self {
        case .cobaltBlue: return Color(red: 0x5B/255.0, green: 0x7C/255.0, blue: 0xFA/255.0) // #5B7CFA
        case .lavender: return Color(red: 0xA9/255.0, green: 0x9B/255.0, blue: 0xFF/255.0)   // #A99BFF
        case .slate: return Color(red: 0xA6/255.0, green: 0xA9/255.0, blue: 0xB0/255.0)      // #A6A9B0
        case .gold: return Color(red: 0.90, green: 0.69, blue: 0.24)       // #E5B03D
        case .emerald: return Color(red: 0.24, green: 0.86, blue: 0.58)    // #3DD894
        case .cyan: return Color(red: 0.39, green: 0.82, blue: 1.0)        // #64D2FF
        case .amber: return Color(red: 1.0, green: 0.55, blue: 0.22)       // #FF8C38
        case .violet: return Color(red: 0.66, green: 0.33, blue: 0.97)     // #A855F7
        case .rose: return Color(red: 0.96, green: 0.45, blue: 0.71)       // #F472B6
        case .crimson: return Color(red: 0.94, green: 0.27, blue: 0.27)    // #EF4444
        }
    }
}

public enum AuralisTheme {
    // Official HEX Brand Kit Palette
    public static let cobaltBlue = Color(red: 0x5B/255.0, green: 0x7C/255.0, blue: 0xFA/255.0) // #5B7CFA
    public static let lavender = Color(red: 0xA9/255.0, green: 0x9B/255.0, blue: 0xFF/255.0)   // #A99BFF
    public static let slateGray = Color(red: 0xA6/255.0, green: 0xA9/255.0, blue: 0xB0/255.0)  // #A6A9B0
    public static let oledBlack = Color(red: 0x05/255.0, green: 0x05/255.0, blue: 0x05/255.0)  // #050505
    public static let pureBlack = Color(red: 0.0, green: 0.0, blue: 0.0)

    public static let hexGradient = LinearGradient(
        colors: [cobaltBlue, lavender],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    // Canvas & Cards
    public static let pitchBlack = Color(red: 0x05/255.0, green: 0x05/255.0, blue: 0x05/255.0)
    public static let deepCanvas = Color(red: 0x0B/255.0, green: 0x0C/255.0, blue: 0x0E/255.0)
    public static let cardSurface = Color(red: 0x12/255.0, green: 0x13/255.0, blue: 0x16/255.0)
    public static let cardSurfaceHover = Color(red: 0x1A/255.0, green: 0x1C/255.0, blue: 0x22/255.0)
    public static let cardBorder = Color(red: 0x26/255.0, green: 0x28/255.0, blue: 0x30/255.0)
    public static let cardBorderActive = Color(red: 0x5B/255.0, green: 0x7C/255.0, blue: 0xFA/255.0).opacity(0.6)
    
    // Accents
    public static let gold = Color(red: 0x5B/255.0, green: 0x7C/255.0, blue: 0xFA/255.0)       // Default accent is Cobalt Blue
    public static let goldGlow = Color(red: 0x5B/255.0, green: 0x7C/255.0, blue: 0xFA/255.0).opacity(0.3)
    public static let cyanHiRes = Color(red: 0xA9/255.0, green: 0x9B/255.0, blue: 0xFF/255.0)   // Lavender Hi-Res
    public static let emeraldDSD = Color(red: 0.24, green: 0.86, blue: 0.58)                   // DSD Green
    
    // Typography
    public static let textPrimary = Color(red: 0.98, green: 0.98, blue: 1.0)
    public static let textSecondary = Color(red: 0xA6/255.0, green: 0xA9/255.0, blue: 0xB0/255.0) // Slate Gray
    public static let textMuted = Color(red: 0x6E/255.0, green: 0x71/255.0, blue: 0x79/255.0)
}

public typealias HexTheme = AuralisTheme

public extension Font {
    static func hexHeading(size: CGFloat, weight: Font.Weight = .bold) -> Font {
        if NSFont(name: "PlusJakartaSans-Bold", size: size) != nil ||
           NSFont(name: "Plus Jakarta Sans", size: size) != nil {
            return .custom("Plus Jakarta Sans", size: size).weight(weight)
        }
        return .system(size: size, weight: weight, design: .default)
    }

    static func hexBody(size: CGFloat, weight: Font.Weight = .regular) -> Font {
        if NSFont(name: "Inter-Regular", size: size) != nil ||
           NSFont(name: "Inter", size: size) != nil {
            return .custom("Inter", size: size).weight(weight)
        }
        return .system(size: size, weight: weight, design: .default)
    }
}

public struct PitchBlackCardModifier: ViewModifier {
    var isHovered: Bool = false
    public func body(content: Content) -> some View {
        content
            .background(isHovered ? AuralisTheme.cardSurfaceHover : AuralisTheme.cardSurface)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isHovered ? AuralisTheme.cardBorderActive : AuralisTheme.cardBorder, lineWidth: 1)
            )
    }
}

public extension View {
    func pitchBlackCard(isHovered: Bool = false) -> some View {
        self.modifier(PitchBlackCardModifier(isHovered: isHovered))
    }
}
