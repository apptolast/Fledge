# FLE-51 Parent Match Plan

## Scope

- Add `MatchSettings` to `Family`.
- Persist match fields in Firestore and in-memory repositories.
- Add `updateMatchSettings` to `FamilyFoundationRepository`.
- Add a parent match settings screen and route.
- Add a Home parent setup action for match.
- Extend goal deposits so child deposits create a capped `Match` ledger transaction in the target goal account.
- Add ledger labels/resources for match entries.

## Validation

- Common tests for model, repository, processor and ViewModel behavior.
- Koin test for the new ViewModel.
- Pencil snapshot for `ICvDC`: no layout problems.
- `./gradlew ktlintFormat --console=plain --no-configuration-cache`.
- `./gradlew :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`.
- `npm test`.
- `git diff --check`.
- Compose Resources placeholder scan.
