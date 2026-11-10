import SwiftUI

public final class EqualizerViewState: ObservableObject {
    @Published public var isSavingPreset: Bool = false
    @Published public var newPresetName: String = ""
    public init() {}
}

public struct EqualizerModalView: View {
    @ObservedObject var eqEngine = ParametricEQEngine.shared
    @StateObject private var ui = EqualizerViewState()
    @Environment(\.dismiss) private var dismiss

    public init() {}

    public var body: some View {
        VStack(spacing: 0) {
            // Header Bar
            HStack(spacing: 16) {
                // Title and Audiophile Engine Indicator
                HStack(spacing: 8) {
                    Circle()
                        .fill(eqEngine.isEnabled ? AuralisTheme.emeraldDSD : AuralisTheme.textMuted)
                        .frame(width: 8, height: 8)
                        .shadow(color: eqEngine.isEnabled ? AuralisTheme.emeraldDSD.opacity(0.8) : Color.clear, radius: 4)

                    Text("HEX AUDIOPHILE EQUALIZER")
                        .font(Font.hexHeading(size: 13, weight: .bold))
                        .foregroundColor(AuralisTheme.textPrimary)
                }

                Spacer()

                // Mode Picker (Simple vs Expert)
                HStack(spacing: 4) {
                    Button(action: {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            eqEngine.mode = .simple
                            eqEngine.applyToAudioUnit()
                        }
                    }) {
                        Text("Simple 7-Band")
                            .font(.system(size: 11, weight: .semibold))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(eqEngine.mode == .simple ? AuralisTheme.cobaltBlue : AuralisTheme.cardSurface)
                            .foregroundColor(eqEngine.mode == .simple ? .white : AuralisTheme.textSecondary)
                            .cornerRadius(6)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(PlainButtonStyle())
                    .contentShape(Rectangle())

                    Button(action: {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            eqEngine.mode = .expert
                            eqEngine.applyToAudioUnit()
                        }
                    }) {
                        Text("Expert Parametric")
                            .font(.system(size: 11, weight: .semibold))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(eqEngine.mode == .expert ? AuralisTheme.cobaltBlue : AuralisTheme.cardSurface)
                            .foregroundColor(eqEngine.mode == .expert ? .white : AuralisTheme.textSecondary)
                            .cornerRadius(6)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(PlainButtonStyle())
                    .contentShape(Rectangle())
                }
                .padding(3)
                .background(AuralisTheme.deepCanvas)
                .cornerRadius(8)

                Spacer()

                // Master Power Switch
                Button(action: {
                    withAnimation {
                        eqEngine.isEnabled.toggle()
                    }
                }) {
                    HStack(spacing: 6) {
                        Image(systemName: eqEngine.isEnabled ? "power.circle.fill" : "power.circle")
                        Text(eqEngine.isEnabled ? "ACTIVE" : "BYPASSED")
                    }
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .foregroundColor(eqEngine.isEnabled ? AuralisTheme.emeraldDSD : AuralisTheme.textMuted)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .pitchBlackCard()
                }
                .buttonStyle(PlainButtonStyle())

                Button(action: { dismiss() }) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 18))
                        .foregroundColor(AuralisTheme.textMuted)
                }
                .buttonStyle(PlainButtonStyle())
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 14)
            .background(AuralisTheme.deepCanvas)
            .overlay(
                Rectangle()
                    .frame(height: 1)
                    .foregroundColor(AuralisTheme.cardBorder),
                alignment: .bottom
            )

            // Preset Bar with Custom Preset Creation
            HStack(spacing: 10) {
                Text("PRESETS")
                    .font(.system(size: 10, weight: .bold, design: .monospaced))
                    .foregroundColor(AuralisTheme.textMuted)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        // Built-in presets
                        ForEach(eqEngine.presets) { preset in
                            Button(action: {
                                withAnimation(.easeInOut(duration: 0.2)) {
                                    eqEngine.loadPreset(preset)
                                }
                            }) {
                                Text(preset.name)
                                    .font(.system(size: 11, weight: .medium))
                                    .foregroundColor(AuralisTheme.textPrimary)
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 5)
                                    .background(AuralisTheme.cardSurface)
                                    .cornerRadius(6)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 6)
                                            .stroke(AuralisTheme.cardBorder, lineWidth: 1)
                                    )
                            }
                            .buttonStyle(PlainButtonStyle())
                        }

                        // User custom presets
                        ForEach(eqEngine.userPresets) { preset in
                            HStack(spacing: 4) {
                                Button(action: {
                                    withAnimation(.easeInOut(duration: 0.2)) {
                                        eqEngine.loadPreset(preset)
                                    }
                                }) {
                                    Text(preset.name)
                                        .font(.system(size: 11, weight: .semibold))
                                        .foregroundColor(AuralisTheme.lavender)
                                }
                                .buttonStyle(PlainButtonStyle())

                                Button(action: {
                                    withAnimation {
                                        eqEngine.deleteUserPreset(id: preset.id)
                                    }
                                }) {
                                    Image(systemName: "xmark")
                                        .font(.system(size: 8, weight: .bold))
                                        .foregroundColor(AuralisTheme.textMuted)
                                        .padding(2)
                                }
                                .buttonStyle(PlainButtonStyle())
                                .help("Delete preset")
                            }
                            .padding(.horizontal, 8)
                            .padding(.vertical, 5)
                            .background(AuralisTheme.cobaltBlue.opacity(0.15))
                            .cornerRadius(6)
                            .overlay(
                                RoundedRectangle(cornerRadius: 6)
                                    .stroke(AuralisTheme.cobaltBlue.opacity(0.4), lineWidth: 1)
                            )
                        }
                    }
                }

                Spacer()

                // Save Preset Button / Inline Name Input
                if ui.isSavingPreset {
                    HStack(spacing: 6) {
                        TextField("Preset name...", text: $ui.newPresetName)
                            .textFieldStyle(PlainTextFieldStyle())
                            .font(.system(size: 11))
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(AuralisTheme.deepCanvas)
                            .cornerRadius(6)
                            .overlay(
                                RoundedRectangle(cornerRadius: 6)
                                    .stroke(AuralisTheme.cobaltBlue, lineWidth: 1)
                            )
                            .frame(width: 140)

                        Button(action: {
                            if !ui.newPresetName.isEmpty {
                                eqEngine.saveCurrentAsPreset(name: ui.newPresetName)
                                ui.newPresetName = ""
                                ui.isSavingPreset = false
                            }
                        }) {
                            Text("Save")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(AuralisTheme.cobaltBlue)
                                .cornerRadius(6)
                        }
                        .buttonStyle(PlainButtonStyle())

                        Button(action: {
                            ui.isSavingPreset = false
                            ui.newPresetName = ""
                        }) {
                            Image(systemName: "xmark")
                                .font(.system(size: 10))
                                .foregroundColor(AuralisTheme.textMuted)
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                } else {
                    Button(action: {
                        ui.isSavingPreset = true
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: "plus.circle.fill")
                            Text("Save Preset")
                        }
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(AuralisTheme.cobaltBlue)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(AuralisTheme.cardSurface)
                        .cornerRadius(6)
                        .overlay(
                            RoundedRectangle(cornerRadius: 6)
                                .stroke(AuralisTheme.cobaltBlue.opacity(0.3), lineWidth: 1)
                        )
                    }
                    .buttonStyle(PlainButtonStyle())
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 8)
            .background(AuralisTheme.cardSurface.opacity(0.5))
            .overlay(
                Rectangle()
                    .frame(height: 1)
                    .foregroundColor(AuralisTheme.cardBorder),
                alignment: .bottom
            )

            // Body: Simple Graphic vs Expert Parametric
            ScrollView {
                if eqEngine.mode == .simple {
                    SimpleEQView()
                        .transition(.opacity)
                } else {
                    ExpertParametricEQView()
                        .transition(.opacity)
                }
            }
        }
        .frame(minWidth: 720, idealWidth: 800, minHeight: 460, idealHeight: 520)
        .background(AuralisTheme.pitchBlack)
    }
}
