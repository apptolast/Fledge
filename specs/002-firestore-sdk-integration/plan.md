# Plan 002: Integrar GitLive firebase-kotlin-sdk (FLE-78)

> Rama: `feature/002-firestore-sdk-integration` · Spec: [`spec.md`](./spec.md) · Estado: draft
> Decisiones del Gate 1 ya cerradas (D-0…D-8). Este plan **no las reabre**.

---

## Enfoque técnico

La feature es de infraestructura y su riesgo no está repartido: **casi todo depende de que el paso 1
compile**. Por eso el plan se ordena como *spike primero, diseño después*.

### Principio rector: el SDK no cruza la frontera de `commonTest`

El requisito duro del spec es que la suite no toque Firebase real. Se resuelve con una regla simple:

> **Ningún tipo `dev.gitlive.*` aparece en la firma de un port de `commonMain`.**

Eso obliga a una decisión que conviene explicitar, porque es la única no obvia del plan:

`FirestoreProvider` **no expone** `FirebaseFirestore`. El port declara solo lo que la lógica común
necesita observar (`databaseId`, `isAvailable`); el handle real de Firestore vive como miembro
concreto del adaptador `GitLiveFirestoreProvider`, fuera de la interfaz.

```kotlin
// commonMain — port, sin tipos GitLive
interface FirestoreProvider {
    val databaseId: String
    val isAvailable: Boolean
}

// commonMain — adaptador; commonTest nunca lo instancia
class GitLiveFirestoreProvider(
    private val bootstrap: FirebaseBootstrap,
    override val databaseId: String,
) : FirestoreProvider {
    override val isAvailable: Boolean get() = bootstrap.state.isReady
    fun firestore(): FirebaseFirestore = Firebase.firestore(Firebase.app, databaseId)   // perezoso
}
```

**Por qué así y no un port que devuelva `FirebaseFirestore`**: un fake en `commonTest` tendría que
implementar ese método y, por tanto, importar `dev.gitlive`. Y el falso beneficio —"así los repos
Firestore serán testeables"— no existe: cuando lleguen FLE-80/81/82, la costura de test de esos repos
es la **interfaz de dominio** (`LedgerRepository`, etc.), que ya está en `domain/repository/` y ya
tiene fakes `InMemory*`. Nadie va a mockear Firestore documento a documento. Los repos concretos
dependerán de `GitLiveFirestoreProvider` sin perder nada.

### Segunda decisión: los bindings de Firebase van en `platformModule`

`AppModulesTest` ya construye el grafo real con `fledgeModules(module { /* fakes */ })`, sustituyendo
el hueco de `platformModule`. Ahí es donde hoy entran `Settings` y `SocialAuthClient` como fakes, **sin
override de Koin**, porque `dataModule` no los define.

Se sigue exactamente ese patrón: `FirebaseInitializer`, el `applicationId` de plataforma y
`FirestoreProvider` se declaran **solo** en `PlatformModule.android.kt` / `PlatformModule.ios.kt`. Así
AC-07 y AC-08 se testean con el grafo de producción completo, inyectando fakes por el hueco que ya
existe, sin `allowOverride` ni módulos de test paralelos.

`FirebaseEnvironment` y `FirebaseBootstrap` son puros y van en `dataModule` (o en un `firebaseModule`
propio si el bloque crece; ver T6).

### Tercera decisión: el `applicationId` necesita un discriminador de plataforma

BuildKonfig genera **un solo objeto visible desde `commonMain`**, así que `FIREBASE_APP_ID_ANDROID` y
`FIREBASE_APP_ID_IOS` son ambos accesibles desde código común y hay que elegir. Se resuelve con
`expect val firebaseApplicationId: String` en `commonMain` y su `actual` por plataforma — el mismo
mecanismo que ya usa `initialFledgeLoginConfig()` en `presentation/AuthConfig.kt`.

La construcción del entorno se deja como **función pura con parámetros explícitos**, no leyendo
BuildKonfig por dentro, para que AC-04 sea un test determinista de verdad:

