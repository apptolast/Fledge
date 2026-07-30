# Store Metadata Families

FLE-46 prepares App Store and Google Play metadata for the first Fledge beta under families/minors
policies. This document is ready to use as console input, but it is not legal advice and should not
be used for automatic submission.

Last policy check: 2026-07-30.

## Official Sources Checked

- Apple App Review Guidelines: https://developer.apple.com/app-store/review/guidelines/
- Apple App Privacy Details: https://developer.apple.com/app-store/app-privacy-details/
- Google Play Families: https://play.google.com/console/about/programs/families/
- Google Play Target audience and content: https://support.google.com/googleplay/android-developer/answer/9867159
- Google Play User Data / Data safety / Privacy Policy: https://support.google.com/googleplay/android-developer/answer/10144311
- Google Play Data safety section: https://support.google.com/googleplay/android-developer/answer/10787469

## Release Stance

Fledge is a family education and record-keeping app for virtual allowance, tasks, savings goals and
cash-out agreements. It is not a bank, neobank, wallet, payment service, investment product or money
transmitter.

Use this positioning everywhere:

- Fledge does not move, hold, transfer, invest or store real money and provides no custody of real
  money.
- Parents remain responsible for any real-world payment outside the app.
- Child access uses avatar/PIN. The parent creates and consents to child profiles.
- The child experience has no behavioural advertising, no third-party tracking and no third-party
  analytics SDK.

## Families/Kids Positioning

### Apple

Recommended beta stance: use a family/education positioning and do not opt into Kids Category until
the final submission review confirms every Kids Category gate:

- external links, purchases and parent areas reserved behind parental gate;
- no third-party advertising;
- no third-party analytics unless it meets Apple's limited Kids Category conditions;
- privacy policy covers collection of data from minors.

If Kids Category is not selected, avoid metadata that says `for kids`, `for children` or equivalent
as a primary marketing claim. Use `for families`, `child profiles` and `family allowance` only to
describe actual functionality.

### Google Play

Declare the real target audience in Play Console. If any selected target age group includes children,
the app must comply with Google Play Families policy requirements.

Recommended beta stance:

- Target audience: families with an adult parent/guardian and child profiles.
- Ads: no ads.
- Store presence: do not use imagery or copy that implies unsupervised child-only use.
- App access instructions: provide a parent test account and child PIN path in review notes.

## Store Listing Copy

### App Store - Spanish

- Name: `Fledge`
- Subtitle: `Paga virtual en familia`
- Promotional text:
  `Organiza paga, tareas y objetivos de ahorro con saldos virtuales que el adulto supervisa.`
- Keywords:
  `paga,tareas,ahorro,familia,hijos,objetivos,dinero virtual,educacion financiera`

Description:

```text
Fledge ayuda a las familias a convertir tareas, paga y objetivos de ahorro en acuerdos claros.

Un adulto crea la familia, configura perfiles infantiles y aprueba las tareas completadas. Cada
menor puede ver su saldo virtual, seguir sus objetivos de ahorro y solicitar retiradas que el adulto
confirma fuera de la app.

Que puedes hacer:
- Crear perfiles infantiles con acceso por avatar y PIN.
- Asignar tareas puntuales o recurrentes con valor virtual.
- Revisar y aprobar tareas desde la zona de padres.
- Registrar paga recurrente y ajustes familiares.
- Crear objetivos de ahorro y mover saldo virtual hacia ellos.
- Registrar solicitudes de retirada para cerrar el acuerdo en persona.

Fledge no es una entidad financiera. No mueve, custodia, transfiere, invierte ni almacena dinero
real. Los pagos reales ocurren fuera de la app entre el adulto y el menor.

La experiencia infantil esta disenada con supervision adulta, parental gate para zonas protegidas y
sin publicidad conductual ni tracking de terceros.
```

### App Store - English

- Name: `Fledge`
- Subtitle: `Virtual allowance for families`
- Promotional text:
  `Organize allowance, chores and savings goals with virtual balances supervised by an adult.`
- Keywords:
  `allowance,chores,savings,family,children,goals,virtual money,financial education`

Description:

```text
Fledge helps families turn allowance, chores and savings goals into clear agreements.

An adult creates the family, sets up child profiles and approves completed chores. Each child profile
can see a virtual balance, follow savings goals and request cash-outs that the adult settles outside
the app.

What you can do:
- Create child profiles with avatar and PIN access.
- Assign one-off or recurring chores with virtual value.
- Review and approve chores from the parent area.
- Record recurring allowance and family adjustments.
- Create savings goals and move virtual balance into them.
- Record cash-out requests so the family can settle them in person.

Fledge is not a financial institution. It does not move, hold, transfer, invest or store real money
and provides no custody of real money. Real-world payments happen outside the app between the adult
and the child.

The child experience is designed for adult supervision, protected parent areas and no behavioural
advertising or third-party tracking.
```

