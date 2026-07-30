# Spec 016: Celebracion de objetivo completado (FLE-40)

> Rama: `feature/FLE-40-goal-completion-celebration` · Ticket: `FLE-40` · Parent: `FLE-35`

## Objetivo

FLE-40 refuerza el habito de ahorro cuando un hijo alcanza el importe objetivo. La app debe detectar
que el saldo `Goal` cubre el target, felicitar al hijo en su Home y avisar al padre en su dashboard.

## Alcance

- Detectar objetivos activos completados cuando `goalBalance >= targetCents`.
- Exponer un aviso de celebracion para el hijo en Home hijo.
- Exponer avisos de objetivos completados para el padre en Home padre.
- Mostrar el importe conseguido y el objetivo alcanzado con tono positivo y no culpabilizante.
- Actualizar `Screen / Home hijo` y `Screen / Home padre` en Pencil con el estado de celebracion.

## Fuera De Alcance

- Push remoto FCM/APNs. Fledge aun no tiene KMPNotifier ni configuracion `google-services`; esta fase
  cubre notificacion in-app.
- Cerrar/archivar automaticamente el objetivo.
- Multiples objetivos activos por hijo.
- Confeti/animacion nativa persistente.

## Diseno Pencil

- Updated:
  - `Screen / Home hijo` (`eYef1`) con banda `Goal Completed Celebration` bajo la tarjeta de objetivo.
  - `Screen / Home padre` (`Q2S0E`) con banda `Parent Goal Completed Notice` sobre las acciones principales.
- `snapshot_layout(parentId=eYef1, problemsOnly=true)` no reporta problemas tras el cambio.
- `snapshot_layout(parentId=Q2S0E, problemsOnly=true)` no reporta problemas tras el cambio.

## Criterios De Aceptacion

### AC-01 - Deteccion de objetivo completado

Given un objetivo activo con target definido
And el saldo `Goal` del hijo es igual o superior al target
When se recalcula el estado de objetivos
Then se genera una notificacion de objetivo completado
And la notificacion incluye objetivo, hijo, importe actual y target.

### AC-02 - Celebracion en Home hijo

Given un hijo con objetivo activo completado
When abre Home hijo
Then ve una celebracion positiva junto a la tarjeta del objetivo
And el aviso muestra el nombre del objetivo y el importe conseguido.

### AC-03 - Aviso en Home padre

Given una familia con al menos un hijo que ha completado su objetivo activo
When el padre abre Home padre
Then ve un aviso de objetivo completado con el nombre del hijo
And el aviso muestra el importe conseguido frente al target.

## Trazabilidad

- AC-01 -> `SavingsGoalCompletionNotifierTest`.`FLE-40 AC-01 detects completed active goal with child and amount context`
- AC-01 -> `SavingsGoalCompletionNotifierTest`.`FLE-40 AC-01 ignores active goal below target`
- AC-02 -> `ChildHomeViewModelTest`.`FLE-40 AC-02 child home exposes celebration notice for completed active goal`
- AC-03 -> `ParentHomeViewModelTest`.`FLE-40 AC-03 parent home exposes completed goal notices for children`
