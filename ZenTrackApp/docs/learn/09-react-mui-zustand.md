# 09 — React + MUI + Zustand para devs .NET

> Cómo entender el stack frontend de ZenTrack si conoces Blazor, ASP.NET Core y C#.

---

## React 19 vs Blazor — el modelo mental clave

Blazor y React comparten la idea de componentes con estado, pero el modelo de reactividad es diferente:

| Concepto Blazor | Equivalente React | Diferencia clave |
|---|---|---|
| `@page "/ruta"` | `<Route path="/ruta">` | React Router / Nav Compose separa routing del componente |
| `StateHasChanged()` | Implícito en `useState` / `useReducer` | React re-renderiza automáticamente al cambiar estado |
| `INotifyPropertyChanged` | `useState` / `useStore` (Zustand) | En React el estado es inmutable — se reemplaza, no se muta |
| `@inject IServicio servicio` | Props, Context, `useStore` | Zustand reemplaza al DI container para estado global |
| `OnInitializedAsync()` | `useEffect(() => {}, [])` | Ambos son el "primer mount" del componente |
| `<EditForm>` + `DataAnnotations` | Librerías como react-hook-form | React no tiene formularios reactivos built-in |
| Razor partials | Componentes hijo (`<MiComponente />`) | JSX mezcla markup y lógica en el mismo archivo |

### Estado inmutable — la diferencia más importante

En Blazor puedes mutar propiedades de un objeto y llamar `StateHasChanged()`. En React **el estado se reemplaza**, nunca se muta directamente:

```tsx
// PROHIBIDO — muta el objeto existente
const [task, setTask] = useState({ title: 'Fix bug', done: false });
task.done = true;  // React no detecta este cambio

// CORRECTO — reemplaza con un nuevo objeto (spread operator)
setTask({ ...task, done: true });
```

La razón: React detecta cambios por referencia (`===`), no por valor profundo. Si el objeto es el mismo, no hay re-render.

---

## Material UI (MUI) v5 vs Blazor MudBlazor

Si usas MudBlazor, MUI te resultará muy familiar — es su equivalente en el mundo React.

| MudBlazor | MUI v5 | Notas |
|---|---|---|
| `<MudButton>` | `<Button>` | Mismas variantes: `contained`, `outlined`, `text` |
| `<MudCard>` | `<Card>` | Igual; `<CardContent>`, `<CardActions>` |
| `<MudTextField>` | `<TextField>` | `variant="outlined"` es el estándar en ZenTrack |
| `<MudThemeProvider>` | `<ThemeProvider>` | Ambos envuelven la app en el árbol |
| `MudTheme` / `Palette` | `createTheme({ palette })` | Ambos permiten customizar tokens de color |
| `<MudCssBaseline>` | `<CssBaseline>` | Reset CSS y normalize de fuentes |
| `sx` prop | `sx` prop | Ambos tienen un `sx` system para estilos inline con tokens |

### El sistema `sx` — estilos con tokens del tema

En lugar de CSS hardcodeado, MUI ofrece el `sx` prop que accede a los tokens del tema:

```tsx
// PROHIBIDO — hardcodea color hex
<Box sx={{ backgroundColor: '#5B6CF9', borderRadius: '8px' }} />

// CORRECTO — usa roles semánticos del tema MUI
<Box sx={{
  backgroundColor: 'primary.main',
  borderRadius: 2,         // 2 × 8px = 16px (escala MUI)
  p: 2,                    // padding: 16px (t-shirt size)
  color: 'text.secondary'
}} />
```

Los tokens de spacing de MUI van en múltiplos de 8px por defecto (igual que el sistema 8dp de Material Design).

### Tema ZenTrack — ThemeProvider

En `index.tsx` envolvemos toda la app con el `ThemeProvider` y detectamos el modo oscuro del sistema operativo:

```tsx
// src/index.tsx
function App() {
  const prefersDark = useMediaQuery('(prefers-color-scheme: dark)');
  const theme = prefersDark ? darkTheme : lightTheme;

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />      {/* equivalente a <MudCssBaseline /> */}
      <AppContent />
    </ThemeProvider>
  );
}
```

El tema está definido en `src/theme.ts` y sus colores derivan del seed `#5B6CF9` (no se usa directamente en componentes — solo en el archivo de tema).

---

## Zustand vs IServiceCollection (gestión de estado global)

En .NET, el estado global de la UI suele vivir en servicios registrados en el DI container. En React, el patrón más común para estado global reactivo es un **store**.

Zustand es la librería de estado que usa ZenTrack. Es mucho más simple que Redux y más eficiente que React Context.

### Por qué no Context API

