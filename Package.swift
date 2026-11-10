// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "HEX",
    platforms: [
        .macOS(.v14)
    ],
    products: [
        .executable(
            name: "HEX",
            targets: ["HEX"]
        ),
        .executable(
            name: "Auralis",
            targets: ["HEX"]
        ),
        .executable(
            name: "VerifyAudioEngine",
            targets: ["VerifyAudioEngine"]
        ),
        .library(
            name: "AuralisKit",
            targets: ["AuralisKit"]
        )
    ],
    dependencies: [],
    targets: [
        .target(
            name: "AuralisKit",
            dependencies: [],
            path: "Sources/AuralisKit"
        ),
        .executableTarget(
            name: "HEX",
            dependencies: ["AuralisKit"],
            path: "Sources/HEX"
        ),
        .executableTarget(
            name: "VerifyAudioEngine",
            dependencies: ["AuralisKit"],
            path: "Sources/VerifyAudioEngine"
        )
    ]
)
