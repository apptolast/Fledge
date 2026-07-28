# Spec 003: Migrar la autenticación al SDK GitLive firebase-auth (FLE-88)

> Rama: `feature/003-gitlive-auth-migration` · Proyecto: `Fledge`
> **Estado: PAUSADO — bloqueado por FLE-90 (repo `apptolast/BaseLogin`, spec 001).**

## ⚠️ Cambio de dirección (2026-07-28) — leer antes que nada

La versión original de este spec proponía escribir en Fledge un `FledgeGitLiveAuthProvider` propio
sobre un puerto local. **Esa decisión se ha revertido.** Todo lo que sigue por debajo de la sección
«Alcance» describe ese plan antiguo y **no debe implementarse**; se conserva porque su análisis de
riesgos y su evidencia de T1 siguen siendo válidos.

Motivo: al revisar el repo local de BaseLogin se comprobó que ya trae un `FirebaseAuthProvider`
completo sobre GitLive (389 líneas: email/password, OAuth, teléfono, magic link, gestión de cuenta) y
que `loginDataModule(authProvider = null)` ya lo registra. Escribir un tercero en Fledge duplicaría
ese código y dejaría BaseLogin igual de frágil para el resto de la flota.

Se cae además una de las tres justificaciones que sostenían el plan original: dije que el `actual` de
Android de BaseLogin «no era auditable» porque el sources jar no traía `androidMain`. Con el repo
local delante **sí lo es**, y esa razón queda retirada. Las otras dos —el `displayName` de Apple que
se pierde y la ausencia total de cobertura— resultaron ser argumentos para **arreglar BaseLogin**, no
para esquivarlo.

**Nueva forma del trabajo:**

| Ticket | Repo | Contenido |
|---|---|---|
| **FLE-90** | `apptolast/BaseLogin` | El puerto que hace testeable `FirebaseAuthProvider`, el `displayName` de Apple, el `signOut` social y la higiene de `Platform.android.kt`. Ver `specs/001-firebase-auth-gateway/spec.md` allí |
| **FLE-88** | `apptolast/Fledge` | Consumir `loginDataModule()`, **borrar** el transporte REST y **borrar** la capa social duplicada de Fledge, repinear BaseLogin |

FLE-88 se vuelve a especificar cuando FLE-90 aterrice, porque su superficie exacta depende de la API
que quede allí. Lo único de este spec que sigue vigente y ya ejecutado es **T1** y su evidencia.

## Contexto y objetivo

Fledge autentica hoy contra **Firebase Identity Toolkit por REST** (Ktor), y guarda el `idToken` en
`TokenManager` sobre `multiplatform-settings`. Ese almacén es **invisible para el SDK nativo de
Firebase**.

`firestore.rules` exige `request.auth != null`, y el SDK de Firestore que se integró en FLE-78 toma la
credencial del **SDK nativo de Firebase Auth**, no de nuestro almacén. No existe API pública para
inyectar un `idToken` propio en el SDK. Consecuencia concreta y verificable: hoy, cualquier lectura o
escritura del SDK de Firestore sería denegada con `PERMISSION_DENIED`. Es la decisión **D-0** del
spec 002, marcada crítica.

Este ticket no *mitiga* ese bloqueo: lo **elimina**. Cuando el login pase por el SDK, el propio SDK
adjunta la credencial a cada operación de Firestore, que es exactamente lo que las reglas esperan.

**Principio de arquitectura fijado por el usuario (2026-07-28):** Fledge no tiene módulo admin web,
así que no necesita hablar con Firebase por HTTP. *Toda* comunicación con Firebase va por el SDK de
GitLive. Eso amplía el alcance respecto a la descripción original del ticket: no basta con añadir
`firebase-auth`, hay que **retirar el transporte REST completo**.

FLE-88 **bloquea FLE-80, FLE-81 y FLE-82** (migración de los tres repositorios a Firestore).

## Alcance

**Dentro:**

- Añadir `dev.gitlive:firebase-auth` al catálogo y a `commonMain`, y el producto SPM `FirebaseAuth`
  al target `iosApp`.
