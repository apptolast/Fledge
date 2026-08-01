# FLE-96 Child Profile Entry Plan

## Scope

- Add the active child display name to `ChildHomeUiState`.
- Change `child_home_title` resources to indexed placeholders in ES/EN/PT.
- Add a Parent Home child-card action that opens `ChildHomeRoute(childProfileId)`.
- Wire the new callback through `ParentHomeScreen` and `FledgeNavHost`.
- Update Pencil `Screen / Home padre` to show the child profile entry action.

## Validation

- Common tests for active child greeting state.
- Navigation test for parent-to-child route helper.
- `./gradlew ktlintFormat --console=plain --no-configuration-cache`.
- `./gradlew :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 ktlintCheck --console=plain --no-configuration-cache`.
- `git diff --check`.
- Compose Resources placeholder scan.
