# Spec 008: Cola de aprobacion parental (FLE-31)

> Rama: `feature/FLE-31-parent-approval-queue` · Proyecto: `Fledge`

## Contexto

`FLE-30` permite que el hijo envie una `TaskInstance` para revision. `FLE-31`
cierra el flujo: el padre ve las tareas enviadas, puede aprobarlas en pocos taps,
ajustar el importe antes de aprobar y rechazar con motivo. Solo la aprobacion
parental genera dinero.

## Tickets

| Ticket | Resumen |
| --- | --- |
| `FLE-31` | Construir cola de aprobacion parental |
| Parent | `FLE-26` Fase 1 - Tasks and Approvals |

## Alcance

- Listar en la home parental las `TaskInstance` con estado `Submitted`.
- Aprobar una tarea con el importe original o un importe editado.
- Crear una transaccion de ledger al aprobar.
- Marcar la tarea como `Approved` con `reviewedAt`, `approvedRewardCents` y
  `approvalTransactionId`.
- Rechazar una tarea con motivo obligatorio.
- Marcar la tarea como `Rejected` con `reviewedAt` y `rejectionReason`.
- Blindar reglas Firestore para que el hijo no pueda aprobar/rechazar.

## Fuera De Alcance

- Fastlane, GitHub Actions y `FLE-26`, por indicacion explicita del usuario.
- Evidencia fotografica real en Storage; FLE-31 solo conserva y muestra la URI
  ya aportada por `FLE-30`.
- Transaccion atomica Firestore cross-collection. Se sigue el patron local de
  procesadores de dinero: primero ledger, luego estado de dominio con referencia
  al movimiento creado.

## Diseno

- Pencil actualizado:
  - `Screen / Cola de aprobacion` muestra acciones `Aprobar`, `Ajustar` y
    `Rechazar`.
  - `Screen / Aprobar tarea - ajustar importe` cubre el ajuste de importe.
  - `Screen / Rechazar tarea - motivo` cubre el motivo obligatorio.
- La UI de Compose debe reflejar esas acciones en una seccion compacta de la
  home parental.

## Criterios De Aceptacion

```gherkin
Feature: Cola de aprobacion parental

  Scenario: [AC-01] El modelo conserva metadata de aprobacion
    Given una TaskInstance enviada
    When el padre la aprueba con un importe
    Then queda Approved con reviewedAt, approvedRewardCents y approvalTransactionId

  Scenario: [AC-02] Aprobar paga al saldo principal
    Given una TaskInstance enviada para un hijo
    When el padre la aprueba
    Then se crea una transaccion TaskReward en Main
    And la tarea guarda el id de esa transaccion

  Scenario: [AC-03] Rechazar exige motivo y no paga
    Given una TaskInstance enviada para un hijo
    When el padre la rechaza con un motivo
    Then queda Rejected con rejectionReason
    And no se crea ninguna transaccion

  Scenario: [AC-04] Home parental lista enviados y permite aprobar en pocos taps
    Given una familia con tareas submitted
    When el padre abre la home
    Then ve la cola de aprobacion
    And puede aprobar usando el importe por defecto

  Scenario: [AC-05] Home parental permite ajustar importe antes de aprobar
    Given una tarea submitted con recompensa original
    When el padre introduce otro importe y aprueba
    Then el ledger usa el importe ajustado

  Scenario: [AC-06] Reglas Firestore protegen las revisiones
    Given una TaskInstance submitted
    Then el padre puede aprobar o rechazar
    And el hijo no puede aprobar su propia tarea
```

## Trazabilidad De Tests

| AC | Tests |
| --- | --- |
| AC-01 | `TaskInstanceModelTest`, `FirestoreRepositorySupportTest` |
| AC-02 | `TaskApprovalProcessorTest`, `InMemoryTaskInstanceRepositoryTest` |
| AC-03 | `TaskApprovalProcessorTest`, `InMemoryTaskInstanceRepositoryTest` |
| AC-04 | `ParentHomeViewModelTest` |
| AC-05 | `ParentHomeViewModelTest` |
| AC-06 | `firestore.rules.test.mjs` |
