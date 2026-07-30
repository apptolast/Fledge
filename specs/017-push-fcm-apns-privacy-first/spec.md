# Spec 017: Push FCM/APNs privacy-first (FLE-42)

> Rama: `feature/FLE-42-push-fcm-apns-privacy-first` · Ticket: `FLE-42` · Parent: `FLE-41`

## Objetivo

FLE-42 prepara la base de notificaciones push de Fledge sin exponer informacion sensible en payloads
ni en reglas de acceso. La app debe poder registrar tokens por familia, rol, plataforma y dispositivo,
y debe dejar definido un contrato seguro para que futuras Cloud Functions puedan enviar avisos por FCM/APNs.

## Alcance

- Modelar registros push con metadatos minimos: familia, rol, plataforma, token, instalacion,
  `childProfileId` solo cuando el rol sea `Child`, y marca temporal.
- Persistir registros push en Firestore dentro de la familia activa.
- Permitir que el padre registre tokens de padre y que un hijo registre solo su propio token.
- Evitar que un hijo lea tokens de padre o de hermanos.
- Sanitizar payloads de notificacion con una lista blanca de claves no sensibles.
- Documentar el contrato de SDKs/configuracion pendiente para FCM/APNs nativo.

## Fuera De Alcance

- Integrar KMPNotifier, `google-services.json`, `GoogleService-Info.plist` o Firebase Messaging nativo.
- Pedir permisos runtime de notificaciones en Android/iOS.
- Enviar push reales desde Cloud Functions.
- Crear pantallas nuevas o prompts visibles.
- Fastlane, GitHub Actions y FLE-26 quedan fuera de este sprint por decision del usuario.

## Diseno Pencil

- Revisado `Fledge.pen` via MCP de Pencil.
- Sin impacto de UI en esta historia: no se introduce pantalla, banner, prompt ni componente visible.
- Gate de diseno N/A. Si una fase posterior anade permiso in-app o centro de notificaciones, se creara
  primero en Pencil y despues se alineara Compose contra ese diseno.

## Criterios De Aceptacion

### AC-01 - Registro push por rol y plataforma

Given una familia autenticada
When la app recibe un token FCM/APNs para una instalacion
Then puede construir un registro valido con `familyId`, `installationId`, `token`, `platform`,
`role`, `updatedAt`
And los registros de rol `Child` requieren `childProfileId`
And los registros de rol `Parent` no incluyen `childProfileId`.

### AC-02 - Repositorio de registros push

Given un registro push valido para una familia
When se guarda en el repositorio
Then queda indexado por rol, plataforma e instalacion
And si cambia el token de la misma instalacion se reemplaza el registro previo
And se puede desactivar el registro sin borrarlo fisicamente.

### AC-03 - Payload privacy-first

Given un evento de notificacion que contiene datos de negocio
When se prepara el payload para FCM/APNs
Then solo se incluyen claves permitidas de enrutado
And se eliminan importes, nombres de hijos, titulos de tareas/objetivos y conceptos monetarios.

### AC-04 - Reglas Firestore para tokens

Given registros push almacenados bajo una familia
When accede un padre autenticado
Then puede crear, leer y actualizar registros de su familia
And otro padre no puede accederlos
And un hijo solo puede crear, leer y actualizar registros `Child` de su propio `childProfileId`.

### AC-05 - Auditoria de SDKs

Given que la entrega nativa queda fuera de este corte
When se documenta la preparacion de push
Then queda explicito que Fledge mantiene `FirebaseOptions` por BuildKonfig
And queda explicito que KMPNotifier/google-services requiere una decision posterior de arquitectura.

## Trazabilidad

- AC-01 -> `PushRegistrationModelTest`.`FLE-42 AC-01 parent registration has no child profile`
- AC-01 -> `PushRegistrationModelTest`.`FLE-42 AC-01 child registration requires child profile`
- AC-01 -> `PushRegistrationModelTest`.`FLE-42 AC-01 parent registration rejects child profile`
- AC-02 -> `InMemoryPushRegistrationRepositoryTest`.`FLE-42 AC-02 upsert replaces rotated token for the same installation`
- AC-02 -> `InMemoryPushRegistrationRepositoryTest`.`FLE-42 AC-02 parent and child registrations share installation without colliding`
- AC-02 -> `InMemoryPushRegistrationRepositoryTest`.`FLE-42 AC-02 deactivate keeps registration as inactive`
- AC-03 -> `PushPayloadSanitizerTest`.`FLE-42 AC-03 keeps only privacy safe routing fields`
- AC-03 -> `PushPayloadSanitizerTest`.`FLE-42 AC-03 drops blank and unknown fields`
- AC-04 -> `firestore.rules.test.mjs`.`push registrations are private by role and child ownership`
- AC-05 -> `docs/PUSH_NOTIFICATIONS_SETUP.md`
- AC-05 -> `AppModulesTest`.`FLE-77 production repositories use firestore after sdk integration`
