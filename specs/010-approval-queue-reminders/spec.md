# Spec 010: Recordatorios de cola de aprobacion acumulada (FLE-33)

> Rama: `feature/FLE-33-approval-queue-reminders` · Ticket: `FLE-33` · Parent: `FLE-26`

## Objetivo

FLE-33 reduce el riesgo de que una familia abandone el flujo de tareas porque el padre no vuelva a
la cola de aprobacion. Si la cola de tareas enviadas se acumula, Functions envia un recordatorio
agrupado al topic de padres.

## Alcance

- Cloud Function programada para `(default)`/release y `debug`.
- La funcion lee `taskInstances` en estado `Submitted` y agrupa por familia.
- Se envia un unico push por familia cuando:
  - Hay mas de 5 tareas enviadas esperando aprobacion.
  - O la tarea enviada mas antigua lleva al menos 72 horas esperando.
- El push reutiliza el topic de padres de FLE-32:
  `fledge_<env>_family_<familyId>_parents`.
- El payload usa `type=approval_queue_reminder`, `familyId`, `pendingCount` y
  `oldestSubmittedAt`.
- El tap navega a la home parental, donde ya vive la cola de aprobacion.
- Se guarda `approvalQueueReminderLastSentAt` en el documento de familia para evitar repetir el
  recordatorio en menos de 24 horas.

## Fuera De Alcance

- Fastlane, GitHub Actions y FLE-26 quedan fuera por peticion explicita del sprint.
- Nuevas pantallas, banners o estados visuales. Pencil/design-check: N/A.
- Expiracion o reintento de tareas; eso pertenece a FLE-34.

## Criterios De Aceptacion

### AC-01 - Recordatorio por volumen

Given una familia tiene 6 o mas `taskInstances` con `status=Submitted`
When corre el scheduler de recordatorios
Then se envia un solo push al topic de padres
And el payload incluye `pendingCount` y `oldestSubmittedAt`.

### AC-02 - Recordatorio por antiguedad

Given una familia tiene al menos una `taskInstance` con `status=Submitted`
And su `submittedAt` tiene 72 horas o mas
When corre el scheduler de recordatorios
Then se envia un solo push al topic de padres aunque la cola no supere 5 tareas.

### AC-03 - Sin ruido bajo umbral

Given una familia tiene menos de 6 tareas enviadas
And ninguna lleva 72 horas esperando
When corre el scheduler de recordatorios
Then no se envia push.

### AC-04 - Cooldown por familia

Given una familia ya recibio un recordatorio hace menos de 24 horas
When corre el scheduler y la cola sigue acumulada
Then no se envia otro push.

### AC-05 - Tap de recordatorio

Given la app recibe un tap con `type=approval_queue_reminder`
When el payload tiene `familyId`
Then se registra un deep link a la cola de aprobacion parental.
