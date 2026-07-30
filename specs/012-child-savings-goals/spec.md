# Spec 012: Objetivos de ahorro infantiles (FLE-36)

> Rama: `feature/FLE-36-child-savings-goals` · Ticket: `FLE-36` · Parent: `FLE-35`

## Objetivo

FLE-36 permite que el padre cree un objetivo de ahorro para un hijo y que el objetivo activo aparezca
en Home hijo con progreso contra la cuenta virtual `Goal`.

## Alcance

- Modelo `SavingsGoal` con titulo, icono/imagen, `targetCents`, hijo propietario, cuenta asociada
  `Goal`, estado activo y timestamps.
- Repositorio `SavingsGoalRepository` con Firestore + fake de tests.
- Reglas Firestore para:
  - Padres crean/actualizan objetivos de su familia.
  - Hijos leen solo sus propios objetivos.
  - Hijos no crean ni modifican objetivos.
- Formulario parental para crear objetivo.
- Home hijo muestra el objetivo activo, importe actual en cuenta `Goal`, objetivo total y progreso.

## Diseno Pencil

- Existing:
  - `Screen / Home hijo` ya incluye `Active Goal Card`.
  - `Screen / Objetivo de ahorro` ya muestra detalle de objetivo.
- Added for FLE-36:
  - `Screen / Crear objetivo de ahorro`.
- `snapshot_layout(problemsOnly=true)` no reporta problemas tras crear el frame.

## Fuera De Alcance

- Depositos desde `Main` a `Goal`: FLE-37.
- Retirar dinero de `Goal`: FLE-38+.
- Multiples objetivos simultaneos por hijo: se permite persistir historico, pero Home hijo muestra el
  primero activo por prioridad/fecha.
- Subida real de imagen: FLE-36 guarda `imageUri`/`iconKey`; upload queda fuera.

## Criterios De Aceptacion

### AC-01 - Crear objetivo valido

Given un padre autenticado con familia activa
And un hijo existente
When crea un objetivo con titulo, icono o imagen y `target_cents > 0`
Then el objetivo queda activo
And queda asociado al hijo y a la cuenta `Goal`.

### AC-02 - Validacion de campos

Given un objetivo sin titulo o con `target_cents <= 0`
When se intenta crear
Then la operacion falla
And no se guarda en el repositorio.

### AC-03 - Home hijo muestra objetivo activo

Given un hijo con objetivo activo
And saldo en cuenta `Goal`
When abre Home hijo
Then ve el titulo, el importe actual, el objetivo total y el progreso.

### AC-04 - Home hijo sin objetivo

Given un hijo sin objetivo activo
When abre Home hijo
Then no se muestra una tarjeta de objetivo activa.

### AC-05 - Reglas Firestore

Given un padre de la familia
When escribe un objetivo valido
Then Firestore lo permite.

Given un hijo autenticado
When intenta escribir un objetivo
Then Firestore lo deniega.
