# FEATURE CONTEXT: Nueva Screen React (React 19 + Zustand + MUI)

Contexto específico para implementar **una pantalla completa** en la web app. Úsalo junto al prompt de la tarea.

## Archivos a leer antes de implementar

```
webApp/CLAUDE.md                       → reglas de arquitectura del módulo
webApp/src/screens/                    → pantallas existentes (referencia de patrón)
webApp/src/store/                      → stores Zustand existentes
webApp/src/services/                   → servicios HTTP existentes
webApp/src/types/api.ts                → tipos generados desde OpenAPI (NO editar)
```

## Estructura obligatoria

```
src/services/taskService.ts            → llamadas HTTP (fetch, con Authorization header)
  → src/store/useTaskStore.ts          → estado Zustand con selector pattern
    → src/screens/TaskScreen.tsx       → pantalla (importa del store con selectores)
      → src/components/TaskCard.tsx    → componentes MUI reutilizables
```

## Checklist de implementación

- [ ] Tipos importados exclusivamente desde `src/types/api.ts` (nunca redefinidos)
- [ ] Service en `src/services/` con `Authorization: Bearer <token>` en todas las llamadas
- [ ] Token obtenido de `useAuthStore`, nunca de `localStorage` directamente
- [ ] Store Zustand con **selector pattern** (componentes suscritos solo a la porción que necesitan)
- [ ] Un archivo de store por dominio (`useTaskStore.ts`, no mezclado con otros dominios)
- [ ] Componentes MUI tipados con las interfaces de `@mui/material` (no `any`)
- [ ] Colores usando `theme.palette.*` o sistema `sx` con referencias al tema (no hardcodeados)
- [ ] Estados de loading y error contemplados en el store y renderizados en la pantalla
- [ ] Verificación: `cd webApp && npx tsc --noEmit`

## Patrón de referencia (selector pattern)

```typescript
// CORRECTO — re-render solo cuando tasks cambia
const tasks = useTaskStore(state => state.tasks);
const isLoading = useTaskStore(state => state.isLoading);
const loadTasks = useTaskStore(state => state.loadTasks);

// PROHIBIDO — re-render en cualquier cambio del store
const store = useTaskStore();
```

## Variables de entorno

La URL base de la API se obtiene de `import.meta.env.VITE_API_BASE_URL`.  
Nunca hardcodees `http://localhost:8080` en el código.