- Puerto `FirebaseAuthGateway` en `commonMain` + adaptador `GitLiveFirebaseAuthGateway`, replicando
  el patrón de `FirestoreProvider`/`GitLiveFirestoreProvider` de FLE-78.
- Nuevo `FledgeGitLiveAuthProvider` que implementa el `AuthProvider` de BaseLogin sobre el puerto,
  **conservando** la capa social existente (`SocialAuthClient`: Credential Manager en Android, puente
  Apple en iOS).
- Clasificador de errores `firebaseAuthErrorKindOf(message)`, porque Android e iOS producen strings
  distintos para el mismo error y `commonMain` solo ve el `message`.
- Rewiring de Koin: el provider en `dataModule`, el gateway en ambos `platformModule`.
- **Demolición del transporte REST** (~825 líneas): `FledgeFirebaseAuthProvider`,
  `FirebaseAuthService`, `FirebaseAuthDto`, `FirebaseAuthHttpClient`, `FirebaseIdToken`,
  `FirebaseConfig`, `TokenManager`, `InMemoryLoginAuthProvider`; Ktor entero (6 artefactos, 4 source
  sets); `multiplatform-settings` y sus 3 bindings; el `single { Json { … } }`.
- Retraducción de los tests de auth y actualización de `CLAUDE.md` §Autenticación.

**Fuera:**

- **Migrar repositorios**: `FamilyFoundationRepository`, `LedgerRepository` y `MoneyFlowRepository`
  siguen siendo `InMemory*` al terminar esta feature (FLE-80 / FLE-81 / FLE-82).
- **Tocar `firestore.rules` o `firestore.indexes.json`** (FLE-83). El smoke de AC-13 se ejecuta
  *contra las reglas actuales*, sin modificarlas.
- Sustituir BaseLogin ni tocar sus pantallas de login. Sigue siendo la capa de UI.
- Cambiar el flujo social: `SocialAuthCoordinator.swift`, `iOSApp.swift`, `IosSocialAuthClient.kt`,
  `AndroidSocialAuthClient.kt` y el separador `"|||"` quedan **intactos**.
- Cualquier cambio de UI, navegación, i18n, tema, ViewModels o pantallas.
- Logging (FLE-89), CI/CD, y cinterop propio en iOS (prohibido por `CLAUDE.md`).

## Conocimiento reutilizable

**engram #106** (`decision`, FLE-78 spec: GitLive Firestore SDK integration facts and blockers):

*Se reutiliza tal cual:*

- `Firebase.initialize` / `FirebaseApp` / `FirebaseOptions` viven en `dev.gitlive:firebase-app`. Ya
  está declarado; `firebase-auth` es un artefacto hermano con la misma versión `gitlive = "2.5.0"`.
- **`:shared:iosSimulatorArm64Test` NO enlaza** (`ld: framework 'FirebaseCore' not found`): los
  binarios de test de Gradle no consumen los frameworks SPM de Xcode. La suite se queda en
  `:shared:testAndroid`. **No** se añade al gate. (D-4 de FLE-78, aquí sigue vigente.)
- El desalineamiento de majors entre el cinterop de GitLive (Firebase iOS 11.8.0) y SPM ya está
  resuelto: el paquete quedó pineado a `upToNextMinor` desde 11.8.0 y resuelve **11.8.1**.
  `FirebaseAuth` viene del mismo pin, así que **no hay que tocar la versión del paquete**.
- **D-0** es literalmente el motivo de existir de este ticket.

**Del propio repo, spec 002 y `GitLiveFirestoreProvider.kt`:**

*Se adapta:*

- El **patrón de puerto** es el hallazgo más valioso de FLE-78 y se replica 1:1:
  `GitLiveFirestoreProvider` deja `FirebaseFirestore` como miembro concreto **fuera** de la interfaz,
  para que `commonTest` pueda escribir fakes sin importar `dev.gitlive.*`. `FirebaseAuthGateway` hace
  lo mismo con `FirebaseUser`/`AuthResult` del SDK.
