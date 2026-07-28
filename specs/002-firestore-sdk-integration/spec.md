# Spec 002: Integrar GitLive firebase-kotlin-sdk en el proyecto KMP (FLE-78)

> Rama: `feature/002-firestore-sdk-integration` · Proyecto: `Fledge` · Estado: draft
> El spec es el mecanismo anti-deriva: debe ser autosuficiente (releíble al inicio de cada fase).

---

## Contexto y objetivo

Hoy **toda la app es en memoria**: `dataModule` cablea `InMemoryFamilyFoundationRepository`,
`InMemoryLedgerRepository` e `InMemoryMoneyFlowRepository`. Al cerrar la app se pierde el ledger, las
familias y los perfiles de hijo. Mientras tanto ya existe infraestructura de servidor que espera datos
reales en Firestore: `functions/index.js` (job `runAllowanceRules`) consulta el collection-group
`allowanceRules` y escribe en `families/{familyId}/ledgerTransactions`, y `firestore.rules` /
`firestore.indexes.json` ya describen ese esquema. Sin cliente Firestore en la app, ese backend no
tiene con quién hablar.

**FLE-78** es el primer corte del epic **FLE-77** (*Fase 1 — Persistencia y sincronización Firestore*),
que se aborda **antes de FLE-26** precisamente por eso. Su objetivo es acotado y de infraestructura:

> Dejar el SDK **GitLive firebase-kotlin-sdk** integrado, compilando en Android e iOS, inicializado en
> ambos entry points, y con una instancia de Firestore **inyectable por Koin** — sin migrar todavía
> ningún repositorio.

La migración de repositorios es FLE-80/81/82 y el endurecimiento de reglas es FLE-83.

**Por qué SDK y no REST**: el proyecto ya tiene un cliente Firebase-por-REST sobre Ktor para *Auth*
(`data/remote/firebase/`, receta `firebase-rest-client`). Para Firestore, el epic FLE-77 pide el SDK
nativo porque necesita listeners en tiempo real, caché offline y transacciones — cosas que la REST no
da. Esa decisión ya está tomada aguas arriba; este spec **no** la reabre, pero sí documenta sus
consecuencias (ver *Decisiones abiertas*, D-0).

---

## Alcance

### Dentro

- Añadir al version catalog (`gradle/libs.versions.toml`) y a `:shared` los artefactos GitLive:
  `dev.gitlive:firebase-app`, `dev.gitlive:firebase-common` y `dev.gitlive:firebase-firestore`.
- Derivar la configuración efectiva de Firebase (`FirebaseOptions`) desde BuildKonfig/`local.properties`,
  reutilizando `FIREBASE_API_KEY`, `FIREBASE_PROJECT_ID` y `FIRESTORE_DATABASE_ID` que **ya existen**.
- Inicializar `FirebaseApp` de forma **idempotente** en Android (`MainActivity`/`Application`) e iOS
  (`MainViewController`), a través de un **port en `commonMain`** con `actual` por plataforma.
- Resolver la dependencia nativa iOS **por package manager (SPM) en `iosApp.xcodeproj`**, sin cinterop
  propio (prohibido por el spec 001 y por los Guardarraíles KMP del `CLAUDE.md`).
- Exponer `FirestoreProvider` (instancia de Firestore + `databaseId` efectivo) como binding de Koin,
  resoluble desde el grafo actual.
- Tests en `shared/src/commonTest` que cubran la lógica de configuración, bootstrap e inyección **sin
  tocar Firebase real** (fakes escritos a mano).

### Fuera

- **Migrar repositorios**: `FamilyFoundationRepository`, `LedgerRepository` y `MoneyFlowRepository`
  siguen siendo `InMemory*` al terminar esta feature (FLE-80 / FLE-81 / FLE-82).
- **Tocar `firestore.rules` o `firestore.indexes.json`** (FLE-83).
- Añadir `dev.gitlive:firebase-auth` o migrar `FledgeFirebaseAuthProvider` (sigue en REST). Resuelto en
  **FLE-88**, que bloquea FLE-80/81/82 pero no a esta feature — ver D-0.
- Cualquier cambio de UI, navegación, i18n, tema, ViewModels o pantallas.
- Persistencia offline / caché de Firestore, listeners en tiempo real, paginación, sincronización o
  resolución de conflictos.
