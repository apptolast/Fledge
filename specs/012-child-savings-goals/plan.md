# Plan 012: Objetivos de ahorro infantiles (FLE-36)

## Contexto

Fledge ya tiene `VirtualAccountType.Goal` y calcula balances GOAL desde ledger, pero aun no existe una
entidad visible de objetivo de ahorro con meta, titulo e icono/imagen.

## Diseno

- Dominio:
  - `SavingsGoalId`, `SavingsGoalStatus`, `SavingsGoal`, `SavingsGoalDraft`.
  - Validaciones de titulo no vacio, `targetCents > 0`, `accountType == Goal` y al menos `iconKey`
    o `imageUri`.
- Repositorio:
  - `SavingsGoalRepository` con `StateFlow<List<SavingsGoal>>`, `saveGoal`, `goalsForChild` y
    `activeGoalForChild`.
  - `FirestoreSavingsGoalRepository` en `families/{familyId}/savingsGoals/{goalId}`.
  - `InMemorySavingsGoalRepository` para tests.
- UI:
  - `SavingsGoalSetupScreen` para padre.
  - Parent Home navega a crear objetivo desde cada hijo.
  - Child Home renderiza la tarjeta de objetivo activo con progreso calculado desde `balances.goal`.
- Rules:
  - Padres pueden crear/actualizar objetivos validos.
  - Hijos solo leen sus objetivos.
- Pencil:
  - `Screen / Crear objetivo de ahorro` creado y sin problemas de layout.

## Tareas

1. [x] Tests rojos de modelo y repositorio.
2. [x] Tests rojos de rules.
3. [x] Tests rojos de ViewModel/DI para crear objetivo y Home hijo.
4. [x] Implementar modelo, mapper, repo Firestore, fake y DI.
5. [x] Implementar formulario Compose y navegación.
6. [x] Actualizar Home hijo y strings i18n.
7. [x] Validar Gradle/npm/rules y crear PR.

## Validacion Esperada

```bash
./gradlew ktlintFormat --console=plain --no-configuration-cache
./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache
npm test
git diff --check
```

## Validacion Ejecutada

```bash
./gradlew ktlintFormat --console=plain --no-configuration-cache
./gradlew :shared:testAndroid --console=plain --no-configuration-cache
./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache
npm test
firebase --project fledge-rules-test --config firebase.rules-test.json emulators:exec --only firestore "node --test firestore.rules.test.mjs"
git diff --check
xcodebuild -quiet -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
firebase deploy --project fledge-c685d --only firestore:rules
```
