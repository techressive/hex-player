import Foundation
import CoreAudio
import AudioToolbox
import AVFoundation

public struct AudioOutputDevice: Identifiable, Hashable, Sendable {
    public let id: AudioDeviceID
    public let uid: String
    public let name: String
    public let transportType: String
    public let isDefault: Bool
    public let sampleRate: Double

    public init(
        id: AudioDeviceID,
        uid: String,
        name: String,
        transportType: String,
        isDefault: Bool,
        sampleRate: Double
    ) {
        self.id = id
        self.uid = uid
        self.name = name
        self.transportType = transportType
        self.isDefault = isDefault
        self.sampleRate = sampleRate
    }

    public var iconName: String {
        switch transportType.lowercased() {
        case "bluetooth", "bluetoothle":
            return "headphones"
        case "usb":
            return "hifispeaker.and.homepod.fill"
        case "displayport", "hdmi":
            return "tv.fill"
        case "airplay":
            return "airplayaudio"
        default:
            return "laptopcomputer"
        }
    }
}

public final class AudioDeviceManager: ObservableObject, @unchecked Sendable {
    public static let shared = AudioDeviceManager()

    @Published public private(set) var availableDevices: [AudioOutputDevice] = []
    @Published public private(set) var selectedDevice: AudioOutputDevice?
    @Published public var preferredBufferSize: Int = 512 // frames (e.g. 128, 256, 512, 1024)

    private var listenerBlock: AudioObjectPropertyListenerBlock?

    public init() {
        refreshDevices()
        setupDeviceListener()
    }

    deinit {
        removeDeviceListener()
    }

    public func refreshDevices() {
        let devices = queryOutputDevices()
        let updateBlock = {
            self.availableDevices = devices
            if let current = self.selectedDevice, devices.contains(where: { $0.id == current.id }) {
                // keep current
            } else {
                self.selectedDevice = devices.first(where: { $0.isDefault }) ?? devices.first
            }
        }

        if Thread.isMainThread {
            updateBlock()
        } else {
            DispatchQueue.main.async(execute: updateBlock)
        }
    }

    private func queryOutputDevices() -> [AudioOutputDevice] {
        var address = AudioObjectPropertyAddress(
            mSelector: kAudioHardwarePropertyDevices,
            mScope: kAudioObjectPropertyScopeGlobal,
            mElement: kAudioObjectPropertyElementMain
        )

        var dataSize: UInt32 = 0
        guard AudioObjectGetPropertyDataSize(AudioObjectID(kAudioObjectSystemObject), &address, 0, nil, &dataSize) == noErr else {
            return []
        }

        let deviceCount = Int(dataSize) / MemoryLayout<AudioDeviceID>.size
        var deviceIDs = [AudioDeviceID](repeating: 0, count: deviceCount)
        guard AudioObjectGetPropertyData(AudioObjectID(kAudioObjectSystemObject), &address, 0, nil, &dataSize, &deviceIDs) == noErr else {
            return []
        }

        let defaultDeviceID = getDefaultOutputDeviceID()
        var outputList: [AudioOutputDevice] = []

        for id in deviceIDs {
            // Check if device has output streams
            var streamAddress = AudioObjectPropertyAddress(
                mSelector: kAudioDevicePropertyStreams,
                mScope: kAudioObjectPropertyScopeOutput,
                mElement: kAudioObjectPropertyElementMain
            )
            var streamSize: UInt32 = 0
            guard AudioObjectGetPropertyDataSize(id, &streamAddress, 0, nil, &streamSize) == noErr, streamSize > 0 else {
                continue
            }

            // Name
            var nameAddress = AudioObjectPropertyAddress(
                mSelector: kAudioObjectPropertyName,
                mScope: kAudioObjectPropertyScopeGlobal,
                mElement: kAudioObjectPropertyElementMain
            )
            var unmanagedName: Unmanaged<CFString>?
            var nameSize = UInt32(MemoryLayout<Unmanaged<CFString>?>.size)
            guard AudioObjectGetPropertyData(id, &nameAddress, 0, nil, &nameSize, &unmanagedName) == noErr,
                  let cfName = unmanagedName?.takeRetainedValue() else {
                continue
            }
            let name = cfName as String

            // UID
            var uidAddress = AudioObjectPropertyAddress(
                mSelector: kAudioDevicePropertyDeviceUID,
                mScope: kAudioObjectPropertyScopeGlobal,
                mElement: kAudioObjectPropertyElementMain
            )
            var unmanagedUID: Unmanaged<CFString>?
            var uidSize = UInt32(MemoryLayout<Unmanaged<CFString>?>.size)
            var uid = ""
            if AudioObjectGetPropertyData(id, &uidAddress, 0, nil, &uidSize, &unmanagedUID) == noErr,
               let cfUID = unmanagedUID?.takeRetainedValue() {
                uid = cfUID as String
            }

            // Transport Type
            var transportAddress = AudioObjectPropertyAddress(
                mSelector: kAudioDevicePropertyTransportType,
                mScope: kAudioObjectPropertyScopeGlobal,
                mElement: kAudioObjectPropertyElementMain
            )
            var transport: UInt32 = 0
            var transportSize = UInt32(MemoryLayout<UInt32>.size)
            var transportStr = "Built-in"
            if AudioObjectGetPropertyData(id, &transportAddress, 0, nil, &transportSize, &transport) == noErr {
                switch transport {
                case kAudioDeviceTransportTypeBluetooth, kAudioDeviceTransportTypeBluetoothLE:
                    transportStr = "Bluetooth"
                case kAudioDeviceTransportTypeUSB:
                    transportStr = "USB"
                case kAudioDeviceTransportTypeDisplayPort, kAudioDeviceTransportTypeHDMI:
                    transportStr = "HDMI / DisplayPort"
                case kAudioDeviceTransportTypeAirPlay:
                    transportStr = "AirPlay"
                default:
                    transportStr = "Built-in"
                }
            }

            // Nominal Sample Rate
            var rateAddress = AudioObjectPropertyAddress(
                mSelector: kAudioDevicePropertyNominalSampleRate,
                mScope: kAudioObjectPropertyScopeGlobal,
                mElement: kAudioObjectPropertyElementMain
            )
            var sampleRate: Float64 = 44100.0
            var rateSize = UInt32(MemoryLayout<Float64>.size)
            _ = AudioObjectGetPropertyData(id, &rateAddress, 0, nil, &rateSize, &sampleRate)

            let isDefault = (id == defaultDeviceID)

            outputList.append(AudioOutputDevice(
                id: id,
                uid: uid,
                name: name,
                transportType: transportStr,
                isDefault: isDefault,
                sampleRate: Double(sampleRate)
            ))
        }

        return outputList
    }

