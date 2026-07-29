# Spec 004: Catalogo de tareas reutilizables (FLE-27)

> Rama: `feature/FLE-27-reusable-task-catalog` · Proyecto: `Fledge`

## Contexto

`FLE-26` abre la fase de tareas y aprobaciones. Antes de asignar tareas (`FLE-28`),
generar instancias (`FLE-29`) o aprobar submissions (`FLE-31`), la app necesita un
catalogo de plantillas reutilizables que represente las tareas que un padre puede
crear o sugerir.

`FLE-27` crea ese catalogo. El primer corte debe ser pequeno y persistente:
modelos de dominio, repositorio, implementacion Firestore, seed de sugerencias
iniciales por edad tras crear el primer hijo, reglas de seguridad y wiring Koin.

## Jira Mapping

| Key | Scope |
| --- | --- |
| `FLE-26` | Fase 1 - Tasks and Approvals |
| `FLE-27` | Crear catalogo de tareas reutilizables |

## Alcance

### Dentro

- Modelo `TaskTemplate` con titulo, descripcion, icono, `defaultValueCents` y
  `requiresPhoto`, mas campos tecnicos necesarios: id, familia, edad sugerida,
  origen, archivado y timestamps.
- `TaskTemplateRepository` con flujo reactivo de plantillas y persistencia
  familiar.
- Implementacion Firestore sobre `families/{familyId}/taskTemplates`.
- Semilla inicial de sugerencias por edad al crear el primer perfil infantil.
- Idempotencia del seed: no duplicar sugerencias al reintentar.
- Koin: registrar repositorio y hacer que `ChildProfileSetupViewModel` dispare
  el seed cuando crea el primer hijo.
- Reglas Firestore para lectura/escritura parental y lectura infantil de
  plantillas familiares.

### Fuera

- Asignar plantillas a hijos (`FLE-28`).
- Recurrencias e instancias (`FLE-29`).
- Submission infantil (`FLE-30`).
- Cola de aprobacion y pagos por tarea (`FLE-31`).
- Push notifications (`FLE-32`).
- Nueva pantalla de catalogo editable. El diseno existente de Pencil para
  `Screen / Crear tarea` y su boton `Usar tarea sugerida` cubren la UI futura.

## Design Check

`FLE-27` no introduce una pantalla nueva. El impacto visible queda cubierto por
el board Pencil ya revisado:

- `Screen / Crear tarea` existe e incluye el boton `Usar tarea sugerida`.
- `Design Gate / FLE-93` deja explicitado que el catalogo debe validarse antes
  de continuar `FLE-26`.

Por tanto, el gate de diseno de esta feature es **N/A para nueva UI**, con la
condicion de que `FLE-28` use `Screen / Crear tarea` como especificacion visual.

## Acceptance Criteria

### AC-01 - Modelo de plantilla reutilizable

Given una plantilla de tarea
When se crea con titulo, descripcion, icono, valor por defecto y flag de foto
Then el modelo conserva esos campos
And rechaza titulo, descripcion o icono vacios
And rechaza valores por defecto no positivos.

### AC-02 - Sugerencias iniciales por edad

Given se crea el primer hijo de una familia
When el hijo tiene una edad calculable desde su ano de nacimiento
Then el catalogo genera sugerencias apropiadas para su franja de edad
And cada sugerencia tiene titulo, descripcion, icono, valor positivo y flag
requiresPhoto explicito.

### AC-03 - Seed idempotente y solo para primer hijo

Given una familia sin plantillas
When se crea el primer hijo
Then se guardan las sugerencias iniciales una sola vez
When se reintenta el seed para el mismo hijo
Then no se duplican plantillas
When se crea un segundo hijo
Then no se vuelve a poblar el catalogo automaticamente.

### AC-04 - Repositorio familiar reactivo

Given una familia autenticada
When se guarda una plantilla custom
Then el repositorio la expone en `templates`
And las plantillas se ordenan de forma estable para mostrarlas en UI.

### AC-05 - Persistencia Firestore y reglas

Given una plantilla persistida
When se serializa a Firestore
Then vive bajo `families/{familyId}/taskTemplates/{templateId}`
And contiene `familyId`, `title`, `description`, `iconKey`,
`defaultValueCents`, `requiresPhoto`, `source`, `createdAt` y `updatedAt`
And las reglas permiten lectura a miembros de la familia y escritura solo al
padre.

### AC-06 - Wiring Koin y onboarding

Given el grafo de produccion
When se resuelve `TaskTemplateRepository`
Then la implementacion es Firestore
And `ChildProfileSetupViewModel` recibe el repositorio de plantillas por
constructor para sembrar sugerencias tras el primer hijo.

## Gherkin Scenarios

```gherkin
Feature: Catalogo de tareas reutilizables

  Scenario: [AC-01] TaskTemplate conserva los campos requeridos
    Given una familia "family-1"
    When se crea una plantilla "Poner la mesa"
    Then titulo, descripcion, icono, valor por defecto y requiresPhoto quedan almacenados

  Scenario: [AC-01] TaskTemplate rechaza datos invalidos
    Given una familia "family-1"
    When se intenta crear una plantilla sin titulo, descripcion, icono o con valor cero
    Then la plantilla se rechaza

  Scenario: [AC-02] El catalogo propone tareas por edad
    Given un hijo nacido en 2017
    When se piden sugerencias para el ano 2026
    Then se devuelven tareas para la franja 7-9 anos
    And todas tienen valor positivo y requiresPhoto definido

  Scenario: [AC-03] El seed inicial es idempotente
    Given una familia sin plantillas y su primer hijo
    When se ejecuta el seed dos veces
    Then el repositorio contiene una sola copia de cada sugerencia

  Scenario: [AC-03] El segundo hijo no vuelve a sembrar automaticamente
    Given una familia con un hijo y plantillas iniciales
    When se crea un segundo hijo desde el onboarding
    Then el numero de plantillas no aumenta por seed automatico

  Scenario: [AC-04] El repositorio guarda plantillas custom
    Given una familia autenticada
    When el padre guarda una plantilla custom
    Then aparece en el flujo reactivo ordenada junto al catalogo

  Scenario: [AC-05] La plantilla se mapea al esquema Firestore
    Given una plantilla persistida
    When se convierte a mapa Firestore
    Then incluye los campos requeridos por las reglas

  Scenario: [AC-06] El grafo Koin resuelve el catalogo
    Given el grafo de produccion con bindings de plataforma fake
    When se resuelve TaskTemplateRepository
    Then se obtiene la implementacion Firestore
```

## Trazabilidad

| AC | Tests |
| --- | --- |
| AC-01 | `TaskTemplateModelTest` |
| AC-02 | `InitialTaskTemplateCatalogTest` |
| AC-03 | `InMemoryTaskTemplateRepositoryTest`, `ChildProfileSetupViewModelTest` |
| AC-04 | `InMemoryTaskTemplateRepositoryTest` |
| AC-05 | `FirestoreRepositorySupportTest` |
| AC-06 | `AppModulesTest`, `ChildProfileSetupViewModelTest` |
