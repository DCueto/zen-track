# CLAUDE.md — webApp/ (React 19 + TypeScript + Zustand + MUI)

## QUÉ hace este módulo
Aplicación web construida con React 19, TypeScript 5.8, Vite 7 y Material UI (MUI). Consume la API REST del servidor Ktor y utiliza el módulo `shared` compilado a JS (disponible como paquete npm `"shared": "0.0.0-unspecified"`) para reutilizar modelos y DTOs.

## POR QUÉ estas reglas existen
React Context API con actualizaciones frecuentes provoca cascadas de re-render. Zustand con el patrón de selectores elimina ese problema. Los tipos estrictos entre el módulo Kotlin/JS compartido y los componentes React previenen errores en tiempo de integración que TypeScript no detectaría sin `interface` explícitas.

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

### Interoperabilidad con el módulo shared (Kotlin/JS)

- **SIEMPRE** importa modelos y DTOs desde el paquete `shared` en lugar de redefinirlos en TypeScript.
- **PROHIBIDO** duplicar tipos que ya existen en `shared/` como clases `@Serializable`. Si el tipo no está en shared, agrégalo allí y regenera las definiciones.
- Al consumir APIs del módulo shared que usen tipos Kotlin específicos (ej. `kotlin.collections.List`), **SIEMPRE** declara un wrapper o adaptador en `src/types/` para evitar que la interoperabilidad Kotlin/JS se propague a los componentes React.

```typescript
// CORRECTO — adaptar en la capa de types
import { Task as SharedTask } from 'shared';
export interface Task extends SharedTask {
  // extensiones específicas del frontend si aplican
}

// PROHIBIDO — redefinir lo que ya existe en shared
export interface Task { id: string; title: string; /* ... */ }
```

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

- Las dependencias de producción del módulo `shared` (bindings Kotlin) se declaran con `npm()` dentro de `shared/build.gradle.kts`.
- Las dependencias propias de `webApp/` (React, MUI, Zustand, etc.) se declaran en `webApp/package.json`.
- **PROHIBIDO** añadir en `package.json` una dependencia que es transitiva del módulo `shared`; esto crea conflictos de versión.

```bash
# Añadir dependencia nueva a webApp:
cd webApp && npm install @mui/material @emotion/react @emotion/styled

# Añadir binding JS al módulo shared (en shared/build.gradle.kts):
# jsMain.dependencies { implementation(npm("some-js-lib", "1.2.3")) }
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

### Comandos de Verificación

```bash
# Antes de cualquier cambio que afecte tipos compartidos:
cd .. && ./gradlew :shared:jsBrowserLibraryDistribution
cd webApp && npm run build   # Verifica que los tipos generados son compatibles

# Type-check sin build:
npx tsc --noEmit
```