- Cloud Functions, despliegue de índices, emuladores de Firebase.
- Módulos GitLive no pedidos (storage, messaging, analytics, crashlytics, config, functions).
- Target web/wasmJs (no existe en Fledge).

---

## Conocimiento reutilizable

### De **engram** (proyecto `fledge`)

| Ref | Qué aporta |
|-----|-----------|
| **#73** *Session summary FLE-8* | «Xcode SPM Firebase integration works for the iOS app target with Firebase iOS SDK **12.7.0**; standalone Gradle native iOS tests fail to link `FirebaseCore` because they do not consume Xcode SPM frameworks.» Confirma la vía SPM y su límite. |
| **#71**, **#76** *[auto] Fallo build/test* | Evidencia literal del fallo: `ld: framework 'FirebaseCore' not found` en `:shared:linkDebugTestIosSimulatorArm64`. **Se reutiliza tal cual** como restricción de diseño (ver D-4). |
| **#86** | «El proyecto evita la inicialización de GitLive Firebase hasta que la config REST/Firebase esté explícitamente confirmada.» Esta feature es exactamente ese momento; el spec **debe** dejar la config explícita. |
| **#91**, **#95** *FLE-12..FLE-16* | Precedente de que la foundation se completó **sobre repositorios en memoria** a propósito. Refuerza el "Fuera de alcance". |
| **#77** *BaseLogin JitPack coordinate* | Recordatorio de que una coordenada mal escrita resuelve un POM agregado vacío. Aplica al declarar los artefactos GitLive: verificar la resolución real, no asumirla. |

### De **kmp-recipes**

| Skill | Se reutiliza **tal cual** | Se **adapta** |
|-------|---------------------------|---------------|
| `integrations/firebase-rest-client` | `FirebaseConfig` (`apiKey`/`projectId`/`databaseId` desde BuildKonfig) sigue siendo la fuente única de config; el gotcha *«fija `databaseId` antes de `initKoin`/primera llamada»* se traslada literal al bootstrap del SDK. | La receta es explícitamente *«sin el SDK GitLive»*. En Fledge convivirán **REST para Auth** y **SDK para Firestore**; `FirestoreClient`/`FirestoreCodec`/`FirebaseStorageClient` de la receta **no** se usan. |
| `architecture/koin-multimodule-di` | `expect val platformModule: Module` en forma `val` (ya en uso), `single { }` explícito con `bind Interface::class`, constructor injection, `androidContext()` para bindings Android. | La receta dice *«`:shared` no arranca Koin»* — en Fledge **sí** lo hace (`initFledgeKoin` en `di/AppModules.kt`) porque no hay módulo `composeApp`. Se respeta el patrón local, no el de la receta. |
| `testing/kotlin-test-turbine-fakes` | Stack `kotlin.test` + `runTest` + fakes a mano; nombres con backticks; bloques `// Given / // When / // Then`. **Sección "Ports de plataforma"**: *«si la feature usa un SDK solo-plataforma (Firebase), define un port/interfaz en `commonMain` y testea con un fake en `commonTest`»* — es literalmente el diseño de esta feature. | Ubicación: la receta prefiere `consumerApp/commonTest`; en Fledge la suite vive en `shared/src/commonTest` y corre como **Android host test** (`:shared:testAndroid`). |
| `build-release/buildkonfig-secrets` | Bloque `buildkonfig {}` de `:shared` con `localProperties.getProperty("KEY", "")` **siempre con default**; `FIRESTORE_DATABASE_ID` derivado de `APP_ENV` (ya implementado en `shared/build.gradle.kts`); inyección en CI vía `local.properties`. | Hay que **añadir campos nuevos** (app ids y sender id) si se opta por `FirebaseOptions` explícitas — ver D-2. Gotcha aplicable: *«las keys de BuildKonfig acaban en el binario»* → solo valores públicos de cliente. |
| `build-release/ktlint-exclude-generated-sources` | El gate usa `ktlintCheck`, nunca `ktlintFormat`; los excludes van en `.editorconfig`. | Solo relevante si el SDK introdujera fuentes generadas (no se espera). Se cita para no re-descubrirlo. |

### Del repo (estado actual verificado)

- `settings.gradle.kts` **ya incluye** `maven("https://gitlive.github.io/firebase-kotlin-sdk/maven/")`
  y `mavenCentral()`. No hay que tocar repositorios.
