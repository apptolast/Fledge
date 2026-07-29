# Spec 007: Envio de tarea completada por el hijo (FLE-30)

> Rama: `feature/FLE-30-child-task-completion` · Proyecto: `Fledge`

## Contexto

`FLE-29` genera `TaskInstance` pendientes por hijo. `FLE-30` permite que el modo
hijo marque una instancia como completada y la envie a revision parental. Esta
accion no mueve dinero: la recompensa se suma al ledger solo cuando el padre o
madre aprueba la tarea en `FLE-31`.

## Jira Mapping

| Key | Scope |
| --- | --- |
| `FLE-26` | Fase 1 - Tasks and Approvals |
| `FLE-30` | Permitir que el hijo marque tarea como completada |

## Alcance

### Dentro

- Anadir evidencia opcional `photoEvidenceUri` a `TaskInstance`.
- Permitir enviar una instancia `Pending` o `Rejected` a `Submitted`.
- Exigir `photoEvidenceUri` no vacia cuando `requiresPhoto = true`.
- Registrar `submittedAt` y `updatedAt` al enviar.
- Mostrar tareas del hijo en `ChildHome`.
- Permitir enviar desde la UI de hijo con estado de carga, error y bloqueo de
  doble tap.
- Actualizar reglas Firestore para aceptar solo la transicion valida a
  `Submitted`.
- Tests de dominio, repositorio fake, ViewModel y reglas.

### Fuera

- Captura nativa de camara o subida a Firebase Storage.
- Cola de aprobacion parental (`FLE-31`).
- Creacion de transacciones de ledger por tarea (`FLE-31`).
- Notificaciones push (`FLE-32`).
- Deep link directo a detalle de tarea.

## Design Check

FLE-30 toca UI y el gate de Pencil **no es N/A**.

Revision realizada en `/Users/hgarcia/Documents/Companies/AppToLast/KMP/Fledge/Fledge.pen`:

- `Screen / Home hijo` contiene la seccion `Tus tareas de hoy` como origen
  visual para listar `TaskInstance` del hijo.
- `Screen / Detalle tarea hijo - pendiente con foto` contiene el flujo de
  completar con CTA `¡Lo he hecho!` y accion secundaria `Añadir foto
  obligatoria`.
- `Screen / Detalle tarea hijo - enviada` se creo para el estado `Submitted`:
  CTA atenuado `Esperando aprobacion`, evidencia `Foto adjunta` y nota de que
  el dinero no aparece hasta aprobacion.
- `snapshot_layout(problemsOnly=true)` no reporta problemas en la pantalla
  enviada.

Condicion de implementacion: la UI de `ChildHome` debe usar el lenguaje visual
del `.pen` ya existente: fondo claro, superficies blancas, borde sutil, acento
teal, botones de 48-56dp y textos externos via Compose Resources.

## Acceptance Criteria

### AC-01 - Modelo de envio

Given una instancia pendiente
When el hijo la envia a revision
Then el estado pasa a `Submitted`
And `submittedAt`, `updatedAt` y `photoEvidenceUri` quedan modelados.

### AC-02 - Foto obligatoria

Given una instancia con `requiresPhoto = true`
When se intenta enviar sin evidencia de foto
Then el dominio o repositorio rechaza la operacion
And la UI muestra un error recuperable.

### AC-03 - Reenvio tras rechazo

Given una instancia `Rejected`
When el hijo vuelve a completarla
Then puede pasar de nuevo a `Submitted`
And conserva los campos economicos y de propiedad.

### AC-04 - No genera dinero

Given una tarea enviada
When el estado pasa a `Submitted`
Then no se crea ninguna transaccion de ledger
And el saldo del hijo no cambia.

### AC-05 - UI modo hijo

Given el hijo tiene tareas pendientes y enviadas
When abre `ChildHome`
Then ve la lista de tareas de hoy
And puede enviar solo las tareas accionables
And las enviadas se muestran como esperando aprobacion.

### AC-06 - Reglas Firestore

Given una `TaskInstance` propia
When el hijo o la sesion familiar actualiza solo los campos permitidos para
enviarla
Then las reglas permiten el update
And rechazan cambios de importe, hijo, familia, aprobacion o instancia ajena.

## Gherkin Scenarios

```gherkin
Feature: Envio de tarea completada por el hijo

  Scenario: [AC-01] TaskInstance modela evidencia de envio
    Given una TaskInstance pendiente
    When se crea como Submitted con submittedAt y photoEvidenceUri
    Then conserva la evidencia y la fecha de envio

  Scenario: [AC-02] La foto obligatoria se valida antes de enviar
    Given una TaskInstance que requiere foto
    When se intenta enviar sin photoEvidenceUri
    Then la operacion se rechaza

  Scenario: [AC-03] Una tarea rechazada puede reenviarse
    Given una TaskInstance Rejected
    When el hijo la envia otra vez
    Then el repositorio la deja en Submitted

  Scenario: [AC-04] Enviar una tarea no cambia el saldo
    Given un hijo con saldo derivado del ledger
    When envia una TaskInstance
    Then el saldo principal sigue igual

  Scenario: [AC-05] ChildHome muestra y envia tareas accionables
    Given el hijo tiene tareas Pending, Submitted y Approved
    When abre ChildHome y envia una tarea pendiente
    Then la pendiente pasa a Submitted
    And las no accionables no ofrecen envio

  Scenario: [AC-06] Reglas de envio de TaskInstance
    Given una TaskInstance propia pendiente
    When se actualiza a Submitted manteniendo campos protegidos
    Then el update se permite
    And se rechazan tampering, aprobacion directa y tarea de otro hijo
```

## Trazabilidad

| AC | Tests |
| --- | --- |
| AC-01 | `TaskInstanceModelTest`, `FirestoreRepositorySupportTest` |
| AC-02 | `TaskInstanceModelTest`, `InMemoryTaskInstanceRepositoryTest`, `ChildHomeViewModelTest` |
| AC-03 | `InMemoryTaskInstanceRepositoryTest` |
| AC-04 | `ChildHomeViewModelTest` |
| AC-05 | `ChildHomeViewModelTest`, `Screen / Home hijo`, `Screen / Detalle tarea hijo - enviada` |
| AC-06 | `firestore.rules.test.mjs` |
