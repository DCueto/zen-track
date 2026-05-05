# CLAUDE.md — webApp/ (React 19 + TypeScript + Zustand + MUI)

## QUÉ hace este módulo
Aplicación web construida con React 19, TypeScript 5.8, Vite 7 y Material UI (MUI). Consume la API REST del servidor Ktor. Los tipos TypeScript se generan desde la spec OpenAPI del servidor con `openapi-typescript` y se guardan en `src/types/api.ts`. No existe dependencia de módulos Kotlin.

## POR QUÉ estas reglas existen
React Context API con actualizaciones frecuentes provoca cascadas de re-render. Zustand con el patrón de selectores elimina ese problema. Generar los tipos desde OpenAPI garantiza que el contrato entre frontend y backend está siempre sincronizado sin duplicación manual.

## CÓMO estructurar el código

### Estructura de directorios

```
src/
├── components/     → Componentes MUI reutilizables (presentacionales, sin estado global)
├── screens/        → Pantallas completas (Workspaces, Board, Backlog, TaskDetail)
├── store/          → Estado global Zustand (un archivo por dominio)
├── services/       → Llamadas HTTP a la API Ktor (fetch wrappers)
├── types/          → Interfaces TypeScript y re-exports del módulo shared
└── main.tsx        → Entry point
```

### Tipos desde OpenAPI

- **SIEMPRE** importa los tipos de dominio desde `src/types/api.ts` (generado por `openapi-typescript`). **PROHIBIDO** redefinir manualmente tipos que ya describe la spec OpenAPI.
- Si necesitas extender un tipo generado, hazlo en `src/types/` con una interfaz que extienda el tipo base:

```typescript
// CORRECTO — extender el tipo generado
import type { components } from './api';
type Task = components['schemas']['Task'];
export interface TaskWithUI extends Task {
  isSelected: boolean;  // estado local de UI, no pertenece a la API
}

// PROHIBIDO — redefinir lo que ya describe la spec
export interface Task { id: string; title: string; /* ... */ }
```

- Cuando se modifique un endpoint o modelo en el servidor, regenera los tipos:

```bash
# Con el servidor corriendo en localhost:8080:
npx openapi-typescript http://localhost:8080/openapi.json -o src/types/api.ts

# O desde un fichero estático exportado:
npx openapi-typescript ../server/openapi.json -o src/types/api.ts
```

**NUNCA** edites manualmente `src/types/api.ts`; es un artefacto generado.

### Estado Global con Zustand

- **PROHIBIDO** usar React Context API para estados dinámicos (tareas, sprints, filtros, usuario activo). Context solo es aceptable para valores estáticos de configuración o tema.
- **SIEMPRE** aplica el **Selector Pattern**: los componentes se suscriben solo a la porción exacta del estado que necesitan.

```typescript
// CORRECTO — selector específico, re-render solo cuando tasks cambia
const tasks = useTaskStore(state => state.tasks);
const loadTasks = useTaskStore(state => state.loadTasks);

// PROHIBIDO — suscripción al store completo, re-render en cualquier cambio
const store = useTaskStore();
```

- Un archivo de store por dominio: `useWorkspaceStore.ts`, `useTaskStore.ts`, `useSprintStore.ts`, `useAuthStore.ts`.
- **PROHIBIDO** anidar stores (un store llama a acciones de otro store directamente). La coordinación se hace en los `services/` o en event handlers de componentes.

### Gestión de Dependencias JS

- Todas las dependencias de `webApp/` se declaran en `webApp/package.json`. No hay dependencias Kotlin/KMP.
- El módulo `shared` Kotlin **no** se incluye como paquete npm; la integración se hace vía OpenAPI.

```bash
# Añadir dependencia nueva a webApp:
cd webApp && npm install @mui/material @emotion/react @emotion/styled
```

### Componentes MUI y Tipado Estricto

- **SIEMPRE** tipa correctamente los props de componentes MUI usando las interfaces que provee `@mui/material`. **NUNCA** uses `any` para los props de MUI.
- Para componentes personalizados que envuelven MUI, extiende la interfaz base de MUI:

