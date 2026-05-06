# DESIGN.md — ZenTrack Design System

Design system adoptado: **Material Design 3 (Material You)**.  
Fuente canónica: `material-3-design-system/project/` (zip extraído en la raíz del monorepo) — consúltala para tokens, componentes y patrones completos.

**Referencia visual de layout**: [Linear.app](https://linear.app) — la estructura de navegación y la disposición de paneles de ZenTrack se inspiran directamente en Linear. La estética visual (colores, formas, tipografía, motion) es 100% Material 3.

---

## Plataformas y stack UI

| Plataforma | Librería | Estado M3 |
|---|---|---|
| Android | `androidx.compose.material3` | M3 nativo completo (incluyendo M3 Expressive en BOM ≥ 1.3) |
| Web | MUI v5 (`@mui/material`) con tema M3 custom | M3 parcial vía custom theme — no instalar `@material/web` |

**La fuente de verdad del diseño es Jetpack Compose.** La web aproxima los tokens M3 mediante el sistema de theming de MUI.

---

## Paleta base y seed color

- **Seed color**: `#5B6CF9` (índigo perceptivo — calmo, productivo, tecnológico).
- Genera el esquema completo con [Material Theme Builder](https://material-foundation.github.io/material-theme-builder/) usando ese seed.
- El color seed se aplica en ambas plataformas:
  - Android: `lightColorScheme` / `darkColorScheme` generado por Theme Builder → `ZenTrackTheme.kt`
  - Web: exportar el CSS de Theme Builder → sobreescribir el `palette` del theme de MUI

**PROHIBIDO** usar `#5B6CF9` directamente en ningún componente. El color solo existe como seed del que se derivan los roles semánticos (`primary`, `secondary`, `tertiary`, etc.).

### Roles de color más usados en ZenTrack

| Rol M3 | Uso en ZenTrack |
|---|---|
| `primary` / `on-primary` | Acciones principales (crear tarea, confirmar) |
| `primary-container` / `on-primary-container` | FAB, chips de estado activo, badge seleccionado |
| `secondary-container` / `on-secondary-container` | Indicador de ítem activo en la navegación |
| `surface` / `on-surface` | Fondo de pantallas y texto principal |
| `surface-container` | Fondo de tarjetas de tarea, columnas Kanban |
| `surface-container-high` | Cabeceras de sección, app bar elevado |
| `error` / `on-error` | Tareas bloqueadas, errores de formulario |
| `tertiary-container` | Etiquetas de sprint, badges de prioridad alta |
| `outline-variant` | Bordes de columnas Kanban, divisores de lista |

---

## Tipografía

- **Typeface**: Roboto Flex (variable) — cargado desde Google Fonts.
- No sustituir por otra fuente salvo decisión explícita de branding.
- Escala de uso en ZenTrack:

| Estilo M3 | Dónde usarlo |
|---|---|
| `Display S / M` | Pantallas de bienvenida, títulos hero |
| `Headline M` | Nombre del workspace activo, títulos de sprint |
| `Title L` | Nombre de proyecto en el board |
| `Title M` | Título de tarea en `TaskDetail`, cabecera de columna |
| `Title S` | Nombres de sección en la navegación |
| `Body L` | Descripción de tarea |
| `Body M` | Texto secundario de tarjetas, comentarios |
| `Label L` | Etiquetas de campo (estado, prioridad, sprint) |
| `Label M` | Chips, badges, contadores |
| `Label S` | Timestamps, metadatos compactos |

---

## Shape

- Respeta la escala de radios M3 sin hardcodear valores:
  - Botones y chips: `corner-full` (9999px / `ShapeDefaults.Full`)
  - Tarjetas de tarea: `corner-medium` (12dp)
  - Diálogos y bottom sheets: `corner-extra-large` (28dp)
  - Task Detail panel lateral: `corner-large` (16dp)
- **PROHIBIDO** `border-radius: 8px` o similar — usar tokens de shape.

---

## Elevation

- Usa **tonal elevation** (superficie más clara), no sombras, como regla general.
- Las sombras solo se usan en elementos flotantes sobre contenido denso: FAB sobre la lista de tareas, diálogos con scrim.
- Orden de profundidad en ZenTrack:
  1. Fondo de app → `surface`
  2. Columnas Kanban → `surface-container`
  3. Tarjetas de tarea → `surface-container-high` (elevated dentro de la columna)
  4. App bar scrolled → `surface-container-high` (elevation 2)
  5. FAB, diálogos → shadow real (elevation 3)

---

## Navegación

### Android

| Window size class | Componente | Destinos |
|---|---|---|
| Compact (< 600dp) | `NavigationBar` (bottom) | Board, Backlog, Sprints, Perfil |
| Medium (600–839dp) | `NavigationRail` (lateral) | Ídem + FAB "Nueva tarea" en el rail |
| Expanded (≥ 840dp) | `PermanentNavigationDrawer` | Ídem; muestra labels completas |

```kotlin
// Estructura obligatoria con Scaffold
Scaffold(
    bottomBar = {
        if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
            ZenTrackNavBar(currentRoute, onNavigate)
        }
    },
    floatingActionButton = { NewTaskFab(onClick) }
) { padding ->
    ZenTrackNavHost(navController, Modifier.padding(padding))
}
```

- El `NavHost` es el único responsable del contenido de pantalla. **PROHIBIDO** gestionar el backstack fuera de `NavController`.
- Las rutas son `sealed object` en `navigation/AppDestination.kt`.

### Web (MUI)

| Viewport | Componente MUI | Equivalente M3 |
|---|---|---|
| < 600px (Compact) | `BottomNavigation` | Navigation Bar |
| 600–839px (Medium) | `Drawer` permanente compacto (solo iconos) | Navigation Rail |
| ≥ 840px (Expanded) | `Drawer` permanente completo (iconos + labels) | Navigation Drawer |

- **PROHIBIDO** `Tabs` como navegación principal entre secciones; solo para sub-secciones dentro de una pantalla (ej. "Activas" / "Archivadas" en Sprints).
- El workspace activo siempre es visible en la cabecera del drawer / en el `AppBar` en compact.

---

## Layout de la aplicación — inspirado en Linear

ZenTrack usa la misma estructura de 2–3 columnas que Linear: sidebar de navegación global a la izquierda, panel de contenido principal, y panel de detalle opcional a la derecha. El sistema M3 de window size classes determina qué paneles son visibles y en qué forma.

### Estructura de paneles (Expanded ≥ 840dp)

```
┌──────────────┬──────────────────────────────┬───────────────────┐
│  Nav Drawer  │     Panel de contenido        │  Panel de detalle │
│   (256dp)    │   (flex, mínimo 400dp)        │  (360–480dp)      │
│              │                               │  (solo si abierto)│
│ [Logo/WS]    │  ┌──────────────────────────┐ │                   │
│              │  │  Top App Bar (64dp)       │ │  TaskDetail       │
│  My Issues   │  └──────────────────────────┘ │  (campos, desc,   │
│  Inbox       │                               │   comentarios,    │
│  ─────────   │  [Board / Backlog / Sprint]   │   actividad)      │
│  Projects    │                               │                   │
│    ZTK ›     │                               │                   │
│    PROJ2 ›   │                               │                   │
│  ─────────   │                               │                   │
│  Members     │                               │                   │
│  Settings    │                               │                   │
└──────────────┴──────────────────────────────┴───────────────────┘
```

### Estructura de paneles (Medium 600–839dp)

```
┌────────┬──────────────────────────────────────┐
│  Rail  │         Panel de contenido             │
│ (80dp) │                                        │
│        │  Top App Bar                           │
│  [◉]   │  ─────────────────────────────────    │
│  [☰]   │  [Board / Backlog / Sprint]            │
│  [⚡]  │                                        │
│  [👤]  │  (TaskDetail: navega a nueva pantalla) │
└────────┴──────────────────────────────────────┘
```

### Estructura de paneles (Compact < 600dp)

```
┌──────────────────────────────────────────────┐
│  Top App Bar (workspace name + menú)         │
├──────────────────────────────────────────────┤
│                                              │
│  [Board / Backlog / Sprint / TaskDetail]     │
│  (pantalla completa, navegación por stack)   │
│                                              │
├──────────────────────────────────────────────┤
│  Navigation Bar (bottom)                     │
│  Issues | Backlog | Sprints | Perfil         │
└──────────────────────────────────────────────┘
```

### Navigation Drawer — estructura interna (inspirada en Linear)

```
┌──────────────────────────┐
│ [Avatar] Workspace name  │  ← WorkspaceSwitcher (clickable, abre selector)
│           ▾              │
├──────────────────────────┤
│ 🔍 Search                │  ← Atajo de búsqueda global (Cmd+K)
├──────────────────────────┤
│ MY WORK                  │  ← Section header (Label S, on-surface-variant)
│   ○ My Issues            │
│   ○ Inbox          (3)   │  ← Badge con counter
├──────────────────────────┤
│ PROJECTS                 │
│   › ZTK — ZenTrack       │  ← Expandible; sub-ítems: Board, Backlog, Cycles
│   › PROJ2                │
│   + New Project          │
├──────────────────────────┤
│ VIEWS                    │
│   ○ All Issues           │
│   ○ Active Sprint        │
├──────────────────────────┤  ← Push to bottom
│ ⚙ Settings               │
│ ? Help                   │
└──────────────────────────┘
```

Reglas del drawer:
- El ítem activo usa `secondary-container` de fondo + `on-secondary-container` para texto e icono.
- Los section headers son `Label S` en `on-surface-variant`. **No** son clicables.
- Los projects son expandibles (accordion). Su estado expand/collapse persiste en el store.
- El workspace switcher en la cabecera abre un `ModalBottomSheet` (Android) o un `Popover`/`Menu` (web) con la lista de workspaces disponibles y la opción de crear uno nuevo.
- **PROHIBIDO** mostrar más de dos niveles de anidamiento en el drawer.

### Panel de detalle de tarea (`TaskDetail`)

- En **Expanded**: se abre como panel lateral derecho (no navega), con ancho fijo de 360–480dp. El panel de contenido se comprime. Implementar con `ListDetailPaneScaffold` en Compose o con un segundo `Drawer` anclado a la derecha en web.
- En **Medium y Compact**: navega a una pantalla full-screen (`TaskDetailScreen`).
- El panel tiene su propio scroll independiente.
- Al cerrar el panel, la selección en la lista se deselecciona.

### Kanban Board (vista Board)

```
┌──────────────────────────────────────────────────────────────────┐
│  Top App Bar: [← Proyecto ZTK]  [Vista: Board ▾]  [Filtros]  [+]│
├──────────┬──────────┬──────────┬──────────────────────────────────┤
│  TODO    │ IN PROG  │  DONE    │  (columnas configurables)        │
│   (4)    │   (2)    │   (7)    │                                  │
├──────────┼──────────┼──────────┼──────────────────────────────────┤
│ TaskCard │ TaskCard │ TaskCard │                                  │
│ TaskCard │ TaskCard │ TaskCard │                                  │
│ TaskCard │          │ TaskCard │  ← scroll vertical por columna   │
│    +     │    +     │    +     │  ← "Add task" inline             │
└──────────┴──────────┴──────────┴──────────────────────────────────┘
```

- Las columnas hacen scroll vertical **independiente** dentro del board (que hace scroll horizontal).
- El board hace scroll horizontal cuando hay más columnas que el ancho visible.
- En Compact, el board muestra **una sola columna** a la vez con tabs para cambiar entre estados.

### Backlog (vista Lista — estilo Linear)

```
┌─────────────────────────────────────────────────────────────────┐
│  Top App Bar: [← ZTK]  [Vista: Backlog ▾]  [Agrupar ▾]  [+]   │
├─────────────────────────────────────────────────────────────────┤
│  ▾ IN PROGRESS  (2)                    [progreso sprint ██░░]  │
│    ZTK-4  Implement JWT auth           [avatar] [prioridad]     │
│    ZTK-7  Add webhook endpoint         [avatar] [prioridad]     │
├─────────────────────────────────────────────────────────────────┤
│  ▾ TODO  (5)                                                    │
│    ZTK-1  Setup PostgreSQL schema      [avatar] [prioridad]     │
│    ZTK-2  Create workspace endpoint    [avatar] [prioridad]     │
│    ...                                                          │
├─────────────────────────────────────────────────────────────────┤
│  ▸ DONE  (12)  ← colapsado por defecto                         │
└─────────────────────────────────────────────────────────────────┘
```

- Las filas de tarea son `ListItem` (M3) con `one-line` o `two-line` según la densidad configurada.
- Las cabeceras de grupo son sticky (`StickyHeader` en Compose / `position: sticky` en web).
- La sección "Done" se colapsa por defecto.

---

## Componentes M3 → ZenTrack

### Tarjeta de tarea (`TaskCard`)

- **Variante**: `Outlined` (border `outline-variant`, fondo `surface`).
- En Kanban usa `Filled` (`surface-container-highest`) para destacar sobre el fondo de columna.
- Contenido: título (`Title S`), ID (`ZTK-N`, `Label M`, `on-surface-variant`), estado chip, avatar del asignado.
- **No** añadir shadow propia — la columna ya eleva las tarjetas mediante tono.

### Columna Kanban

- Fondo: `surface-container` (un nivel sobre el fondo de pantalla).
- Cabecera: `Title M` + counter badge (`Label M`, `primary-container`).
- Scroll interno vertical independiente.

### Chip de estado (`StatusChip`)

- **Variante**: `FilterChip` (toggle) en el board; `AssistChip` (read-only) en TaskDetail.
- Color de fondo: mapear cada estado a un rol M3:
  - `Todo` → `surface-container-high`
  - `In Progress` → `primary-container`
  - `Done` → `secondary-container`
  - `Blocked` → `error-container`
- **PROHIBIDO** hardcodear colores por nombre de estado; los estados son configurables por workspace.

### FAB — "Nueva tarea"

- **Variante**: `ExtendedFAB` cuando el usuario no ha hecho scroll; colapsa a `SmallFAB` al bajar.
- Color: `primary-container` (Compose) / `theme.palette.primaryContainer` (MUI).
- Posición: `end-bottom` del `Scaffold`. No mover a la cabecera.

### Formulario de tarea (crear / editar)

- **Android**: `ModalBottomSheet` con campos `OutlinedTextField` (M3).
- **Web**: `Dialog` (MUI) o panel lateral (`Drawer` modal) para pantallas ≥ 840px.
- Campos siempre con `Outlined` variant, nunca `Filled` en formularios modales.

### Prioridad

- Representar con **`Badge`** de color sobre el icono, o con **`Icon` + `Label S`** en la tarjeta.
  - Urgente → `error`
  - Alta → `tertiary`
  - Media → `primary` (tono suave)
  - Baja → `on-surface-variant`
- **PROHIBIDO** usar emojis para indicar prioridad.

### Sprint header

- `ListSubheader` (web) / `Text(Title M)` dentro de `StickyHeader` (Compose) con fondo `surface-container-high`.
- Incluye nombre, fechas y barra de progreso lineal (`LinearProgressIndicator`, M3).

---

## Modo oscuro

- Ambas plataformas **deben** soportar modo claro y oscuro desde el inicio.
- Android: `isSystemInDarkTheme()` → selecciona `darkColorScheme`; soporta dynamic color en API ≥ 31.
- Web: `useMediaQuery('(prefers-color-scheme: dark)')` → conmuta entre los dos objetos `createTheme` (light / dark).
- **PROHIBIDO** pantallas con fondo negro puro (`#000000`); usar `surface` del tema oscuro (ej. `#141218` para el seed elegido).

---

## Iconografía

- **Material Symbols Outlined** en todas las plataformas:
  - Android: paquete `androidx.compose.material.icons` (set Extended para íconos adicionales).
  - Web: cargado via `@fontsource/material-symbols-outlined` o Google Fonts CDN.
- Tamaño estándar: **24dp**. Touch target mínimo: **48dp**.
- **PROHIBIDO** PNGs, SVGs bespoke ni emojis como iconos funcionales.

---

## Reglas transversales

- **PROHIBIDO** colores hardcodeados en cualquier componente: ni hex, ni `Color(0xFF...)`, ni `#` en CSS.  
  Usa siempre roles semánticos (`MaterialTheme.colorScheme.primary`, `theme.palette.primary.main`).
- **PROHIBIDO** `elevation` con sombra para comunicar jerarquía en pantallas planas; usa tonal surfaces.
- **SIEMPRE** testa el contraste de cada par color/texto: ≥ 4.5:1 para texto normal, ≥ 3:1 para texto grande e iconos.
- **SIEMPRE** provee `contentDescription` en imágenes e iconos funcionales (Android) / `aria-label` (web).
- **PROHIBIDO** animar propiedades que no sean `transform`, `opacity` o `border-radius` (para shape morph) salvo justificación explícita.
- Los estados interactivos (hover, focus, pressed) se comunican **exclusivamente** con state layers M3 (8% / 12% / 12% del color `on-X`). **PROHIBIDO** cambiar el color de fondo del componente completo en hover.

---

## Referencias

| Recurso | Dónde |
|---|---|
| Tokens completos (CSS custom properties) | `material-3-design-system/project/colors_and_type.css` |
| Catálogo de componentes (Compose + web) | `material-3-design-system/project/references/component-catalog.md` |
| Patrones de navegación | `material-3-design-system/project/references/navigation-patterns.md` |
| Layout y responsive | `material-3-design-system/project/references/layout-and-responsive.md` |
| Theming y color dinámico | `material-3-design-system/project/references/theming-and-dynamic-color.md` |
| Skill M3 (reglas de implementación) | `material-3-design-system/project/SKILL.md` |
| Material Theme Builder (generar paleta) | https://material-foundation.github.io/material-theme-builder/ |
