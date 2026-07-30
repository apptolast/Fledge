# FLE-54 Weekly Parent Digest Plan

## Scope

- Add weekly parent digest domain models and a pure calculator.
- Aggregate existing task instances, ledger transactions, cash-out settlements and savings goals.
- Add a read-only parent digest ViewModel backed by repository flows.
- Add a parent weekly digest screen and link it from parent home.
- Update Compose resources with positional placeholders.
- Add Pencil screen for the new digest UI.

## Validation

- Common tests for the digest calculator.
- Common ViewModel test using in-memory repositories.
- Koin graph test for calculator and ViewModel resolution.
- Pencil snapshot for the new digest screen: no layout problems.
- `./gradlew ktlintFormat --console=plain --no-configuration-cache`.
- `./gradlew :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 ktlintCheck --console=plain --no-configuration-cache`.
- `git diff --check`.
- Compose Resources placeholder scan.