### Google Play - Spanish

- App name: `Fledge`
- Short description:
  `Gestiona paga, tareas y ahorro familiar con dinero virtual.`

Full description:

```text
Fledge ayuda a las familias a organizar paga, tareas y objetivos de ahorro sin mover dinero real.

El adulto crea la familia, configura perfiles infantiles y revisa las tareas completadas. Los menores
ven saldos virtuales, objetivos y solicitudes de retirada en un entorno supervisado.

Funciones principales:
- Perfiles infantiles con avatar y PIN.
- Tareas con valor virtual y aprobacion adulta.
- Paga recurrente y ajustes de saldo.
- Objetivos de ahorro con progreso visible.
- Solicitudes de retirada que se pagan fuera de la app.
- Parental gate para zonas protegidas.

Fledge no es un banco, wallet ni servicio de pagos. No custodia, transfiere, invierte ni almacena
dinero real. Los pagos reales ocurren fuera de la aplicacion y son responsabilidad del adulto.

Sin publicidad conductual, sin tracking de terceros y sin SDKs de analitica en la experiencia
infantil.
```

### Google Play - English

- App name: `Fledge`
- Short description:
  `Manage allowance, chores and savings goals with virtual family money.`

Full description:

```text
Fledge helps families organize allowance, chores and savings goals without moving real money.

The adult creates the family, sets up child profiles and reviews completed chores. Child profiles can
see virtual balances, goals and cash-out requests in a supervised experience.

Main features:
- Child profiles with avatar and PIN access.
- Chores with virtual value and adult approval.
- Recurring allowance and balance adjustments.
- Savings goals with visible progress.
- Cash-out requests that are paid outside the app.
- Parental gate for protected areas.

Fledge is not a bank, wallet or payment service. It does not hold, transfer, invest or store real
money and provides no custody of real money. Real-world payments happen outside the app and remain
the adult's responsibility.

No behavioural advertising, no third-party tracking and no analytics SDKs in the child experience.
```

## Screenshot Captions

Use captions only over screenshots that show real in-app UI and do not expose personal data.

Spanish:

- `Crea tu familia y perfiles infantiles`
- `Asigna tareas con valor virtual`
- `Aprueba avances desde la zona de padres`
- `Sigue objetivos de ahorro`
- `Registra retiradas fuera de la app`

English:

- `Create your family and child profiles`
- `Assign chores with virtual value`
- `Approve progress from the parent area`
- `Track savings goals`
- `Record cash-outs outside the app`

## Virtual Money Disclaimer

Use this exact disclaimer in review notes and privacy/store docs:

```text
Fledge records virtual family balances only. It does not move, hold, transfer, invest or store real
money and provides no custody of real money. Any real-world payment is handled outside the app by the
adult/guardian.
```

Avoid these terms as positive product claims. They are allowed only inside negative disclaimers such
as `not a bank` or `not a payment service`.

- `bank account`
- `wallet`
- `card`
- `payment`
- `transfer`
- `earn real money`
- `investment`
- `interest`
- `cash app`
- `financial institution`

## Privacy Metadata Map

Conservative draft for App Store Privacy and Google Play Data safety. Verify against the final SDK
inventory and privacy policy before submission.

| Data | App Store category | Google Play category | Linked to user/family | Purpose | Notes |
| --- | --- | --- | --- | --- | --- |
| Parent email | Contact Info > Email Address | Personal info > Email address | Yes | Account management, app functionality | Firebase Auth. |
| Parent display name, family name | Contact Info > Name / Other User Contact Info | Personal info > Name | Yes | App functionality | User-provided. |
| Child profile name/nickname, avatar, birth year | Contact Info > Name / Other Data | Personal info > Name / Other info | Yes | App functionality, parental consent context | Parent-created child data. |
| Task titles, assignments, allowance rules | User Content / Other Data | App activity / Other user-generated content | Yes | App functionality | Family-private records. |
| Virtual ledger, savings goals, cash-out requests | Financial Info > Other Financial Info / Other Data | Financial info > Other financial info | Yes | App functionality | Virtual balances only; no bank/card/payment data. |
| Firebase UID/session identifiers | Identifiers > User ID | Device or other IDs / User IDs | Yes | Authentication, security, app functionality | Not used for tracking. |
| Push token and routing metadata | Identifiers > Device ID / Other Data | Device or other IDs | Yes | Notifications | Payloads must stay privacy-first per FLE-42. |
| Diagnostics/crash data | Diagnostics | App info and performance | Only if enabled | App quality | Set to `No` until Crashlytics or equivalent is actually integrated. |
| Location, contacts, photos, health, microphone, camera | N/A | N/A | No | N/A | Do not request unless a future feature changes this. |

