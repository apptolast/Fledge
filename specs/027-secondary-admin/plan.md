# FLE-52 Secondary Admin Plan

## Scope

- Add family admin role and invite models.
- Add owner/admin metadata to the family document while preserving old documents.
- Add top-level `familyAdminInvites/{normalizedEmail}` documents to resolve invited admins.
- Resolve the active family through owner uid first, then invited email.
- Allow admin day-to-day subcollection operations in Firestore rules.
- Keep family root updates and invitation management owner-only.
- Add parent UI to invite and revoke secondary admins.
- Add Home parent setup action and navigation route.

## Validation

- Common tests for invite normalization, in-memory repository behavior and ViewModel validation.
- Firestore support tests for family admin metadata.
- Firestore rules tests for owner, invited admin and non-admin access.
- Koin test for the new ViewModel.
- Pencil snapshot for `V46dfP`: no layout problems.
- `./gradlew ktlintFormat --console=plain --no-configuration-cache`.
- `./gradlew :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`.
- `npm test`.
- `git diff --check`.
- Compose Resources placeholder scan.
