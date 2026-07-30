# FLE-95 Account and data deletion

## User story

As a parent using Fledge, I want a clear way to request deletion of my account and family data so that store privacy requirements are met before closed beta submission.

## Scope

- Parent-only in-app flow from parent home to a dedicated deletion confirmation screen.
- The client records a deletion request on the authenticated family document.
- Cloud Functions processes the request, deletes the Firebase Auth user, recursively deletes the family tree, and writes an aggregate audit record.
- Store/compliance docs explain the in-app path and the public account deletion URL.

## Out of scope

- Fastlane, GitHub Actions, and FLE-26.
- A child-facing deletion flow.
- Partial export or granular data retention controls.

## Scenarios

### Scenario [AC-01] Parent can discover account deletion without triggering it accidentally

Given a signed-in parent is viewing the parent home
When the setup section is rendered
Then it shows a "Cuenta y datos" entry
And tapping it navigates to the account deletion confirmation screen instead of deleting data directly

### Scenario [AC-02] Deletion requires explicit confirmation

Given the parent is on the account deletion screen
When the confirmation input is not exactly "ELIMINAR"
Then the destructive request action is disabled

### Scenario [AC-03] Deletion request is submitted through the repository

Given the parent typed "ELIMINAR"
When the parent submits the deletion request
Then the ViewModel calls the account deletion repository
And the UI enters the submitted state

### Scenario [AC-04] Firestore request writes only a server-processed marker

Given an authenticated parent
When account deletion is requested
Then the repository marks `families/{uid}` with `accountDeletionStatus = Requested`
And the client does not directly delete family subcollections

### Scenario [AC-05] Cloud Function deletes Auth and Firestore data

Given `families/{uid}` transitions to `accountDeletionStatus = Requested`
When the function runs
Then it marks the request as deleting
And deletes the Firebase Auth user
And recursively deletes the family document tree
And writes an `accountDeletionAudit/{uid}` completion record

### Scenario [AC-06] Cloud Function is idempotent and observable

Given the Auth user was already deleted
When the deletion function runs
Then it still recursively deletes the family data and records completion

Given recursive Firestore deletion fails
When the deletion function runs
Then it marks the family document as failed
And writes a failed audit record

### Scenario [AC-07] DI resolves the production flow

Given the application Koin graph is created with Firebase dependencies
When account deletion dependencies are resolved
Then `AccountDeletionRepository` and `AccountDeletionViewModel` are available.

## Design

Pencil updated in `Fledge.pen`:

- `Screen / Borrado de cuenta - confirmar`
- `Screen / Borrado de cuenta - enviado`

The flow follows the parent-home visual language, keeps the destructive action in a dedicated confirmation screen, and requires the typed phrase `ELIMINAR`.

## Traceability

- AC-01 -> `ParentHomeViewModelTest`, `ParentHomeContent` wiring
- AC-02 -> `AccountDeletionViewModelTest`
- AC-03 -> `AccountDeletionViewModelTest`
- AC-04 -> `FirestoreAccountDeletionRepositoryTest` or repository implementation review
- AC-05 -> `functions/index.test.js`
- AC-06 -> `functions/index.test.js`
- AC-07 -> `AppModulesTest`
