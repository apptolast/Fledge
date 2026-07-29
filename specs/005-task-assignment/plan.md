# Implementation Plan

## Enfoque tecnico

`FLE-28` anade el subdominio `TaskAssignment` encima del catalogo creado en
`FLE-27`. La asignacion es un contrato persistente familiar, no una instancia
ejecutable. `FLE-29` sera quien transforme asignaciones recurrentes en
`TaskInstance`.

## Diseno

Fuente visual obligatoria:

- Pencil `Screen / Crear tarea` (`eHOBZ`).
- Variables Pencil: `bg #F7F9F8`, `surface #FFFFFF`, `surface2 #EEF4F2`,
  `line #D8E1DD`, `ink #17211F`, `muted #50605A`, `teal #0F8B8D`,
  `green #4F7A5A`, `red #C45151`.

La nueva pantalla Compose debe:

- Usar un wrapper vertical con padding 16-20dp y gaps 14-18dp.
- Mantener CTA principal de 52dp y secundario de 48dp.
- Mostrar los cinco campos del `.pen` como bloques compactos.
- Permitir seleccion multi-hijo con chips/toggles de 48dp minimo.
- Evitar texto hardcodeado: strings en Compose Resources.

## Ficheros a tocar

### commonMain

- `domain/model/TaskAssignmentModels.kt`
- `domain/repository/TaskAssignmentRepository.kt`
- `data/repository/FirestoreTaskAssignmentRepository.kt`
- `data/repository/FirestoreRepositorySupport.kt`
- `presentation/foundation/taskassignment/TaskAssignmentViewModel.kt`
- `presentation/foundation/taskassignment/TaskAssignmentScreen.kt`
- `navigation/Routes.kt`
- `navigation/FledgeNavHost.kt`
- `presentation/foundation/parenthome/ParentHomeScreen.kt`
- `di/AppModules.kt`
- `presentation/theme/{Color,Theme,Type}.kt` si hacen falta tokens Pencil.
- `composeResources/values/strings.xml`

### commonTest

- `domain/TaskAssignmentModelTest.kt`
- `data/InMemoryTaskAssignmentRepositoryTest.kt`
- `data/repository/InMemoryTaskAssignmentRepository.kt`
- `data/repository/FirestoreRepositorySupportTest.kt`
- `presentation/TaskAssignmentViewModelTest.kt`
- `di/AppModulesTest.kt`

### Repo raiz

- `firestore.rules`
- `specs/005-task-assignment/spec.md`
- `specs/005-task-assignment/plan.md`

## Modelo propuesto

- `TaskAssignmentId(value: String)`
- `TaskRecurrence`: `Once`, `Daily`, `Weekly`, `Custom`
- `TaskAssignment`
  - `id`
  - `familyId`
  - `taskTemplateId`
  - `title`
  - `rewardCents`
  - `requiresPhoto`
  - `childProfileIds: List<ChildProfileId>`
  - `recurrence`
  - `dueAt: Instant`
  - `customIntervalDays: Int?`
  - `active`
  - `createdAt`
  - `updatedAt`
- `TaskAssignmentDraft`

Validaciones:

- Al menos un hijo.
- Titulo no vacio.
- Importe positivo.
- Custom exige `customIntervalDays >= 1`.
- No custom no permite intervalo custom.

## Repositorio

`TaskAssignmentRepository`:

- `syncStatus: StateFlow<RepositorySyncStatus>`
- `assignments: StateFlow<List<TaskAssignment>>`
- `saveAssignment(draft, createdAt)`
- `assignmentsForFamily(familyId)`
- `activeAssignmentsForChild(childProfileId)`

Firestore:

- Coleccion `families/{familyId}/taskAssignments`.
- Orden local por `active` descendente y `dueAt` ascendente.
- Escrituras parent-only con `authProvider.currentFamilyId()`.

## UI / ViewModel

`TaskAssignmentViewModel` recibe:

- `FamilyFoundationRepository`
- `TaskTemplateRepository`
- `TaskAssignmentRepository`

Estado:

- familia, hijos, plantillas.
- plantilla seleccionada.
- titulo, importe y foto editables precargados desde plantilla.
- hijos seleccionados.
- recurrencia seleccionada.
- dueAt textual controlado por opciones simples.
- error de validacion.
- `savedAssignment`.

Primera version UI:

- Si hay plantillas, seleccionar la primera sugerida/activa.
- Boton `Usar tarea sugerida` rota o selecciona plantillas disponibles.
- Titulo, importe y requisito de foto se editan y persisten como snapshot de
  asignacion.
- Chips de hijos para multi-seleccion.
- Opciones `Una vez`, `Diaria`, `Semanal`, `Personalizada`.
- Guardar crea `TaskAssignment`.

## Reglas Firestore

Anadir `taskAssignmentWriteIsValid(familyId)` con campos requeridos, lista no
vacia de hijos y `dueAt` timestamp. Mantener delete false.

## Validacion

1. `/test`: crear tests en rojo y confirmar fallo por implementacion pendiente.
2. `/implement`: implementar produccion hasta verde.
3. `./gradlew ktlintFormat --console=plain --no-configuration-cache`
4. `./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`
5. `git diff --check`
6. Abrir PR contra `develop`; no mergear sin revision humana.

## Trazabilidad prevista

| AC | Tests |
| --- | --- |
| AC-01 | `TaskAssignmentModelTest` |
| AC-02 | `TaskAssignmentModelTest`, `FirestoreRepositorySupportTest` |
| AC-03 | `InMemoryTaskAssignmentRepositoryTest`, `TaskAssignmentViewModelTest` |
| AC-04 | `InMemoryTaskAssignmentRepositoryTest` |
| AC-05 | `FirestoreRepositorySupportTest` |
| AC-06 | `TaskAssignmentViewModelTest`, `TaskAssignmentScreen` previews/manual screenshot |
| AC-07 | `AppModulesTest` |