- **Resolución perezosa**: `GitLiveFirestoreProvider.firestore()` no toca Firebase al construirse,
  porque Koin lo instancia antes de que `FirebaseBootstrap.run()` haya corrido. `Firebase.auth` tiene
  el mismo problema y la misma solución.
- **Los bindings de Firebase van en `platformModule`**, no en `dataModule`: es el único hueco de
  sustitución que `AppModulesTest` ya usa (sin `allowOverride`). Se duplica una línea idéntica en
  Android e iOS de forma deliberada — consistencia con FLE-78 por encima de DRY.
- `koinApplication { }` **sí** crea instancias eager en Koin 4.2.x (corrección registrada en el plan
  002). Ningún binding nuevo puede ser `createdAtStart`.

**Descartado tras verificarlo** — BaseLogin ya trae su propio `FirebaseAuthProvider` sobre GitLive
(389 líneas: email/password, OAuth, phone, magic link). No se usa, por tres razones concretas:

1. Su `actual` de `getSocialIdToken` en Android **no es auditable**: el sources jar no trae
   `androidMain`.
2. Usa un separador de nonce distinto (`"|||rawNonce|||"` frente al `"|||"` con el que
   `SocialAuthCoordinator.swift` ya habla) y **descarta el `displayName` de Apple**, que solo llega
   en el primer login.
3. Recibe `FirebaseAuth` por constructor, así que bajo D-4 (sin tests nativos iOS) la cobertura
   automática de auth caería a **cero**.

## Diseño: el puerto

`shared/src/commonMain/.../data/remote/firebase/`

| Fichero | Rol |
|---|---|
| `FirebaseAuthGateway.kt` | El puerto. Ningún tipo `dev.gitlive.*` lo cruza. Tipos propios: `FirebaseAuthUser`, `FirebaseAuthCredential` (sellado: `EmailPassword` / `Google` / `OAuth(providerId, idToken, rawNonce)`), `FirebaseAuthErrorKind` (14 valores) y `FirebaseAuthFailure`, la única excepción que el puerto puede lanzar |
| `GitLiveFirebaseAuthGateway.kt` | El adaptador. **Único** fichero de `commonMain` que importa `dev.gitlive.firebase.auth.*`. Resolución perezosa de `Firebase.auth`. Un funnel `runGateway { }` convierte cualquier throwable del SDK en `FirebaseAuthFailure` |
| `FirebaseAuthErrorClassifier.kt` | `firebaseAuthErrorKindOf(message: String?)`, puro y testeable |

> ⚠️ **Trampa verificada**: `FirebaseNetworkException` y `FirebaseTooManyRequestsException` viven en
> `dev.gitlive.firebase` (paquete `firebase-app`) y **no heredan** de `FirebaseAuthException`. Un
> `catch (e: FirebaseAuthException)` los deja pasar. El `when` del adaptador los trata aparte.

`shared/src/commonMain/.../data/auth/FledgeGitLiveAuthProvider.kt` implementa los 20 miembros del
`AuthProvider` de BaseLogin sobre el gateway + el `SocialAuthClient` existente. `PROVIDER_ID` mantiene
el valor `"fledge-firebase"` para no invalidar nada persistido. **Nada ocurre en el constructor.** Se
preservan 1:1 los 11 mapeos de `AuthError` con sus mensajes en español que hoy están en
`FledgeFirebaseAuthProvider.kt:315-331`.

**Cambios de comportamiento deliberados** (no son accidentes; van aquí para que `/validate` no los
lea como regresiones):

1. `restoreSession()` y el parseo manual del JWT **desaparecen**. La restauración la hace el SDK desde
   su almacén nativo (Keychain en iOS, `SharedPreferences` internas en Android). Es justo lo que hace
   que Firestore vea la credencial.
2. `getCurrentSession()` deja de traer `accessToken`. Cumple el contrato documentado del interfaz
   («MUST NOT perform network I/O»). Quien necesite token usa `getIdToken(forceRefresh)`.
3. `observeAuthState()` pasa de `MutableStateFlow` caliente a flow frío del SDK y emite
   `AuthState.Loading` al empezar. **Verificado**: ningún composable de Fledge consume `AuthState` —
   solo las pantallas de BaseLogin, que ya lo manejan.
