# Spec 014: Retiradas desde objetivo a MAIN (FLE-38)

> Rama: `feature/FLE-38-goal-withdrawals` · Ticket: `FLE-38` · Parent: `FLE-35`

## Objetivo

FLE-38 permite sacar dinero virtual del bote `Goal` del hijo hacia `Main`, manteniendo trazabilidad y
mostrando una confirmacion educativa antes de confirmar la retirada.

## Alcance

- Reutilizar `transferGroupId` y `appendTransferPair` para registrar retirada `Goal -> Main`.
- Crear `SavingsGoalWithdrawalProcessor` que valida objetivo activo, propiedad del hijo, importe positivo y saldo `Goal`.
- La politica familiar por defecto queda libre con confirmacion explicativa en UI.
- Exponer pantalla infantil de retirada con importe editable, resumen `GOAL -> MAIN` y coste de oportunidad.
- Navegar desde la tarjeta de objetivo activo en Home hijo.

## Diseno Pencil

- Added for FLE-38:
  - `Screen / Retirar de objetivo` (`RA2pJ`).
- `snapshot_layout(problemsOnly=true)` no reporta problemas.

## Fuera De Alcance

- Proyeccion temporal avanzada: FLE-39.
- Celebracion de objetivo completado: FLE-40.
- Pantalla de ajustes parentales para bloquear retiradas; se modela el default libre con confirmacion.

## Criterios De Aceptacion

### AC-01 - Retirada valida

Given un hijo con objetivo activo
And saldo suficiente en `Goal`
When confirma una retirada positiva
Then se crea una transaccion negativa en `Goal`
And se crea una transaccion positiva en `Main`
And ambas comparten `transferGroupId`.

### AC-02 - Confirmacion educativa

Given un hijo abre la retirada de objetivo
When intenta confirmar sin aceptar el coste de oportunidad
Then la operacion falla con un mensaje educativo
And no se crea ninguna transaccion nueva.

### AC-03 - Saldo Goal insuficiente

Given un hijo con saldo `Goal` inferior al importe
When intenta retirar
Then la operacion falla
And no se crea ninguna transaccion nueva.

### AC-04 - Objetivo incorrecto

Given un objetivo archivado o de otro hijo
When intenta retirar
Then la operacion falla
And no se crea ninguna transaccion nueva.

## Trazabilidad

- AC-01 -> `SavingsGoalWithdrawalProcessorTest`.`AC-01 child withdraws goal balance back to main with ledger traceability`
- AC-02 -> `SavingsGoalWithdrawalViewModelTest`.`AC-02 child must confirm the opportunity cost before withdrawing`
- AC-03 -> `SavingsGoalWithdrawalProcessorTest`.`AC-03 withdrawal fails when goal balance is insufficient and leaves ledger unchanged`
- AC-03 -> `SavingsGoalWithdrawalViewModelTest`.`AC-03 child sees validation when withdrawal amount is invalid or too high`
- AC-04 -> `SavingsGoalWithdrawalProcessorTest`.`AC-04 withdrawal fails for archived or sibling goal and leaves ledger unchanged`
