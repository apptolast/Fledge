# Implementation Plan

## Enfoque tecnico

`FLE-30` extiende el subdominio `TaskInstance` con una operacion de envio desde
modo hijo. La fuente de verdad sigue siendo Firestore bajo
`families/{familyId}/taskInstances/{instanceId}`. El cliente solo puede cambiar
el estado a `Submitted` con timestamps y evidencia permitida; no crea ledger ni
aprueba tareas.

## Diseno

Gate Pencil ejecutado contra `Fledge.pen`.

- Pantalla base revisada: `Screen / Home hijo`.
- Pantalla base revisada y renombrada: `Screen / Detalle tarea hijo - pendiente
  con foto`.
- Pantalla nueva creada: `Screen / Detalle tarea hijo - enviada` (`FAKQV`).
- Layout verificado con `snapshot_layout(problemsOnly=true)` sobre `FAKQV` sin
  problemas.

La implementacion Compose debe aproximar esa especificacion dentro de
`ChildHome`: tarjetas blancas, borde sutil, acento principal, CTA visible de
envio y estado enviado atenuado.

## Ficheros a tocar

### commonMain

- `domain/model/TaskModels.kt`
- `domain/repository/TaskInstanceRepository.kt`
- `data/repository/FirestoreRepositorySupport.kt`
- `data/repository/FirestoreTaskInstanceRepository.kt`
- `presentation/foundation/childhome/ChildHomeViewModel.kt`
- `presentation/foundation/childhome/ChildHomeScreen.kt`
- `composeResources/values/strings.xml`
- `composeResources/values-en/strings.xml`

### commonTest

- `domain/TaskInstanceModelTest.kt`
- `data/InMemoryTaskInstanceRepositoryTest.kt`
- `data/repository/InMemoryTaskInstanceRepository.kt`
- `presentation/ChildHomeViewModelTest.kt`
- `data/repository/FirestoreRepositorySupportTest.kt`

### Firestore

- `firestore.rules`
- `firestore.rules.test.mjs`

## Modelo y contratos

`TaskInstance` anade:

- `photoEvidenceUri: String? = null`

Validaciones:

- `photoEvidenceUri`, si existe, debe ser no vacia.
- `Submitted` requiere `submittedAt`.
- `requiresPhoto` + `Submitted` requiere `photoEvidenceUri`.

`TaskInstanceRepository` anade:

```kotlin
suspend fun submitForReview(
    instanceId: TaskInstanceId,
    childProfileId: ChildProfileId,
    photoEvidenceUri: String?,
    submittedAt: Instant = Clock.System.now(),
): TaskInstance
```

Reglas de dominio:

- Solo `Pending` y `Rejected` pueden pasar a `Submitted`.
- La transicion conserva familia, hijo, plantilla, asignacion, titulo, importe,
  vencimiento, periodo, creacion y campos de revision/expiracion.
- No toca ledger ni balances.

## UI y ViewModel

`ChildHomeUiState` anade:

- `taskInstances: List<TaskInstance>`
- `selectedPhotoEvidenceByTaskId: Map<TaskInstanceId, String>`
- `taskSubmissionError: ChildTaskSubmissionError?`

`ChildHomeViewModel`:

- Observa `TaskInstanceRepository.syncStatus` junto al resto de repositorios.
- Refresca las tareas del `childProfileId` cargado.
- Expone `attachPhotoEvidence(instanceId, uri)`.
- Expone `submitTask(instanceId)`, con busy state y error especifico para foto
  obligatoria.

`ChildHomeScreen`:

- Muestra `Tus tareas de hoy` antes de cash-out.
- Tarjetas accionables para `Pending` y `Rejected`.
- CTA `Enviar para revisar`.
- Si `requiresPhoto`, muestra selector simulado de evidencia fase 1 y error si
  falta.
- Estados `Submitted`, `Approved` y `Expired` se muestran como informativos.

## Reglas Firestore

`taskInstanceSubmissionIsValid(familyId)` debe permitir update si:

- Actor: `isParent(familyId)` o `isChild(familyId)` propietario.
- `resource.data.childProfileId` coincide con el hijo cuando hay custom claim.
- Estado anterior `Pending` o `Rejected`.
- Estado nuevo `Submitted`.
- Campos protegidos no cambian.
- `submittedAt` y `updatedAt` son timestamp.
- `reviewedAt` y `expiredAt` no cambian.
- Si `requiresPhoto = true`, `photoEvidenceUri` es string no vacio.

`create` y `delete` siguen bloqueados para clientes.

## Validacion

1. Confirmar tests rojos tras escribirlos:
   - `./gradlew :shared:testAndroid --console=plain --no-configuration-cache`
   - `npm test`
2. Implementar produccion hasta verde.
3. Ejecutar:
   - `./gradlew ktlintFormat --console=plain --no-configuration-cache`
   - `./gradlew ktlintCheck --console=plain --no-configuration-cache`
   - `./gradlew :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`
   - `npm test`
   - `npm --prefix functions run check`
   - `npm --prefix functions test`
   - `git diff --check`
4. Abrir PR contra `develop` y mover FLE-30 a `In Review`.

## Trazabilidad prevista

| AC | Tests |
| --- | --- |
| AC-01 | `TaskInstanceModelTest`, `FirestoreRepositorySupportTest` |
| AC-02 | `TaskInstanceModelTest`, `InMemoryTaskInstanceRepositoryTest`, `ChildHomeViewModelTest` |
| AC-03 | `InMemoryTaskInstanceRepositoryTest` |
| AC-04 | `ChildHomeViewModelTest` |
| AC-05 | `ChildHomeViewModelTest`, `PreviewChildHomeContent` manual/Pencil |
| AC-06 | `firestore.rules.test.mjs` |
