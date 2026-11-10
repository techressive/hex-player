import SwiftUI
import AppKit

public enum SettingsTab: String, CaseIterable, Identifiable {
    case library = "Library"
    case audio = "Audio & Output"
    case lyrics = "Lyrics & Metadata"
    case appearance = "Appearance"

    public var id: String { rawValue }

    public var iconName: String {
        switch self {
        case .library: return "folder.fill"
        case .audio: return "speaker.wave.3.fill"
        case .lyrics: return "quote.bubble.fill"
        case .appearance: return "paintpalette.fill"
        }
    }
}

public final class SettingsViewState: ObservableObject {
    @Published public var selectedSettingsTab: SettingsTab = .library
    @Published public var showClearConfirm: Bool = false
    public init() {}
}

public struct SettingsView: View {
    @ObservedObject var state = AppState.shared
    @ObservedObject var audioController = AudioEngineController.shared
    @ObservedObject var deviceManager = AudioDeviceManager.shared
    @ObservedObject var eqEngine = ParametricEQEngine.shared
    @StateObject private var ui = SettingsViewState()

    public init() {}

    public var body: some View {
        VStack(spacing: 0) {
            // Settings Header Bar
            HStack(spacing: 16) {
                if state.canGoBack {
                    Button(action: {
                        withAnimation(.easeInOut(duration: 0.15)) {
                            state.goBack()
                        }
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: "chevron.left")
                            Text("Back")
                        }
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(state.activeAccentColor)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 5)
                        .background(state.themeCardSurface)
                        .cornerRadius(6)
                        .overlay(
                            RoundedRectangle(cornerRadius: 6)
                                .stroke(state.themeCardBorder, lineWidth: 1)
                        )
                    }
                    .buttonStyle(PlainButtonStyle())
                    .help("Go Back")
                }

                Text("SETTINGS")
                    .font(Font.hexHeading(size: 13, weight: .black))
                    .foregroundColor(AuralisTheme.textPrimary)
                    .tracking(1.5)

                Spacer()

                // Tab Switcher
                HStack(spacing: 4) {
                    ForEach(SettingsTab.allCases) { tab in
                        let isSelected = (ui.selectedSettingsTab == tab)
                        Button(action: {
                            withAnimation(.easeInOut(duration: 0.15)) {
                                ui.selectedSettingsTab = tab
                            }
                        }) {
                            HStack(spacing: 6) {
                                Image(systemName: tab.iconName)
                                    .font(.system(size: 12))
                                Text(tab.rawValue)
                                    .font(.system(size: 12, weight: isSelected ? .bold : .medium))
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(isSelected ? AuralisTheme.gold : AuralisTheme.cardSurface)
                            .foregroundColor(isSelected ? .black : AuralisTheme.textSecondary)
                            .cornerRadius(6)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(PlainButtonStyle())
                        .contentShape(Rectangle())
                    }
                }
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 14)
            .background(AuralisTheme.deepCanvas)
            .overlay(
                Rectangle().frame(height: 1).foregroundColor(AuralisTheme.cardBorder),
                alignment: .bottom
            )

            // Settings Content Body
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    switch ui.selectedSettingsTab {
                    case .library:
                        librarySettingsSection
                    case .audio:
                        audioSettingsSection
                    case .lyrics:
                        lyricsSettingsSection
                    case .appearance:
                        appearanceSettingsSection
                    }
                }
                .padding(28)
                .frame(maxWidth: 800)
            }
            .frame(maxWidth: .infinity, alignment: .topLeading)
        }
        .background(AuralisTheme.pitchBlack)
        .alert(isPresented: $ui.showClearConfirm) {
            Alert(
                title: Text("Clear Library"),
                message: Text("Are you sure you want to remove all tracks from your Auralis library? Your audio files on disk will not be deleted."),
                primaryButton: .destructive(Text("Clear All")) {
                    state.clearLibrary()
                },
                secondaryButton: .cancel()
            )
        }
    }

    // MARK: - Library Settings Section

    private var librarySettingsSection: some View {
        VStack(alignment: .leading, spacing: 20) {
            sectionHeader(title: "Library Folders", subtitle: "Auralis monitors and auto-syncs these directories for lossless & hi-res audio.")

            // Watched Folders Card
            VStack(alignment: .leading, spacing: 12) {
                if state.watchedFolders.isEmpty {
                    HStack(spacing: 12) {
                        Image(systemName: "folder.badge.questionmark")
                            .font(.system(size: 24))
                            .foregroundColor(AuralisTheme.textMuted)

                        VStack(alignment: .leading, spacing: 2) {
                            Text("No Library Folders Added")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(AuralisTheme.textPrimary)
                            Text("Add your ~/Music or external DAC storage folder to begin automatic cataloging.")
                                .font(.system(size: 11))
                                .foregroundColor(AuralisTheme.textMuted)
                        }
                        Spacer()
                    }
                    .padding(14)
                    .pitchBlackCard()
                } else {
                    VStack(spacing: 6) {
                        ForEach(state.watchedFolders, id: \.path) { folder in
                            HStack(spacing: 10) {
                                Image(systemName: "folder.fill")
                                    .font(.system(size: 14))
                                    .foregroundColor(AuralisTheme.gold)

                                Text(folder.path)
                                    .font(.system(size: 12, design: .monospaced))
                                    .foregroundColor(AuralisTheme.textPrimary)
                                    .lineLimit(1)

                                Spacer()

                                Button(action: { state.removeLibraryFolder(url: folder) }) {
                                    Image(systemName: "trash")
                                        .font(.system(size: 12))
                                        .foregroundColor(Color.red.opacity(0.8))
                                }
                                .buttonStyle(PlainButtonStyle())
                            }
                            .padding(.horizontal, 14)
                            .padding(.vertical, 9)
                            .background(AuralisTheme.cardSurface)
                            .cornerRadius(6)
                        }
                    }
                }

                // Add Folder Action Bar
                HStack(spacing: 12) {
                    Button(action: { openFolderPicker() }) {
                        HStack(spacing: 6) {
                            Image(systemName: "plus.circle.fill")
                            Text("Add Music Folder...")
                        }
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(AuralisTheme.gold)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .pitchBlackCard()
                    }
                    .buttonStyle(PlainButtonStyle())

                    Button(action: {
                        Task { await state.rescanAllFolders() }
                    }) {
                        HStack(spacing: 6) {
                            if state.isSyncing {
                                ProgressView().controlSize(.small)
                            } else {
                                Image(systemName: "arrow.clockwise")
                            }
                            Text(state.isSyncing ? state.syncProgressMessage : "Rescan Library Now")
                        }
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(AuralisTheme.textSecondary)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .pitchBlackCard()
                    }
                    .buttonStyle(PlainButtonStyle())
                    .disabled(state.isSyncing || state.watchedFolders.isEmpty)

                    Spacer()

                    Button(action: { ui.showClearConfirm = true }) {
                        Text("Clear Library")
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(Color.red.opacity(0.8))
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                    }
                    .buttonStyle(PlainButtonStyle())
                    .disabled(state.tracks.isEmpty)
                }
            }

            Divider().background(AuralisTheme.cardBorder)

            // Scanning Toggles
            VStack(alignment: .leading, spacing: 14) {
                Toggle(isOn: Binding(
                    get: { state.scanSubfoldersRecursively },
                    set: { state.setRecursiveScan($0) }
                )) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Scan Subfolders Recursively")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(AuralisTheme.textPrimary)
                        Text("Automatically discovers audio nested inside artist/album folders at any directory depth.")
                            .font(.system(size: 11))
                            .foregroundColor(AuralisTheme.textMuted)
                    }
                }
                .toggleStyle(SwitchToggleStyle(tint: AuralisTheme.gold))

                Toggle(isOn: Binding(
                    get: { state.autoSyncOnLaunch },
                    set: { state.setAutoSync($0) }
                )) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Auto-Sync Library Each Time App Opens")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(AuralisTheme.textPrimary)
                        Text("Silently checks watched folders on launch and indexes newly added music without interrupting playback.")
                            .font(.system(size: 11))
                            .foregroundColor(AuralisTheme.textMuted)
                    }
                }
                .toggleStyle(SwitchToggleStyle(tint: AuralisTheme.gold))
            }

            Divider().background(AuralisTheme.cardBorder)

            // Library Statistics Card
            sectionHeader(title: "Catalog Overview", subtitle: "Real-time index statistics across all imported tracks.")
            HStack(spacing: 12) {
                statCard(title: "TRACKS", value: "\(state.tracks.count)", icon: "music.note")
                statCard(title: "ALBUMS", value: "\(state.albums.count)", icon: "square.stack.fill")
                statCard(title: "ARTISTS", value: "\(state.artists.count)", icon: "person.2.fill")
                let hiResCount = state.tracks.filter { $0.isHiRes }.count
                let pct = state.tracks.isEmpty ? 0 : Int((Double(hiResCount) / Double(state.tracks.count)) * 100.0)
                statCard(title: "HI-RES RATIO", value: "\(pct)%", icon: "sparkles")
            }
        }
    }

    // MARK: - Audio & Output Settings Section

    private var audioSettingsSection: some View {
        VStack(alignment: .leading, spacing: 20) {
            sectionHeader(title: "Audio Output & DAC Routing", subtitle: "Select which speaker, headphones, or external DAC Auralis streams bit-perfect audio to.")

            // Device Picker
            VStack(alignment: .leading, spacing: 10) {
                Text("ACTIVE OUTPUT DEVICE")
                    .font(.system(size: 10, weight: .bold, design: .monospaced))
                    .foregroundColor(AuralisTheme.textMuted)

                VStack(spacing: 6) {
                    ForEach(deviceManager.availableDevices) { device in
                        let isSelected = (deviceManager.selectedDevice?.id == device.id)

                        Button(action: {
                            audioController.setOutputDevice(device)
                        }) {
                            HStack(spacing: 12) {
                                Image(systemName: device.iconName)
                                    .font(.system(size: 16))
                                    .foregroundColor(isSelected ? AuralisTheme.gold : AuralisTheme.textMuted)
                                    .frame(width: 24)

                                VStack(alignment: .leading, spacing: 2) {
                                    HStack(spacing: 8) {
                                        Text(device.name)
                                            .font(.system(size: 13, weight: isSelected ? .bold : .medium))
                                            .foregroundColor(isSelected ? AuralisTheme.gold : AuralisTheme.textPrimary)

                                        if device.isDefault {
                                            Text("SYSTEM DEFAULT")
                                                .font(.system(size: 8, weight: .black, design: .monospaced))
                                                .foregroundColor(AuralisTheme.textMuted)
                                                .padding(.horizontal, 4)
                                                .padding(.vertical, 1)
                                                .background(AuralisTheme.cardSurface)
                                                .cornerRadius(3)
                                        }
                                    }

                                    Text("\(device.transportType) • \(Int(device.sampleRate / 1000.0)) kHz hardware clock")
                                        .font(.system(size: 11, design: .monospaced))
                                        .foregroundColor(AuralisTheme.textMuted)
                                }

                                Spacer()

                                if isSelected {
                                    HStack(spacing: 4) {
                                        Circle()
                                            .fill(AuralisTheme.emeraldDSD)
                                            .frame(width: 8, height: 8)
                                        Text("ACTIVE")
                                            .font(.system(size: 9, weight: .black, design: .monospaced))
                                            .foregroundColor(AuralisTheme.emeraldDSD)
                                    }
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 10)
                            .background(isSelected ? AuralisTheme.cardSurfaceHover : AuralisTheme.cardSurface)
                            .cornerRadius(8)
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(isSelected ? AuralisTheme.gold : AuralisTheme.cardBorder, lineWidth: 1)
                            )
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(PlainButtonStyle())
                        .contentShape(Rectangle())
                    }
                }
            }

            Divider().background(AuralisTheme.cardBorder)

            // Buffer Size Tuning
            VStack(alignment: .leading, spacing: 10) {
                Text("CORE AUDIO BUFFER SIZE")
                    .font(.system(size: 10, weight: .bold, design: .monospaced))
                    .foregroundColor(AuralisTheme.textMuted)

                HStack(spacing: 8) {
                    ForEach([128, 256, 512, 1024], id: \.self) { size in
                        let isChosen = deviceManager.preferredBufferSize == size
                        Button(action: { deviceManager.preferredBufferSize = size }) {
                            VStack(spacing: 3) {
                                Text("\(size)")
                                    .font(.system(size: 13, weight: .bold, design: .monospaced))
                                Text(bufferLabel(size))
                                    .font(.system(size: 9, design: .monospaced))
                                    .foregroundColor(isChosen ? .black.opacity(0.8) : AuralisTheme.textMuted)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(isChosen ? AuralisTheme.gold : AuralisTheme.cardSurface)
                            .foregroundColor(isChosen ? .black : AuralisTheme.textPrimary)
                            .cornerRadius(6)
                            .overlay(
                                RoundedRectangle(cornerRadius: 6)
                                    .stroke(isChosen ? AuralisTheme.gold : AuralisTheme.cardBorder, lineWidth: 1)
                            )
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
            }

            Divider().background(AuralisTheme.cardBorder)

            // Architecture Note Card
            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 6) {
                    Image(systemName: "checkmark.shield.fill")
                        .foregroundColor(AuralisTheme.emeraldDSD)
                    Text("64-BIT INTERNAL FLOATING-POINT BUS")
                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                        .foregroundColor(AuralisTheme.emeraldDSD)
                }
                Text("All audio decoding and multi-band biquad parametric filtering occurs in uncompressed 64-bit IEEE float space, eliminating inter-sample distortion and truncation artifacts.")
                    .font(.system(size: 11))
                    .foregroundColor(AuralisTheme.textMuted)
            }
            .padding(14)
            .pitchBlackCard()
        }
    }

    // MARK: - Lyrics Settings Section

    private var lyricsSettingsSection: some View {
        VStack(alignment: .leading, spacing: 20) {
            sectionHeader(title: "Lyrics & Cloud Metadata", subtitle: "Configure live timestamped lyric fetching and offline caching.")

            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 12) {
                    Image(systemName: "globe.americas.fill")
                        .font(.system(size: 20))
                        .foregroundColor(AuralisTheme.cyanHiRes)

                    VStack(alignment: .leading, spacing: 2) {
                        Text("LRCLIB Cloud Lyrics Provider")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(AuralisTheme.textPrimary)
                        Text("Connected to lrclib.net community database. Provides bit-exact synced line timestamps.")
                            .font(.system(size: 11))
                            .foregroundColor(AuralisTheme.textMuted)
                    }
                    Spacer()
                    Text("ONLINE")
                        .font(.system(size: 9, weight: .black, design: .monospaced))
                        .foregroundColor(AuralisTheme.emeraldDSD)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(AuralisTheme.emeraldDSD.opacity(0.12))
                        .cornerRadius(3)
                }
                .padding(14)
                .pitchBlackCard()

                Button(action: {
                    let cacheDir = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
                        .appendingPathComponent("Auralis/Lyrics", isDirectory: true)
                    try? FileManager.default.removeItem(at: cacheDir)
                }) {
                    HStack(spacing: 6) {
                        Image(systemName: "trash")
                        Text("Clear Local Lyrics Cache")
                    }
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(AuralisTheme.textSecondary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .pitchBlackCard()
                }
                .buttonStyle(PlainButtonStyle())
            }
        }
    }

    // MARK: - Appearance Settings Section

    private var appearanceSettingsSection: some View {
        VStack(alignment: .leading, spacing: 22) {
            sectionHeader(
                title: "Aesthetics & Interface Skins",
                subtitle: "Customize the visual persona of Auralis to your display and ambient environment."
            )

            // 1. Skins Selection
            VStack(alignment: .leading, spacing: 10) {
                Text("THEME SKIN")
                    .font(.system(size: 10, weight: .bold, design: .monospaced))
                    .foregroundColor(AuralisTheme.textMuted)

                LazyVGrid(columns: [GridItem(.adaptive(minimum: 220), spacing: 12)], spacing: 12) {
                    ForEach(AppSkin.allCases) { skin in
                        let isSelected = (state.selectedSkin == skin)
                        Button(action: {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                state.setSkin(skin)
                            }
                        }) {
                            HStack(spacing: 12) {
                                // Color swatches preview
                                ZStack {
                                    Circle()
                                        .fill(skin.canvasColor)
                                        .frame(width: 24, height: 24)
                                        .overlay(
                                            Circle()
                                                .stroke(skin.cardBorderColor, lineWidth: 1.5)
                                        )

                                    if isSelected {
                                        Image(systemName: "checkmark")
                                            .font(.system(size: 11, weight: .bold))
                                            .foregroundColor(skin.isLight ? .black : .white)
                                    }
                                }

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(skin.rawValue)
                                        .font(.system(size: 13, weight: isSelected ? .bold : .semibold))
                                        .foregroundColor(AuralisTheme.textPrimary)

                                    Text(skin.subtitle)
                                        .font(.system(size: 10))
                                        .foregroundColor(AuralisTheme.textMuted)
                                        .lineLimit(1)
                                }

                                Spacer()
                            }
                            .padding(12)
                            .background(isSelected ? state.activeAccentColor.opacity(0.12) : AuralisTheme.cardSurface)
                            .cornerRadius(10)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(isSelected ? state.activeAccentColor : AuralisTheme.cardBorder, lineWidth: isSelected ? 1.5 : 1)
                            )
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
            }

            Divider().background(AuralisTheme.cardBorder)

            // 2. Accent Colors
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Text("AUDIOPHILE ACCENT COLOR")
                        .font(.system(size: 10, weight: .bold, design: .monospaced))
                        .foregroundColor(AuralisTheme.textMuted)

                    Spacer()

                    Text("Active: \(state.selectedAccentColor.rawValue)")
                        .font(.system(size: 10, design: .monospaced))
                        .foregroundColor(state.activeAccentColor)
                }

                LazyVGrid(columns: [GridItem(.adaptive(minimum: 140), spacing: 10)], spacing: 10) {
                    ForEach(AppAccentColor.allCases) { accent in
                        let isSelected = (state.selectedAccentColor == accent)
                        Button(action: {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                state.setAccentColor(accent)
                            }
                        }) {
                            HStack(spacing: 8) {
                                Circle()
                                    .fill(accent.color)
                                    .frame(width: 14, height: 14)
                                    .overlay(
                                        Circle()
                                            .stroke(Color.white.opacity(0.3), lineWidth: isSelected ? 2 : 0)
                                    )

                                Text(accent.rawValue)
                                    .font(.system(size: 11, weight: isSelected ? .bold : .medium))
                                    .foregroundColor(isSelected ? AuralisTheme.textPrimary : AuralisTheme.textSecondary)

                                Spacer()

                                if isSelected {
                                    Image(systemName: "checkmark")
                                        .font(.system(size: 9, weight: .black))
                                        .foregroundColor(accent.color)
                                }
                            }
                            .padding(.horizontal, 10)
                            .padding(.vertical, 8)
                            .background(isSelected ? accent.color.opacity(0.12) : AuralisTheme.cardSurface)
                            .cornerRadius(8)
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(isSelected ? accent.color : AuralisTheme.cardBorder, lineWidth: 1)
                            )
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
            }

            Divider().background(AuralisTheme.cardBorder)

            // 3. Dynamic Mode info banner
            HStack(spacing: 12) {
                Image(systemName: "wand.and.stars")
                    .font(.system(size: 18))
                    .foregroundColor(state.activeAccentColor)

                VStack(alignment: .leading, spacing: 2) {
                    Text("Dynamic Artwork Accent Mode")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(AuralisTheme.textPrimary)
                    Text("When on the Now Playing screen, transport buttons and the navigation sidebar automatically adopt the dynamic color palette of the playing album cover.")
                        .font(.system(size: 11))
                        .foregroundColor(AuralisTheme.textMuted)
                }
                Spacer()
            }
            .padding(14)
            .background(state.activeAccentColor.opacity(0.08))
            .cornerRadius(10)
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(state.activeAccentColor.opacity(0.3), lineWidth: 1)
            )
        }
    }

    // MARK: - Helpers

    private func sectionHeader(title: String, subtitle: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(AuralisTheme.textPrimary)
            Text(subtitle)
                .font(.system(size: 12))
                .foregroundColor(AuralisTheme.textMuted)
        }
    }

    private func statCard(title: String, value: String, icon: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(title)
                    .font(.system(size: 9, weight: .black, design: .monospaced))
                    .foregroundColor(AuralisTheme.textMuted)
                Spacer()
                Image(systemName: icon)
                    .font(.system(size: 11))
                    .foregroundColor(AuralisTheme.gold)
            }
            Text(value)
                .font(.system(size: 20, weight: .bold, design: .monospaced))
                .foregroundColor(AuralisTheme.textPrimary)
        }
        .padding(12)
        .frame(maxWidth: .infinity)
        .pitchBlackCard()
    }

    private func themeCard(title: String, subtitle: String, isSelected: Bool) -> some View {
        HStack(spacing: 10) {
            Circle()
                .fill(Color.black)
                .frame(width: 20, height: 20)
                .overlay(Circle().stroke(AuralisTheme.gold, lineWidth: 2))

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(AuralisTheme.textPrimary)
                Text(subtitle)
                    .font(.system(size: 10))
                    .foregroundColor(AuralisTheme.textMuted)
            }
            Spacer()
        }
        .padding(12)
        .pitchBlackCard()
    }

    private func accentColorCircle(color: Color, name: String, isSelected: Bool) -> some View {
        HStack(spacing: 8) {
            Circle()
                .fill(color)
                .frame(width: 14, height: 14)
            Text(name)
                .font(.system(size: 12, weight: isSelected ? .bold : .medium))
                .foregroundColor(isSelected ? AuralisTheme.textPrimary : AuralisTheme.textSecondary)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .pitchBlackCard()
    }

    private func bufferLabel(_ size: Int) -> String {
        switch size {
        case 128: return "Lowest Latency"
        case 256: return "Balanced"
        case 512: return "Recommended"
        case 1024: return "Maximum Safety"
        default: return ""
        }
    }

    private func openFolderPicker() {
        let panel = NSOpenPanel()
        panel.canChooseFiles = false
        panel.canChooseDirectories = true
        panel.allowsMultipleSelection = true
        panel.prompt = "Select Music Folder"
        panel.message = "Choose a folder containing your audio files to add to Auralis."

        if panel.runModal() == .OK {
            for url in panel.urls {
                state.addLibraryFolder(url: url)
            }
        }
    }
}