```kotlin
fun firebaseEnvironmentOf(
    apiKey: String, projectId: String, applicationId: String,
    gcmSenderId: String, storageBucket: String, databaseId: String,
): FirebaseEnvironment
```

El único punto que lee BuildKonfig es el binding de Koin. La cobertura de que ese cableado es correcto
la da AC-03 (build), no un test unitario.

### Cuarta decisión: cómo se consigue que los tests estén en ROJO "por falta de implementación"

Tensión real del harness en Kotlin: un test que referencia tipos inexistentes **no compila**, y `/test`
exige rojo *por falta de implementación de producción*, no por error de compilación.

Resolución: la fase `/test` crea, junto a los tests, el **esqueleto de API mínimo** en `commonMain`
—tipos, firmas y `TODO()` en los cuerpos—. Los tests compilan y fallan en ejecución con
`NotImplementedError`, que es rojo legítimo. `/implement` sustituye los `TODO()` por la lógica real
sin tocar los tests (el hook lo bloquea de todos modos).

Esto hay que respetarlo o la fase `/test` se atasca. Queda anotado como T3a.

---

## Ficheros a tocar por source-set

### `commonMain` — `shared/src/commonMain/kotlin/com/apptolast/fledge/data/remote/firebase/`

Se ubican junto al cliente REST existente porque son la misma preocupación (configuración de Firebase),
y así `FirebaseConfig` y `FirebaseEnvironment` se leen juntos.

| Fichero | Contenido | Nuevo/Modif. |
|---|---|---|
| `FirebaseEnvironment.kt` | `data class FirebaseEnvironment(apiKey, projectId, applicationId, gcmSenderId, storageBucket, databaseId)` + `fun isComplete(): Boolean` + `fun firebaseEnvironmentOf(...)` | **nuevo** |
| `FirebaseInitializer.kt` | **PORT**: `interface FirebaseInitializer { fun isInitialized(): Boolean; fun initialize(env: FirebaseEnvironment) }` | **nuevo** |
| `FirebaseBootstrap.kt` | Lógica pura: valida, aplica idempotencia, captura fallos. `class FirebaseBootstrap(env, initializer) { val state: FirebaseBootstrapState; fun run(): FirebaseBootstrapState }` | **nuevo** |
| `FirebaseBootstrapState.kt` | `sealed interface`: `NotConfigured`, `AlreadyInitialized`, `Initialized`, `Failed(reason)` + `val isReady: Boolean` | **nuevo** |
| `FirestoreProvider.kt` | **PORT**: `interface FirestoreProvider { val databaseId: String; val isAvailable: Boolean }` + `expect val firebaseApplicationId: String` | **nuevo** |
| `GitLiveFirestoreProvider.kt` | Adaptador fino sobre `Firebase.firestore(Firebase.app, databaseId)`, resolución **perezosa** | **nuevo** |
| `FirebaseConfig.kt` | Sin cambios funcionales. Se deja como está: sigue sirviendo al cliente REST de Auth. **No** se fusiona con `FirebaseEnvironment` en esta feature | *sin tocar* |

### `androidMain` / `iosMain` — puntos `expect/actual`

| Fichero | Contenido |
|---|---|
| `androidMain/.../data/remote/firebase/FirebaseInitializer.android.kt` | `actual val firebaseApplicationId = BuildKonfig.FIREBASE_APP_ID_ANDROID` · `class AndroidFirebaseInitializer(context: Context) : FirebaseInitializer` → `Firebase.initialize(context, options)` |
| `iosMain/.../data/remote/firebase/FirebaseInitializer.ios.kt` | `actual val firebaseApplicationId = BuildKonfig.FIREBASE_APP_ID_IOS` · `class IosFirebaseInitializer : FirebaseInitializer` → `Firebase.initialize(null, options)` |
| `androidMain/.../di/PlatformModule.android.kt` | + `single<FirebaseInitializer> { AndroidFirebaseInitializer(androidContext()) }`, + `single<FirestoreProvider> { GitLiveFirestoreProvider(get(), get<FirebaseEnvironment>().databaseId) }` |
| `iosMain/.../di/PlatformModule.ios.kt` | idem con `IosFirebaseInitializer()` |

