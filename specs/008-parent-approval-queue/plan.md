# Plan 008: Cola de aprobacion parental (FLE-31)

## Enfoque

Anadir la revision parental como una transicion explicita de `TaskInstance`.
La aprobacion crea un movimiento de ledger y despues marca la instancia como
aprobada con la referencia al movimiento. El rechazo no toca ledger y exige
motivo.

## Cambios Tecnicos

- `TaskInstance`
  - `approvedRewardCents: MoneyCents?`
  - `approvalTransactionId: TransactionId?`
  - `rejectionReason: String?`
  - helpers `approvedByParent(...)` y `rejectedByParent(...)`.
- `TaskInstanceRepository`
  - lectura por id.
  - `approve(...)`.
  - `reject(...)`.
- `TaskApprovalProcessor`
  - aprueba y crea `LedgerTransactionType.TaskReward`.
  - rechaza sin ledger.
- Firestore
  - serializacion de nuevos campos.
  - reglas para transiciones parent-only `Submitted -> Approved/Rejected`.
  - Cloud Functions generan instancias con metadata nula.
- UI parental
  - `ParentHomeViewModel` observa `TaskInstanceRepository`.
  - estado para importes editables y motivos editables.
  - `ParentHomeScreen` muestra cola de aprobacion antes de hijos/liquidaciones.

## Orden

1. Escribir tests rojos de dominio, procesador, repositorio fake, ViewModel,
   serializacion y reglas.
2. Implementar modelo, repositorios, procesador y DI.
3. Implementar UI + strings + preview.
4. Ejecutar `ktlintFormat`, tests KMP, tests npm/rules/functions y validacion
   SDD.
5. Crear PR contra `develop`, mover Jira a `In Review` y esperar validacion.
