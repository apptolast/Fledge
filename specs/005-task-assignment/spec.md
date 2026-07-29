# Spec 005: Asignacion puntual y recurrente (FLE-28)

> Rama: `feature/FLE-28-task-assignment` · Proyecto: `Fledge`

## Contexto

`FLE-27` dejo creado el catalogo reutilizable de plantillas familiares. `FLE-28`
debe permitir que un padre convierta una plantilla en una asignacion para uno o
varios hijos, con recurrencia puntual o repetida. Esta feature no crea aun
instancias ejecutables de tarea: eso queda en `FLE-29`.

## Jira Mapping

| Key | Scope |
| --- | --- |
| `FLE-26` | Fase 1 - Tasks and Approvals |
| `FLE-28` | Implementar asignacion puntual y recurrente |

## Alcance

### Dentro

- Modelo `TaskAssignment` con:
  - `taskTemplateId`
  - uno o varios `childProfileIds`
  - `recurrence` con `Once`, `Daily`, `Weekly` y `Custom`
  - `dueAt`
  - `active`
  - campos tecnicos: id, familia, timestamps y configuracion custom minima.
- `TaskAssignmentRepository` con flujo reactivo de asignaciones familiares.
- Implementacion Firestore en `families/{familyId}/taskAssignments`.
- Reglas Firestore: lectura para miembros de familia y escritura solo parental.
- UI parental de crear asignacion, basada en Pencil `Screen / Crear tarea`.
- Navegacion desde zona padre hacia crear tarea.
- Usar una tarea sugerida o existente del catalogo para precargar titulo,
  descripcion, icono, valor y `requiresPhoto`.
- Soportar seleccion de uno o varios hijos en el formulario.
- Tests SDD para dominio, repositorio, mappers, Koin, ViewModel y navegacion.

### Fuera

- Generar `TaskInstance` por recurrencia (`FLE-29`).
- Permitir que el hijo marque tareas completadas (`FLE-30`).
- Cola de aprobacion real y pagos por tarea (`FLE-31`).
- Push notifications (`FLE-32`).
- Edicion avanzada del catalogo reutilizable.

## Design Check

FLE-28 toca UI y el gate de Pencil **no es N/A**.

Revision realizada en `/Users/hgarcia/Documents/Companies/AppToLast/KMP/Fledge/Fledge.pen`:

- `Screen / Crear tarea` existe y cubre:
  - titulo `Nueva tarea`
  - subtitulo `Define valor, recurrencia y control de calidad.`
  - `Field / Titulo`
  - `Field / Valor`
  - `Field / Repeticion`
  - `Field / Asignado a`
  - `Field / Foto`
  - `Save Task Button`
  - `Template Button`
  - tab bar con `Tareas` activo.
- `Screen / Home padre`, `Screen / Home hijo`, `Screen / Detalle tarea hijo` y
  `Screen / Cola de aprobacion` existen como referencias visuales relacionadas.
- `snapshot_layout(problemsOnly=true)` no reporta problemas en el documento.

Condicion de implementacion: toda UI nueva o tocada por FLE-28 debe usar el
lenguaje visual del `.pen`: fondo `#F7F9F8`, superficies blancas, bordes
`#D8E1DD`, acento teal `#0F8B8D`, radios 12/16/18, botones de 48-52dp y
jerarquia de titulo de 28sp aproximada mediante `MaterialTheme.typography`.

Nota de auditoria: las home actuales de Compose siguen siendo funcionales pero
mas genericas que Pencil. FLE-28 alineara la nueva pantalla y el punto de entrada
parental que toque; la alineacion completa de todas las pantallas restantes debe
mantenerse como criterio obligatorio en cada feature que toque UI.

## Acceptance Criteria

### AC-01 - Modelo de asignacion

Given una plantilla y una familia
When se crea una asignacion con una recurrencia soportada, `dueAt`, hijos y
`active`
Then el modelo conserva esos campos
And rechaza una lista vacia de hijos
And rechaza custom sin intervalo valido.

### AC-02 - Soporte de recurrencias

Given una asignacion
When se configura como `Once`, `Daily`, `Weekly` o `Custom`
Then la recurrencia queda representada de forma serializable y validable.

