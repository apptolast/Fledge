# Plan 015: Progreso y proyeccion temporal de objetivo (FLE-39)

## Contexto

FLE-36 muestra el objetivo activo en Home hijo, FLE-37 mueve dinero `Main -> Goal` y FLE-38 permite
retirar `Goal -> Main`. FLE-39 usa esos apuntes append-only para convertir la tarjeta del objetivo en
feedback temporal accionable.

## Diseno

- Dominio:
  - `SavingsGoalProjectionCalculator` calcula porcentaje, restante, ritmo diario neto y fecha estimada.
  - El ritmo usa transacciones de la cuenta `Goal` del mismo hijo/familia posteriores a `goal.createdAt`.
  - Si el objetivo ya esta completo, no fuerza fecha futura.
  - Si el ritmo neto diario no es positivo, la proyeccion queda sin fecha.
- Presentacion:
  - `ChildHomeUiState` expone `activeSavingsGoalProjection`.
  - `ChildHomeViewModel` recalcula la proyeccion al cambiar ledger, objetivo o child activo.
  - `ActiveSavingsGoalCard` muestra progreso, restante, ritmo y banda de fecha/sin ritmo.
- i18n:
  - Nuevas claves en `values/strings.xml` y `values-en/strings.xml`.
- Pencil:
  - `Screen / Home hijo` actualizado y validado sin problemas de layout.

## Tareas

1. [x] Tests rojos de calculadora de proyeccion.
2. [x] Test rojo de ViewModel para Home hijo.
3. [x] Implementar calculadora y modelo de proyeccion.
4. [x] Cablear proyeccion en `ChildHomeViewModel`.
5. [x] Actualizar `ActiveSavingsGoalCard` e i18n.
6. [x] Formatear, validar Gradle/iOS y abrir PR.

## Validacion Esperada

```bash
./gradlew ktlintFormat --console=plain --no-configuration-cache
./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache
npm test
git diff --check
xcodebuild -quiet -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

## Evidencia

- Rojo previo: `./gradlew :shared:testAndroid --console=plain --no-configuration-cache` fallo por referencias esperadas sin implementar (`SavingsGoalProjectionCalculator`, `SavingsGoalProjectionStatus`, `activeSavingsGoalProjection`).
- Verde: `./gradlew :shared:testAndroid --console=plain --no-configuration-cache`.
- Verde: `./gradlew ktlintFormat --console=plain --no-configuration-cache`.
- Verde: `./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`.
- Verde: `npm test`.
- Verde: `git diff --check`.
- Verde: `xcodebuild -quiet -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`.
- Pencil: `Screen / Home hijo` (`eYef1`) con `snapshot_layout(problemsOnly=true)` sin problemas.