- `shared/build.gradle.kts` **ya expone** `FIRESTORE_DATABASE_ID` por BuildKonfig (`debug` / `(default)`).
- `shared/src/commonTest/.../di/AppModulesTest.kt` ya tiene el patrón exacto para testear el grafo Koin
  sin plataforma: `koinApplication { modules(fledgeModules(module { /* fakes */ })) }`. **Se reutiliza
  tal cual** para el AC de inyección.
- No hay `google-services.json` ni `GoogleService-Info.plist` en el repo (ambos gitignored, líneas 35-36
  de `.gitignore`).
- `iosApp.xcodeproj` tiene hoy `packageReferences = ( )` **vacío** y no existe `Package.resolved`: la
  integración SPM de FLE-8 fue revertida. Hay que rehacerla desde cero.
- No existe clase `Application` en `:androidApp`; Koin arranca en `MainActivity.onCreate()`.

---

## Investigación técnica verificada

Datos comprobados contra fuente (no asumir, no re-investigar en `/plan` salvo que algo falle):

| Dato | Valor verificado | Fuente |
|------|------------------|--------|
| Última versión GitLive | **2.5.0** (16-07-2026) | GitHub releases atom + README + klibs.io |
| Kotlin con el que se compiló 2.5.0 | **2.2.20 / 2.2.21** | `gradle/libs.versions.toml` del tag `v2.5.0` + klibs.io |
| Firebase Android BOM en 2.5.0 | **33.15.0** (`api(google.firebase.firestore)` en `androidMain`) | `firebase-firestore/build.gradle.kts` + `libs.versions.toml@v2.5.0` |
| Firebase iOS SDK contra el que hace cinterop 2.5.0 | **11.8.0** (pods `FirebaseFirestore` + `FirebaseFirestoreInternal`, `-fmodules`) | idem |
| Targets publicados | `iosArm64`, `iosSimulatorArm64`, `iosX64`, `androidJvm`, `jvm`, `js`, `macos*`, `tvos*` | klibs.io |
| Dónde vive `Firebase.initialize` | **`dev.gitlive:firebase-app`** (`firebase-app/src/commonMain/.../firebase.kt`), *no* en `firebase-common` | fuente del repo |
| Grafo de dependencias de `firebase-firestore` | `api(project(":firebase-app"))`, `api(project(":firebase-common"))`, `implementation(":firebase-common-internal")` | `firebase-firestore/build.gradle.kts` |
| API de init | `Firebase.initialize(context: Any?): FirebaseApp?` · `initialize(context, options: FirebaseOptions): FirebaseApp` · `Firebase.app` · `Firebase.apps(context)` | fuente |
| `FirebaseOptions` | `applicationId`, `apiKey`, `databaseUrl?`, `gaTrackingId?`, `storageBucket?`, `projectId?`, `gcmSenderId?`, `authDomain?` | fuente |
| Android sin `google-services.json` | **Posible**: `initialize(ctx, options)` → `FirebaseApp.initializeApp(ctx, options.toAndroid())`. El plugin `com.google.gms.google-services` **no es obligatorio** por esta vía | `firebase-app/src/androidMain/.../firebase.kt` |
| iOS sin `GoogleService-Info.plist` | **Posible**: `initialize(ctx, options)` → `FIRApp.configureWithOptions(FIROptions(applicationId, gcmSenderId).apply { APIKey…; projectID…; storageBucket… })`. `applicationId` y `gcmSenderId` son **obligatorios** | `firebase-app/src/appleMain/.../firebase.kt` |
| Base de datos nombrada | `Firebase.firestore(app: FirebaseApp, databaseId: String? = null)` → soporta la base `debug` | `firebase-firestore/src/commonMain/.../firestore.kt` |
| Linkado nativo iOS | *«On iOS the official Firebase iOS SDK is **not** linked as a transitive dependency […] add the required frameworks to the **search path of your test targets**»* | README GitLive |
| Última versión firebase-ios-sdk | **12.17.0** (23-07-2026); FLE-8 probó **12.7.0** | GitHub releases atom |

⚠️ **No verificable sin ejecutar el build** (marcado como riesgo, no como hecho):
compatibilidad klib de artefactos compilados con Kotlin 2.2.2x consumidos por un compilador **2.4.10**,
y compatibilidad del cinterop de GitLive (headers Firebase iOS 11.8.0) contra frameworks SPM 12.x.

