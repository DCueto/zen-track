---
name: react-web-feature
description: >
  Instructivo técnico para desarrollar vistas web en `webApp/` de ZenTrack
  (React 19 + TypeScript 5.8 + Zustand + MUI 6). Carga esta habilidad siempre
  que la tarea implique: crear o modificar una pantalla React, definir un store
  Zustand, escribir un servicio HTTP con fetch, añadir o extender un componente
  MUI, tipar props con interfaces de `@mui/material`, importar tipos desde el
  módulo `shared` compilado a JS, adaptar las definiciones `.d.ts` autogeneradas
  por KMP, conectar una vista a la API Ktor, o prevenir re-renders en componentes
  React. También aplica ante menciones de "vista web", "página", "store",
  "Zustand selector", "MUI", "Material UI", "shared types", "fetch API",
  "tipo Kotlin en React", "`.d.ts`", "re-render", "VITE_API_BASE_URL" o cualquier
  tarea de interfaz web aunque no mencione React explícitamente. Actívate
  proactivamente para todo trabajo en el directorio `webApp/`.
---

# React Web Feature — Instructivo para webApp/

## Por Qué Este Instructivo Existe

React Context API con estado dinámico provoca cascadas de re-render: cada vez
que cualquier valor del contexto cambia, todos los consumidores re-renderizan,
aunque no usen ese valor concreto. Zustand con selectores precisos elimina el
problema. Adicionalmente, importar tipos desde el módulo Kotlin/JS compilado
(`shared`) sin un adaptador propaga la interoperabilidad Kotlin al árbol de
componentes, haciendo que los errores de integración aparezcan en runtime en
lugar de en compilación.

---

## Mapa de Arquitectura

```
webApp/src/
├── components/     → Componentes MUI reutilizables, sin estado global, 100% tipados
├── screens/        → Pantallas completas (WorkspacesScreen, BoardScreen, BacklogScreen…)
├── store/          → Un archivo por dominio de estado (useTaskStore, useAuthStore…)
├── services/       → Wrappers tipados de fetch — nunca en componentes directamente
├── types/          → Adaptadores e interfaces que envuelven los tipos de 'shared'
└── main.tsx        → Entry point: ThemeProvider + Router
```

El flujo de datos sigue una dirección única:

```
shared (.d.ts generado por KMP)
  ↓  adaptado en types/
services/  (fetch + tipado)
  ↓  llamado desde
store/  (Zustand: estado + acciones)
  ↓  consumido con selectores en
screens/ y components/
```

---

## Reglas Absolutas

### Tipos — Interoperabilidad con shared

Los tipos generados por KMP en `shared.d.ts` son la fuente de verdad de los modelos
de dominio. Redefinirlos en TypeScript crea divergencias silenciosas: ambas
definiciones compilarán, pero la del frontend puede quedarse desactualizada cuando
el modelo Kotlin cambie.

```typescript
// PROHIBIDO — duplica lo que ya existe en shared y divergirá silenciosamente
export interface Task { id: string; title: string; statusId: string; /* ... */ }

// CORRECTO — extiende o re-exporta desde shared
import { Task as SharedTask } from 'shared';
// Re-export directo si no necesitas extensiones
export type { Task } from 'shared';

// O con extensión fronted-específica:
export interface Task extends SharedTask {
  isSelected?: boolean;   // estado UI local, no existe en el dominio
}
```

Cuando un tipo de shared use `kotlin.collections.List` u otros tipos Kotlin
específicos, crea un adaptador en `src/types/` que lo convierta a tipos TypeScript
nativos. Esa conversión debe ocurrir en `services/`, no en componentes.

**Antes de tocar un tipo:** ejecuta:
```bash
cd .. && ./gradlew :shared:jsBrowserLibraryDistribution
```
Si el tipo no existe en `shared.d.ts`, agrégalo al modelo KMP y regenera.

