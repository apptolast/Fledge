# FLE-8 - MVP Foundation Scaffold

## Estado

Draft implementation spec for `FLE-8` (`Fase 1 - MVP Foundation`).

The normal SDD harness flow uses a human gate after `/plan` and `/design-check`.
For this run, the user explicitly asked Codex to continue through implementation
and PR creation in the same turn.

## Contexto

Fledge is a family finance app based on a parent-as-bank virtual model. The MVP
must avoid real custody, card issuing, investment, or regulated money movement.
The foundation epic sets up the application shell, authentication entry point,
family onboarding, shared-device role selection, child profile/PIN flows, and
the first parent/child modes that later epics will connect to the ledger, tasks,
approvals, savings goals, and notifications.

The implementation must follow AppToLast KMP standards:

- Clean Architecture boundaries with domain models/repositories, data
  repository implementations, Koin modules, MVVM presentation state, type-safe
  Navigation Compose Multiplatform routes, Material 3 theming, and Compose
  Resources i18n.
- One shared Compose UI for Android and iOS with platform-specific entry points.
- No custom iOS cinterop in Fledge. External iOS-native dependencies must be
  handled by package manager configuration when they are enabled.
- BaseLogin must be integrated from branch
  `feature/improve-custom-login-integration` as the authentication module
  instead of copying login screens into Fledge.
- JetBrains template code must be removed from production and tests.

## Jira Mapping

| Key | Scope |
| --- | --- |
| `FLE-8` | Fase 1 - MVP Foundation |
| `FLE-9` | Implementar registro/login de padre |
| `FLE-10` | Crear familia con divisa y zona horaria |
| `FLE-11` | Crear perfiles de hijo sin cuenta propia |
| `FLE-12` | Construir selector de rol y modo dispositivo compartido |
| `FLE-13` | Implementar PIN infantil y reset parental |
| `FLE-14` | Implementar emparejamiento de dispositivo del hijo |
| `FLE-15` | Implementar parental gate |
| `FLE-16` | Diseñar onboarding del modelo virtual |

## Design Check

`Fledge.pen` contains the first reviewable Pencil board:

- Product specs board
- Selector de rol
- Home padre
- Cola de aprobación
- Crear tarea
- Home hijo
- Detalle tarea hijo
- Objetivo de ahorro
- Retirada cash-out

The previous Pencil layout snapshot reported no layout problems. This epic uses
that board as the visual baseline for the scaffold, while keeping scope focused
on foundation screens and flows.

## Acceptance Criteria

### AC-01 - App shell and template cleanup

Given the app starts on Android or iOS
When the shared Compose entry point is rendered
Then the user sees Fledge onboarding and navigation
And no JetBrains template strings, greeting utilities, or
`compose_multiplatform` demo asset remain in production or tests.

### AC-02 - BaseLogin parent auth integration

Given a parent selects parent mode
When the app navigates to authentication
Then the app uses `custom-login` BaseLogin navigation routes
And Fledge starts Koin itself while loading BaseLogin `loginModules(...)`
And Fledge passes a conservative `LoginLibraryConfig` with social, phone, and
magic-link providers disabled until Firebase and iOS package-manager setup are
explicitly configured.

### AC-03 - Family setup model

Given a signed-in parent reaches the foundation flow
When they provide family name, currency, and timezone
Then the presentation state can create a family draft
And the family model stores currency and timezone explicitly.

### AC-04 - Child profiles are not accounts

Given a family exists
When a parent adds a child profile
Then the child is represented as a `ChildProfile`
And the model does not require email, password, or external account identity
for the child.

### AC-05 - Shared-device role selection

Given the app is used on a shared device
When the user chooses parent or child mode
Then parent mode enters auth/family management
And child mode enters a child profile/PIN flow.

### AC-06 - Child PIN and parental reset

Given a child profile exists
When a child enters a valid PIN
Then child mode opens the child home state
When PIN reset is requested
Then the flow requires parental gate state before reset.

### AC-07 - Device pairing scaffold

Given a parent manages a child profile
When they start pairing
Then the UI state exposes a short-lived pairing code and instructions
And pairing does not imply a child account.

### AC-08 - Parental gate scaffold

Given a sensitive action is requested
When the parental gate is required
Then a dedicated state captures the pending action and gate requirement
And the UI can return to the protected flow after confirmation.

### AC-09 - MVVM, Koin, navigation, theme, and i18n

Given the code is inspected
When reviewing the foundation scaffold
Then ViewModels expose `StateFlow`
And Koin modules use constructor injection
And navigation routes are `@Serializable`
And UI strings are externalized through Compose Resources
And stateless content/composable components have previews in the same file.

### AC-10 - Mobile UX baseline

Given the first foundation screens are displayed on phones
When users interact with primary actions
Then tappable elements use Material components with 48dp Android / 44pt iOS
minimum target behavior
And primary actions remain in reachable areas
And lists use lazy containers.

## Gherkin Scenarios

```gherkin
Feature: MVP foundation scaffold

  Scenario: [AC-01] Start app without template remnants
    Given the Fledge shared app entry point exists
    When the app shell is rendered
    Then the starting route is onboarding
    And template demo content is absent

  Scenario: [AC-02] Parent mode delegates auth to BaseLogin
    Given a user is on the role selector
    When parent mode is selected
    Then the next route is the BaseLogin auth graph
    And optional providers are disabled in the initial auth config

  Scenario: [AC-03] Parent creates family draft
    Given a parent has reached family setup
    When they submit name "Familia Garcia", currency "EUR", and timezone "Europe/Madrid"
    Then a family draft is available with those values

  Scenario: [AC-04] Parent creates child profile without account credentials
    Given family "Familia Garcia" exists
    When the parent adds child "Lucas" age 9
    Then the profile has a child id and display name
    And the profile has no email or password fields

  Scenario: [AC-05] Shared device selects child mode
    Given a child profile exists
    When child mode is selected from the shared-device selector
    Then the next route requests the child PIN

  Scenario: [AC-06] PIN reset requires parental gate
    Given child profile "Lucas" has a PIN
    When reset PIN is requested
    Then the pending action is "reset_child_pin"
    And parental gate is required

  Scenario: [AC-07] Pairing code belongs to child profile
    Given child profile "Lucas" exists
    When a pairing session is started
    Then a pairing code is generated for that profile
    And no child account is created

  Scenario: [AC-08] Protected flow resumes after parental gate
    Given parental gate is required for a pending action
    When the gate succeeds
    Then the pending action can resume

  Scenario: [AC-09] ViewModels expose StateFlow states
    Given the foundation presentation layer is constructed
    When the role selector and family setup ViewModels are created
    Then each exposes immutable StateFlow UI state

  Scenario: [AC-10] Foundation screen models support mobile-friendly lists
    Given the parent home state has children and pending setup actions
    When state is observed
    Then children and actions are stable lists with unique ids
```

## Out of Scope

- Real ledger entries, task approvals, savings goals, or notifications.
- Real Firebase project credentials, social providers, iOS Swift auth handlers,
  or App Store family-policy metadata.
- Persisted offline storage. This epic provides the architecture and in-memory
  repository seam; durable storage belongs in subsequent implementation tickets.
