# FLE-49 Compound Interest Visualization Plan

## Scope

- Add a common projection calculator for monthly compounding.
- Add projection state to `ChildHomeViewModel`.
- Add a compact child-home card with three bars: now, one year and three years.
- Externalize all strings with indexed placeholders.
- Add/update Pencil screen for the child-home compound-interest state.

## Validation

- Common tests for calculator and child home visibility behavior.
- Pencil snapshot for `GOQIm`: no layout problems.
- `./gradlew ktlintFormat --console=plain --no-configuration-cache`.
- `./gradlew :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`.
