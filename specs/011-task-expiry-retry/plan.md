# Plan 011: Expiracion y reintento amable de tareas (FLE-34)

## Contexto

FLE-34 continua Fase 1 sobre la pila de FLE-32/FLE-33. Usa el modelo `TaskInstanceStatus.Expired`
ya existente, pero aun no habia scheduler ni flujo explicito de `Rejected -> Pending`.

## Diseno

- Scheduler:
  - `runTaskInstanceExpirations` y `runTaskInstanceExpirationsDebug`.
  - `collectionGroup("taskInstances")` con `status in ["Pending", "Rejected"]` y `dueAt <= now`.
  - Actualiza a `Expired`, `expiredAt=now`, limpia campos de envio/revision y no toca ledger.
- Dominio/repositorio:
  - `submittedForReview` solo acepta `Pending`.
  - `retriedForSameDay` acepta `Rejected` antes de `dueAt` y devuelve `Pending`.
  - `expiredBySystem` acepta `Pending` o `Rejected` vencidas.
- Reglas:
  - Cliente puede `Rejected -> Pending` solo si es el hijo propietario o padre y `updatedAt <= dueAt`.
  - Cliente no puede marcar `Expired`; solo Functions/Admin SDK.
- UI:
  - Rechazada muestra motivo como ayuda y CTA `Volver a intentar`.
  - Vencida muestra estado cerrado y texto amable, sin accion de envio.
- Pencil:
  - Se añadieron `Screen / Detalle tarea hijo - rechazada para repetir` y
    `Screen / Detalle tarea hijo - vencida sin culpa`.

## Tareas

1. Tests rojos de dominio y repositorio para retry/expired.
2. Tests rojos de Functions para caducidad.
3. Tests rojos de reglas para retry y bloqueo de expired cliente.
4. Actualizar modelo/repositorios.
5. Implementar scheduler Functions.
6. Ajustar UI child home y strings.
7. Validar Gradle/npm/rules y desplegar Functions nuevas.

## Validacion Esperada

```bash
./gradlew ktlintFormat --console=plain --no-configuration-cache
./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache
npm test
npm --prefix functions run check
git diff --check
firebase deploy --project fledge-c685d --only functions:functions:runTaskInstanceExpirations,functions:functions:runTaskInstanceExpirationsDebug
```

## Evidencia

- Tests rojos iniciales:
  - `npm --prefix functions test`: fallaba por export ausente de `processExpiredTaskInstances`.
  - `./gradlew :shared:testAndroid --console=plain --no-configuration-cache`: fallaba por APIs de retry/expired aun no implementadas.
  - `firebase --project fledge-rules-test --config firebase.rules-test.json emulators:exec --only firestore "node --test firestore.rules.test.mjs"`: fallaba porque `Rejected -> Pending` no estaba permitido.
- Pendiente de cierre:
  - Cerrado el 2026-07-29.
- Validacion verde:
  - `./gradlew ktlintFormat --console=plain --no-configuration-cache`
  - `./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`
  - `npm test`
  - `npm --prefix functions run check`
  - `git diff --check`
  - `xcodebuild -quiet -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`
- Deploy:
  - `firebase deploy --project fledge-c685d --only functions:functions:runTaskInstanceExpirations,functions:functions:runTaskInstanceExpirationsDebug`
  - Resultado: `runTaskInstanceExpirations` y `runTaskInstanceExpirationsDebug` creadas correctamente en `europe-west1`.
