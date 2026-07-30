# Spec 019: Pulir accesibilidad infantil (FLE-44)

> Rama: `feature/FLE-44-child-accessibility-polish` · Ticket: `FLE-44` · Parent: `FLE-41`

## Objetivo

FLE-44 mejora el modo hijo para que ninos de 6-9 anos puedan entender y ejecutar las acciones
principales con menos lectura: iconos reconocibles, acciones grandes, semantica accesible y textos
breves. El resultado debe mantener el flujo MVP existente, no anadir funcionalidades nuevas.

## Alcance

- Home hijo:
  - Balance principal/objetivo con lectura mas escaneable.
  - Tareas con icono de estado y acciones icono+texto.
  - Objetivo activo con acciones grandes para meter/sacar dinero.
  - Cash-out y zona de adulto con iconos claros.
  - Estados vacios de FLE-43 con iconografia y semantica.
- Semantica Compose para acciones interactivas del modo hijo:
  - `role = Button` donde aplique.
  - `contentDescription` o equivalente legible para TalkBack/VoiceOver.
  - Estados deshabilitados comprensibles.
- Contrato testeable en `commonMain/commonTest` que asegure iconografia, prioridad y tamano minimo.
- Strings en recursos Compose (`values/` espanol y `values-en/` ingles).
- Pencil actualizado con las variantes del modo hijo afectadas.

## Fuera De Alcance

- Fastlane, GitHub Actions y cualquier trabajo de `FLE-26` descartado para este sprint.
- Nuevas rutas, tabs reales, onboarding, animaciones avanzadas o celebraciones de logro (`FLE-94`).
- Cambios de backend, reglas Firestore, push o modelo de datos de ledger.
- E2E con TalkBack/VoiceOver en dispositivo real; queda como validacion manual recomendada.

## Diseno Pencil

- `hXwig` - `Screen / Home hijo - accessible active`.
  - Objetivo activo con icono de meta, progreso con texto, aviso de cantidad restante y acciones
    grandes `Meter`/`Sacar` con icono+texto.
  - CTA inferior `Pedir dinero` elevada a 60 px con icono visible.
- `gpMQ8` - `Screen / Home hijo - accessible empty`.
  - Estados vacios de tareas, meta y movimientos con iconos visibles y CTA de 58-60 px.
  - `Zona de padres` mantiene jerarquia secundaria, pero con icono y target grande.
- Validacion Pencil:
  - `snapshot_layout(problemsOnly=true)` sin problemas para `hXwig` y `gpMQ8`.
  - Exports revisados: `/tmp/fledge-fle44-accessibility/hXwig.png` y
    `/tmp/fledge-fle44-accessibility/gpMQ8.png`.

## Criterios De Aceptacion

### AC-01 - Acciones principales icon-first

Given un hijo con saldo, tareas y objetivo activo
When abre el home hijo
Then las acciones principales usan icono visible antes que texto
And el texto queda como etiqueta corta, no como unica pista visual.

### AC-02 - Targets infantiles grandes

Given cualquier accion interactiva del modo hijo
When se renderiza en telefono
Then las acciones principales tienen un area minima de 56dp
And las acciones secundarias mantienen al menos 48dp y separacion suficiente.

### AC-03 - Semantica accesible

Given TalkBack o VoiceOver esta activo
When el foco llega a una accion del modo hijo
Then la accion anuncia una descripcion comprensible
And se anuncia como boton si dispara una accion.

### AC-04 - Estados y progreso no dependen solo del color

Given una tarea o objetivo muestra estado/progreso
When el nino revisa la pantalla
Then ve icono o texto junto al color
And el significado se puede entender sin distinguir colores.

### AC-05 - Lectura minima y texto escalable

Given el sistema tiene texto grande configurado
When se abre el home hijo
Then los textos de acciones siguen pudiendo partir linea sin quedar recortados
And el contenido permanece en scroll vertical sin alturas rigidas que bloqueen la lectura.

## Trazabilidad

- AC-01: `ChildHomeAccessibilityTest` valida icono y etiqueta corta para acciones primarias.
- AC-02: `ChildHomeAccessibilityTest` valida 56dp primario / 48dp secundario en el contrato.
- AC-03: `ChildHomeAccessibilityTest` valida rol semantico de boton y descripcion accesible.
- AC-04: `ChildHomeAccessibilityTest` valida icono+texto en estados de tarea y progreso de objetivo.
- AC-05: `ChildHomeAccessibilityTest` valida que el contrato no fuerza alturas exactas.
- Validacion completa:
  - `./gradlew ktlintFormat --console=plain --no-configuration-cache`.
  - `./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`.
  - `git diff --check`.
