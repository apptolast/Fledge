# Plan 021: Store metadata Families (FLE-46)

## Enfoque Tecnico

FLE-46 es un entregable de documentacion operativa, no de runtime. La metadata vivira en
`docs/STORE_METADATA_FAMILIES.md` para que App Store Connect/Play Console tengan un texto unico y
versionado antes de cargar la ficha.

Decisiones:

- Usar copy conservador: `paga virtual`, `registro familiar`, `educacion financiera practica`; evitar
  claims de banco/neobanco/inversion/pagos.
- Declarar datos de forma conservadora aunque algunos campos sean virtuales o internos.
- Separar "metadata lista" de "pre-submit blockers": la ficha puede estar preparada aunque no se deba
  enviar todavia.
- Crear `FLE-95` para account/data deletion, descubierto como requisito de store sin ticket previo.
- Citar fuentes oficiales actuales de Apple/Google revisadas el 2026-07-30.
- Mantener Pencil como N/A porque no se anaden pantallas ni estados visuales.

## Ficheros A Tocar

- `specs/021-store-metadata-families/spec.md`
  - Spec SDD con AC y trazabilidad.
- `specs/021-store-metadata-families/plan.md`
  - Este plan.
- `.claude/.sdd-state.json`
  - Estado SDD de la tarea activa.
- `docs/STORE_METADATA_FAMILIES.md`
  - Entregable principal para consola/review.

No hay source-set KMP afectado:

- `commonMain`: N/A.
- `commonTest`: N/A, no se introduce contrato ejecutable.
- `androidMain` / `iosMain`: N/A.
- Koin/DI/expect-actual: N/A.
- i18n de app: N/A, los textos son metadata de tienda, no strings runtime.

## Impacto En Plataformas

- Android/Google Play:
  - Target audience incluye familias y menores si se declara audiencia infantil.
  - Data safety debe declarar datos personales, actividad familiar y tokens/identificadores si aplican.
  - No ads ni SDKs no aprobados para apps con menores.
  - Account deletion es dependencia pre-submit.
- iOS/App Store:
  - App Privacy debe declarar datos enlazados a cuenta/familia.
  - Si se activa Kids Category, enlaces/compras/distracciones deben estar tras parental gate y no puede
    haber terceros de ads/analytics no permitidos.
  - Si no se activa Kids Category, evitar metadata tipo "for kids/for children" que implique audiencia
    infantil principal.

## Tareas

1. [x] Preparar rama `feature/FLE-46-store-metadata-families` y mover Jira a In Progress.
2. [x] Crear spec/plan SDD. AC-01..AC-06.
3. [x] Marcar design-check/Pencil como N/A en el spec. AC-02, AC-03.
4. [ ] Crear `docs/STORE_METADATA_FAMILIES.md` con:
   - copy ES/EN; AC-05.
   - disclaimers de dinero virtual; AC-04.
   - mapa privacy labels/data safety; AC-01.
   - review notes; AC-03.
   - checklist y bloqueos, incluido `FLE-95`; AC-06.
5. [ ] Validar contenido con `rg`, Gradle y `git diff --check`.
6. [ ] Comentar Jira, crear PR contra `develop`, pasar a In Review.
7. [ ] Squash merge tras validacion local y pasar FLE-46 a Done.

## Validacion

No se crean tests Kotlin nuevos porque no hay comportamiento ejecutable. La validacion sera:

- Barrido de placeholders Compose para evitar la regresion `%d%%`.
- Barrido de contenido obligatorio de metadata.
- Suite KMP existente para confirmar que la documentacion no entra con cambios de runtime rotos.
- `git diff --check`.
