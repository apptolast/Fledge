# Plan 016: Celebracion de objetivo completado (FLE-40)

## Contexto

FLE-36 crea y muestra el objetivo activo, FLE-37 mueve dinero `Main -> Goal`, FLE-38 permite sacar
dinero del objetivo y FLE-39 calcula progreso/proyeccion. FLE-40 usa ese estado para convertir el
momento de llegada al target en feedback visible para hijo y padre.

## Diseno

- Dominio:
  - `SavingsGoalCompletionNotifier` calcula `SavingsGoalCompletionNotice` a partir de objetivo activo,
    saldo `Goal`, hijo y `now`.
  - El detector solo emite avisos para objetivos `Active` cuyo saldo cubre `targetCents`.
  - No persiste cambios ni muta el estado del objetivo; el ledger sigue siendo la fuente de saldo.
- Presentacion:
  - `ChildHomeUiState` expone `activeSavingsGoalCompletionNotice`.
  - `ChildHomeViewModel` recalcula el aviso al cambiar ledger, objetivo o hijo activo.
  - `ParentHomeUiState` expone `goalCompletionNotices`.
  - `ParentHomeViewModel` inyecta `SavingsGoalRepository` y recalcula avisos al cambiar hijos, ledger u objetivos.
- UI/i18n:
  - Home hijo muestra una banda celebratoria bajo la tarjeta de objetivo.
  - Home padre muestra avisos antes del listado de hijos.
  - Todas las cadenas van en `composeResources/values{,-en}/strings.xml`.
- Pencil:
  - Actualizar `Screen / Home hijo` y `Screen / Home padre` con el aviso in-app.

## Tareas

1. [x] Tests rojos del detector de completado.
2. [x] Tests rojos de Home hijo y Home padre.
3. [x] Implementar modelo/detector de completado.
4. [x] Cablear ViewModels y DI.
5. [x] Actualizar UI, previews e i18n.
6. [x] Actualizar Pencil y validar layout.
7. [x] Formatear, validar Gradle/iOS/functions y abrir PR.

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
  (`SavingsGoalCompletionNotifier`, `activeSavingsGoalCompletionNotice`,
  `goalCompletionNotices` y nuevo constructor de `ParentHomeViewModel`).
- Verde: `./gradlew :shared:testAndroid --console=plain --no-configuration-cache`.
- Verde: `./gradlew ktlintFormat --console=plain --no-configuration-cache`.
- Verde: `./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`.
- Verde: `npm test`.
- Verde: `git diff --check`.
- Verde: `xcodebuild -quiet -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`.
- Pencil: `Screen / Home hijo` (`eYef1`) y `Screen / Home padre` (`Q2S0E`) con
  `snapshot_layout(problemsOnly=true)` sin problemas.
