import SwiftUI

@main
struct iOSApp: App {
    init() {
        SocialAuthCoordinator.shared.registerBridges()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