---

## Diseño de testabilidad (obligatorio)

FLE-78 es una tarea de integración de SDK. Para que sea testeable en `commonTest` **sin Firebase real**,
la lógica se separa del SDK con **ports en `commonMain`**:

```
commonMain
  data/remote/firebase/
    FirebaseEnvironment.kt      # data class pura: projectId, applicationId, apiKey,
                                # gcmSenderId, storageBucket?, databaseId + isComplete()
    FirebaseInitializer.kt      # PORT: fun isInitialized(): Boolean ; fun initialize(env)
    FirebaseBootstrap.kt        # lógica pura: valida env, idempotencia, expone estado
                                # FirebaseBootstrapState = NotConfigured | AlreadyInitialized
                                #                        | Initialized | Failed(reason)
    FirestoreProvider.kt        # PORT: val databaseId: String ; fun firestore(): FirebaseFirestore
    GitLiveFirestoreProvider.kt # adaptador fino: Firebase.firestore(Firebase.app, databaseId)
androidMain / iosMain
    FirebaseInitializer.<plat>.kt  # actual: Firebase.initialize(context, options)
    expect/actual val firebaseApplicationId  # el app id es distinto por plataforma
```

Regla: **nada de `commonTest` importa tipos de `dev.gitlive`.** Los tests atacan
`FirebaseEnvironment`, `FirebaseBootstrap` y el grafo Koin usando un `FakeFirebaseInitializer` y un
`FakeFirestoreProvider` escritos a mano, inyectados por el hueco de `platformModule` que ya usa
`AppModulesTest`. El adaptador GitLive queda cubierto por los ACs verificables por build.

---

## Criterios de aceptación (Gherkin)

```gherkin
Feature: Integración del SDK GitLive Firestore en Fledge

  # ---------- Verificables por build ----------

  Scenario [AC-01]: El SDK GitLive compila en Android e iOS sin cinterop propio
    Given el version catalog no declara ningún artefacto "dev.gitlive"
    When se añaden "firebase-app", "firebase-common" y "firebase-firestore" al catálogo
     And se declaran en commonMain de ":shared"
     And se ejecuta "./gradlew :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache"
    Then el build termina en éxito
     And "shared/build.gradle.kts" no contiene ningún bloque "cocoapods {" ni "cinterops"
     And "./gradlew ktlintCheck" pasa

  Scenario [AC-02]: iOS resuelve el SDK nativo por package manager
    Given "iosApp.xcodeproj" declara el paquete SPM "https://github.com/firebase/firebase-ios-sdk"
      con los productos "FirebaseCore" y "FirebaseFirestore" pinneados a una versión concreta
     And existe "iosApp/iosApp.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved" versionado
    When se ejecuta "xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build"
    Then la app iOS compila y enlaza sin "ld: framework 'FirebaseCore' not found"
     And el repositorio no contiene Podfile ni carpeta Pods

  Scenario [AC-03]: La configuración de Firebase no introduce ficheros de config en el repo
    Given la configuración pública de Firebase vive en "local.properties" y se expone por BuildKonfig
    When se inspecciona el árbol del repositorio y los ficheros de build
    Then no hay "google-services.json" ni "GoogleService-Info.plist" versionados
     And ningún build.gradle.kts aplica el plugin "com.google.gms.google-services"
     And todos los campos nuevos de BuildKonfig tienen valor por defecto, de modo que el build
         funciona en una máquina sin secretos

  # ---------- Verificables en commonTest (fakes, sin Firebase real) ----------

  Scenario [AC-04]: El entorno efectivo de Firebase se deriva de BuildKonfig
    Given los valores de configuración projectId "fledge-c685d", una apiKey no vacía,
      un applicationId de plataforma y un gcmSenderId
     And FIRESTORE_DATABASE_ID vale "debug"
    When se construye el FirebaseEnvironment efectivo
    Then projectId, apiKey, applicationId y gcmSenderId coinciden con los de configuración
     And databaseId es "debug"
     And isComplete() es true
    When FIRESTORE_DATABASE_ID vale "(default)"
    Then databaseId es "(default)" y se usa literal, sin transformaciones

  Scenario [AC-05]: Una configuración incompleta no rompe el arranque
    Given un FirebaseEnvironment con apiKey vacía (máquina de desarrollo sin local.properties)
     And un FirebaseInitializer falso que registra sus llamadas
    When se ejecuta el bootstrap de Firebase
    Then el estado resultante es NotConfigured
     And el initializer recibió 0 llamadas a initialize
     And no se lanzó ninguna excepción

  Scenario [AC-06]: La inicialización de Firebase es idempotente
    Given un FirebaseEnvironment completo
     And un FirebaseInitializer falso que reporta isInitialized = false
    When se ejecuta el bootstrap dos veces
    Then initialize se invocó exactamente 1 vez
     And el estado final es Initialized
    Given un FirebaseInitializer falso que reporta isInitialized = true
    When se ejecuta el bootstrap
    Then initialize se invocó 0 veces
     And el estado es AlreadyInitialized

  Scenario [AC-07]: Firestore es inyectable por Koin sin tocar Firebase real
    Given el grafo "fledgeModules(platformModule)" con un platformModule de test que aporta
      un FirebaseInitializer falso y un FirestoreProvider falso
    When se construye el KoinApplication y se resuelven FirebaseBootstrap y FirestoreProvider
    Then ambos se resuelven a instancias no nulas
     And FirestoreProvider.databaseId coincide con el databaseId del FirebaseEnvironment resuelto
     And el initializer falso no registró ninguna llamada por el mero hecho de construir el grafo

  Scenario [AC-08]: Ningún repositorio se migra en esta feature
    Given el grafo Koin de producción "dataModule"
    When se resuelven FamilyFoundationRepository, LedgerRepository y MoneyFlowRepository
    Then cada uno sigue siendo su implementación InMemory correspondiente
     And ningún repositorio depende de FirestoreProvider

  # ---------- Verificable por build + smoke manual ----------

  Scenario [AC-09]: Ambos entry points inicializan Firebase antes de usar Firestore
    Given la app instalada en Android y en el simulador iOS
    When se arranca desde MainActivity (Android) y desde MainViewController (iOS)
    Then el bootstrap de Firebase se ejecuta antes de la primera resolución de FirestoreProvider
     And el log de arranque reporta el estado del bootstrap exactamente una vez por proceso
     And la app no crashea por "FirebaseApp name [DEFAULT] already exists"
         ni por "Default app has already been configured"
```

