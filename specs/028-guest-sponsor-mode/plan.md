# FLE-53 Guest Sponsor Mode Plan

## Scope

- Add guest invite and contribution domain models.
- Add `GuestSponsorRepository` with Firestore implementation.
- Resolve active guest access by normalized signed-in email from `familyGuestInvites/{email}`.
- Add parent screen to invite a guest for a selected child.
- Add guest home screen for sponsored-child gift/match contributions.
- Update post-login routing to prefer parent flow when a family exists and route pure guests to guest home.
- Add Firestore rules for guest invite read/write, guest child reads and guest ledger creates.
- Add Pencil screens for owner invite and guest home.

## Validation

- Common tests for guest invite normalization and contribution ViewModels.
- Navigation tests for guest post-login routing.
- Firestore support tests for guest invite maps.
- Firestore rules tests for guest allowed/denied access.
- Koin test for repository and ViewModels.
- Pencil snapshots for the new screens: no layout problems.
- `./gradlew ktlintFormat --console=plain --no-configuration-cache`.
- `./gradlew :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`.
- `npm test`.
- `git diff --check`.
- Compose Resources placeholder scan.