4. `session.refreshToken` pasa a `null`. Nadie lo consume.

## Criterios de aceptación (Gherkin)

```gherkin
# ---------- Verificable por comando de build ----------

Scenario [AC-01]: El SDK de Auth compila en ambas plataformas y Ktor desaparece
  Given el catálogo declara dev.gitlive:firebase-auth con la versión gitlive compartida
  When  se ejecutan ktlintCheck, :shared:testAndroid, :androidApp:assembleDebug
        y :shared:compileKotlinIosSimulatorArm64
  Then  todos terminan en BUILD SUCCESSFUL
   And  un grep de "io.ktor" sobre shared/, androidApp/ y gradle/ devuelve cero coincidencias

Scenario [AC-02]: El transporte REST no existe en el árbol
  Given la migración completada
  When  se busca en el código fuente
  Then  no existen FledgeFirebaseAuthProvider, FirebaseAuthService, FirebaseAuthDto,
        FirebaseAuthHttpClient, FirebaseIdToken, FirebaseConfig, TokenManager
        ni InMemoryLoginAuthProvider
   And  un grep de "identitytoolkit" o "securetoken" devuelve cero coincidencias
   And  multiplatform-settings no aparece en el catálogo ni en ningún source set

Scenario [AC-03]: iOS enlaza FirebaseAuth por package manager, sin cinterop propio
  Given el paquete SPM firebase-ios-sdk ya pineado a 11.8.x en el target iosApp
  When  se añade el producto FirebaseAuth y se ejecuta xcodebuild sobre el simulador
  Then  el build termina en BUILD SUCCEEDED
   And  el proyecto no contiene ninguna definición de cinterop propia
   And  Package.resolved sigue versionado y sin pines nuevos inesperados

# ---------- Verificable con tests en commonTest ----------

Scenario [AC-04]: El registro con email y password crea sesión por el SDK
  Given un gateway falso que acepta la creación de usuario y devuelve uid "u-1"
  When  el provider ejecuta signUp con email, password y displayName
  Then  el resultado es AuthResult.Success con userId "u-1"
   And  el gateway recibió exactamente una credencial EmailPassword
   And  el displayName se propagó al perfil del usuario

Scenario [AC-05]: El login con email y password delega en el SDK
  Given un gateway falso con un usuario existente
  When  el provider ejecuta signIn con Credentials.EmailPassword
  Then  el resultado es AuthResult.Success
   And  el gateway recibió una credencial EmailPassword con ese email
   And  no se construyó ninguna petición HTTP

Scenario [AC-06]: Google entrega su idToken como credencial del SDK
  Given un SocialAuthClient falso que devuelve providerId "google.com" e idToken "g-token"
  When  el provider ejecuta signIn con Credentials.OAuthToken(IdentityProvider.Google)
  Then  el gateway recibió FirebaseAuthCredential.Google con idToken "g-token"
   And  el resultado es AuthResult.Success

Scenario [AC-07]: Apple entrega idToken y rawNonce como credencial OAuth
  Given un SocialAuthClient falso que devuelve providerId "apple.com", idToken "a-token",
        rawNonce "nonce-123" y displayName "Ana"
  When  el provider ejecuta signIn con Credentials.OAuthToken(IdentityProvider.Apple)
  Then  el gateway recibió FirebaseAuthCredential.OAuth con providerId "apple.com",
        idToken "a-token" y rawNonce "nonce-123"
   And  el displayName "Ana" llega a la sesión resultante

Scenario [AC-08]: Un proveedor no soportado o cancelado no toca el SDK
  Given un SocialAuthClient falso que lanza SocialAuthCancelledException
  When  el provider ejecuta signIn con Credentials.OAuthToken
  Then  el resultado es AuthResult.Failure con AuthError.OperationNotAllowed
   And  el gateway registró cero interacciones

Scenario [AC-09]: Los errores del SDK se mapean a los AuthError de BaseLogin
  Given un gateway falso que lanza FirebaseAuthFailure con cada FirebaseAuthErrorKind
  When  el provider ejecuta la operación correspondiente
  Then  cada uno produce su AuthError con el mensaje en español que hoy expone el provider REST
   And  el clasificador reconoce las tres familias de string (código Android, mensaje iOS
        y mensaje genérico) para el mismo error

Scenario [AC-10]: getCurrentSession lee el caché sin hacer I/O
  Given un gateway falso con un usuario autenticado en caché
  When  el provider ejecuta getCurrentSession
  Then  devuelve la UserSession de ese usuario
   And  el gateway registró cero llamadas a getIdToken

Scenario [AC-11]: Construir el grafo de Koin no toca el SDK de Auth
  Given los módulos de producción con un FirebaseAuthGateway falso en platformModule
  When  se construye el grafo y se resuelve AuthProvider
  Then  la resolución tiene éxito
   And  el gateway falso registró cero interacciones con el SDK

Scenario [AC-12]: Sin configuración de Firebase el login falla con mensaje claro
  Given un entorno de Firebase incompleto (bootstrap en NotConfigured)
  When  el usuario intenta iniciar sesión
  Then  el resultado es AuthResult.Failure con un mensaje accionable
   And  la app no crashea

# ---------- Verificable por smoke manual ----------

Scenario [AC-13]: Tras el login, Firestore recibe request.auth
  Given las reglas actuales de Firestore, que exigen request.auth != null
  When  antes de iniciar sesión se intenta escribir un documento de sonda
  Then  la escritura se deniega con PERMISSION_DENIED
  When  el usuario inicia sesión (email/password, y social en cada plataforma)
  Then  la misma escritura tiene éxito
  When  se reinicia la app sin volver a iniciar sesión
  Then  la escritura sigue teniendo éxito
  When  el usuario cierra sesión
  Then  la escritura vuelve a denegarse con PERMISSION_DENIED
```

