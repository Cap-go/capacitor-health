// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapgoCapacitorHealth",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapgoCapacitorHealth",
            targets: ["HealthPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.1.0")
    ],
    targets: [
        .target(
            name: "HealthPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/HealthPlugin",
            linkerSettings: [
                .linkedFramework("HealthKit")
            ]),
        .testTarget(
            name: "HealthPluginTests",
            dependencies: ["HealthPlugin"],
            path: "ios/Tests/HealthPluginTests")
    ]
)
