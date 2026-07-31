# FLE-57 Streaks And Badges Plan

## Scope

- Add a pure `ChildAchievementCalculator` that consumes task instances and a timezone.
- Add domain models for child habit summary and badge progress.
- Expose the summary from `ChildHomeViewModel`.
- Add a compact Child Home achievement card aligned with Pencil.
- Externalize new UI strings in ES/EN/PT.

## Validation

- Common tests for streak calculation across approved task days.
- Common tests for locked/unlocked badge progress.
- ChildHomeViewModel test proving the summary is derived from repository data.
- Pencil snapshot for `Screen / Home hijo - rachas y badges`: no layout problems.
- `./gradlew ktlintFormat --console=plain --no-configuration-cache`.
- `./gradlew :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 ktlintCheck --console=plain --no-configuration-cache`.
- `git diff --check`.
- Compose Resources placeholder scan.
