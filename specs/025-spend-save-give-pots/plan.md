# FLE-50 Spend Save Give Pots Plan

## Scope

- Add `MoneyPotType` with Spend, Save and Give.
- Add `VirtualAccountType.Give` and extend `ChildLedgerBalances`.
- Add `potType` to `SavingsGoal` and `SavingsGoalDraft`.
- Map Save goals to GOAL and Give goals to GIVE, defaulting legacy goals to Save.
- Update deposit and withdrawal processors to use the goal's account type.
- Update goal setup UI with a Save/Give selector.
- Update Child Home with a Spend/Save/Give pot summary.
- Update Firestore mappers and rules.

## Validation

- Common tests for model, processors, ViewModels and mapper compatibility.
- Firestore rules tests for Save/Give validation.
- Pencil snapshots for `c5ASor` and `UfPts`: no layout problems.
- `npm test`.
- `firebase --project fledge-rules-test --config firebase.rules-test.json emulators:exec --only firestore "node --test firestore.rules.test.mjs"`.
- `./gradlew ktlintFormat --console=plain --no-configuration-cache`.
- `./gradlew :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`.
- `git diff --check`.