## Desglose de tareas (ligero)

```
T1 ──► T2 ──► T3 ──► T4 ──► T5 ──┬─► T7 ──► T8 ──► T9
                                 └─► T6 ──►
```

- [x] **T1** 🚦 — Spike de resolución: `dev.gitlive:firebase-auth` al catálogo y a `commonMain`, sin
      lógica. Validación completa. Gate de riesgo D-1. → **AC-01**
- [ ] **T2a** — Esqueletos con `TODO()`: puerto, clasificador, provider. → *(fase `/test`)*
- [ ] **T2b** — Tests + `FakeFirebaseAuthGateway`; **borrar** `FledgeFirebaseAuthProviderTest.kt` e
      `InMemoryLoginAuthProviderTest.kt`; actualizar `AppModulesTest`. → *(fase `/test`)*
- [ ] **T3** — Implementar el provider (20 miembros) y el clasificador. → **AC-04…AC-10**, **AC-12**
- [ ] **T4** — Implementar el adaptador GitLive (perezoso, funnel de excepciones). → **AC-09**
- [ ] **T5** — Wiring Koin: provider en `dataModule`, gateway en ambos `platformModule`. → **AC-11**
- [ ] **T6** — Demolición: 8 ficheros, Ktor, `multiplatform-settings`, `TestSettings.kt`, el bloque
      `iosMain.dependencies { }` que queda vacío. → **AC-02**
- [ ] **T7** 🚦 — **Paso manual del usuario en Xcode**: target `iosApp` → General → Frameworks,
      Libraries, and Embedded Content → `+` → `FirebaseAuth`. Después se commitean `project.pbxproj`
      y `Package.resolved`. → **AC-03**
- [ ] **T8** — Actualizar `CLAUDE.md` §Autenticación: el transporte es GitLive, no REST.
- [ ] **T9** — Validación completa + smoke de AC-13 en ambas plataformas, con evidencia en el spec.

## Notas no funcionales

**Plataformas**: Android e iOS. No hay web admin, y ese es precisamente el motivo de que no haga
falta transporte HTTP.

**Firebase**: no se crean ni modifican reglas, índices ni bases. El smoke de AC-13 escribe y borra un
único documento de sonda (`families/__fle88_smoke__`) contra las reglas actuales.

