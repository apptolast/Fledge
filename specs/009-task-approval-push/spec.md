# Spec 009: Push de submission y aprobacion (FLE-32)

> Rama: `feature/FLE-32-task-approval-push` · Ticket: `FLE-32` · Parent: `FLE-26`

## Objetivo

FLE-32 cierra el hueco operativo de la cola de aprobacion: cuando un hijo envia una tarea, los
dispositivos del padre reciben un push; cuando el padre aprueba la tarea y el saldo se incrementa,
los dispositivos del hijo reciben un push.

## Alcance

- FCM por topics deterministas, sin almacenar tokens de dispositivo en Firestore.
- Android e iOS se suscriben desde la app a los topics que correspondan al rol actual:
  - Padre: `fledge_<env>_family_<familyId>_parents`.
  - Hijo: `fledge_<env>_family_<familyId>_child_<childProfileId>`.
- Cloud Functions reacciona a cambios de `taskInstances`:
  - `Submitted` nuevo o desde otro estado notifica al topic de padres.
  - `Approved` nuevo o desde otro estado notifica al topic del hijo.
- El payload incluye `type`, `familyId`, `childProfileId` y `taskInstanceId`.
- Al tocar una notificacion, la app abre la zona natural del rol:
  - Submission: `ParentHomeRoute`.
  - Aprobacion: `ChildPinRoute(childProfileId)`.

## Fuera De Alcance

- Fastlane, GitHub Actions y FLE-26 quedan fuera por peticion explicita del sprint.
- Google en iOS y Apple en Android no cambian.
- Push de rechazo, recordatorios o caducidad no forman parte de FLE-32.
- Nueva UI in-app: no hay pantallas nuevas. El design check de Pencil es N/A para esta spec.

## Criterios De Aceptacion

### AC-01 - Topic de padre al enviar tarea

Given una `taskInstance` de una familia existente
When su estado pasa a `Submitted`
Then Functions envia un push al topic de padres de esa familia
And el payload incluye `type=task_submitted`, `familyId`, `childProfileId` y `taskInstanceId`.

### AC-02 - Topic de hijo al aprobar tarea

Given una `taskInstance` enviada por un hijo
When su estado pasa a `Approved`
Then Functions envia un push al topic del hijo de esa familia
And el payload incluye `type=task_approved`, `familyId`, `childProfileId`, `taskInstanceId` y
`approvedRewardCents`.

### AC-03 - Sin duplicados por updates irrelevantes

Given una `taskInstance` que ya esta en `Submitted` o `Approved`
When se actualiza otro campo sin cambiar el estado
Then Functions no envia otro push.

### AC-04 - Suscripcion cliente por rol

Given un padre autenticado con familia activa
When el flujo post-login resuelve `ParentHome`
Then la app intenta suscribirse al topic de padres.

Given un hijo desbloquea su PIN correctamente
When existe familia activa
Then la app intenta suscribirse al topic del perfil infantil.

### AC-05 - Tap de notificacion

Given la app recibe un tap de push con `type=task_submitted`
When el payload tiene `familyId` y `taskInstanceId`
Then se registra un deep link a la cola de aprobacion del padre.

Given la app recibe un tap de push con `type=task_approved`
When el payload tiene `childProfileId`
Then se registra un deep link al acceso del hijo.
