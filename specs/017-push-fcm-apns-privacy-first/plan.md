# Plan 017: Push FCM/APNs privacy-first (FLE-42)

## Contexto

Fledge ya usa BaseLogin para Auth y GitLive Firestore con `FirebaseOptions` construidas desde
BuildKonfig. La receta de la flota para push recomienda KMPNotifier 1.6.1, pero su integracion nativa
requiere `google-services.json`/`GoogleService-Info.plist` y Firebase Messaging. Para este primer corte,
FLE-42 prepara el contrato de datos y seguridad sin cambiar el bootstrap Firebase actual.

## Diseno Tecnico

- Dominio:
  - Crear `PushRegistration`, `PushInstallationId`, `PushRegistrationId`, `PushToken` y enums de
    plataforma/estado.
  - Validar invariantes de rol: `Parent` sin `childProfileId`, `Child` con `childProfileId`.
  - Crear `PushPayloadSanitizer` con lista blanca de claves de routing.
- Repositorio:
  - Interfaz `PushRegistrationRepository`.
  - `FirestorePushRegistrationRepository` escucha `families/{familyId}/pushRegistrations`.
  - Upsert determinista por `installationId + role + childProfileId + platform`, para no duplicar
    tokens cuando FCM/APNs rota el token de la instalacion.
  - Desactivacion con `status=Inactive` y `updatedAt`, no delete.
- Seguridad:
  - Nueva subcoleccion `pushRegistrations`.
  - Padres: create/read/update en su familia con campos validos.
  - Hijos: create/read/update solo si `role == "Child"` y `childProfileId == authChildProfileId()`.
  - `payloadData` no se persiste en registros de dispositivo; la privacidad del envio vive en el
    sanitizador y en el futuro emisor server-side.
- DI:
  - Registrar `FirestorePushRegistrationRepository` en `dataModule`.
- Documentacion:
  - Crear guia corta de setup/auditoria para la futura fase de FCM/APNs real y la decision pendiente
    sobre KMPNotifier vs bootstrap explicito.

## Tareas

1. [x] Tests rojos de modelo de registro push.
2. [x] Tests rojos de repositorio in-memory/upsert/desactivacion.
3. [x] Tests rojos de sanitizador de payloads.
4. [x] Tests rojos de reglas Firestore.
5. [x] Implementar dominio, repositorios, mappers y DI.
6. [x] Actualizar reglas y documentacion de auditoria SDK.
7. [ ] Formatear, validar Gradle/rules/iOS y abrir PR.

## Validacion Esperada

```bash
./gradlew :shared:testAndroid --console=plain --no-configuration-cache
./gradlew ktlintFormat --console=plain --no-configuration-cache
./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache
npm test
git diff --check
xcodebuild -quiet -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

## Evidencia

- Rojo previo: `./gradlew :shared:testAndroid --console=plain --no-configuration-cache` fallo en
  `:shared:compileAndroidHostTest` por referencias esperadas aun sin implementar
  (`PushRegistration*`, `PushPayloadSanitizer`, `PushRegistrationRepository`,
  `FirestorePushRegistrationRepository`).
- Rojo previo: `npm run test:rules` fallo en `push registrations are private by role and child ownership`
  por no existir allow statements para `pushRegistrations`.
- Verde: `./gradlew :shared:testAndroid --console=plain --no-configuration-cache`.
- Verde: `npm run test:rules`.
- Verde: `./gradlew ktlintFormat --console=plain --no-configuration-cache`.
- Verde: `./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`.
- Verde: `npm test`.
- Verde: `git diff --check`.
- Verde: `xcodebuild -quiet -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`.
