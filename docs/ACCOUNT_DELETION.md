# Account Deletion

Public URL after Firebase Hosting deployment:

```text
https://fledge-c685d.web.app/account-deletion/
```

## In-App Flow

1. Sign in as the parent.
2. Open parent home.
3. Open `Cuenta y datos`.
4. Review the deletion scope.
5. Type `ELIMINAR`.
6. Submit the deletion request.

The client writes a request marker to `families/{uid}`:

```text
accountDeletionStatus = Requested
accountDeletionRequestedAt = <timestamp>
accountDeletionUpdatedAt = <timestamp>
```

Cloud Functions then marks the request as `Deleting`, deletes the Firebase Auth user, recursively
deletes `families/{uid}` and subcollections, and writes `accountDeletionAudit/{uid}`.

## Data Covered

- Parent login and Firebase Auth user.
- Family profile.
- Child profiles and linked child devices.
- Virtual ledger movements.
- Savings goals.
- Allowance rules.
- Task templates, assignments and generated task instances.
- Cash-out settlements.
- Push notification registrations.

## Manual Validation

- Request deletion from Android parent flow.
- Request deletion from iPhone parent flow.
- Verify the target Firestore database has no `families/{uid}` tree after Functions completes.
- Verify `accountDeletionAudit/{uid}` exists with `status = Completed`.
