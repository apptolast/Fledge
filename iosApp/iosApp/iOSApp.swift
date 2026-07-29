import SwiftUI
import Shared
import FirebaseMessaging
import UserNotifications

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    private var messagingConfigured = false
    private var pendingDeviceToken: Data?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, _ in
            if granted {
                DispatchQueue.main.async {
                    application.registerForRemoteNotifications()
                }
            }
        }
        return true
    }

    func configureMessagingAfterFirebaseBootstrap() {
        if messagingConfigured {
            return
        }
        FledgePushIosKt.initializeFledgePushNotificationsIos()
        Messaging.messaging().delegate = self
        messagingConfigured = true
        applyPendingApnsToken()
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        pendingDeviceToken = deviceToken
        applyPendingApnsToken()
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        if let fcmToken {
            print("[FCM] Token recibido: \(fcmToken)")
        }
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        FledgePushIosKt.handleFledgeNotificationTap(
            type: userInfo["type"] as? String,
            familyId: userInfo["familyId"] as? String,
            childProfileId: userInfo["childProfileId"] as? String,
            taskInstanceId: userInfo["taskInstanceId"] as? String
        )
        completionHandler()
    }

    private func applyPendingApnsToken() {
        guard messagingConfigured, let pendingDeviceToken else {
            return
        }
        Messaging.messaging().apnsToken = pendingDeviceToken
        self.pendingDeviceToken = nil
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    init() {
        SocialAuthCoordinator.shared.registerBridges()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear {
                    delegate.configureMessagingAfterFirebaseBootstrap()
                }
        }
    }
}
