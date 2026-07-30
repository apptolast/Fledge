# FLE-55 Statement Export Plan

## Scope

- Add statement export domain models and a pure builder.
- Build CSV content from existing ledger transactions and cash-out settlements.
- Build a minimal PDF artifact from the same statement rows.
- Add a parent statement export ViewModel backed by repository flows.
- Add the statement export screen and link it from parent home.
- Update Compose resources with positional placeholders.
- Add Pencil screen for the export UI.

## Validation

- Common tests for CSV/PDF statement generation.
- Common ViewModel test using in-memory repositories.
- Koin graph test for builder and ViewModel resolution.
- Pencil snapshot for the new export screen: no layout problems.
- `./gradlew ktlintFormat --console=plain --no-configuration-cache`.
- `./gradlew :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 ktlintCheck --console=plain --no-configuration-cache`.
- `git diff --check`.
- Compose Resources placeholder scan.