## App Store App Privacy Draft

- Tracking: `No`.
- Data linked to user: `Yes`.
- Data not linked to user: `No` unless diagnostics are later enabled without account linkage.
- Data used for third-party advertising: `No`.
- Data used for developer advertising or marketing: `No`.
- Data shared with data brokers: `No`.
- Purposes:
  - App Functionality.
  - Account Management.
  - Security, if App Store Connect asks for auth/session identifiers.

Declare collection of user/family data because it is transmitted to backend storage and is part of
primary app functionality.

## Google Play Data Safety Draft

- Does the app collect user data? `Yes`.
- Does the app share user data? `No`, if Firebase/Google Cloud is treated only as a service provider
  processing data for app functionality. Confirm final Play Console definitions before submission.
- Is all user data encrypted in transit? `Yes`.
- Can users request data deletion? `Yes`, through the in-app `Cuenta y datos` flow and the public
  deletion URL `https://fledge-c685d.web.app/account-deletion/`.
- Data collection required or optional:
  - Parent email and auth identifiers: required for account.
  - Child profile/family records: required for core functionality after onboarding.
  - Task/ledger/goal/cash-out records: user-generated, required for the selected feature.
  - Push token: optional if notifications permission is denied.
- Ads: `No`.
- Independent security review: `No`, unless completed later.

## Ads, Tracking And SDKs

Current metadata stance:

- No behavioural advertising.
- No third-party tracking.
- No third-party analytics SDK in the child experience.
- No sale of personal or sensitive data.
- No public social feed, anonymous chat, random chat or child-to-child messaging.

Before submission, audit the final dependency graph and Play SDK Index/App Store privacy manifests.
If a future SDK collects data for analytics, ads, attribution or diagnostics, update this document and
the store forms before release.

## Review Notes

### Apple Review Notes

```text
Fledge is a family allowance record-keeping app. It uses virtual balances only and does not move,
hold, transfer, invest or store real money. It provides no custody of real money.

The parent/guardian creates the account and child profiles. Child access uses avatar/PIN. Parent
areas and protected actions are guarded by parental gate. There are no third-party ads, no
behavioural advertising and no third-party tracking in the child experience.

Test path:
1. Sign in with the supplied parent test account.
2. Create or open a family profile.
3. Open a child profile with the supplied avatar/PIN.
4. Submit a task or inspect a virtual savings goal.
5. Return to the parent area to review/approve and confirm any cash-out outside the app.

Test credentials:
- Parent email: [provide in App Store Connect only]
- Parent password: [provide in App Store Connect only]
- Child profile/PIN: [provide in App Store Connect only]
```

### Google Play Review Notes

```text
Fledge is designed for supervised family use. The adult account manages child profiles, tasks,
virtual balances, savings goals and cash-out records. The app does not provide banking, wallet,
payment, investment or money-transfer functionality.

There are no ads, no behavioural advertising, no public social features and no third-party tracking
in the child experience. Push notifications use privacy-first routing and must not include task
details, names, balances or cash-out amounts in the payload.

Use the supplied parent test account and child PIN to verify the parent/child flow.
```

## Pre-Submit Checklist

Do not submit to review until these are complete:

- [ ] Public privacy policy URL exists, is not a PDF, is not geofenced and names Fledge/AppToLast.
- [ ] Privacy policy is linked inside the app and in both store consoles.
- [x] FLE-95 implements in-app and web account/data deletion.
- [ ] FLE-83 hardens Firestore rules by family membership and role before any external beta.
- [ ] FLE-86 validates Firebase/Functions/Firestore end-to-end in the target environment.
- [ ] Store forms match actual SDK inventory and privacy policy.
- [ ] Review notes include working parent test credentials and child PIN, stored only in the consoles.
- [ ] No child/family personal data appears in screenshots.
- [ ] No metadata uses bank/neobank/wallet/payment/investment language as a positive product claim.
- [ ] No submit-to-review automation is enabled for this task.

## Ticket Links

- FLE-42: Push FCM/APNs privacy-first.
- FLE-45: Closed beta readiness gates.
- FLE-46: This metadata task.
- FLE-83: Firestore rules by membership and role.
- FLE-86: Deploy Firestore/Functions and validate end-to-end.
- FLE-95: Account and data deletion for store compliance.
