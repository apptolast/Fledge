# Plan 018: Estados vacios accionables (FLE-43)

## Contexto

El MVP ya cubre crear familia, hijos, paga, tareas, aprobacion, objetivos, depositos, retiradas y
ledger. La UI actual muestra varios vacios como texto suelto, lo que deja rutas sin proximo paso claro
cuando una familia todavia no ha generado actividad.

## Diseno Tecnico

- Crear un componente comun de estado vacio accionable para secciones de home:
  - titulo, cuerpo y CTA opcional.
  - sin cards anidadas; cada vacio es un bloque de seccion con `MaterialTheme`.
  - touch targets de 48dp+ y textos con wrapping.
- `ParentHomeContent`:
  - Si `children.isEmpty()`, sustituir la CTA primaria `Nueva tarea` por un bloque de primer uso con
    CTA `Anadir hijo` y vacios informativos para tareas/retiradas.
  - Si existen hijos, mantener las acciones por hijo y hacer accionables las secciones sin aprobaciones,
    sin objetivo y sin retiradas.
  - Evitar que `SetupActionRow` muestre siempre `Parental gate` para acciones que no son parental gate.
- `ChildHomeContent`:
  - Mostrar vacios especificos para tareas, objetivo y ledger cuando no hay actividad.
  - La accion disponible para el hijo sera protegida y pasara por `FoundationAction.OpenParentZone`.
  - El boton de cash-out debe seguir deshabilitado si no hay saldo MAIN, pero el vacio explica por que.
- i18n:
  - Anadir claves en `values/strings.xml` y `values-en/strings.xml`.
  - No introducir textos hardcodeados en composables.
- Previews:
  - Anadir previews realistas para parent first-run, parent empty activity y child empty activity.

## Tareas

1. [x] Revisar estado actual del repo, Jira y pantallas Pencil.
2. [x] Crear variantes Pencil para los estados vacios de FLE-43.
3. [x] Escribir tests rojos de ViewModel/UI state y trazabilidad AC.
4. [x] Implementar componentes, strings y wiring de CTAs.
5. [x] Formatear, validar Gradle/iOS y abrir PR contra `develop`.

## Validacion Esperada

```bash
./gradlew :shared:testAndroid --console=plain --no-configuration-cache
./gradlew ktlintFormat --console=plain --no-configuration-cache
./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache
git diff --check
```

## Evidencia

- Pencil: `Screen / Home padre - empty first run` (`WOln9`) sin problemas de layout.
- Pencil: `Screen / Home padre - empty activity` (`h3xCv`) sin problemas de layout.
- Pencil: `Screen / Home hijo - empty activity` (`oYeza`) sin problemas de layout.
- Gate de diseno aprobado por el usuario antes de `/test`.
- Rojo esperado: `./gradlew :shared:testAndroid --console=plain --no-configuration-cache` fallo en
  `:shared:compileAndroidHostTest` por los contratos FLE-43 todavia no implementados.
- Verde: `./gradlew :shared:testAndroid --console=plain --no-configuration-cache`.
- Formato: `./gradlew ktlintFormat --console=plain --no-configuration-cache`.
- Validacion completa: `./gradlew ktlintCheck :shared:testAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --console=plain --no-configuration-cache`.
- Whitespace: `git diff --check`.