**Único punto `expect/actual` nuevo**: `firebaseApplicationId`. El `FirebaseInitializer` **no** es
`expect/actual` sino una interfaz con dos implementaciones inyectadas por Koin — más testeable y
coherente con cómo está resuelto hoy `SocialAuthClient`.

No hay `mobileMain` ni `wasmJsMain` en Fledge.

### `androidApp` / `iosApp`

| Fichero | Cambio |
|---|---|
| `androidApp/.../MainActivity.kt` | **sin cambios**. El bootstrap entra por Koin (D-5) y `androidContext(this@MainActivity)` ya está puesto |
| `shared/src/iosMain/.../MainViewController.kt` | **sin cambios**. Ya llama a `initFledgeKoin()` |
| `iosApp/iosApp.xcodeproj/project.pbxproj` | + `XCRemoteSwiftPackageReference` a `firebase-ios-sdk` pinneado a **11.8.x**, + `XCSwiftPackageProductDependency` de `FirebaseCore` y `FirebaseFirestore` enlazados al target `iosApp` |
| `iosApp/.../swiftpm/Package.resolved` | **nuevo**, versionado en git |

### Build

| Fichero | Cambio |
|---|---|
| `gradle/libs.versions.toml` | `gitlive = "2.5.0"` + `gitlive-firebase-app`, `gitlive-firebase-common`, `gitlive-firebase-firestore` |
| `shared/build.gradle.kts` | los tres en `commonMain.dependencies` + 3 campos BuildKonfig nuevos |
| `settings.gradle.kts` | **sin cambios** — el maven de GitLive ya está declarado |
| `CLAUDE.md` | §Configuración local: documentar `FIREBASE_APP_ID_ANDROID`, `FIREBASE_APP_ID_IOS`, `FIREBASE_GCM_SENDER_ID` |

### `commonTest`

| Fichero | Contenido |
|---|---|
| `data/remote/firebase/FirebaseEnvironmentTest.kt` | **nuevo** — AC-04 |
| `data/remote/firebase/FirebaseBootstrapTest.kt` | **nuevo** — AC-05, AC-06. Incluye `FakeFirebaseInitializer` (contador de llamadas + `isInitialized` configurable) |
| `di/AppModulesTest.kt` | **modificado** — AC-07, AC-08. Se le añade `FakeFirestoreProvider` al módulo de test que ya existe |

Ningún fichero de `commonTest` importa `dev.gitlive.*`. Es verificable con un grep en `/validate`.

---

## Wiring de DI (Koin)

```kotlin
// commonMain — dataModule
single { firebaseEnvironmentOf(
    apiKey = BuildKonfig.FIREBASE_API_KEY,
    projectId = BuildKonfig.FIREBASE_PROJECT_ID,
    applicationId = firebaseApplicationId,           // expect/actual
    gcmSenderId = BuildKonfig.FIREBASE_GCM_SENDER_ID,
    storageBucket = BuildKonfig.FIREBASE_STORAGE_BUCKET,
    databaseId = BuildKonfig.FIRESTORE_DATABASE_ID,
) }
single(createdAtStart = true) { FirebaseBootstrap(get(), get()).also { it.run() } }

// androidMain / iosMain — platformModule (hueco que AppModulesTest ya sustituye)
single<FirebaseInitializer> { … }
single<FirestoreProvider> { GitLiveFirestoreProvider(get(), get<FirebaseEnvironment>().databaseId) }
```

Dos detalles de Koin que condicionan los ACs y que hay que verificar en `/implement`, no dar por hechos:

1. **`createdAtStart` y la rama `loadKoinModules`.** `initFledgeKoin` tiene dos caminos: `startKoin`
   (primer arranque) y `loadKoinModules` (si ya hay contexto). Las instancias `createdAtStart` se
   materializan en `createEagerInstances()`. Hay que confirmar que la rama `loadKoinModules` también
   las dispara; si no, el bootstrap se invoca explícitamente tras cargar los módulos.
