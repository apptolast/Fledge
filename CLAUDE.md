# CLAUDE.md

Guía para Claude Code al trabajar en este repositorio. Complementa —no sustituye— los estándares
globales de AppToLast en `~/.claude/CLAUDE.md`.

## Qué es Fledge

App de finanzas familiares con modelo **parent-as-bank**: el padre hace de banco virtual del hijo.
No hay custodia real de dinero, ni emisión de tarjetas, ni inversión, ni movimiento de dinero
regulado. Todo el saldo es **dinero virtual** dentro de la familia.

Bucle esencial del producto: *el padre define trabajo → el hijo lo realiza → el padre valida → el
saldo sube → el hijo ahorra o cobra*. Si una feature no sostiene ese bucle, está fuera del MVP.

- Jira: proyecto `FLE` en https://apptolast.atlassian.net
- Confluence: páginas `Fledge - *` en el espacio `SD` (MVP Scope, Delivery Plan, Compliance and Risk)
- Firebase: proyecto `fledge-c685d`

## Reglas de negocio invariantes

Estas reglas son de producto y no se negocian sin actualizar el spec:

- El ledger es **append-only**. Corregir un error es añadir una transacción `Reversal`, nunca editar
  ni borrar la original.
- El **saldo nunca se almacena** como número editable: se deriva sumando el ledger por cuenta.
- Todos los importes son **enteros en céntimos** (`MoneyCents`). Nunca `Float`/`Double`.
- Cuentas virtuales por hijo: `Main` y `Goal`. El saldo retirable es el de `Main`.
- El saldo de un hijo no puede ser negativo en MVP.
- Una `TaskInstance` solo genera dinero **al aprobarse**. El hijo nunca aprueba sus propias tareas.
- El cash-out solo descuenta saldo **cuando el hijo confirma** que ha recibido el dinero.
- La divisa es de familia y no editable tras el alta; la zona horaria familiar gobierna vencimientos
  y pagas.
- Archivar un hijo conserva su ledger.
- Tono: la app no juzga ni presiona al padre, y en modo hijo el lenguaje es infantil y no culpabiliza.

## Estructura del proyecto

```
Fledge/
├── shared/src/
│   ├── commonMain/kotlin/com/apptolast/fledge/
│   │   ├── data/           # auth, remote/firebase, repository (implementaciones)
│   │   ├── domain/         # model, repository (interfaces), service, security
│   │   ├── presentation/   # foundation/<feature>/{Screen,ViewModel}, theme
│   │   ├── navigation/     # Routes.kt (@Serializable) + FledgeNavHost.kt
│   │   ├── di/AppModules.kt
│   │   └── App.kt
│   ├── commonTest/         # toda la suite: kotlin.test + coroutines-test + Turbine
│   ├── androidMain/        # PlatformModule.android.kt, AndroidSocialAuthClient, AuthConfig.android
│   ├── iosMain/            # PlatformModule.ios.kt, IosSocialAuthClient, MainViewController
│   └── commonMain/composeResources/values{,-en}/strings.xml
├── androidApp/             # MainActivity + entry point Android
├── iosApp/                 # entry point iOS (Xcode, SwiftUI mínimo)
├── functions/              # Cloud Functions Gen2 (Node ESM): job de paga recurrente
├── firestore.rules, firestore.indexes.json, firebase.json
├── specs/NNN-slug/         # specs y planes del harness SDD
└── Fledge.pen              # diseño Pencil (especificación visual)
```

**Importante**: no hay módulo `composeApp`. La UI compartida vive en `:shared`, y `androidApp`/`iosApp`
son solo entry points.

## Guardarraíles KMP

- **Source sets**: el código va a `commonMain` por defecto. Solo baja a `androidMain`/`iosMain` cuando
  necesita API de plataforma, siempre vía `expect/actual`. **Prohibido cinterop propio en iOS**: las
  dependencias nativas iOS se gestionan por package manager (SPM/CocoaPods).
- **Clean Architecture**: las interfaces de repositorio viven en `domain/repository/`, las
  implementaciones en `data/repository/`. El dominio no conoce Ktor, Firebase ni Compose.
- **MVVM**: `MutableStateFlow` privado + `StateFlow` público en el ViewModel. La UI se divide en
  `<Feature>Screen` (stateful, `koinViewModel()`) y `<Feature>Content` (stateless, recibe estado y
  lambdas). State hoisting siempre.
- **DI**: Koin con inyección por constructor. `viewModelOf(::X)` en `presentationModule`,
  repositorios en `dataModule`, `expect val platformModule` por plataforma.
- **Coroutines**: nada de `runBlocking` ni `GlobalScope` en producción. `viewModelScope` en los VM.
- **Navegación**: rutas `@Serializable` en `navigation/Routes.kt`, `composable<Route>` en el NavHost.
- **i18n**: cero strings hardcodeados en composables. `values/strings.xml` es **español** (idioma por
  defecto del proyecto) y `values-en/strings.xml` es inglés. Acceso con `stringResource(Res.string.x)`.
- **Previews**: toda composable `Content` stateless y todo componente reutilizable lleva su
  `@Preview` con datos realistas, en el mismo fichero, envuelto en `FledgeTheme`.
- **Theming**: siempre `MaterialTheme.colorScheme` y `MaterialTheme.typography`. Espaciados múltiplos
  de 4dp.
- **Comentarios en inglés**, strings de UI en español.

## Testing

Toda la suite vive en `shared/src/commonTest`. Stack: `kotlin.test` + `kotlinx-coroutines-test`
(`runTest`) + `Turbine` para Flows + fakes escritos a mano (nada de librerías de mocking).

