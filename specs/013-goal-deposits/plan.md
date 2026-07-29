# Plan 013: Depositos desde MAIN a objetivo (FLE-37)

## Contexto

FLE-36 introduce `SavingsGoal` y muestra el objetivo activo con progreso contra `balances.goal`.
FLE-37 debe mover saldo entre cuentas virtuales sin perder auditoria.

## Diseno

- Ledger:
  - `LedgerTransferGroupId` enlaza los dos apuntes.
  - `LedgerTransaction` y `LedgerTransactionDraft` guardan `transferGroupId`.
  - `LedgerRepository.appendTransferPair` crea el par en una sola operacion logica.
- Servicio:
  - `SavingsGoalDepositProcessor` valida objetivo activo, propiedad, importe y saldo.
  - Crea `GoalTransfer` debitando `Main` y abonando `Goal`.
- Firestore:
  - `FirestoreLedgerRepository` usa `WriteBatch` de GitLive para escribir el par.
  - Mapper conserva `transferGroupId`.
- UI:
  - `SavingsGoalDepositScreen` con importe editable y resumen del par.
  - Home hijo navega desde objetivo activo.
- Pencil:
  - `Screen / Depositar a objetivo` creado y validado sin problemas de layout.

## Tareas

1. [x] Tests rojos de processor y trazabilidad ledger.
2. [x] Tests rojos de mapper/DI/ViewModel.
3. [x] Implementar modelo ledger y repositorios.
4. [x] Implementar processor y DI.
5. [x] Implementar pantalla Compose y navegacion.
6. [x] Actualizar i18n y spec evidence.
7. [x] Validar Gradle/npm/rules/iOS y crear PR.

## Validacion Esperada

```bash
./gradlew ktlintFormat --console=plain --no-configuration-cache
./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache
npm test
git diff --check
xcodebuild -quiet -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

## Evidencia

- Rojo previo: `./gradlew :shared:testAndroid --console=plain --no-configuration-cache` fallo por referencias esperadas sin implementar.
- Verde: `./gradlew :shared:testAndroid --console=plain --no-configuration-cache`.
- Verde: `./gradlew ktlintFormat --console=plain --no-configuration-cache`.
- Verde: `./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`.
- Verde: `npm test`.
- Verde: `git diff --check`.
- Verde: `xcodebuild -quiet -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`.
- Pencil: `Screen / Depositar a objetivo` (`upmZH`) con `snapshot_layout(problemsOnly=true)` sin problemas.