### Estado — Zustand con Selector Pattern

El selector pattern es la técnica central para evitar re-renders innecesarios.
Un componente que llama a `useTaskStore()` sin selector se re-renderiza ante
cualquier cambio del store completo, aunque no use los campos modificados.

```typescript
// PROHIBIDO — re-render en cualquier cambio del store, aunque el componente
// solo muestre el título de la tarea activa
const store = useTaskStore();

// CORRECTO — re-render solo cuando tasks cambia, independientemente del resto
const tasks = useTaskStore(state => state.tasks);
const loadTasks = useTaskStore(state => state.loadTasks);

// Para múltiples campos del mismo store, usa shallow para evitar crear objetos
// nuevos en cada render:
import { useShallow } from 'zustand/react/shallow';
const { tasks, isLoading } = useTaskStore(
  useShallow(state => ({ tasks: state.tasks, isLoading: state.isLoading }))
);
```

- Un archivo de store por dominio: `useTaskStore.ts`, `useWorkspaceStore.ts`,
  `useSprintStore.ts`, `useAuthStore.ts`.
- **Prohibido** que un store llame directamente a acciones de otro store. La
  coordinación ocurre en los `services/` o en handlers de componentes.
- **Prohibido** React Context para estados dinámicos. Context solo para valores
  estáticos: configuración de tema, locale fijo.

### Colores y Tema — MUI System

Hardcodear colores en JSX tiene el mismo problema que en Compose: el modo oscuro
nunca funciona y el theming global no tiene efecto.

```tsx
// PROHIBIDO
<Box sx={{ backgroundColor: '#1A237E', color: '#ffffff' }} />
<Typography sx={{ fontSize: '14px', fontWeight: 600 }} />

// CORRECTO — tokens del tema MUI
<Box sx={{ backgroundColor: 'primary.main', color: 'primary.contrastText' }} />
<Typography variant="bodyMedium" />
```

Usa siempre las variantes tipográficas del tema (`variant="h6"`, `"body1"`, etc.)
en lugar de tamaños literales.

### Componentes MUI — Tipado Estricto

Extender interfaces de MUI en lugar de crear las propias evita perder los props
nativos del componente base (accesibilidad, eventos, `sx`, `ref`…).

```typescript
// PROHIBIDO — any destruye la seguridad de tipos de todo el árbol de props
interface TaskCardProps { taskId: string; [key: string]: any; }

// CORRECTO — hereda todos los props nativos de Card
import type { CardProps } from '@mui/material';
interface TaskCardProps extends CardProps {
  task: Task;           // prop adicional tipado con el modelo de dominio
  onMove?: (statusId: string) => void;
}

// El componente puede usar spreading de props nativas sin pérdida de tipos:
export const TaskCard: React.FC<TaskCardProps> = ({ task, onMove, ...cardProps }) => (
  <Card {...cardProps}>…</Card>
);
```

### Llamadas HTTP — Solo fetch Nativo

Axios está prohibido (vulnerabilidades conocidas en la cadena de suministro).
`fetch` nativo con un wrapper tipado cubre todos los casos de uso.

```typescript
// PROHIBIDO — no instalar Axios
import axios from 'axios';  // ❌

// CORRECTO — fetch nativo con tipado explícito
const API_BASE = import.meta.env.VITE_API_BASE_URL;  // nunca hardcodeado

async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token = useAuthStore.getState().token;  // acceso directo al store, no hook
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  });
  if (!response.ok) {
    throw new ApiError(response.status, await response.text());
  }
  return response.json() as Promise<T>;
}
```

El token JWT proviene exclusivamente de `useAuthStore`. Los componentes y stores
reciben resultados tipados, no objetos `Error` crudos.

---

## Procedimiento: Crear una Nueva Vista (paso a paso)

### Paso 1 — Verificar los tipos en shared

