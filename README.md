This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

### Firebase Auth setup

Fledge delegates authentication to BaseLogin, which registers its GitLive-backed FirebaseAuthProvider.
Use Firebase project `fledge-c685d`. The project has these Firebase apps:

- Android package: `com.apptolast.fledge`
- iOS bundle: `com.apptolast.fledge`

Add the public client config to `local.properties`:

```properties
FIREBASE_API_KEY=...
FIREBASE_PROJECT_ID=fledge-c685d
GOOGLE_WEB_CLIENT_ID=...
APP_ENV=debug
```

Auth providers are deployed from `firebase.json` with:

```bash
firebase deploy --only auth --project fledge-c685d
```

Expected provider state:

- Email/Password enabled, password required.
- Google for Android. `GOOGLE_WEB_CLIENT_ID` must be the OAuth Web client ID.
- Apple for iOS, with bundle ID `com.apptolast.fledge`.
- Anonymous, phone, magic link and unsupported social providers disabled in the app.

For Google Sign-In, register SHA-1 and SHA-256 certificates for every Android keystore that will request
credentials: local debug, CI, and release. For Apple, the current iOS-native flow uses the Apple ID token
and raw nonce; configure the Services ID, Team ID, Key ID and private key only if a code-flow/web Apple
sign-in path is added later.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