2. **`koinApplication { }` no llama a `createEagerInstances()`.** Esto es justo lo que AC-07 necesita
   («el initializer falso no registró ninguna llamada por el mero hecho de construir el grafo») y hace
   que el test sea honesto en vez de accidental. Si en algún momento se cambiara a `startKoin` en tests,
   ese AC dejaría de significar lo que dice.

`dataModule` **no se toca** en sus bindings `InMemory*` — es literalmente lo que AC-08 comprueba.

---

## Impacto en plataformas e i18n

- **Android**: crece el APK por gRPC/protobuf/guava con R8 desactivado (D-8, deuda anotada).
  `Firebase.initialize` añade coste de arranque; se ejecuta una vez por proceso.
- **iOS**: es el frente delicado. Los frameworks entran por SPM en el target `iosApp`; el `:shared`
  compila contra el cinterop de GitLive. Por eso `:shared:iosSimulatorArm64Test` **no** entra en el
  gate (D-4) y la validación iOS es `compileKotlinIosSimulatorArm64` + `xcodebuild`.
- **Web**: no aplica, no hay target.
- **i18n**: **ninguna cadena nueva**. No se tocan `values/strings.xml` ni `values-en/strings.xml`.
- **UI**: cero. → **`/design-check` es N/A en este spec** (feature de infraestructura pura, sin
  pantallas ni componentes). Se anota aquí para que el Gate 3 se salte con justificación.

---

## Desglose de tareas ordenado

El orden no es estético: **T1 es un gate de riesgo**. Si el compilador rechaza los klibs de GitLive
(D-1), T2…T9 no llegan a tener sentido y hay que volver a producto.

