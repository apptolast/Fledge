# Spec 020: Preparar beta cerrada con 10-15 familias (FLE-45)

> Rama: `feature/FLE-45-closed-beta-readiness` · Ticket: `FLE-45` · Parent: `FLE-41`

## Objetivo

FLE-45 prepara la beta cerrada para 10-15 familias con criterios de salida medibles y agregados:
aprobacion de tareas en menos de 24 h, cash-out en menos de 72 h y cero discrepancias de saldo tras
4 semanas reales. El resultado debe servir para decidir si la beta esta lista sin exponer datos
personales de familias o hijos.

## Contexto

- Jira FLE-45 define:
  - Beta cerrada con 10-15 familias.
  - Criterios de salida: aprobacion `<24 h`, sin discrepancias de saldo y cash-out `<72 h`.
  - Acceptance criteria: medir con analitica agregada.
- Confluence `Fledge - Delivery Plan` anade criterios MVP:
  - Una familia completa el bucle entero sin ayuda.
  - Tiempo medio de aprobacion menor de 24 h.
  - Cero discrepancias de saldo tras 4 semanas reales.
  - Aprobacion App Store y Google Play bajo politicas de familias.
- Confluence `Fledge - Compliance and Risk` exige minimizacion y cero tracking conductual en la
  experiencia infantil.

## Alcance

- Definir un contrato comun de readiness de beta basado solo en agregados:
  - tamano de cohorte;
  - duracion media de aprobacion de tareas;
  - duracion media de cash-out y cash-outs abiertos vencidos;
  - numero agregado de discrepancias de saldo;
  - dias de observacion.
- Crear un evaluador puro en dominio que produzca un informe `Ready`, `NeedsMoreData` o `AtRisk`.
- Documentar el runbook de beta cerrada:
  - seleccion de 10-15 familias;
  - cadencia de revision;
  - criterios de salida;
  - datos permitidos/prohibidos;
  - decision final.
- Mantener la medicion como agregada y privacy-first. No se anade Firebase Analytics ni SDK de
  tracking.

## Fuera De Alcance

- Fastlane, GitHub Actions, TestFlight/Play upload y cualquier trabajo de `FLE-76`.
- `FLE-26` y nueva funcionalidad de tareas/aprobaciones.
- Dashboard administrativo o pantalla nueva dentro de la app.
- Captura automatica de eventos en Firebase Analytics.
- Store metadata de `FLE-46`.

## Diseno Pencil

- N/A: FLE-45 no toca pantallas de producto ni flujos de UI.

## Criterios De Aceptacion

### AC-01 - Cohorte cerrada

Given un snapshot agregado de beta
When contiene entre 10 y 15 familias activas
Then el informe marca la cohorte como valida
And no expone ids de familia, hijos ni dispositivos.

### AC-02 - SLA de aprobacion

Given un snapshot agregado con tareas enviadas y revisadas
When la duracion media entre `submittedAt` y `reviewedAt` es menor o igual a 24 h
Then el informe marca la aprobacion como valida.

### AC-03 - Integridad de saldos

Given una beta con 28 dias o mas de observacion
When el contador agregado de discrepancias de saldo es cero
Then el informe marca la integridad de saldo como valida.

### AC-04 - SLA de cash-out

Given un snapshot agregado con retiradas solicitadas y confirmadas
When la duracion media request-to-confirm es menor o igual a 72 h
And no hay cash-outs abiertos con mas de 72 h
Then el informe marca el cash-out como valido.

### AC-05 - Decision global

Given una cohorte valida
And aprobacion, saldos y cash-out cumplen sus umbrales
When se evalua el snapshot
Then la beta queda `Ready`.

### AC-06 - Datos insuficientes o riesgo

Given un snapshot por debajo de cohorte, sin eventos o con menos de 28 dias
When se evalua
Then la beta queda `NeedsMoreData`.
And si algun umbral operativo falla, queda `AtRisk`.

## Trazabilidad

- AC-01: `ClosedBetaReadinessEvaluatorTest` · `FLE-45 AC-01 accepts a closed cohort without identifiers`.
- AC-02: `ClosedBetaReadinessEvaluatorTest` · `FLE-45 AC-02 passes approval SLA when mean review time is within 24 hours`.
- AC-03: `ClosedBetaReadinessEvaluatorTest` · `FLE-45 AC-03 passes balance integrity only after four weeks with no discrepancies`.
- AC-04: `ClosedBetaReadinessEvaluatorTest` · `FLE-45 AC-04 passes cash-out SLA when confirmed mean is within 72 hours and no open cash-out is overdue`.
- AC-05: `ClosedBetaReadinessEvaluatorTest` · `FLE-45 AC-05 marks beta ready when all aggregate gates pass`.
- AC-06: `ClosedBetaReadinessEvaluatorTest` · `FLE-45 AC-06 marks beta as needs more data or at risk when aggregate gates are not ready`.
- Validacion:
  - `./gradlew ktlintFormat --console=plain --no-configuration-cache`.
  - `./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`.
