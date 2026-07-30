# FLE-48 Parent Interest Plan

## Scope

- Extend `Family` with parent interest settings.
- Add parent UI to configure enablement, annual rate and posting day.
- Add `LedgerTransactionType.Interest`.
- Add Cloud Functions scheduler for `(default)` and `debug`.
- Update Pencil with the new parent interest screen.

## Validation

- Common tests for repository, ViewModel, ledger and DI.
- Node tests for interest calculation and idempotent scheduler behavior.
- `./gradlew :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`.
- Targeted deploy for `runInterestAccruals` and `runInterestAccrualsDebug`.
