# Push Notifications Setup

FLE-42 establishes the privacy-first data contract for FCM/APNs registration. It does not enable
native delivery yet.

## Current Contract

- Device tokens are stored under `families/{familyId}/pushRegistrations/{registrationId}`.
- A registration contains only routing metadata:
  - `familyId`
  - `childProfileId` only for child registrations
  - `installationId`
  - `token`
  - `platform`
  - `role`
  - `status`
  - `updatedAt`
- Notification payload data must be built through the shared sanitizer and must only contain routing
  ids such as `type`, `familyId`, `childProfileId`, `taskInstanceId`, `savingsGoalId` or `settlementId`.
- Do not include names, task titles, savings goal titles, money amounts or ledger concepts in FCM/APNs
  payload data.

## SDK Audit

Fledge currently initializes Firebase with explicit `FirebaseOptions` from BuildKonfig/local.properties.
The AppToLast push recipe uses KMPNotifier 1.6.1, which expects Firebase Messaging configured natively:

- Android: `google-services.json`, Google Services Gradle plugin and `POST_NOTIFICATIONS` runtime
  permission on Android 13+.
- iOS: `GoogleService-Info.plist`, FirebaseMessaging via SPM, Push Notifications capability,
  Background Modes/Remote notifications and APNs `.p8` configured in Firebase Console.

Before enabling native delivery, decide whether Fledge keeps the explicit BuildKonfig bootstrap and
adds Messaging manually per platform, or adopts the standard KMPNotifier/google-services setup for this
app. That decision belongs to a later push-delivery task, not FLE-42.
