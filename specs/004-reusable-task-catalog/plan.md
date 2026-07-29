# Implementation Plan

## Enfoque tecnico

`FLE-27` anade un subdominio nuevo de tareas sin implementar aun el flujo de
asignacion. El catalogo debe poder vivir en Firestore desde el primer corte,
porque `FLE-28` y `FLE-29` dependeran de ids de plantilla estables.

Arquitectura:

- `domain/model/TaskModels.kt`: value objects y data classes.
- `domain/repository/TaskTemplateRepository.kt`: interfaz reactiva.
- `domain/service/InitialTaskTemplateCatalog.kt`: sugerencias deterministas por
  edad, puras y testeables.
- `data/repository/FirestoreTaskTemplateRepository.kt`: escucha
  `families/{familyId}/taskTemplates`, guarda plantillas y ejecuta seed
  idempotente.
- `data/repository/FirestoreRepositorySupport.kt`: mappers a/desde Firestore.
- `presentation/foundation/childsetup/ChildProfileSetupViewModel.kt`: al crear
  el primer hijo, llama al seed del catalogo. No cambia UI.
- `di/AppModules.kt`: binding del repositorio y constructor injection.
- `firestore.rules`: subcoleccion `taskTemplates`.

## Decisiones

1. Las sugerencias iniciales se guardan como plantillas familiares, no como
   datos globales: asi el padre puede reutilizarlas y el futuro editor podra
   archivarlas o modificarlas.
2. El seed se dispara desde `ChildProfileSetupViewModel` porque el AC habla del
   flujo "tras crear primer hijo". El repositorio lo hace idempotente para que
   un reintento no duplique datos.
3. No se anade una pantalla nueva en esta feature. Pencil ya contiene
   `Screen / Crear tarea` con `Usar tarea sugerida`; `FLE-28` conectara esa UI.
4. El catalogo usa textos semilla en espanol porque son contenido visible para
   familias espanolas. No son strings de UI de Compose, sino datos iniciales de
   usuario.

## Ficheros a tocar

### commonMain

- `domain/model/TaskModels.kt`
- `domain/repository/TaskTemplateRepository.kt`
- `domain/service/InitialTaskTemplateCatalog.kt`
- `data/repository/FirestoreTaskTemplateRepository.kt`
- `data/repository/FirestoreRepositorySupport.kt`
- `presentation/foundation/childsetup/ChildProfileSetupViewModel.kt`
- `di/AppModules.kt`

### commonTest

- `domain/TaskTemplateModelTest.kt`
- `domain/InitialTaskTemplateCatalogTest.kt`
- `data/InMemoryTaskTemplateRepositoryTest.kt`
- `data/repository/InMemoryTaskTemplateRepository.kt`
- `data/repository/FirestoreRepositorySupportTest.kt`
- `di/AppModulesTest.kt`
- `presentation/ChildProfileSetupViewModelTest.kt`

### Repo raiz

- `firestore.rules`
- `specs/004-reusable-task-catalog/spec.md`
- `specs/004-reusable-task-catalog/plan.md`

## Koin

`dataModule` registrara:

```kotlin
single { FirestoreTaskTemplateRepository(get(), get()) } bind TaskTemplateRepository::class
```

`ChildProfileSetupViewModel` pasara de recibir solo `FamilyFoundationRepository`
a recibir tambien `TaskTemplateRepository`.

## Impacto de plataforma

- Android/iOS: sin codigo especifico; la implementacion vive en commonMain sobre
  GitLive Firestore.
- Firebase rules: nueva subcoleccion familiar.
- i18n: sin strings Compose nuevas.
- Pencil: no hay pantalla nueva; se usa la pantalla existente de crear tarea.

## Desglose

1. Tests en rojo para modelos y catalogo inicial (`AC-01`, `AC-02`).
2. Tests en rojo para repositorio in-memory y seed idempotente (`AC-03`,
   `AC-04`).
3. Tests en rojo para mappers Firestore y Koin (`AC-05`, `AC-06`).
4. Implementar modelos, servicio y repositorio in-memory de test.
5. Implementar mappers y repositorio Firestore.
6. Wirear Koin y seed desde `ChildProfileSetupViewModel`.
7. Actualizar reglas Firestore.
8. Validacion: `ktlintFormat`, `:shared:testAndroid`,
   `:androidApp:assembleDebug`, `:shared:compileKotlinIosSimulatorArm64`,
   `git diff --check`.

## Trazabilidad prevista

| AC | Tests |
| --- | --- |
| AC-01 | `TaskTemplateModelTest` |
| AC-02 | `InitialTaskTemplateCatalogTest` |
| AC-03 | `InMemoryTaskTemplateRepositoryTest`, `ChildProfileSetupViewModelTest` |
| AC-04 | `InMemoryTaskTemplateRepositoryTest` |
| AC-05 | `FirestoreRepositorySupportTest` |
| AC-06 | `AppModulesTest` |
