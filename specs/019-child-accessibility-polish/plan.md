# Plan 019: Pulir accesibilidad infantil (FLE-44)

## Contexto

El modo hijo ya cubre tareas, objetivo activo, cash-out, parent zone y ledger. Tras FLE-43 los vacios
son accionables, pero la pantalla sigue siendo muy textual: no hay iconos en botones, no hay semantica
Compose explicita y algunos elementos de estado dependen demasiado de texto/color suelto.

## Checkpoint Movil

- Plataforma: Android + iOS.
- Framework: Kotlin Multiplatform + Compose Multiplatform.
- Referencias leidas:
  - `mobile-design-thinking.md`
  - `touch-psychology.md`
  - `mobile-performance.md`
  - `mobile-backend.md`
  - `mobile-testing.md`
  - `mobile-debugging.md`
  - `mobile-navigation.md`
  - `mobile-typography.md`
  - `mobile-color-system.md`
  - `decision-trees.md`
  - `platform-ios.md`
  - `platform-android.md`
- Principios:
  - 56dp para acciones principales infantiles y 48dp para secundarias.
  - Icono + etiqueta corta en acciones; no depender solo del texto largo.
  - Color siempre reforzado por texto/icono para estados.
- Anti-patrones a evitar:
  - Gestos ocultos o acciones solo por color.
  - Alturas fijas que recorten texto grande.

## Enfoque Tecnico

- Crear un contrato comun `ChildHomeAccessibility.kt` en `childhome`:
  - `ChildHomeActionKind`, `ChildHomeActionPriority`, `ChildHomeIconKey`.
  - `ChildHomeActionSpec` con `minTouchTargetDp`, `iconKey`, `contentDescriptionKey` y prioridad.
  - Helpers puros para acciones de tarea, objetivo, cash-out, parent zone y empty states.
- Escribir tests de contrato en `commonTest`:
  - AC-01: acciones principales tienen icono y descripcion.
  - AC-02: targets minimos por prioridad.
  - AC-03: cada accion interactiva tiene descripcion semantica.
  - AC-04: estados de tarea/objetivo exponen icono/texto, no solo color.
  - AC-05: los specs no fuerzan alturas exactas; Compose usa `heightIn`.
- Actualizar `ChildHomeScreen.kt`:
  - Introducir componente local de boton infantil icono+texto basado en Material 3.
  - Usar pictogramas locales sin dependencia nueva para mantener compilacion KMP estable.
  - Anadir `Modifier.semantics { role = Role.Button; contentDescription = ... }` en acciones.
  - Reforzar `TaskStatusPill`, progreso/objetivo y cash-out con icono/texto.
  - Mantener `LazyColumn` y `heightIn`, evitando alturas exactas en acciones con texto.
- Actualizar recursos:
  - Descripciones accesibles ES/EN para acciones y estados.
  - Etiquetas cortas si alguna accion actual resulta demasiado larga.
- Actualizar Pencil:
  - Variante `Home hijo - accessible active`.
  - Variante `Home hijo - accessible empty`.
  - Validar layout sin solapes.

## Ficheros A Tocar

- `shared/src/commonMain/kotlin/com/apptolast/fledge/presentation/foundation/childhome/ChildHomeAccessibility.kt`
- `shared/src/commonMain/kotlin/com/apptolast/fledge/presentation/foundation/childhome/ChildHomeScreen.kt`
- `shared/src/commonMain/composeResources/values/strings.xml`
- `shared/src/commonMain/composeResources/values-en/strings.xml`
- `shared/src/commonTest/kotlin/com/apptolast/fledge/presentation/ChildHomeAccessibilityTest.kt`
- `gradle/libs.versions.toml` y `shared/build.gradle.kts` solo si hace falta dependencia de iconos.
- `specs/019-child-accessibility-polish/spec.md`
- `specs/019-child-accessibility-polish/plan.md`
- `Fledge.pen` via Pencil MCP.

## Impacto En Plataformas

- `commonMain`: UI Compose compartida Android/iOS.
- `androidMain`: sin actuals nuevos; TalkBack consume semantica Compose.
- `iosMain`: sin actuals nuevos; VoiceOver consume semantica Compose.
- Web/desktop: sin objetivo especifico, pero el compile comun debe mantenerse.

## DI / Arquitectura

- No hay nuevos repositorios, Koin ni expect/actual.
- El contrato de accesibilidad vive en presentation porque describe decisiones de UI, no dominio.

## Tareas

1. [x] Revisar Jira, repo y estado del modo hijo.
2. [x] Actualizar Pencil con variantes accesibles de modo hijo.
3. [x] Escribir tests rojos de contrato accesible para AC-01..AC-05.
4. [x] Implementar contrato, strings, iconografia y semantica Compose.
5. [x] Formatear, validar Gradle/iOS y abrir PR contra `develop`.

## Validacion Esperada

```bash
./gradlew :shared:testAndroid --console=plain --no-configuration-cache
./gradlew ktlintFormat --console=plain --no-configuration-cache
./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache
git diff --check
```

## Evidencia

- Jira FLE-44 movido a `In Progress`.
- Rama creada: `feature/FLE-44-child-accessibility-polish`.
- Pencil actualizado:
  - `hXwig` (`Screen / Home hijo - accessible active`).
  - `gpMQ8` (`Screen / Home hijo - accessible empty`).
  - `snapshot_layout(problemsOnly=true)` sin problemas en ambas variantes.
  - Exports: `/tmp/fledge-fle44-accessibility/hXwig.png`,
    `/tmp/fledge-fle44-accessibility/gpMQ8.png`.
- Test rojo inicial: `:shared:testAndroid` fallo por contrato `ChildHomeAccessibility` inexistente.
- Test verde: `./gradlew :shared:testAndroid --console=plain --no-configuration-cache`.
- Formato: `./gradlew ktlintFormat --console=plain --no-configuration-cache`.
- Validacion completa: `./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`.
- Whitespace: `git diff --check`.