React Context API re-renderiza **todos** los consumidores cuando cualquier valor cambia. Con datos frecuentes (tareas, filtros, sprints) esto provoca cascadas de re-renders incontrolables. Zustand con el **Selector Pattern** solo re-renderiza el componente que usa la porción de estado que cambió.

### Crear un store

```typescript
// src/store/useWorkspaceStore.ts
import { create } from 'zustand';

interface Workspace {
  id: string;
  name: string;
}

interface WorkspaceState {
  workspaces: Workspace[];
  activeWorkspaceId: string | null;
  setActiveWorkspace: (id: string) => void;
  loadWorkspaces: () => Promise<void>;
}

export const useWorkspaceStore = create<WorkspaceState>((set) => ({
  workspaces: [],
  activeWorkspaceId: null,

  setActiveWorkspace: (id) => set({ activeWorkspaceId: id }),

  loadWorkspaces: async () => {
    // servicios van en services/, no aquí
    const data = await workspaceService.fetchAll();
    set({ workspaces: data });
  },
}));
```

El equivalente en Blazor sería un `WorkspaceService` inyectado como `Scoped` con `INotifyPropertyChanged` o con un evento de cambio que los componentes suscriben.

### Consumir el store — Selector Pattern

```tsx
// CORRECTO — Selector Pattern: re-render solo cuando 'workspaces' cambia
const workspaces = useWorkspaceStore(state => state.workspaces);
const loadWorkspaces = useWorkspaceStore(state => state.loadWorkspaces);

// PROHIBIDO — suscripción completa: re-render en cualquier cambio del store
const store = useWorkspaceStore();
```

La analogía .NET: es como si en lugar de inyectar todo el `IServicio`, inyectaras solo la propiedad `IServicio.Workspaces` como un observable independiente. Si otra propiedad del servicio cambia, tu componente no se "notifica".

---

## Estructura de carpetas — decisiones de diseño

```
src/
├── components/   → Componentes MUI reutilizables (sin estado global)
├── screens/      → Pantallas completas (WorkspacesScreen, BoardScreen)
├── store/        → Stores Zustand (useWorkspaceStore, useTaskStore, etc.)
├── services/     → Llamadas HTTP fetch (no Axios — ver webApp/CLAUDE.md)
├── types/        → api.ts (generado) + extensiones de tipos locales
└── theme.ts      → Configuración del ThemeProvider MUI
```

Analogía .NET:
- `screens/` ↔ páginas Razor (`Pages/`)
- `components/` ↔ componentes Razor compartidos (`Shared/`)
- `store/` ↔ servicios Scoped de estado de UI
- `services/` ↔ `HttpClient` wrappers / typed clients

---

## Variables de entorno — VITE_API_BASE_URL

Vite (el equivalente a `dotnet run` + bundler) usa archivos `.env` igual que .NET usa `appsettings.json`:

| .NET | Vite / React | Notas |
|---|---|---|
| `appsettings.json` | `.env` | Valores por defecto |
| `appsettings.Development.json` | `.env.local` | Overrides locales (excluido de git) |
| `appsettings.Production.json` | Variables de entorno del host | En Vercel/producción |
| `Environment.GetEnvironmentVariable("X")` | `import.meta.env.VITE_X` | Prefijo `VITE_` obligatorio para exponer al browser |

```typescript
// src/services/apiClient.ts (futuro)
const BASE_URL = import.meta.env.VITE_API_BASE_URL;  // "http://localhost:8080"
```

**NUNCA** hardcodees `http://localhost:8080` en código fuente — usa siempre `import.meta.env.VITE_API_BASE_URL`.

---

## `src/types/api.ts` — contrato con el backend

Los tipos TypeScript se generan automáticamente desde la spec OpenAPI del servidor Ktor:

```bash
# Una vez el servidor esté corriendo:
npm run types:generate
```

El archivo generado describe todos los endpoints y modelos en TypeScript. **NUNCA** edites `api.ts` manualmente — es un artefacto de build, como un archivo generado por EF Migrations o un proxy de WCF.

Para usar los tipos en componentes:

```typescript
import type { components } from '../types/api';

// Extrae el tipo del modelo generado
type Task = components['schemas']['Task'];

// Si necesitas añadir estado local de UI al tipo:
interface TaskWithSelection extends Task {
  isSelected: boolean;  // campo local, no pertenece a la API
}
```

---

## Comandos de verificación

```bash
cd webApp

# Dev server con HMR (Hot Module Replacement — equivalente a dotnet watch)
npm run start

# Type-check sin compilar (equivalente a dotnet build sin ejecutar)
npx tsc --noEmit

# Build de producción
npm run build

# Regenerar tipos desde OpenAPI (requiere servidor en localhost:8080)
npm run types:generate
```