### AC-03 - Asignacion a uno o varios hijos

Given una familia con varios hijos
When el padre guarda una asignacion con dos hijos seleccionados
Then el repositorio guarda ambos ids en una sola asignacion familiar.

### AC-04 - Repositorio familiar reactivo

Given una familia autenticada
When se guarda una asignacion activa
Then aparece en `assignments`
And `activeAssignmentsForChild(childId)` devuelve las asignaciones que incluyen
ese hijo.

### AC-05 - Persistencia Firestore y reglas

Given una asignacion persistida
When se serializa a Firestore
Then vive bajo `families/{familyId}/taskAssignments/{assignmentId}`
And contiene `familyId`, `taskTemplateId`, `childProfileIds`, `recurrence`,
`dueAt`, `active`, `createdAt` y `updatedAt`
And las reglas permiten lectura a miembros de familia y escritura solo al padre.

### AC-06 - UI Create Task alineada con Pencil

Given el padre abre el flujo de crear tarea
When hay plantillas e hijos disponibles
Then la pantalla muestra titulo, valor, repeticion, asignado a, foto, guardar y
usar tarea sugerida siguiendo `Screen / Crear tarea`
And permite seleccionar uno o varios hijos.

### AC-07 - Koin y navegacion

Given el grafo de produccion
When se resuelve `TaskAssignmentRepository` y se navega a crear tarea
Then se obtiene la implementacion Firestore
And la ruta type-safe `TaskAssignmentRoute` renderiza el formulario.

## Gherkin Scenarios

```gherkin
Feature: Asignacion de tareas

  Scenario: [AC-01] TaskAssignment conserva los campos requeridos
    Given una familia "family-1" y una plantilla "template-1"
    When se crea una asignacion diaria para "child-1"
    Then familia, plantilla, hijos, recurrencia, dueAt y active quedan almacenados

  Scenario: [AC-01] TaskAssignment rechaza datos invalidos
    Given una familia "family-1"
    When se intenta crear una asignacion sin hijos o custom sin intervalo
    Then la asignacion se rechaza

  Scenario: [AC-02] Las recurrencias soportadas son serializables
    Given una asignacion
    When se configura con Once, Daily, Weekly o Custom
    Then cada tipo conserva su nombre estable para persistencia

  Scenario: [AC-03] El padre asigna una tarea a varios hijos
    Given una familia con dos hijos
    When el padre guarda una asignacion seleccionando ambos
    Then el repositorio guarda ambos childProfileIds en la misma asignacion

  Scenario: [AC-04] El repositorio expone asignaciones activas por hijo
    Given asignaciones activas e inactivas
    When se consultan las asignaciones de un hijo
    Then solo se devuelven las activas que incluyen ese hijo

  Scenario: [AC-05] La asignacion se mapea al esquema Firestore
    Given una asignacion persistida
    When se convierte a mapa Firestore
    Then incluye los campos requeridos por las reglas

  Scenario: [AC-06] El formulario crea asignacion desde una plantilla sugerida
    Given hay una familia con hijos y plantillas sugeridas
    When el padre abre crear tarea y guarda con dos hijos
    Then se crea una asignacion con los datos precargados de la plantilla

  Scenario: [AC-07] El grafo Koin resuelve asignaciones
    Given el grafo de produccion con bindings de plataforma fake
    When se resuelve TaskAssignmentRepository
    Then se obtiene la implementacion Firestore
```

## Trazabilidad

| AC | Tests |
| --- | --- |
| AC-01 | `TaskAssignmentModelTest` |
| AC-02 | `TaskAssignmentModelTest`, `FirestoreRepositorySupportTest` |
| AC-03 | `InMemoryTaskAssignmentRepositoryTest`, `TaskAssignmentViewModelTest` |
| AC-04 | `InMemoryTaskAssignmentRepositoryTest` |
| AC-05 | `FirestoreRepositorySupportTest` |
| AC-06 | `TaskAssignmentViewModelTest`, `Screen / Crear tarea` Pencil audit |
| AC-07 | `AppModulesTest`, `TaskAssignmentRouteTest` |
