# Spec 006: Generacion de instancias de tarea (FLE-29)

> Rama: `feature/FLE-29-task-instance-generation` · Proyecto: `Fledge`

## Contexto

`FLE-28` permite al padre crear una `TaskAssignment` puntual o recurrente. `FLE-29`
convierte esas asignaciones activas y vencidas en `TaskInstance` ejecutables por
hijo. La generacion debe vivir en Cloud Functions para que no dependa de que el
padre o el hijo abran la app.

## Jira Mapping

| Key | Scope |
| --- | --- |
| `FLE-26` | Fase 1 - Tasks and Approvals |
| `FLE-29` | Generar instancias de tarea por recurrencia |

## Alcance

### Dentro

- Modelo `TaskInstance` con estado `Pending`, `Submitted`, `Approved`,
  `Rejected` y `Expired`.
- Snapshot por instancia: titulo, importe, requisito de foto, plantilla,
  asignacion, hijo, vencimiento y `periodKey`.
- Repositorio de lectura reactiva para `families/{familyId}/taskInstances`.
- Reglas Firestore: lectura para miembros de familia, escritura cliente cerrada
  hasta FLE-30/FLE-31.
- Scheduler Cloud Functions Gen2 para `(default)` y `debug`.
- Generacion idempotente de una instancia por hijo y periodo vencido.
- Avance de `TaskAssignment.dueAt` para `Daily`, `Weekly` y `Custom`.
- Desactivacion de asignaciones `Once` tras generar su instancia.
- Tests de dominio, mapper, repositorio fake, reglas y logica del scheduler.

### Fuera

- Que el hijo marque completada una tarea (`FLE-30`).
- Cola de aprobacion parental (`FLE-31`).
- Crear transacciones de ledger al aprobar (`FLE-31`).
- Notificaciones push (`FLE-32`).
- Pantallas nuevas o redisenos visuales.

## Design Check

FLE-29 no introduce UI nueva: **gate de diseno N/A**.

Pantallas Pencil revisadas como contexto visual relacionado:

- `Screen / Home hijo`
- `Screen / Detalle tarea hijo`
- `Screen / Cola de aprobacion`

Condicion para FLE-30/FLE-31: cualquier pantalla que muestre `TaskInstance`
debera tomar esas pantallas de Pencil como especificacion visual antes de tocar
Compose.

## Acceptance Criteria

### AC-01 - Modelo de instancia

Given una asignacion activa
When se genera una instancia para un hijo
Then conserva familia, asignacion, plantilla, hijo, titulo, importe, foto,
vencimiento, periodo y estado `Pending`.

### AC-02 - Estados soportados

Given una instancia
When cambia de estado en fases posteriores
Then el dominio soporta `Pending`, `Submitted`, `Approved`, `Rejected` y
`Expired` con nombres estables de persistencia.

### AC-03 - Generacion por recurrencia

Given asignaciones `Once`, `Daily`, `Weekly` y `Custom` vencidas
When corre el job
Then crea instancias pendientes para cada hijo
And avanza `dueAt` segun recurrencia
And desactiva la asignacion puntual.

### AC-04 - Idempotencia

Given el job se ejecuta dos veces para el mismo vencimiento
When ya existe la instancia determinista
Then no duplica documentos.

### AC-05 - Job independiente de la app

Given no hay clientes abiertos
When Cloud Scheduler invoca la funcion
Then la generacion usa Firestore Admin y no depende de ViewModels ni acciones de
la app.

### AC-06 - Persistencia y reglas

Given instancias persistidas
When un padre o el hijo propietario lee la coleccion
Then las reglas permiten lectura
And rechazan escrituras de clientes hasta las tareas FLE-30/FLE-31.

## Gherkin Scenarios

```gherkin
Feature: Generacion de instancias de tarea

  Scenario: [AC-01] TaskInstance conserva snapshot requerido
    Given una TaskAssignment para "child-1"
    When se crea una TaskInstance pendiente
    Then el snapshot contiene los campos requeridos

  Scenario: [AC-02] Los estados son serializables
    Given los estados de una tarea
    When se guardan por nombre
    Then Pending, Submitted, Approved, Rejected y Expired son estables

  Scenario: [AC-03] El job genera instancias y avanza recurrencia
    Given assignments Once, Daily, Weekly y Custom vencidas
    When corre processDueTaskAssignments
    Then se crean instancias para sus hijos
    And las recurrentes avanzan su dueAt
    And la puntual queda inactiva

  Scenario: [AC-04] El job es idempotente
    Given una instancia ya creada para un assignment, hijo y periodo
    When corre processDueTaskAssignments otra vez
    Then no se crea una segunda instancia

  Scenario: [AC-06] Reglas de TaskInstance
    Given una instancia de "child-1"
    When leen el padre y "child-1"
    Then ambos pueden leer
    And otro hijo no puede leer ni ningun cliente escribir
```

## Trazabilidad

| AC | Tests |
| --- | --- |
| AC-01 | `TaskInstanceModelTest`, `FirestoreRepositorySupportTest` |
| AC-02 | `TaskInstanceModelTest` |
| AC-03 | `functions/index.test.js` |
| AC-04 | `functions/index.test.js` |
| AC-05 | `functions/index.test.js`, `functions/README.md` |
| AC-06 | `firestore.rules.test.mjs`, `InMemoryTaskInstanceRepositoryTest` |
