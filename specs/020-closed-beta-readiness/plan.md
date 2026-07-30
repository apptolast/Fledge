# Plan 020: Preparar beta cerrada con 10-15 familias (FLE-45)

## Enfoque Tecnico

- Crear `ClosedBetaReadiness.kt` en `shared/src/commonMain/.../domain/service/`.
- Exponer modelos puros:
  - `ClosedBetaMetricSnapshot`: solo agregados, sin ids ni datos personales.
  - `ClosedBetaReadinessThresholds`: umbrales por defecto del ticket.
  - `ClosedBetaMetricStatus`: `Pass`, `NeedsMoreData`, `Fail`.
  - `ClosedBetaReadinessStatus`: `Ready`, `NeedsMoreData`, `AtRisk`.
  - `ClosedBetaReadinessReport`: resumen de cada metrica y decision global.
- Implementar `ClosedBetaReadinessEvaluator.evaluate(snapshot, thresholds)`.
- Documentar `docs/CLOSED_BETA_READINESS.md` como runbook operativo.

## Decisiones

- No se anade Firebase Analytics ni SDK de tracking: el contrato admite datos ya agregados.
- La app infantil sigue sin tracking conductual ni identificadores analiticos.
- El criterio de aprobacion usa media, alineado con Confluence `Fledge - Delivery Plan`.
- Cash-out usa media request-to-confirm y bloquea si existe un cash-out abierto con mas de 72 h.
- Saldos requieren al menos 28 dias de observacion antes de poder pasar a `Pass`.

## Ficheros A Tocar

- `shared/src/commonMain/kotlin/com/apptolast/fledge/domain/service/ClosedBetaReadiness.kt`
- `shared/src/commonTest/kotlin/com/apptolast/fledge/domain/ClosedBetaReadinessEvaluatorTest.kt`
- `docs/CLOSED_BETA_READINESS.md`
- `specs/020-closed-beta-readiness/spec.md`
- `specs/020-closed-beta-readiness/plan.md`

## Impacto En Plataformas

- `commonMain`: logica pura multiplataforma.
- Android/iOS: sin cambios directos.
- UI/Pencil: N/A.
- Koin/DI: N/A, servicio puro sin repositorios.
- i18n: N/A, no hay strings de UI.

## Tareas

1. [x] Revisar Jira, Confluence y repo.
2. [x] Crear spec/plan y marcar design-check N/A.
3. [x] Escribir tests rojos de readiness para AC-01..AC-06.
4. [x] Implementar evaluador y runbook.
5. [x] Validar Gradle y abrir PR contra `develop`.

## Validacion Esperada

```bash
./gradlew :shared:testAndroid --console=plain --no-configuration-cache
./gradlew ktlintFormat --console=plain --no-configuration-cache
./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache
git diff --check
```

## Evidencia

- Jira FLE-45 movido a `In Progress`.
- Confluence revisado:
  - `Fledge - MVP Scope`
  - `Fledge - Delivery Plan`
  - `Fledge - Compliance and Risk`
- Design-check: N/A porque no hay impacto de UI.
- Test rojo inicial: `:shared:testAndroid` fallo por contrato `ClosedBetaReadiness` inexistente.
- Test verde: `./gradlew :shared:testAndroid --console=plain --no-configuration-cache`.
- Formato: `./gradlew ktlintFormat --console=plain --no-configuration-cache`.
- Validacion completa: `./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`.
