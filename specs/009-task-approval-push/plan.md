# Plan 009: Push de submission y aprobacion (FLE-32)

## Contexto

FLE-32 depende de FLE-31 porque los eventos relevantes nacen de las transiciones
`Pending/Rejected -> Submitted` y `Submitted -> Approved`. Esta rama queda apilada sobre
`feature/FLE-31-parent-approval-queue` mientras PR #13 no este mergeado.

## Diseno

- Reutilizar Firebase/KMPNotifier como receta canónica de la flota.
- Evitar almacenamiento de tokens en Firestore para no abrir reglas/modelo de device-token en esta fase.
- Usar topics deterministas y sanitizados:
  - `fledge_debug_family_<id>_parents` o `fledge_release_family_<id>_parents`.
  - `fledge_debug_family_<id>_child_<id>` o `fledge_release_family_<id>_child_<id>`.
- Mantener textos de notificacion en Functions; son UI visible y deben ir en espanol.
- Deep link minimo:
  - submission -> ParentHome.
  - approved -> ChildPin del perfil.
- Pencil/design-check: N/A, sin pantallas nuevas.

## Tareas

1. Tests rojos de topic/deep-link/common notification initializer.
2. Tests rojos de Functions para payloads `task_submitted` y `task_approved`.
3. Anadir dependencia `kmpnotifier` y `firebase-messaging`.
4. Implementar `TaskPushTopics`, `NotificationInitializer` y `DeepLinkManager`.
5. Suscribir padre en `PostLoginViewModel` y hijo en `ChildPinViewModel`.
6. Inicializar notificaciones y permisos en Android.
7. Cablear APNs/FCM/iOS tap en Swift sin `GoogleService-Info.plist`.
8. Implementar trigger Functions para default y debug.
9. Validar Gradle, npm, rules/functions y desplegar Functions.

## Validacion Esperada

```bash
./gradlew ktlintFormat --console=plain --no-configuration-cache
./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache
npm test
npm --prefix functions run check
git diff --check
firebase deploy --project fledge-c685d --only functions
```

## Evidencia De Validacion

- `./gradlew ktlintFormat --console=plain --no-configuration-cache` — OK.
- `./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache` — OK.
- `npm test` — OK.
- `npm --prefix functions run check` — OK.
- `git diff --check` — OK.
- `xcodebuild -quiet -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build` — OK.
- `firebase deploy --project fledge-c685d --only functions:functions:notifyTaskInstancePush,functions:functions:notifyTaskInstancePushDebug` — OK en segundo intento tras propagacion de Eventarc.
