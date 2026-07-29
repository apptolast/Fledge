# Plan 010: Recordatorios de cola de aprobacion acumulada (FLE-33)

## Contexto

FLE-33 depende de FLE-32 porque reutiliza los topics de padres, KMPNotifier y el puente de deep
links de push. Esta rama se mantiene apilada sobre `feature/FLE-32-task-approval-push` mientras el
PR #14 no este mergeado.

## Diseno

- Implementar scheduler Gen2 `runApprovalQueueReminders` y variante debug.
- Usar `collectionGroup("taskInstances")` filtrado por `status=Submitted`.
- Agrupar en memoria por familia; en esta fase el volumen esperado es bajo y se limita el batch.
- Condiciones:
  - `submittedCount > 5`.
  - O `oldestSubmittedAt <= now - 72h`.
- Cooldown:
  - Guardar `approvalQueueReminderLastSentAt` en `families/{familyId}`.
  - No reenviar si el ultimo recordatorio fue hace menos de 24h.
- Payload:
  - `type=approval_queue_reminder`.
  - `familyId`, `pendingCount`, `oldestSubmittedAt`.
- Deep link:
  - Reutilizar `ParentHomeRoute` mediante `DeepLink.ParentApprovalQueue`.
- Pencil/design-check: N/A, sin UI nueva.

## Tareas

1. Tests rojos de Functions para volumen, antiguedad, bajo umbral y cooldown.
2. Test comun de deep link para `approval_queue_reminder`.
3. Implementar tipo de push y topic alias en common.
4. Implementar scheduler y helpers de mensaje en Functions.
5. Validar Gradle/npm/checks.
6. Desplegar las Functions nuevas si la validacion pasa.

## Validacion Esperada

```bash
./gradlew ktlintFormat --console=plain --no-configuration-cache
./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache
npm test
npm --prefix functions run check
git diff --check
firebase deploy --project fledge-c685d --only functions:functions:runApprovalQueueReminders,functions:functions:runApprovalQueueRemindersDebug
```

## Evidencia De Validacion

- `npm --prefix functions test` — rojo esperado antes de implementar por exports ausentes.
- `./gradlew :shared:testAndroid --console=plain --no-configuration-cache` — rojo esperado antes de implementar por tipo de push/deep link ausente.
- `./gradlew ktlintFormat --console=plain --no-configuration-cache` — OK.
- `./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache` — OK.
- `npm test` — OK.
- `npm --prefix functions run check` — OK.
- `git diff --check` — OK.
- `xcodebuild -quiet -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build` — OK.
- `firebase deploy --project fledge-c685d --only functions:functions:runApprovalQueueReminders,functions:functions:runApprovalQueueRemindersDebug` — OK.
