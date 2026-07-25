# Implementation Plan

## Inputs

- Jira epic: `FLE-8`
- Child tasks: `FLE-9` through `FLE-16`
- Design artifact: `Fledge.pen`
- BaseLogin branch: `feature/improve-custom-login-integration`
- BaseLogin SHA: `35a5e15fc452157b887121dd464f8dc4f14f1ea9`

## Technical Decisions

1. Consume BaseLogin from the local sibling checkout when available using
   `includeBuild("../BaseLogin")`, substituting `com.github.apptolast:baselogin`
   with `:custom-login`.
2. Keep the same dependency coordinate with the branch SHA as fallback version
   for JitPack-style resolution if the local checkout is absent.
3. Start Koin once from Fledge and load BaseLogin `loginModules(...)`, adding
   Fledge data/presentation/platform modules in the same graph.
4. Keep the first foundation repository in memory. The data interface and models
   are stable enough for later Firebase/Room replacement, but no backend
   credentials are invented in this epic.
5. Disable BaseLogin optional providers in the initial config:
   `phoneEnabled = false`, social providers false, `magicLinkConfig = null`.
6. Use type-safe Navigation Compose Multiplatform route objects for Fledge
   routes and BaseLogin's `AuthRoutesFlow` for authentication.
7. Use Compose Resources with Spanish fallback (`values/strings.xml`) and
   English fallback (`values-en/strings.xml`).

## Work Breakdown

### 1. Build and dependency setup

- Add JitPack and GitLive Maven repositories required by BaseLogin.
- Add direct dependencies for Navigation Compose MP, Koin, serialization,
  Turbine, coroutines test, and BaseLogin.
- Apply Kotlin serialization plugin.

### 2. Tests first

- Replace template tests with SDD tests for domain models, repository behavior,
  route decisions, and ViewModel StateFlow behavior.
- Run shared tests once to confirm red because production code is not present.

### 3. Domain and data

- Create `Family`, `ChildProfile`, `PairingSession`, `ParentalGateRequest`,
  `FoundationAction`, and related identifiers/value objects.
- Add `FamilyFoundationRepository` interface and in-memory implementation.
- Ensure child profiles have no account credential fields.

### 4. Dependency injection

- Add `dataModule`, `presentationModule`, `platformModule` expect/actual, and
  `initFledgeKoin`.
- Register ViewModels with constructor injection.
- Initialize Koin from Android and iOS entry points before rendering `App()`.

### 5. Navigation shell

- Replace template `App()` with `FledgeApp`.
- Add `Routes.kt`, `FledgeNavHost`, and route decision helpers for role
  selection, auth graph, family setup, child PIN, pairing, and parental gate.
- Integrate BaseLogin `authRoutesFlow`.

### 6. Presentation

- Add foundation ViewModels for onboarding/role selector, family setup, child
  profiles, child PIN, pairing, parental gate, parent home, and child home.
- Add stateless Content composables with previews in the same file.
- Use Material 3 components, lazy lists, theme tokens, and Compose Resource
  strings.

### 7. Cleanup and validation

- Remove `Greeting`, `GreetingUtil`, `Platform` template demos, old tests, and
  the demo Compose drawable.
- Run ktlint format if configured.
- Run Gradle checks/tests that are available locally.
- Commit, push, and open PR against `develop`.

## Validation Commands

Preferred commands, adjusted if Gradle task names differ:

```bash
./gradlew :shared:allTests
./gradlew :androidApp:assembleDebug
```

If ktlint is configured, run:

```bash
./gradlew ktlintFormat
```

## Traceability

| AC | Tests |
| --- | --- |
| AC-01 | `FoundationTemplateCleanupTest` |
| AC-02 | `FoundationRouteDecisionTest`, `AuthConfigTest` |
| AC-03 | `FamilySetupViewModelTest`, `InMemoryFamilyFoundationRepositoryTest` |
| AC-04 | `ChildProfileModelTest`, `ChildProfilesViewModelTest` |
| AC-05 | `FoundationRouteDecisionTest` |
| AC-06 | `ChildPinViewModelTest`, `ParentalGateViewModelTest` |
| AC-07 | `PairingViewModelTest` |
| AC-08 | `ParentalGateViewModelTest` |
| AC-09 | `FamilySetupViewModelTest`, `RoleSelectorViewModelTest` |
| AC-10 | `ParentHomeViewModelTest` |