Lee `docs/SDD/PLAN.md` sección 3 para confirmar qué endpoint consume la vista.
Luego verifica que los tipos necesarios existen en `shared.d.ts`:

```bash
# Regenera las definiciones TypeScript desde los modelos KMP actuales
cd .. && ./gradlew :shared:jsBrowserLibraryDistribution
# Revisa qué exporta el módulo
cat shared/build/dist/js/productionLibrary/shared.d.ts | grep "export"
```

Si falta un tipo, primero añádelo en `shared/commonMain` y regenera.

---

### Paso 2 — Adaptar los Tipos en `src/types/`

Crea un fichero por dominio en `src/types/`. Su rol es importar desde `shared`
y exponer tipos TypeScript puros, sin rastro de interoperabilidad Kotlin.

```typescript
// src/types/task.types.ts
export type { Task, TaskPriority } from 'shared';
export type { CreateTaskRequest, TaskResponse } from 'shared';

// Si un tipo de shared usa colecciones Kotlin que TypeScript no resuelve bien,
// crea un adaptador:
import type { Task as SharedTask } from 'shared';
export interface Task extends Omit<SharedTask, 'assignees'> {
  assignees: string[];  // convierte kotlin.collections.List<String> a string[]
}
```

---

### Paso 3 — Definir el Service

El service encapsula todas las llamadas HTTP del dominio. Devuelve tipos
importados desde `src/types/`, nunca del módulo `shared` directamente.

```typescript
// src/services/task.service.ts
import type { Task, CreateTaskRequest, TaskResponse } from '../types/task.types';

export const taskService = {
  async getByWorkspace(workspaceId: string): Promise<Task[]> {
    return apiFetch<Task[]>(`/api/workspaces/${workspaceId}/tasks`);
  },

  async create(projectId: string, request: CreateTaskRequest): Promise<TaskResponse> {
    return apiFetch<TaskResponse>(`/api/projects/${projectId}/tasks`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  async updateStatus(taskId: string, statusId: string): Promise<Task> {
    return apiFetch<Task>(`/api/tasks/${taskId}`, {
      method: 'PUT',
      body: JSON.stringify({ statusId }),
    });
  },
};
```

**Reglas del service:**
- Siempre tipado explícito en el genérico de `apiFetch<T>()`.
- Los errores HTTP se capturan en `apiFetch`. El service solo lanza `ApiError`
  con código y mensaje; los stores deciden cómo presentarlo.
- Sin lógica de transformación de datos compleja aquí; eso va en el store.

---

### Paso 4 — Definir el Store Zustand

El store combina estado y acciones en una interfaz cohesiva. Separa el tipo de
estado del tipo de acciones para facilitar el testing.

```typescript
// src/store/useTaskStore.ts
import { create } from 'zustand';
import type { Task } from '../types/task.types';
import { taskService } from '../services/task.service';

interface TaskState {
  tasks: Task[];
  isLoading: boolean;
  error: string | null;
}

interface TaskActions {
  loadTasks: (workspaceId: string) => Promise<void>;
  moveTask: (taskId: string, newStatusId: string) => Promise<void>;
  clearError: () => void;
}

// La unión de State + Actions es el tipo público del store
type TaskStore = TaskState & TaskActions;

export const useTaskStore = create<TaskStore>((set, get) => ({
  // Estado inicial
  tasks: [],
  isLoading: false,
  error: null,

  // Acciones
  loadTasks: async (workspaceId) => {
    set({ isLoading: true, error: null });
    try {
      const tasks = await taskService.getByWorkspace(workspaceId);
      set({ tasks, isLoading: false });
    } catch (err) {
      set({ isLoading: false, error: err instanceof ApiError ? err.message : 'Error desconocido' });
    }
  },

  moveTask: async (taskId, newStatusId) => {
    // Optimistic update — actualiza localmente antes de confirmar en el servidor
    const previous = get().tasks;
    set({ tasks: previous.map(t => t.id === taskId ? { ...t, statusId: newStatusId } : t) });
    try {
      await taskService.updateStatus(taskId, newStatusId);
    } catch {
      set({ tasks: previous });  // rollback si falla
    }
  },

  clearError: () => set({ error: null }),
}));
```

