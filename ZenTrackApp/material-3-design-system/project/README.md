# Material 3 Design System

A complete recreation of Google's **Material Design 3** (Material You) design system as a self-contained design folder for prototyping and production work.

> Source: [hamen/material-3-skill](https://github.com/hamen/material-3-skill) (master) — distilled from [m3.material.io](https://m3.material.io/), [Android Developers · Compose Material 3](https://developer.android.com/develop/ui/compose/designsystems/material3), and [@material/web](https://github.com/material-components/material-web).

Material 3 is **personal** (dynamic color), **adaptive** (responsive across 5 window-size classes), and **expressive** (spring motion, shape morphing, emphasized typography). The May 2025 *M3 Expressive* update added bouncier motion, larger corner radii, new button sizes, and emphasized type — primarily on Compose; Material Web is in maintenance mode and does **not** ship Expressive.

---

## Index

- `README.md` — this file
- `colors_and_type.css` — all CSS custom properties (`--md-sys-*`) — color (light + dark), type scale, shape, elevation, motion, spacing
- `assets/m3-hero.png` — official Material 3 hero image
- `preview/` — design-system showcase cards (color, type, components, Expressive)
- `ui_kits/web/` — interactive web UI kit using `@material/web` + tokens
- `SKILL.md` — agent skill manifest (drop this folder into `~/.claude/skills/material-3-design`)

### Component coverage (preview/)

**Foundations** — type (display, body), colors (accent, surface, dark), elevation, shape scale, spacing, motion, state layers, iconography, dynamic color seed picker.

**Navigation** — top app bar, navigation bar, navigation rail, navigation drawer.

**Canonical layouts** — feed (multi-column grid), list-detail, supporting pane.

**Actions** — buttons (all 5 variants), button sizes XS–XL (Expressive), button group, split button, toggle buttons, FABs + icon buttons.

**Inputs** — text fields, chips, checkbox, radio + segmented buttons, slider, switch, date picker, time picker, search bar.

**Containment** — cards, dialog, bottom sheet, side sheet, carousel, divider.

**Communication** — snackbar, banner, progress (linear + circular), loading indicator (shape-morphing Expressive), tooltip + badge.

**Navigation sub-patterns** — tabs (primary + secondary).

**M3 Expressive (May 2025)** — button sizes (XS → XL), button group (standard + connected), split button, toggle buttons, FAB menu, shape-morphing loading indicator, floating toolbar, docked toolbar (replaces bottom app bar). Tokens for spring motion (`--md-sys-motion-spring-*`) and emphasized weights (`--md-sys-typescale-emphasized-*-weight`) live in `colors_and_type.css`.

**Icons** — full Material Symbols browser (3,500+ icons, all 3 styles, all 4 variable axes, searchable by name + category).

> Expressive is primarily a Compose-first update. Web approximations use longer cubic-bezier easings to mimic spring overshoot, and CSS transitions on `border-radius` for shape morph.

### Reference documents (references/)

Full spec references imported from `hamen/material-3-skill`:

- `references/color-system.md` — Color roles, tonal palettes, dynamic color, CSS + Compose mapping
- `references/typography-and-shape.md` — Type scale, shape corners, elevation, motion, Expressive notes
- `references/component-catalog.md` — All 30+ components with web + Compose code examples
- `references/navigation-patterns.md` — Navigation decision tree, responsive shell, Compose adaptive patterns
- `references/layout-and-responsive.md` — Breakpoints, canonical layouts, foldables, large-screen guidance
- `references/theming-and-dynamic-color.md` — Theming with Compose, Flutter, web; dynamic color; scoped themes

---

## Content fundamentals

Material's reference docs read like an engineering guide written for designers — **calm, declarative, and exact**. The voice is product-neutral; the system describes itself, not Google.

- **Tone**: Plain, instructional, slightly clinical. No marketing flourishes. Sentences are short and active.
- **Person**: Third-person and imperative. Speaks about *components*, not *you*. ("The FAB shows a primary action.") Use *you* only in tutorials.
- **Casing**: **Sentence case** everywhere — buttons, headings, dialogs, menu items. Avoid Title Case. Capitalize only proper nouns and the first word.
- **Naming**: kebab-case tokens (`primary-container`), camelCase APIs (`MaterialTheme`, `colorScheme`), PascalCase components (`FilledButton`).
- **Numbers & units**: dp/sp on Android, px/rem on web, written without spaces (`16dp`, `1rem`).
- **Emoji**: Not used in the spec or default UI. Material Symbols (icon font) carries all glyphic communication.
- **Specific examples**:
  - ✅ "Buttons help people initiate actions."
  - ✅ "Use `outline-variant` for dividers."
  - ❌ "Get started in seconds with our beautiful, modern buttons! 🚀"

---

## Visual foundations

### Colors
- Built from a **single seed color** that generates 5 tonal palettes (Primary, Secondary, Tertiary, Neutral, Neutral-Variant).
- Always use roles, never raw hex: `var(--md-sys-color-primary)`, not `#6750A4`.
- Pair only by intention — `primary` + `on-primary`, `surface-container` + `on-surface`. Mismatched pairs break contrast under dynamic color and high-contrast modes.
- Three contrast levels: standard / medium / high.
- **Vibe**: Soft, slightly desaturated, hue-cohesive. Backgrounds are tinted (e.g. light surface is `#FEF7FF`, not pure white). Dark theme is true dark with violet undertones.

### Typography
- Default typeface: **Roboto** / **Roboto Flex** (variable). Roboto Flex supports `wght 100–1000`, `wdth 25–151`, `opsz 8–144`.
- 15-step scale across 5 categories (Display / Headline / Title / Body / Label) × 3 sizes (L / M / S).
- 15 *emphasized* variants for active/important content (Expressive update).
- Two typeface roles: **brand** (Display + Headline) and **plain** (Title + Body + Label). Both default to Roboto Flex.

### Shape
- **Fully rounded by default.** Buttons and chips use `corner-full` (9999px), cards `medium` (12dp), dialogs `extra-large` (28dp).
- 10-step radius scale. Expressive adds `large-increased` (20dp), `extra-large-increased` (32dp), `extra-extra-large` (48dp).
- Shapes can morph on press in Compose; on web approximate with CSS transitions on `border-radius`.

### Elevation
- **Tonal first, shadow second.** Higher elevation = lighter surface tone, not a dropped shadow. The 5 surface-container roles are the elevation scale.
- Shadows only when a component floats over busy content (FAB over images, dialog scrim).
- 6 levels (0–5). Hover/focus increases by one level.

### Backgrounds & imagery
- No hand-drawn illustrations or repeating patterns by default. Material is a chrome system, not a brand layer.
- No gradients in the chrome. Tonal surfaces do all the work.
- Photography is product-supplied; the system stays neutral.

### Motion
- **Spring physics** for component interactions (Expressive — Compose primary).
- **Easing + duration** for transitions (enter/exit/shared-axis): Emphasized (`cubic-bezier(0.2, 0, 0, 1)`, 500ms), Emphasized Decelerate (entering, 400ms), Emphasized Accelerate (exiting, 200ms).
- No bounces in chrome unless using Expressive springs deliberately. No fades-only — Material favors transforms.

### Hover, press, focus
- All interactives carry a **state layer** — a translucent overlay of the role's "on" color over the component:
  - Hover: 8% opacity
  - Focus: 12%
  - Pressed: 12% (with a ripple in Compose)
  - Dragged: 16%
- No darken / lighten / shrink on press by default. The state layer is the signal.

### Borders & outlines
- `outline` (≥ 3:1 contrast) for interactive boundaries — text fields, outlined buttons, outlined cards.
- `outline-variant` (decorative) for dividers and card frames.
- 1dp borders are the convention. Never use color-only top/left "accent borders" — that's a non-Material trope.

### Cards
- **Three variants**: filled (`surface-container-highest`), elevated (`surface-container-low` + level-1 shadow), outlined (`surface` + `outline-variant` border).
- Corner radius: `medium` (12dp). Padding: 16dp. Title: Title Medium. Body: Body Medium.

### Transparency, blur, scrim
- **Scrim**: `rgba(0,0,0,0.32)` (32% black) behind modals and side sheets.
- Blur is not a primary Material motif — used sparingly behind nav bars on Android only.
- Opacity tokens: 0.08 (hover), 0.12 (focus/pressed), 0.16 (dragged), 0.38 (disabled content), 0.12 (disabled container).

### Layout
- **Window size classes**: Compact (<600dp), Medium (600–839), Expanded (840–1199), Large (1200–1599), Extra-large (≥1600).
- Navigation morphs by class: bottom nav bar (Compact) → nav rail (Medium/Expanded) → nav drawer (Large+).
- Body content has a **max readable width** (~840–1040dp) on Large+ screens.
- 4dp base grid, 8/16/24dp the most common spacings.

### Iconography
- **Material Symbols** is the canonical icon system — variable icon font with axes for `weight` (100–700), `fill` (0/1), `grade` (-50…200), and `optical-size` (20–48).
- Three styles: **Outlined** (default), **Rounded**, **Sharp**. This system uses Outlined.
- Loaded via Google Fonts CDN: `https://fonts.googleapis.com/icon?family=Material+Symbols+Outlined`.
- **No emoji.** No PNG icons. No bespoke SVG illustrations. If you need a glyph, it's in Material Symbols.
- Standard icon size: 24dp. Touch target: 48dp.

---

## Iconography

This system uses **Material Symbols Outlined** loaded from the Google Fonts CDN. It carries 3,500+ icons covering navigation, actions, communication, content, devices, and more. Use the icon name as text content:

```html
<span class="material-symbols-outlined">home</span>
<span class="material-symbols-outlined">search</span>
<span class="material-symbols-outlined">favorite</span>
```

Adjust style with font-variation-settings:

```css
.icon-filled { font-variation-settings: 'FILL' 1; }
.icon-bold   { font-variation-settings: 'wght' 700; }
.icon-large  { font-size: 40px; font-variation-settings: 'opsz' 40; }
```

No PNGs, no SVGs, no emoji. If a glyph isn't in Material Symbols, request it — don't substitute.

---

## Substitutions & caveats

- **Fonts**: Roboto Flex and Roboto are loaded from Google Fonts (no local TTFs). Material Symbols is loaded from the Google icon font CDN. If offline-bundling is needed, host the woff2 files locally.
- **No Compose / Flutter UI kits** — this folder targets web. Compose and Flutter remain the primary platforms in the spec; the SKILL.md contains pointers.
- **No custom seed**. The shipped palette is the default baseline (`#6750A4`). For brand seeds, regenerate with `@material/material-color-utilities` and overwrite the `:root` block in `colors_and_type.css`.

---

## License

The Material 3 spec is © Google and published under the [Material guidelines](https://m3.material.io/). The skill source repo (hamen/material-3-skill) is MIT.