    private func getDefaultOutputDeviceID() -> AudioDeviceID {
        var defaultAddress = AudioObjectPropertyAddress(
            mSelector: kAudioHardwarePropertyDefaultOutputDevice,
            mScope: kAudioObjectPropertyScopeGlobal,
            mElement: kAudioObjectPropertyElementMain
        )
        var defaultID: AudioDeviceID = 0
        var idSize = UInt32(MemoryLayout<AudioDeviceID>.size)
        _ = AudioObjectGetPropertyData(AudioObjectID(kAudioObjectSystemObject), &defaultAddress, 0, nil, &idSize, &defaultID)
        return defaultID
    }

    private func setupDeviceListener() {
        var address = AudioObjectPropertyAddress(
            mSelector: kAudioHardwarePropertyDevices,
            mScope: kAudioObjectPropertyScopeGlobal,
            mElement: kAudioObjectPropertyElementMain
        )

        let block: AudioObjectPropertyListenerBlock = { [weak self] _, _ in
            self?.refreshDevices()
        }
        self.listenerBlock = block

        AudioObjectAddPropertyListenerBlock(
            AudioObjectID(kAudioObjectSystemObject),
            &address,
            DispatchQueue.main,
            block
        )
    }

    private func removeDeviceListener() {
        if let block = listenerBlock {
            var address = AudioObjectPropertyAddress(
                mSelector: kAudioHardwarePropertyDevices,
                mScope: kAudioObjectPropertyScopeGlobal,
                mElement: kAudioObjectPropertyElementMain
            )
            AudioObjectRemovePropertyListenerBlock(
                AudioObjectID(kAudioObjectSystemObject),
                &address,
                DispatchQueue.main,
                block
            )
        }
    }

    /// Selects an output device and routes the AVAudioEngine output unit directly to it
    public func selectDevice(_ device: AudioOutputDevice, on engine: AVAudioEngine) {
        guard let outputUnit = engine.outputNode.audioUnit else { return }

        var deviceID = device.id
        let status = AudioUnitSetProperty(
            outputUnit,
            kAudioOutputUnitProperty_CurrentDevice,
            kAudioUnitScope_Global,
            0,
            &deviceID,
            UInt32(MemoryLayout<AudioDeviceID>.size)
        )

        if status == noErr {
            DispatchQueue.main.async {
                self.selectedDevice = device
            }
        } else {
            print("[AudioDeviceManager] Failed to set output device \(device.name): \(status)")
        }
    }
}
