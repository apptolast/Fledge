# FLE-56 ES/EN/PT Internationalization Plan

## Scope

- Complete missing English Compose resource keys.
- Add Portuguese Compose resources covering every default translatable key.
- Add host-side resource validation for locale parity and placeholder compatibility.
- Keep the design gate as N/A because the task does not add or modify UI layouts.

## Validation

- Android host test for ES/EN/PT resource parity.
- Android host test for positional placeholder and percent escaping rules.
- `./gradlew ktlintFormat --console=plain --no-configuration-cache`.
- `./gradlew :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 ktlintCheck --console=plain --no-configuration-cache`.
- `git diff --check`.
- Compose Resources XML and placeholder scan.
