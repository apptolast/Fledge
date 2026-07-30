# Spec 015: Progreso y proyeccion temporal de objetivo (FLE-39)

> Rama: `feature/FLE-39-goal-progress-projection` · Ticket: `FLE-39` · Parent: `FLE-35`

## Objetivo

FLE-39 mejora la tarjeta de objetivo activo en Home hijo para que el nino vea cuanto lleva,
cuanto falta, a que ritmo esta avanzando y una fecha estimada si mantiene ese ritmo.

## Alcance

- Calcular progreso porcentual y dinero restante contra `targetCents`.
- Calcular ritmo medio diario usando movimientos netos de la cuenta `Goal` desde el inicio del objetivo.
- Estimar fecha de llegada cuando el ritmo neto es positivo.
- Mostrar en Home hijo porcentaje, ritmo y fecha estimada.
- Mostrar un mensaje accionable si todavia no hay ritmo positivo suficiente para estimar fecha.
- Actualizar `Screen / Home hijo` en Pencil con la banda de proyeccion.

## Diseno Pencil

- Updated:
  - `Screen / Home hijo` (`eYef1`) extiende `Active Goal Card` con porcentaje, ritmo y banda de fecha.
- `snapshot_layout(parentId=eYef1, problemsOnly=true)` no reporta problemas tras el cambio.

## Fuera De Alcance

- Celebracion del objetivo completado: FLE-40.
- Multiples objetivos simultaneos por hijo.
- Cambios en reglas Firestore o escrituras nuevas.
- Grafica historica avanzada de aportaciones.

## Criterios De Aceptacion

### AC-01 - Progreso y restante

Given un hijo con objetivo activo
And saldo en cuenta `Goal`
When abre Home hijo
Then ve el porcentaje conseguido
And ve cuanto dinero falta para llegar al objetivo.

### AC-02 - Fecha estimada con ritmo positivo

Given un hijo con objetivo activo
And movimientos netos positivos en `Goal` desde que se creo el objetivo
When abre Home hijo
Then ve su ritmo medio diario
And ve una fecha estimada de llegada al objetivo.

### AC-03 - Sin ritmo suficiente

Given un hijo con objetivo activo
And no hay movimientos netos positivos suficientes en `Goal`
When abre Home hijo
Then no se muestra una fecha inventada
And se muestra un mensaje para meter dinero y calcular la fecha.

## Trazabilidad

- AC-01 -> `SavingsGoalProjectionCalculatorTest`.`FLE-39 AC-01 calculates progress and remaining amount for active goal`
- AC-02 -> `SavingsGoalProjectionCalculatorTest`.`FLE-39 AC-02 estimates completion date from net daily goal pace`
- AC-03 -> `SavingsGoalProjectionCalculatorTest`.`FLE-39 AC-03 omits estimated date when the goal has no positive pace`
- AC-01/AC-02 -> `ChildHomeViewModelTest`.`FLE-39 child home exposes progress projection for active goal`