---

## Decisiones resueltas (🚦 Gate 1 — 2026-07-28)

| # | Decisión | Contexto verificado | **Resolución** |
|---|----------|---------------------|----------------|
| **D-0** ⚠️ **crítica** | **Firestore SDK vs. auth por REST**: `firestore.rules` exige `request.auth != null`. El SDK nativo de Firestore toma la credencial del **SDK nativo de Firebase Auth**, que en Fledge **no existe** (el login va por REST y el idToken lo guarda `TokenManager`). No hay API pública para inyectar un idToken en el SDK nativo. Con las reglas actuales, **toda lectura/escritura del SDK sería denegada**. | `firestore.rules` + `data/auth/FledgeFirebaseAuthProvider` + `data/remote/firebase/` | ✅ **Migrar la autenticación a `dev.gitlive:firebase-auth`**, en el ticket nuevo **FLE-88**, manteniendo BaseLogin como capa de UI de login. FLE-88 **bloquea FLE-80/81/82** y **no bloquea FLE-78** (aquí no se lee ni escribe ningún documento). Fuera del alcance de este spec. |
| **D-1** ⚠️ | **Versión GitLive**: 2.5.0 está compilada con **Kotlin 2.2.21**; Fledge usa **2.4.10**. La compatibilidad klib no se puede verificar sin build. | tag `v2.5.0` | ✅ Empezar por **2.5.0**. Si el compilador rechaza los klibs, fallback documentado: probar 2.4.0 y, si tampoco, escalar a decisión de producto (mantener REST hasta que GitLive publique con Kotlin 2.4.x). |
| **D-2** ⚠️ | **Config sin ficheros de Google**: usar `FirebaseOptions` explícitas exige **campos nuevos** en `local.properties` + BuildKonfig + CI: `FIREBASE_APP_ID_ANDROID`, `FIREBASE_APP_ID_IOS`, `FIREBASE_GCM_SENDER_ID` y opcionalmente `FIREBASE_STORAGE_BUCKET`. La alternativa es adoptar `google-services.json` + plugin `com.google.gms.google-services` + `GoogleService-Info.plist`. | `CLAUDE.md` §Configuración local; `.gitignore:35-36` | ✅ **Mantener el enfoque BuildKonfig** (AC-03) y añadir los campos nuevos, todos con default vacío. Hay que **documentar el nuevo contrato de `local.properties` en `CLAUDE.md`** (tarea T2) y avisar de que afecta a todas las máquinas de dev y al CI. |
| **D-3** ⚠️ | **Versión SPM del Firebase iOS SDK**: GitLive 2.5.0 hace cinterop contra **11.8.0**; el último es **12.17.0**; FLE-8 probó **12.7.0**. Mezclar majors puede dar símbolos ausentes en `FirebaseFirestoreInternal`. | `libs.versions.toml@v2.5.0`; memoria #73 | ✅ Pinear SPM a **11.8.x** (`upToNextMinor`) para igualar el cinterop. Si se quisiera 12.x, validar con `xcodebuild` antes de cerrar `/plan`. |
| **D-4** ⚠️ | **Tests nativos iOS por Gradle**: `:shared:iosSimulatorArm64Test` **no enlaza** (`ld: framework 'FirebaseCore' not found`, memorias #71/#76) porque el binario de test de Gradle no consume los frameworks SPM de Xcode. El README de GitLive lo confirma. | memorias #71/#73/#76 + README GitLive | ✅ **No** añadir `iosSimulatorArm64Test` al gate. La suite corre en `:shared:testAndroid` (host) y iOS se valida con `:shared:compileKotlinIosSimulatorArm64` + `xcodebuild`, como ya prescribe `CLAUDE.md`. Descartada la alternativa `linkerOpts("-F<ruta DerivedData>")` por frágil y dependiente de la máquina. |
| **D-5** | **Dónde arranca el bootstrap en Android**: hoy no hay clase `Application`; Koin se inicia en `MainActivity.onCreate()`. | `androidApp/.../MainActivity.kt` | ✅ **Opción A**: ejecutar el bootstrap dentro de `initFledgeKoin` como `single(createdAtStart = true)`. Simétrico Android/iOS y sin tocar el manifest. **No** se crea clase `Application`. |
| **D-6** | **Artefactos a declarar**: `Firebase.initialize`/`FirebaseApp`/`FirebaseOptions` viven en **`firebase-app`**, no en `firebase-common`. Los tres son `api` transitivos de `firebase-firestore`. | fuente GitLive | ✅ Declarar los **tres** explícitamente en el catálogo (dependencias explícitas > transitivas). |
| **D-7** | **Base de datos `debug`**: `FIRESTORE_DATABASE_ID=debug` en debug builds. `firestore.rules` usa el comodín `{database}` (aplica a todas), pero **`firestore.indexes.json` se despliega por base**: hay que confirmar que la base `debug` existe en `fledge-c685d` y tiene el índice de `allowanceRules`. | `firestore.rules`, `firestore.indexes.json`, `firebase.json` | ✅ Verificar el estado en Firebase Console durante `/plan`. No se toca ningún fichero de reglas/índices en esta feature (es FLE-83). |
| **D-8** | **Peso e impacto Android**: `dev.gitlive:firebase-firestore` arrastra `com.google.firebase:firebase-firestore` (BOM 33.15.0) con gRPC/protobuf/guava. R8 está desactivado (`isMinifyEnabled = false`). | `firebase-firestore/build.gradle.kts`, `androidApp/build.gradle.kts` | ✅ Aceptar el crecimiento en esta fase y anotarlo como deuda para la fase de release. Sin acción en FLE-78. |

---

## Desglose de tareas (ligero)

- [ ] **T1** — Catálogo y dependencias: versión `gitlive = "2.5.0"` + tres `[libraries]`; declarar en
  `commonMain` de `shared/build.gradle.kts`. Verificar la resolución real (gotcha memoria #77).
  → **AC-01**
- [ ] **T2** — BuildKonfig: añadir `FIREBASE_APP_ID_ANDROID`, `FIREBASE_APP_ID_IOS`,
  `FIREBASE_GCM_SENDER_ID` (y `FIREBASE_STORAGE_BUCKET` si hace falta) con default `""`, y documentar
  el nuevo contrato de `local.properties` en `CLAUDE.md` §Configuración local. → **AC-03**, **AC-04**
- [ ] **T3** — `commonMain`: `FirebaseEnvironment` (+ `isComplete()`), port `FirebaseInitializer`,
  `FirebaseBootstrap` con estados y idempotencia. Sin tipos GitLive. → **AC-04**, **AC-05**, **AC-06**
- [ ] **T4** — `commonMain`: port `FirestoreProvider` + adaptador `GitLiveFirestoreProvider` que llama
  a `Firebase.firestore(Firebase.app, databaseId)` de forma perezosa. → **AC-07**
- [ ] **T5** — `androidMain`/`iosMain`: `actual` de `FirebaseInitializer` (`Firebase.initialize(ctx, options)`)
  y del `applicationId` por plataforma; registro en cada `PlatformModule.<plat>.kt`. → **AC-09**
- [ ] **T6** — Koin: nuevo `firebaseModule` (o extensión de `dataModule`) con `FirebaseEnvironment`,
  `FirebaseBootstrap` y `FirestoreProvider`; el bootstrap se engancha como `single(createdAtStart = true)`
  dentro de `initFledgeKoin` (D-5), **sin tocar** los bindings `InMemory*`. → **AC-07**, **AC-08**, **AC-09**
- [ ] **T7** — iOS/Xcode: añadir el paquete SPM `firebase-ios-sdk` a `iosApp.xcodeproj`
  (`packageReferences` hoy vacío) pinneado a **11.8.x** (D-3), enlazar `FirebaseCore` +
  `FirebaseFirestore` al target `iosApp`, versionar `Package.resolved`. → **AC-02**
- [ ] **T8** — Tests en `shared/src/commonTest` con `FakeFirebaseInitializer` / `FakeFirestoreProvider`,
  reutilizando el patrón de `AppModulesTest`. → **AC-04..AC-08**
- [ ] **T9** — Validación: `ktlintCheck`, `:shared:testAndroid`, `:androidApp:assembleDebug`,
  `:shared:compileKotlinIosSimulatorArm64`, `xcodebuild` de `iosApp`, y smoke manual en ambos
  entry points. → **AC-01**, **AC-02**, **AC-09**

---

## Notas no funcionales

**Plataformas afectadas**: Android e iOS (ambas). No hay web/wasmJs en Fledge. Cambia el build de
`:shared`, `:androidApp` (transitivamente) y el proyecto Xcode `iosApp`.

**Testabilidad** (requisito duro de esta feature): al tocar un SDK de plataforma, la lógica se aísla
tras ports en `commonMain` (`FirebaseInitializer`, `FirestoreProvider`) y se testea con **fakes escritos
a mano** en `shared/src/commonTest`. Stack: `kotlin.test` + `kotlinx-coroutines-test` (`runTest`) +
Turbine; nombres con backticks y prefijo `FLE-78`; cuerpo con `// Given` / `// When` / `// Then`.
Ningún test importa `dev.gitlive.*`. Los ACs que requieren linkado real (AC-01, AC-02, AC-09) se
verifican **por comando de build**, no por test unitario — está marcado en la trazabilidad.

**Firebase**: proyecto `fledge-c685d`. Bases: `debug` (builds debug) y `(default)` (release). Colecciones
que ya consume el job servidor y que los repos futuros deberán respetar:
`families/{familyId}`, `families/{familyId}/allowanceRules`, `families/{familyId}/ledgerTransactions`
(append-only: `allow update, delete: if false`), `families/{familyId}/settlements`.
En FLE-78 **no se escribe ni se lee** ningún documento.

**Reglas de negocio invariantes** (no se violan, pero condicionan el diseño futuro): ledger append-only,
saldo derivado nunca almacenado, importes enteros en céntimos (`MoneyCents`). Firestore usa `integerValue`
de 64 bits, compatible con `Long`; prohibido mapear importes a `Double` al llegar FLE-81.

**Seguridad**: la `FIREBASE_API_KEY` y los app ids son valores **públicos de cliente**; ningún secreto
de servidor entra en BuildKonfig ni en el repo (gotcha de `buildkonfig-secrets`). La autoridad de acceso
son las reglas server-side. `local.properties` sigue gitignored; CI inyecta por GitHub Secrets.

**Rendimiento**: `Firebase.initialize` añade coste al arranque (I/O + init de gRPC en Android). Debe
ejecutarse una sola vez por proceso y no bloquear el primer frame más de lo estrictamente necesario.
Se acepta el crecimiento del APK (ver D-8).

**i18n / accesibilidad / diseño visual**: no aplican — la feature no toca UI ni añade strings. No hay
`@Preview` nuevos.

**🚦 Gate 3 — `/design-check`: N/A (2026-07-28).** Feature de infraestructura pura: **sin impacto de
UI**. No se crea ni modifica ninguna pantalla, componente, ruta de navegación, cadena de
`composeResources` ni tema. Los ficheros que toca son build (`libs.versions.toml`,
`shared/build.gradle.kts`), `data/remote/firebase/`, los dos `PlatformModule.<plat>.kt`, el proyecto
Xcode y `commonTest`. No se abre `Fledge.pen` ni se añaden tableros. Gate saltado hacia `/test` según
el paso 1 del comando.

**ktlint**: `./gradlew ktlintFormat` tras tocar Kotlin; el gate corre `ktlintCheck`.

---

## Trazabilidad

| AC | Test(s) que lo cubren | ¿Rojo antes de implementar? |
|----|-----------------------|------------------------------|
| AC-01 | *(no unitario)* — comando: `./gradlew :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64` + `ktlintCheck` + grep de `cocoapods`/`cinterops` en `shared/build.gradle.kts` | n/a — verificable por build |
| AC-02 | *(no unitario)* — comando: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build` + existencia de `Package.resolved` | n/a — verificable por build |
| AC-03 | *(no unitario)* — inspección de repo: ausencia de `google-services.json` / `GoogleService-Info.plist` y del plugin `com.google.gms.google-services`; build verde sin `local.properties` | n/a — verificable por build/inspección |
| AC-04 | `FirebaseEnvironmentTest` · `` `FLE-78 firebase environment derives effective database id from build config` `` | **sí** — `kotlin.NotImplementedError` en `firebaseEnvironmentOf` (`FirebaseEnvironmentTest.kt:19`) |
| AC-05 | `FirebaseBootstrapTest` · `` `FLE-78 incomplete firebase config skips initialization without crashing` `` | **sí** — `kotlin.NotImplementedError` en `FirebaseBootstrap.run()` (`FirebaseBootstrapTest.kt:17`) |
| AC-06 | `FirebaseBootstrapTest` · `` `FLE-78 firebase bootstrap initializes only once` `` , `` `FLE-78 firebase bootstrap skips when app already exists` `` | **sí** — `kotlin.NotImplementedError` en `FirebaseBootstrap.run()` (`FirebaseBootstrapTest.kt:33` y `:51`) |
| AC-07 | `AppModulesTest` · `` `FLE-78 koin graph resolves firestore provider without initializing firebase` `` | **sí** — `org.koin.core.error.NoDefinitionFoundException: No definition found for type 'FirebaseEnvironment'` (falta el binding de T6) |
| AC-08 | `AppModulesTest` · `` `FLE-78 repositories remain in memory after firestore integration` `` | **no** — verde desde el inicio (test de regresión, no hay implementación asociada): comprueba que `dataModule` sigue resolviendo `InMemory*` y que ningún repositorio necesita `FirestoreProvider` |
| AC-09 | *(no unitario)* — `:androidApp:assembleDebug` + `xcodebuild` + smoke manual de arranque en Android y simulador iOS, comprobando un único log de bootstrap y ausencia de crash por doble init | n/a — verificable por build + smoke manual |

> Nota para `/validate`: los ACs marcados «n/a» **no** deben bloquear el gate de trazabilidad por falta
> de test rojo previo; se validan con el comando indicado en la misma fila.

> Nota de la fase `/test` (2026-07-28): ejecutado `./gradlew :shared:testAndroid` con el esqueleto T3a
> (`FirebaseEnvironment`, `FirebaseBootstrapState`, `FirebaseInitializer`, `FirebaseBootstrap`,
> `FirestoreProvider`, todos con `TODO()`): **78 tests, 5 fallidos**, todos en ejecución y ninguno por
> compilación. AC-08 es el único test nuevo en verde y así se declara arriba: es regresión pura sobre el
> grafo actual, no cubre implementación pendiente.
