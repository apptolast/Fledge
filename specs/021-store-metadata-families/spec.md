# Spec 021: Preparar metadata de App Store/Google Play Families (FLE-46)

> Rama: `feature/FLE-46-store-metadata-families` · Ticket: `FLE-46` · Parent: `FLE-41`

## Objetivo

FLE-46 prepara la metadata de App Store y Google Play para reducir el riesgo de rechazo bajo
politicas de familias/menores. El entregable debe cubrir privacidad, parental gate, ausencia de
publicidad conductual y disclaimers claros de dinero virtual.

## Contexto

- Jira FLE-46 pide completar:
  - privacidad;
  - parental gate;
  - ausencia de publicidad conductual;
  - disclaimers de dinero virtual.
- Confluence `Fledge - MVP Scope` define el bucle: el padre asigna/valida, el hijo completa, el
  saldo sube y el hijo ahorra o cobra fuera de la app.
- Confluence `Fledge - Compliance and Risk` exige describir Fledge como herramienta de registro:
  no mueve, custodia, gestiona ni invierte dinero real.
- Apple App Review Guidelines, revisadas el 2026-07-30, indican que las apps en Kids Category deben
  reservar enlaces, compras y distracciones tras parental gate, y restringen analitica/adverts de
  terceros.
- Google Play, revisado el 2026-07-30, exige declarar audiencia objetivo y cumplir Families si la
  audiencia incluye menores; tambien exige Data safety y privacy policy coherentes.
- Al revisar FLE-46 se creo `FLE-95` para cubrir borrado de cuenta/datos, porque no existia tarea
  previa y es bloqueo pre-submit de store.

## Alcance

- Crear una guia versionada de metadata para App Store y Google Play con:
  - copy ES/EN listo para consola;
  - mapa App Privacy/Data safety conservador;
  - notas de review para Apple/Google;
  - disclaimers de dinero virtual;
  - checklist pre-submit;
  - dependencias que no debe ocultar la metadata.
- Dejar explicito que no hay publicidad conductual, tracking de terceros ni SDKs de analitica en la
  experiencia infantil.
- Mantener la metadata coherente con el modelo parent-as-bank sin presentar Fledge como neobanco.

## Fuera De Alcance

- Fastlane, GitHub Actions y cualquier trabajo de `FLE-76`.
- Subida real a App Store Connect o Play Console.
- Screenshots finales, ASO creativo, video preview o assets graficos.
- Politica de privacidad publicada en URL publica.
- Implementacion de borrado de cuenta/datos, cubierta por `FLE-95`.
- Cambios de UI dentro de la app.

## Diseno Pencil

- N/A: FLE-46 no toca pantallas de producto ni crea nuevos flujos de UI. La metadata define texto y
  checklist de consola; las screenshots finales se podran derivar de las pantallas ya disenadas.

## Criterios De Aceptacion

### AC-01 - Privacidad declarada de forma conservadora

Given la app permite cuenta de padre y perfiles infantiles
When se prepare la metadata de privacidad
Then App Store Privacy y Google Play Data safety declaran los datos enlazados a cuenta/familia
And incluyen email de padre, perfiles infantiles, actividad de tareas/ledger/objetivos, cash-out
virtual y tokens/identificadores de notificacion si aplican.

### AC-02 - Families/Kids sin tracking ni ads conductuales

Given la audiencia incluye familias y menores
When se prepare la metadata de consola y review notes
Then queda declarado que no hay publicidad conductual, tracking de terceros ni SDKs de analitica en
la experiencia infantil
And el texto evita promesas que atraigan menores si no se activa Kids Category en Apple.

### AC-03 - Parental gate visible para review

Given existen zona de padres, enlaces externos, compras futuras o ajustes protegidos
When el revisor lea la metadata
Then las review notes explican donde se aplica parental gate y como probarlo.

### AC-04 - Dinero virtual sin riesgo financiero

Given Fledge registra acuerdos familiares de paga, tareas y objetivos
When el usuario o revisor lea la ficha
Then la metadata afirma que la app no mueve, custodia, transfiere, invierte ni almacena dinero real
And que los pagos reales ocurren fuera de la app entre adulto y menor.

### AC-05 - Copy de tienda preparado

Given se necesita cargar App Store y Google Play
When se abra la guia de metadata
Then hay titulo/subtitulo, descripcion corta/larga, keywords, captions sugeridos y notas de review
en ES/EN sin claims financieros regulados.

### AC-06 - Checklist pre-submit con bloqueos trazables

Given hay dependencias que pueden causar rechazo aunque la metadata sea correcta
When se revise el checklist pre-submit
Then documenta que no se debe enviar a review sin privacy policy publica, `FLE-95`, reglas Firestore
endurecidas y validacion end-to-end.

## Trazabilidad

- AC-01 -> `docs/STORE_METADATA_FAMILIES.md` · secciones `Privacy Metadata Map` y `Google Play Data Safety`.
- AC-02 -> `docs/STORE_METADATA_FAMILIES.md` · secciones `Families/Kids Positioning` y `Ads, Tracking And SDKs`.
- AC-03 -> `docs/STORE_METADATA_FAMILIES.md` · seccion `Review Notes`.
- AC-04 -> `docs/STORE_METADATA_FAMILIES.md` · secciones `Virtual Money Disclaimer` y copy ES/EN.
- AC-05 -> `docs/STORE_METADATA_FAMILIES.md` · secciones `Store Listing Copy` y `Screenshot Captions`.
- AC-06 -> `docs/STORE_METADATA_FAMILIES.md` · seccion `Pre-Submit Checklist`.
- Validacion:
  - `rg -n "%(d|s|f)" shared/src/commonMain/composeResources`.
  - `rg -n "no mueve|no custody|no real money|Data safety|parental gate|FLE-83|FLE-95|account deletion" docs/STORE_METADATA_FAMILIES.md`.
  - `./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`.
  - `git diff --check`.
