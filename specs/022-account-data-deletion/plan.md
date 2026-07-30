# FLE-95 Plan

## Implementation

1. Add account deletion domain models and `AccountDeletionRepository`.
2. Implement `FirestoreAccountDeletionRepository` as a request marker writer and state observer.
3. Add `AccountDeletionViewModel` and Compose screen with previews, using externalized resources.
4. Add route wiring from parent home setup into the account deletion screen.
5. Register repository and ViewModel in Koin.
6. Add Cloud Functions Gen2 triggers for `(default)` and `debug` Firestore databases.
7. Add function tests for requested, skipped, auth-user-missing, and recursive-delete-failed cases.
8. Update compliance docs with the in-app flow and public deletion URL.

## Validation

```bash
npm --prefix functions test
./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache
git diff --check
```

## Manual QA

- Android: login as parent, open parent home, go to Cuenta y datos, confirm disabled until `ELIMINAR`, submit request.
- iPhone: repeat same flow after parent login.
- Firebase: verify `accountDeletionAudit/{uid}` completion and removed `families/{uid}` tree in target database.
