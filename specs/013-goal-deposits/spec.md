# Spec 013: Depositos desde MAIN a objetivo (FLE-37)

> Rama: `feature/FLE-37-goal-deposits` · Ticket: `FLE-37` · Parent: `FLE-35`

## Objetivo

FLE-37 permite mover dinero virtual del saldo `Main` del hijo hacia su objetivo activo en `Goal`.
El movimiento debe quedar como dos apuntes de ledger trazables entre si.

## Alcance

- Anadir trazabilidad explicita al ledger mediante `transferGroupId`.
- Crear un servicio de dominio que valida objetivo, propiedad del hijo, importe positivo y saldo `Main`.
- Crear dos transacciones:
  - `Main`: importe negativo.
  - `Goal`: importe positivo.
- Exponer pantalla infantil de deposito con importe editable y resumen `MAIN -> GOAL`.
- Navegar desde la tarjeta de objetivo activo en Home hijo.

## Diseno Pencil

- Existing:
  - `Screen / Objetivo de ahorro` contiene la accion de deposito.
- Added for FLE-37:
  - `Screen / Depositar a objetivo`.
- `snapshot_layout(problemsOnly=true)` no reporta problemas en el frame final.

## Fuera De Alcance

- Retirada desde `Goal`: FLE-38.
- Proyeccion temporal avanzada: FLE-39.
- Celebracion de objetivo completado: FLE-40.
- Reglas para escritura directa con token infantil; el flujo actual escribe desde la sesion familiar existente.

## Criterios De Aceptacion

### AC-01 - Deposito valido

Given un hijo con objetivo activo
And saldo suficiente en `Main`
When deposita un importe positivo
Then se crea una transaccion negativa en `Main`
And se crea una transaccion positiva en `Goal`
And ambas comparten `transferGroupId`.

### AC-02 - Saldo insuficiente

Given un hijo con saldo `Main` inferior al importe
When intenta depositar
Then la operacion falla
And no se crea ninguna transaccion nueva.

### AC-03 - Objetivo incorrecto

Given un objetivo archivado o de otro hijo
When intenta depositar
Then la operacion falla
And no se crea ninguna transaccion nueva.

### AC-04 - UI infantil

Given un hijo con objetivo activo
When abre la pantalla de deposito
Then puede editar el importe
And ve el resumen de movimientos `MAIN` y `GOAL`
And al confirmar vuelve al Home con saldos actualizados.

## Trazabilidad

- AC-01 -> `SavingsGoalDepositProcessorTest`.`AC-01 child deposits main balance into an active goal with ledger traceability`
- AC-02 -> `SavingsGoalDepositProcessorTest`.`AC-02 deposit fails when main balance is insufficient and leaves ledger unchanged`
- AC-03 -> `SavingsGoalDepositProcessorTest`.`AC-03 deposit fails for archived or sibling goal and leaves ledger unchanged`
- AC-04 -> `SavingsGoalDepositViewModelTest`.`AC-04 child can edit deposit amount and submit from main to goal`
- AC-04 -> `SavingsGoalDepositViewModelTest`.`AC-04 child sees validation when deposit amount is invalid or too high`