**i18n / accesibilidad / diseño visual**: no aplican — la feature no toca UI ni añade cadenas. No hay
`@Preview` nuevos. **`/design-check` se prevé N/A**, a confirmar en su gate.

**Testabilidad** (guardarraíl de la plantilla): la feature toca plataforma, así que el port
`FirebaseAuthGateway` vive en `commonMain` y se testea con un fake escrito a mano en `commonTest`.
Regla verificable con grep en `/validate`: **`commonTest` no importa `dev.gitlive.*`**.

**Honestidad sobre la cobertura** — la migración **reduce** la superficie de test automático.
`MockEngine` cubría también la serialización de peticiones HTTP; ahora esa capa la pone el SDK y solo
se verifica en dispositivo. Es el precio de resolver D-0. Se compensa con AC-13, una verificación
end-to-end real que hoy no existe. Quedan **solo bajo smoke manual**: el adaptador GitLive completo,
el `when` sobre excepciones del SDK, la persistencia entre reinicios, Credential Manager /
`ASAuthorizationController`, y que Firestore reciba `request.auth`.

**Cómo se consigue el ROJO legítimo en `/test`**: se crean los esqueletos con `TODO()` en los cuerpos
**junto a** los tests, de modo que el código compile y falle con `NotImplementedError`, no con error
de compilación. Los **borrados de ficheros de test tienen que ocurrir en `/test`** — en `/implement`
un hook los bloquea. Es el error de secuenciación más probable de esta feature.

**ktlint**: `./gradlew ktlintFormat` tras tocar Kotlin; el gate corre `ktlintCheck`.

## Decisiones abiertas (🚦 Gate 1)

| # | Riesgo / decisión | Estado |
|---|---|---|
| **D-1** ⚠️ | **Versión de GitLive**: BaseLogin fue compilado contra `firebase-auth:2.4.0` y en iOS lo pide en `apiElements`; se declara 2.5.0. No verificable sin build. | ✅ **CERRADO en T1 (2026-07-28).** Ver evidencia abajo. Sin fallback necesario. |
| **D-2** | `observeAuthState()` empieza a emitir `AuthState.Loading`. ¿Parpadea el login? | Verificado que ningún composable de Fledge consume `AuthState`. Smoke en `/validate`. |
| **D-3** | Android: tras `signOut()` el selector de cuenta de Google no reaparece, porque Credential Manager cachea. | **Bug preexistente, fuera de alcance.** Propuesta: abrir ticket para añadir `suspend fun signOut()` a `SocialAuthClient`. |
| **D-4** ⚠️ | Sin `local.properties` la app **no podrá loguearse** (bootstrap en `NotConfigured`). Hasta ahora el REST solo necesitaba `FIREBASE_API_KEY`; a partir de aquí los 4 campos de Firebase pasan de opcionales a **obligatorios** de facto. | AC-12 cubre el mensaje claro y el no-crash. **Decisión del usuario**: ¿se documenta y ya, o se quiere además un aviso visible en la UI? |
| **D-5** | iOS podría necesitar `CFBundleURLTypes` / `REVERSED_CLIENT_ID` en `Info.plist`. | **Inferencia, no verificada**: `signInWithCredential` con un `idToken` de Apple no usa flujos web, así que no debería hacer falta. Se comprueba en el smoke de AC-13. |
| **D-6** | La cobertura automática baja respecto a hoy (ver Notas no funcionales). | Propuesta: **aceptar** y dejarlo escrito. Se compensa con AC-13. |

### Evidencia de T1 (D-1 cerrado) — 2026-07-28

Ejecutado sobre `feature/003-gitlive-auth-migration` con `dev.gitlive:firebase-auth:2.5.0` declarado
en `commonMain` y **sin una línea de lógica**:

| Comando | Resultado |
|---|---|
| `ktlintCheck` + `:shared:testAndroid` + `:androidApp:assembleDebug` + `:shared:compileKotlinIosSimulatorArm64` | ✅ `BUILD SUCCESSFUL in 3m 44s` |
| `:shared:linkDebugFrameworkIosSimulatorArm64` | ✅ `BUILD SUCCESSFUL in 47s` — el linkage de Kotlin/Native, que era el riesgo real |
| `xcodebuild` sobre el simulador, **sin** el producto SPM `FirebaseAuth` | ✅ `BUILD SUCCEEDED` |

