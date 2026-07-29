# Spec 011: Expiracion y reintento amable de tareas (FLE-34)

> Rama: `feature/FLE-34-task-expiry-retry` · Ticket: `FLE-34` · Parent: `FLE-26`

## Objetivo

FLE-34 cierra el ciclo diario de tareas sin culpar al hijo: las tareas que no se envian antes de su
vencimiento pasan a `Expired`, y una tarea rechazada puede volver a `Pending` para corregirse y
reenviarse durante el mismo dia.

## Alcance

- Scheduler Gen2 para caducar `taskInstances` en estado `Pending` o `Rejected` cuando `dueAt <= now`.
- Transicion de dominio `Rejected -> Pending` para reintento antes de `dueAt`.
- El reenvio directo `Rejected -> Submitted` queda bloqueado; primero debe volver a `Pending`.
- UI infantil muestra:
  - Rechazada: nota adulta en tono de ayuda y CTA `Volver a intentar`.
  - Vencida: estado cerrado sin CTA de envio y texto no culpabilizador.
- Reglas Firestore permiten al hijo propietario devolver su tarea rechazada a `Pending` antes de
  `dueAt`, pero no setear `Expired` desde cliente.

## Diseno Pencil

- Existing:
  - `Screen / Home hijo`.
  - `Screen / Detalle tarea hijo - pendiente con foto`.
  - `Screen / Detalle tarea hijo - enviada`.
- Added for FLE-34:
  - `Screen / Detalle tarea hijo - rechazada para repetir`.
  - `Screen / Detalle tarea hijo - vencida sin culpa`.
- `snapshot_layout(problemsOnly=true)` no reporta problemas tras crear los frames.

## Fuera De Alcance

- Fastlane, GitHub Actions y FLE-26 quedan fuera por peticion explicita del sprint.
- Push especifico de expiracion/rechazo.
- Explicar al padre metricas historicas de tareas vencidas.

## Criterios De Aceptacion

### AC-01 - Expiracion automatica

Given una `taskInstance` con `status=Pending` o `Rejected`
And `dueAt <= now`
When corre el scheduler de expiracion
Then el estado pasa a `Expired`
And se informa `expiredAt`.

### AC-02 - Sin caducar tareas ya enviadas

Given una `taskInstance` con `status=Submitted`
And `dueAt <= now`
When corre el scheduler de expiracion
Then no cambia de estado.

### AC-03 - Reintento amable mismo dia

Given una tarea rechazada con `dueAt` aun vigente
When el hijo toca `Volver a intentar`
Then la tarea vuelve a `Pending`
And se limpian `submittedAt`, `reviewedAt`, evidencia y motivo de rechazo.

### AC-04 - Bloqueo tras vencimiento

Given una tarea rechazada ya vencida
When el hijo intenta volver a intentarla
Then la operacion falla y no se modifica la tarea.

### AC-05 - Tono infantil

Given una tarea rechazada o vencida aparece en la UI del hijo
When se renderiza la tarjeta
Then los textos son neutrales, accionables y no culpabilizadores.
