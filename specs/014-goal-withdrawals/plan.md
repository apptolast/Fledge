# Plan 014: Retiradas desde objetivo a MAIN (FLE-38)

## Contexto

FLE-37 introduce transferencias trazables MAIN -> GOAL. FLE-38 reutiliza el mismo ledger pair en sentido
inverso y anade friccion educativa en la UI.

## Diseno

- Ledger:
  - Generalizar la validacion de `appendTransferPair` para pares `Main <-> Goal`.
  - La retirada debita `Goal` y abona `Main` con el mismo `transferGroupId`.
- Servicio:
  - `SavingsGoalWithdrawalProcessor` valida objetivo activo, propiedad, importe y saldo `Goal`.
- UI:
  - `SavingsGoalWithdrawalViewModel` exige confirmacion del coste de oportunidad.
  - `SavingsGoalWithdrawalScreen` muestra importe editable, resumen `GOAL -> MAIN` y checkbox de confirmacion.
  - Home hijo navega desde objetivo activo.
- Pencil:
  - `Screen / Retirar de objetivo` creado y validado sin problemas de layout.

## Tareas

1. [x] Tests rojos de processor y confirmacion educativa.
2. [x] Tests rojos de ViewModel/DI.
3. [x] Generalizar ledger pair para transferencias `Main <-> Goal`.
4. [x] Implementar withdrawal processor y DI.
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
- Pencil: `Screen / Retirar de objetivo` (`RA2pJ`) con `snapshot_layout(problemsOnly=true)` sin problemas.