**Por qué el optimistic update:** mejora la percepción de velocidad en el tablero
Kanban (drag & drop). El rollback automático mantiene la consistencia si el servidor
rechaza el cambio.

---

### Paso 5 — Construir la Pantalla

La pantalla conecta el store con la UI. Mantén la lógica de presentación (qué
mostrar) aquí y delega el rendering de cada elemento a componentes en `components/`.

```tsx
// src/screens/BoardScreen.tsx
import { useEffect } from 'react';
import { useShallow } from 'zustand/react/shallow';
import { Box, CircularProgress, Alert } from '@mui/material';
import { useTaskStore } from '../store/useTaskStore';
import { TaskColumn } from '../components/TaskColumn';

interface BoardScreenProps {
  workspaceId: string;
}

export const BoardScreen: React.FC<BoardScreenProps> = ({ workspaceId }) => {
  // Selector preciso: solo re-renderiza cuando estos tres campos cambian
  const { tasks, isLoading, error } = useTaskStore(
    useShallow(state => ({
      tasks: state.tasks,
      isLoading: state.isLoading,
      error: state.error,
    }))
  );
  const loadTasks = useTaskStore(state => state.loadTasks);
  const moveTask = useTaskStore(state => state.moveTask);

  useEffect(() => {
    loadTasks(workspaceId);
  }, [workspaceId, loadTasks]);

  if (isLoading) return (
    <Box display="flex" justifyContent="center" alignItems="center" height="100%">
      <CircularProgress />
    </Box>
  );

  if (error) return <Alert severity="error">{error}</Alert>;

  return (
    <Box display="flex" gap={2} overflow="auto" height="100%">
      {groupByStatus(tasks).map(column => (
        <TaskColumn
          key={column.statusId}
          column={column}
          onTaskMove={moveTask}  // referencia estable del store — no re-crea la función
        />
      ))}
    </Box>
  );
};
```

---

### Paso 6 — Construir Componentes Reutilizables

Los componentes en `components/` reciben solo los datos que necesitan mediante
props tipados. No acceden a stores directamente — eso garantiza que sean
reutilizables en cualquier contexto y previsualizables en Storybook.

```tsx
// src/components/TaskColumn.tsx
import type { CardProps } from '@mui/material';
import { Card, CardHeader, CardContent, Typography, Stack } from '@mui/material';
import type { Task } from '../types/task.types';

interface TaskColumnData {
  statusId: string;
  statusName: string;
  tasks: Task[];
}

interface TaskColumnProps extends Omit<CardProps, 'children'> {
  column: TaskColumnData;
  onTaskMove: (taskId: string, newStatusId: string) => void;
}

export const TaskColumn: React.FC<TaskColumnProps> = ({
  column,
  onTaskMove,
  ...cardProps
}) => (
  <Card
    {...cardProps}
    sx={{ minWidth: 280, maxWidth: 320, height: '100%', ...cardProps.sx }}
  >
    <CardHeader
      title={
        <Typography variant="subtitle1" fontWeight="medium">
          {column.statusName}
          <Typography component="span" variant="caption" sx={{ ml: 1, color: 'text.secondary' }}>
            {column.tasks.length}
          </Typography>
        </Typography>
      }
    />
    <CardContent>
      <Stack spacing={1}>
        {column.tasks.map(task => (
          <TaskCard key={task.id} task={task} onMove={onTaskMove} />
        ))}
      </Stack>
    </CardContent>
  </Card>
);
```

---

## Patrones de Rendimiento: Referencia Rápida