| # | Tarea | AC | Notas |
|---|---|---|---|
| **T1** 🚦 | **Spike de compatibilidad.** Añadir `gitlive = "2.5.0"` + los tres artefactos al catálogo y a `commonMain`; ejecutar `:shared:compileKotlinIosSimulatorArm64` y `:androidApp:assembleDebug` **sin escribir código todavía**. Verificar que la coordenada resuelve de verdad (gotcha memoria #77: un POM agregado vacío resuelve "bien" y falla después) | AC-01 | **Si falla**: probar 2.4.0; si tampoco, parar y escalar (D-1) |
| **T2** | BuildKonfig: `FIREBASE_APP_ID_ANDROID`, `FIREBASE_APP_ID_IOS`, `FIREBASE_GCM_SENDER_ID`, `FIREBASE_STORAGE_BUCKET`, todos con default `""`. Documentar el contrato nuevo en `CLAUDE.md` | AC-03, AC-04 | El build debe seguir funcionando en una máquina sin `local.properties` |
| **T3a** | **Esqueleto de API** en `commonMain` con `TODO()`: `FirebaseEnvironment`, `FirebaseInitializer`, `FirebaseBootstrap`, `FirebaseBootstrapState`, `FirestoreProvider`, `expect val firebaseApplicationId`. Sin lógica | — | Prerrequisito para que `/test` compile y falle *en ejecución*. Va **en la fase `/test`** |
| **T3b** | Implementar la lógica pura: validación de `isComplete()`, idempotencia y estados | AC-04, AC-05, AC-06 | Fase `/implement` |
| **T4** | `GitLiveFirestoreProvider`: `Firebase.firestore(Firebase.app, databaseId)` perezoso | AC-07 | No debe tocar Firebase al construirse |
| **T5** | `actual` por plataforma: `firebaseApplicationId` + `AndroidFirebaseInitializer` / `IosFirebaseInitializer` con `FirebaseOptions` explícitas | AC-09 | En iOS `applicationId` y `gcmSenderId` son obligatorios |
| **T6** | Koin: bindings de entorno y bootstrap en `dataModule`; initializer y provider en cada `platformModule`. **Sin tocar** los `InMemory*` | AC-07, AC-08, AC-09 | Verificar los dos detalles de Koin de la sección anterior |
| **T7** | Xcode/SPM: `firebase-ios-sdk` pinneado a 11.8.x, productos `FirebaseCore` + `FirebaseFirestore` en el target `iosApp`, `Package.resolved` versionado | AC-02 | Ver riesgo abajo |
| **T8** | Tests en `commonTest` con `FakeFirebaseInitializer` y `FakeFirestoreProvider`, reutilizando el patrón de `AppModulesTest` | AC-04…AC-08 | Fase `/test`, junto a T3a |
| **T9** | Validación completa: `ktlintCheck`, `:shared:testAndroid`, `:androidApp:assembleDebug`, `:shared:compileKotlinIosSimulatorArm64`, `xcodebuild`, smoke manual en ambos entry points + grep de que `commonTest` no importa `dev.gitlive` | AC-01, AC-02, AC-09 | Fase `/validate` |

**Secuencia por fase del harness**: T1 y T2 son preparatorias y caben en `/implement`; T3a y T8 son la
fase `/test`; T3b, T4, T5, T6 y T7 son `/implement`; T9 es `/validate`. `/design-check` se salta por N/A.

---

## Riesgos y cómo se mitigan

| Riesgo | Probabilidad | Mitigación |
|---|---|---|
| **Klibs de Kotlin 2.2.21 rechazados por el compilador 2.4.10** (D-1) | media | T1 lo detecta en el primer comando, antes de escribir una línea de lógica. Fallback: 2.4.0 → escalar |
| **Editar `project.pbxproj` a mano para el SPM** | media-alta | Es el paso más frágil de todo el plan: formato propietario y `packageReferences` hoy vacío. Plan A: editarlo con cuidado y verificar con `xcodebuild`. **Plan B, más seguro: que lo añadas tú desde Xcode** (File → Add Package Dependencies → `firebase-ios-sdk`, regla *Up to Next Minor* 11.8.0, productos `FirebaseCore` y `FirebaseFirestore`) y yo commiteo el `Package.resolved` |
| **Desajuste de majors 11.8.x (cinterop) vs 12.x (SPM)** (D-3) | baja tras pinear | Pineado a 11.8.x. Si aparecen símbolos ausentes en `FirebaseFirestoreInternal`, subir SPM y revalidar con `xcodebuild` |
| **La app arranca en `NotConfigured` en tu máquina** | **alta si no se avisa** | Los 3 campos nuevos de `local.properties` no existen todavía en ninguna máquina. Por diseño no crashea (AC-05), pero Firestore quedará no disponible hasta que los rellenes. Se documenta en `CLAUDE.md` (T2) y se avisa en el Gate 2 |
| **Doble inicialización de Firebase** | baja | AC-06 cubre la idempotencia; el `actual` de Android consulta `Firebase.apps(context)` antes de inicializar |

---

## Conocimiento reutilizable aplicado

- **engram #73 / #71 / #76** — la vía SPM funciona para el target de app, pero los tests nativos de
  Gradle no consumen esos frameworks. Es el origen directo de D-4 y de que el gate iOS sea
  `compileKotlinIosSimulatorArm64` + `xcodebuild`.
- **engram #77** — una coordenada mal escrita resuelve un POM agregado vacío. Por eso T1 verifica la
  resolución real y no se conforma con "el build no se quejó".
- **`architecture/koin-multimodule-di`** — `expect val platformModule: Module` como hueco de
  sustitución; es lo que hace que AC-07 y AC-08 se puedan testear sobre el grafo de producción.
- **`testing/kotlin-test-turbine-fakes`** — sección *Ports de plataforma*: port en `commonMain` + fake
  a mano en `commonTest` para SDKs solo-plataforma. Es literalmente el diseño de esta feature.
- **`integrations/firebase-rest-client`** — se conserva intacto para Auth. El gotcha *«fija
  `databaseId` antes de la primera llamada»* se traslada al bootstrap del SDK.
- **`build-release/buildkonfig-secrets`** — todos los campos con default; solo valores públicos de
  cliente en BuildKonfig.
