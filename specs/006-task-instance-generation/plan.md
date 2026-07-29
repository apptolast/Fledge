# Implementation Plan

## Enfoque tecnico

`FLE-29` anade el subdominio `TaskInstance` y un job Cloud Functions Gen2. El
cliente observa instancias, pero la escritura inicial la hace el backend con
Admin SDK para cumplir que el job no dependa de abrir la app.

## Diseno

No hay UI nueva en esta tarea.

- Gate Pencil: N/A.
- Referencias revisadas para tareas posteriores: `Screen / Home hijo`,
  `Screen / Detalle tarea hijo`, `Screen / Cola de aprobacion`.

## Ficheros a tocar

### commonMain

- `domain/model/TaskModels.kt`
- `domain/repository/TaskInstanceRepository.kt`
- `data/repository/FirestoreRepositorySupport.kt`
- `data/repository/FirestoreTaskInstanceRepository.kt`
- `di/AppModules.kt`

### commonTest

- `domain/TaskInstanceModelTest.kt`
- `data/InMemoryTaskInstanceRepositoryTest.kt`
- `data/repository/InMemoryTaskInstanceRepository.kt`
- `data/repository/FirestoreRepositorySupportTest.kt`
- `di/AppModulesTest.kt`

### Functions / Firestore

- `functions/index.js`
- `functions/index.test.js`
- `functions/package.json`
- `functions/README.md`
- `firestore.rules`
- `firestore.rules.test.mjs`
- `firestore.indexes.json`

## Modelo propuesto

- `TaskInstanceId(value: String)`
- `TaskInstanceStatus`: `Pending`, `Submitted`, `Approved`, `Rejected`,
  `Expired`
- `TaskInstance`
  - `id`
  - `familyId`
  - `taskAssignmentId`
  - `taskTemplateId`
  - `childProfileId`
  - `title`
  - `rewardCents`
  - `requiresPhoto`
  - `status`
  - `dueAt`
  - `periodKey`
  - `createdAt`
  - `updatedAt`
  - lifecycle timestamps opcionales para FLE-30/FLE-31.

Validaciones:

- Titulo no vacio.
- Importe positivo.
- `periodKey` no vacio.
- Timestamps de lifecycle coherentes con estado cuando se usen.

## Scheduler

Exports:

- `runTaskAssignments` para `(default)`.
- `runTaskAssignmentsDebug` para database `debug`.
- `processDueTaskAssignments(database, nowDate)` testeable.

Consulta:

- collection group `taskAssignments`
- `active == true`
- `dueAt <= now`
- limite por run para evitar ejecuciones largas.

Transaccion por assignment:

1. Releer assignment.
2. Validar snapshot editable y recurrencia.
3. Leer familia para obtener `timeZone`, con fallback `Europe/Madrid`.
4. Calcular `periodKey` a partir de `dueAt` en zona familiar.
5. Crear `taskInstances/task_{assignmentId}_{childId}_{periodKey}` si no existe.
6. Avanzar `dueAt` para `Daily`, `Weekly` y `Custom`, o marcar `active=false`
   para `Once`.

## Reglas Firestore

`families/{familyId}/taskInstances/{instanceId}`:

- `read`: padre o child con claim del mismo `childProfileId`.
- `create/update/delete`: `false` en FLE-29. Functions Admin SDK bypassa reglas.

## Validacion

1. `./gradlew ktlintFormat --console=plain --no-configuration-cache`
2. `./gradlew ktlintCheck --console=plain --no-configuration-cache`
3. `./gradlew :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`
4. `npm test`
5. `npm --prefix functions run check`
6. `npm --prefix functions test`
7. `git diff --check`
8. Deploy:
   - `firebase deploy --only firestore --project fledge-c685d`
   - `firebase deploy --only functions:runTaskAssignments,functions:runTaskAssignmentsDebug --project fledge-c685d`

## Trazabilidad prevista

| AC | Tests |
| --- | --- |
| AC-01 | `TaskInstanceModelTest`, `FirestoreRepositorySupportTest` |
| AC-02 | `TaskInstanceModelTest` |
| AC-03 | `functions/index.test.js` |
| AC-04 | `functions/index.test.js` |
| AC-05 | `functions/index.test.js`, `functions/README.md` |
| AC-06 | `firestore.rules.test.mjs`, `InMemoryTaskInstanceRepositoryTest` |