| Situación | Evitar | Hacer |
|---|---|---|
| Suscripción al store | `useTaskStore()` (store completo) | `useTaskStore(s => s.tasks)` (selector) |
| Múltiples campos del store | Varios hooks separados | `useShallow` con objeto |
| Callback a componente hijo | `onClick={() => moveTask(id)}` inline | `moveTask` del store — ya es referencia estable |
| Color en `sx` | `sx={{ color: '#1A237E' }}` | `sx={{ color: 'primary.main' }}` |
| Props de componente MUI custom | `{ [key: string]: any }` | `extends ButtonProps` / `extends CardProps` |
| URL de la API | `'http://localhost:8080'` hardcoded | `import.meta.env.VITE_API_BASE_URL` |
| Tipo de dominio | Definir `interface Task { … }` local | `import type { Task } from 'shared'` |
| Llamada HTTP | `axios.get(…)` | `apiFetch<T>(…)` con fetch nativo |
| Token JWT | `localStorage.getItem('token')` directo | `useAuthStore.getState().token` |

---

## Checklist de Validación

Antes de marcar cualquier vista o componente como completado:

```
TIPOS Y SHARED
[ ] Todos los modelos de dominio importados desde 'shared', sin redefiniciones locales
[ ] Se ejecutó ./gradlew :shared:jsBrowserLibraryDistribution antes de empezar
[ ] Los adaptadores Kotlin/JS están en src/types/, no en componentes ni stores
[ ] Cero usos de 'any' en interfaces de props o retornos de servicios

ZUSTAND
[ ] Cada componente usa selectores precisos (nunca useXxxStore() sin selector)
[ ] Múltiples campos del mismo store usan useShallow
[ ] Los stores no llaman acciones de otros stores directamente
[ ] Cero React Context para estados dinámicos

MUI Y TEMA
[ ] Cero colores hexadecimales en JSX — solo tokens del tema ('primary.main', etc.)
[ ] Los componentes custom extienden interfaces MUI (CardProps, ButtonProps…)
[ ] Las tipografías usan variantes del tema, no tamaños literales

SERVICIOS Y HTTP
[ ] Sin Axios — fetch nativo con apiFetch<T>()
[ ] VITE_API_BASE_URL via import.meta.env, sin hardcodeos de localhost
[ ] Token JWT exclusivamente desde useAuthStore, sin localStorage directo en componentes
[ ] Los errores HTTP están encapsulados en ApiError — stores y componentes no reciben Error crudo

BUILD
[ ] npx tsc --noEmit sin errores
[ ] npm run build compila sin warnings de tipos
[ ] Las rutas del endpoint coinciden con docs/SDD/PLAN.md sección 3
```

Comandos de verificación:

```bash
cd webApp
npx tsc --noEmit           # Type-check sin build
npm run build              # Build completo con Vite
npm run start              # Dev server con HMR para verificación visual
```

---

## Referencia Rápida de Ficheros

```
webApp/src/
├── types/
│   ├── task.types.ts         → re-exports + adaptadores desde 'shared'
│   ├── workspace.types.ts
│   └── auth.types.ts
├── services/
│   ├── api.ts                → apiFetch<T>() base con gestión de token y errores
│   ├── task.service.ts       → métodos tipados por dominio
│   └── workspace.service.ts
├── store/
│   ├── useTaskStore.ts       → TaskState + TaskActions + create()
│   ├── useWorkspaceStore.ts
│   ├── useSprintStore.ts
│   └── useAuthStore.ts       → fuente única del token JWT
├── components/
│   ├── TaskCard.tsx          → extends CardProps, sin acceso a stores
│   ├── TaskColumn.tsx
│   └── StatusBadge.tsx
├── screens/
│   ├── BoardScreen.tsx       → conecta store con TaskColumn, gestiona loading/error
│   ├── BacklogScreen.tsx
│   └── WorkspacesScreen.tsx
└── main.tsx                  → ThemeProvider (MUI) + Router + startKoin equivalente
```