⚠️ **Ese último resultado es una trampa, no una buena noticia.** El klib de cinterop de GitLive
declara `linkerOpts = -framework FirebaseAuth`, pero mientras ningún símbolo de `firebase-auth` se
referencie desde el código, el klib no entra en el binario y el linker nunca pide el framework. Es
decir: **T7 no deja de ser necesario; simplemente su ausencia no se manifiesta todavía.** El
`xcodebuild` empezará a fallar con `ld: framework 'FirebaseAuth' not found` en cuanto T4 implemente el
adaptador. Consecuencias prácticas:

- El commit de T1 se puede integrar sin romper iOS a nadie.
- **AC-03 no es verificable hasta después de T4.** Un `xcodebuild` verde entre T1 y T4 **no** prueba
  que el producto SPM esté añadido.

Resolución real observada (no asumida), `:shared:dependencies`:

```
androidCompileClasspath        → dev.gitlive:firebase-auth-android:2.5.0
metadataIosMainCompileClasspath → dev.gitlive:firebase-auth:2.4.0 -> 2.5.0
```

Esa flecha es el transitivo de BaseLogin siendo elevado por Gradle a la versión declarada: el
desalineamiento **se cierra solo**, sin `strictly` ni `force`, y sin necesidad del fallback a 2.4.0.

## Trazabilidad

| AC | Test(s) que lo cubren | ¿Rojo antes de implementar? |
|----|-----------------------|------------------------------|
| AC-01 | *(no unitario)* — `ktlintCheck`, `:shared:testAndroid`, `:androidApp:assembleDebug`, `:shared:compileKotlinIosSimulatorArm64` + grep de `io.ktor` | n/a — verificable por build |
| AC-02 | *(no unitario)* — inspección del árbol + greps de `identitytoolkit`, `securetoken`, `TokenManager` | n/a — verificable por build |
| AC-03 | *(no unitario)* — `xcodebuild` sobre el simulador | n/a — verificable por build |
| AC-04 | `FledgeGitLiveAuthProviderTest` · `` `FLE-88 sign up creates session through the sdk` `` | sí — pendiente de `/test` |
| AC-05 | `FledgeGitLiveAuthProviderTest` · `` `FLE-88 email password sign in delegates to the sdk` `` | sí — pendiente de `/test` |
| AC-06 | `FledgeGitLiveAuthProviderTest` · `` `FLE-88 google sign in forwards the id token as a credential` `` | sí — pendiente de `/test` |
| AC-07 | `FledgeGitLiveAuthProviderTest` · `` `FLE-88 apple sign in forwards id token and raw nonce` `` | sí — pendiente de `/test` |
| AC-08 | `FledgeGitLiveAuthProviderTest` · `` `FLE-88 cancelled social sign in never touches the sdk` `` | sí — pendiente de `/test` |
| AC-09 | `FledgeGitLiveAuthProviderTest` + `FirebaseAuthErrorClassifierTest` | sí — pendiente de `/test` |
| AC-10 | `FledgeGitLiveAuthProviderTest` · `` `FLE-88 get current session reads the cache without io` `` | sí — pendiente de `/test` |
| AC-11 | `AppModulesTest` · `` `FLE-88 koin graph resolves auth provider without touching the sdk` `` | sí — pendiente de `/test` |
| AC-12 | `FledgeGitLiveAuthProviderTest` · `` `FLE-88 sign in without firebase config fails with a clear message` `` | sí — pendiente de `/test` |
| AC-13 | *(no unitario)* — smoke manual en emulador Android y simulador iOS, con sonda temporal no commiteada | n/a — verificable por smoke |

> Nota para `/validate`: los ACs marcados «n/a» **no** deben bloquear el gate de trazabilidad por
> falta de test rojo previo; se validan con el comando indicado en la misma fila.