Convención de nombres: función con backticks describiendo el escenario, prefijada con el ticket
cuando aplica, y cuerpo con bloques `// Given` / `// When` / `// Then`:

```kotlin
@Test
fun `FLE-22 parent saves monthly allowance rule with day 31`() = runTest { ... }
```

## Comandos

```bash
# Formatear (SIEMPRE tras tocar código Kotlin)
./gradlew ktlintFormat

# Comprobar estilo (lo ejecuta el gate /validate del harness SDD)
./gradlew ktlintCheck

# Validación completa Android + iOS
./gradlew :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 \
  --console=plain --no-configuration-cache

# Build de Xcode (simulador)
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build

# Cloud Functions
npm --prefix functions run check
```

La configuración de ktlint (estilo, exclusiones, reglas por tipo de fichero) vive en `.editorconfig`,
no en bloques `filter {}` de Gradle. El bloque `ktlint {}` de cada módulo solo lleva opciones del
plugin.

## Configuración local

`local.properties` necesita:

```properties
FIREBASE_API_KEY=...
FIREBASE_PROJECT_ID=fledge-c685d
FIRESTORE_DATABASE_ID=debug        # "(default)" en release
GOOGLE_WEB_CLIENT_ID=...           # OAuth Web client ID, no el de Android
APP_ENV=debug

# Firebase SDK nativo (FLE-78): FirebaseOptions explícitas, sin google-services.json
FIREBASE_APP_ID_ANDROID=1:<sender>:android:<hash>   # App ID del app Android en fledge-c685d
FIREBASE_APP_ID_IOS=1:<sender>:ios:<hash>           # App ID del app iOS en fledge-c685d
FIREBASE_GCM_SENDER_ID=<project number>             # Project number (Cloud Messaging sender id)
FIREBASE_STORAGE_BUCKET=fledge-c685d.firebasestorage.app
```

Se exponen al código vía BuildKonfig (`com.apptolast.fledge.shared.BuildKonfig`). **Ningún secreto se
commitea.**

Los cuatro campos de Firebase SDK son **valores públicos de cliente** (Firebase Console → Configuración
del proyecto → Tus apps) y **todos tienen default `""`**: el build funciona en una máquina sin
`local.properties`. Si faltan, el bootstrap de Firebase queda en `NotConfigured` y Firestore no está
disponible — la app no crashea, pero tampoco habla con Firestore. El CI los inyecta por GitHub Secrets
escribiendo `local.properties`.

## Autenticación

Fledge **no implementa autenticación**: la delega entera en **BaseLogin**
(`com.github.apptolast.BaseLogin:baselogin`, pineado por commit). El módulo se cablea llamando a
`loginDataModule()` en `fledgeModules`, que registra el `FirebaseAuthProvider` de la librería sobre
`dev.gitlive:firebase-auth` y su `FirebaseAuthGateway`.

**No escribas un `AuthProvider` propio.** Fledge tuvo uno sobre la REST de Identity Toolkit hasta
FLE-88, y no fue una decisión de diseño: era un apaño de cuando la integración SPM de iOS se había
revertido y no había Firebase nativo. Se eliminó junto con Ktor y `multiplatform-settings`.

Cableado por plataforma, ambos obligatorios:

- **Android**: `MainActivity` llama a `FledgeAndroidAuth.attach(this)` en `onCreate` y `detach` en
  `onDestroy`. Envuelve `CustomLoginAndroid` para que `androidApp` no dependa de BaseLogin. Sin ese
  `attach`, Google Sign-In revienta con un `lateinit` sin inicializar: es Credential Manager quien
  necesita el contexto de aplicación.
- **iOS**: `SocialAuthCoordinator.swift` asigna `AppleSignInProviderIOS.shared.signInHandler` y
  devuelve la cadena empaquetada `idToken|||rawNonce|||<nonce>|||displayName|||<name>`. Los
  separadores son literales y compartidos con la librería; el segmento del nombre solo se añade
  cuando Apple lo envía, que es **solo en la primerísima autorización** de cada usuario.

Si hace falta tocar el comportamiento de auth, el sitio es BaseLogin, no Fledge: allí está testeado
y lo aprovecha toda la flota.

## Flujo de trabajo: harness SDD

Este proyecto usa el plugin **sdd-flow**. Las 7 fases, con 4 gates humanos:

`/spec` → `/plan` → `/design-check` → `/test` → `/implement` → `/validate` → `/promote`

Reglas del harness que hay que respetar:

- Los specs viven en `specs/NNN-slug/{spec.md,plan.md}` y los criterios de aceptación se escriben en
  **Gherkin** (`Scenario [AC-01] … Given/When/Then`).
- **TDD estricto**: en `/test` los tests se escriben y se confirma que están en **ROJO** por falta de
  implementación (no por errores de compilación). En `/implement` un hook **bloquea** cualquier
  edición de ficheros de test.
- `/validate` exige trazabilidad `AC-xx → test(s)` y bloquea si hay implementación sin test rojo previo.
- El estado de fase se guarda en `.claude/.sdd-state.json`.

## Git y Jira

GitFlow con `develop` como rama por defecto. Ramas `{tipo}/{FLE-XX}-{kebab-case}`, commits
convencionales en minúscula (`feat(fle-22): ...`), PRs contra `develop` con título `FLE-XX Descripción`
y squash merge. El ticket de Jira se mueve a *In Progress* al empezar, *In Review* al abrir PR y
*Done* tras el merge.
