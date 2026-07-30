# Spec 018: Estados vacios accionables (FLE-43)

> Rama: `feature/FLE-43-actionable-empty-states` · Ticket: `FLE-43` · Parent: `FLE-41`

## Objetivo

FLE-43 convierte los vacios del MVP en pasos accionables para que una familia pueda completar el
bucle esencial sin instrucciones externas: el padre crea un hijo, define una tarea/paga/objetivo, el
hijo entiende que esperar, el saldo se mueve y el ledger explica lo ocurrido.

## Alcance

- Home padre sin hijos: priorizar `Anadir hijo` y no empujar al padre a crear tareas imposibles.
- Home padre con hijos pero sin actividad: mostrar vacios accionables para tareas, objetivos y
  retiradas.
- Home hijo sin tareas, objetivo ni movimientos: explicar el estado sin tono culpabilizador y ofrecer
  una accion protegida para pedir intervencion adulta.
- Ledger vacio: dejar claro que los movimientos apareceran tras pagas, tareas aprobadas o retiradas.
- Mantener todos los textos en recursos Compose (`values/` espanol y `values-en/` ingles).

## Fuera De Alcance

- Animaciones o celebraciones mas llamativas de objetivo completado: queda anotado en `FLE-94`.
- Nuevas tabs reales, centro de notificaciones, Fastlane, GitHub Actions y cualquier trabajo de `FLE-26`
  que el usuario saco de este sprint.
- Cambios de modelo de ledger, reglas Firestore o backend.

## Diseno Pencil

- Revisado `Fledge.pen` via MCP de Pencil.
- Nuevas variantes creadas y validadas sin problemas de layout:
  - `WOln9`: `Screen / Home padre - empty first run`.
  - `h3xCv`: `Screen / Home padre - empty activity`.
  - `oYeza`: `Screen / Home hijo - empty activity`.
- Las variantes reutilizan status bar/tab bar de las pantallas existentes y sustituyen solo el contenido
  central para preservar la linea visual.

## Criterios De Aceptacion

### AC-01 - Primer uso del padre

Given un padre autenticado con familia creada
And la familia no tiene perfiles infantiles
When abre el home padre
Then ve un estado vacio principal con CTA para anadir el primer hijo
And no ve una CTA primaria que le lleve a crear tareas sin hijos asignables.

### AC-02 - Padre con hijo pero sin actividad

Given una familia con al menos un hijo
And no hay tareas pendientes de aprobacion
And no hay retiradas pendientes
When el padre abre el home
Then las secciones vacias de tareas y retiradas explican el siguiente paso
And el padre ve CTAs claras para crear tarea, configurar paga o crear objetivo desde el contexto del hijo.

### AC-03 - Objetivo pendiente de configurar

Given un hijo sin objetivo activo
When el padre revisa el home familiar
Then el hijo muestra una accion visible para crear objetivo
And el texto explica que la meta da destino al ahorro sin bloquear otros flujos del MVP.

### AC-04 - Home hijo sin actividad

Given un hijo con saldo cero
And sin tareas, objetivo activo, retiradas ni movimientos
When entra en modo hijo
Then ve estados vacios para tareas, objetivo y ledger con lenguaje infantil no culpabilizador
And puede pedir intervencion adulta mediante una accion protegida.

### AC-05 - Ledger vacio comprensible

Given no existen movimientos de ledger para el hijo
When se renderiza la seccion de movimientos
Then el vacio explica que ahi apareceran pagas, tareas aprobadas y retiradas
And no presenta el ledger como un error o una carga incompleta.

## Trazabilidad

- AC-01 -> `ParentHomeViewModelTest`.`FLE-43 AC-01 parent first run prioritizes adding the first child`
- AC-02 -> `ParentHomeViewModelTest`.`FLE-43 AC-02 AC-03 parent with child gets task and goal next actions when activity is empty`
- AC-03 -> `ParentHomeViewModelTest`.`FLE-43 AC-02 AC-03 parent with child gets task and goal next actions when activity is empty`
- AC-04 -> `ChildHomeViewModelTest`.`FLE-43 AC-04 AC-05 child empty home asks for adult help and explains ledger`
- AC-05 -> `ChildHomeViewModelTest`.`FLE-43 AC-04 AC-05 child empty home asks for adult help and explains ledger`