```typescript
// CORRECTO
import { ButtonProps } from '@mui/material';
interface ZenButtonProps extends ButtonProps {
  taskId: string;
}

// PROHIBIDO
interface ZenButtonProps {
  taskId: string;
  [key: string]: any; // destruye la seguridad de tipos
}
```

- **NUNCA** uses colores hardcodeados en el JSX/TSX. **SIEMPRE** usa tokens del tema MUI (`theme.palette.primary.main`) o el sistema `sx` con referencias al tema.

```tsx
// PROHIBIDO
<Box sx={{ backgroundColor: '#1A237E' }} />

// OBLIGATORIO
<Box sx={{ backgroundColor: 'primary.main' }} />
```

### Llamadas a la API (services/)

- **SIEMPRE** incluye el header `Authorization: Bearer <token>` en todas las llamadas excepto `POST /api/auth/login` y `POST /api/auth/register`.
- El token JWT se obtiene exclusivamente de `useAuthStore`. **PROHIBIDO** acceder a `localStorage` directamente en componentes o en otros stores.
- Maneja los errores HTTP en la capa `services/`. Los componentes y stores reciben resultados tipados, nunca objetos `Error` crudos.
- Evita usar Axios para realizar llamadas a la API. NO INSTALES ESTA DEPENDENCIA. Tiene vulnerabilidades graves. Simplemente usa la API nativa de fetch.

### Configuración de Vite y Build

- La URL base de la API se configura mediante variable de entorno `VITE_API_BASE_URL`. **NUNCA** hardcodees `http://localhost:8080` en el código fuente.
- Para desarrollo local, crea `webApp/.env.local` (ignorado por git). Para producción, configura la variable en el entorno de despliegue.

```bash
# Desarrollo
npm run start   # Vite dev server con HMR

# Producción
npm run build   # tsc + vite build → dist/
```

### Tests

#### Stack y ubicación

- Herramientas: **Vitest** + **@testing-library/react** + **@testing-library/user-event**.
- Instala con: `npm install -D vitest @testing-library/react @testing-library/user-event @testing-library/jest-dom jsdom`
- Los archivos de test viven junto al código que testean: `TaskCard.tsx` → `TaskCard.test.tsx`.

#### Qué testear y cómo

| Capa | Qué testear | Cómo |
|---|---|---|
| `store/` | Transiciones de estado (acciones Zustand) | Test puro del store sin renderizar componentes |
| `services/` | Lógica de construcción de requests y parseo de respuestas | Mock de `fetch` con `vi.fn()` |
| `components/` | Comportamiento observable (render, clicks, inputs) | React Testing Library |
| `screens/` | Flujos críticos de usuario | React Testing Library con store real |

#### Reglas

- **PROHIBIDO** snapshot tests como estrategia principal — se convierten en mantenimiento sin valor.
- **SIEMPRE** testea comportamiento observable, nunca detalles de implementación (no busques por nombre de clase CSS ni por estructura DOM interna).
- **NUNCA** uses `any` en tests; aplica las mismas reglas de tipado que en producción.
- Mockea los `services/` en tests de screens/stores, no la implementación interna de `fetch`.

```typescript
// CORRECTO — testea comportamiento
expect(screen.getByRole('button', { name: /crear tarea/i })).toBeInTheDocument();

// PROHIBIDO — testea implementación interna
expect(wrapper.find('.MuiButton-root')).toHaveLength(1);
```

```bash
# Ejecutar tests:
npm run test        # watch mode
npm run test:run    # una sola pasada (CI)
```

### Comandos de Verificación

```bash
# Regenerar tipos desde OpenAPI (requiere servidor corriendo):
npx openapi-typescript http://localhost:8080/openapi.json -o src/types/api.ts

# Type-check sin build:
npx tsc --noEmit

# Tests:
npm run test:run

# Build de producción:
npm run build
```