# Material 3 — Web UI Kit

Interactive web components and screens built using `@material/web` (loaded via CDN) and CSS custom properties from `colors_and_type.css`.

## Files
- `index.html` — main demo with click-thru navigation between screens
- `Inbox.jsx` — email-style list view with top app bar, FAB, navigation drawer
- `Settings.jsx` — settings screen with switches, sliders, list items
- `Compose.jsx` — full-screen dialog / form with text fields, chips
- `Components.jsx` — shared low-level components (Button, IconBtn, Card, Chip, etc)

## Notes
This UI kit uses **only HTML + CSS + React** with the design tokens. The official `@material/web` library is referenced for component naming but the visual recreation is done from tokens — this gives the design agent fully styleable, fork-able components without needing the npm dependency at runtime.
