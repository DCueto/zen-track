# Tasks: ZenTrack MVP

## Fase 1: Setup / Fundacional

### Limpieza de boilerplate de plantilla (prerrequisito de la Fase 1)

- [ ] **[Cleanup Shared]** Eliminar `shared/Greeting.kt` y `shared/Constants.kt` (boilerplate del template KMP, sin valor para ZenTrack). El `SERVER_PORT` de `Constants.kt` se moverá a `server/application.conf`.
- [ ] **[Cleanup Shared]** Reemplazar el contenido de `Platform.kt` / `Platform.jvm.kt` / `Platform.js.kt` — el patrón `expect/actual` es válido, pero la implementación actual solo devuelve un nombre de plataforma. Redefinir la interfaz con utilidades reales de ZenTrack (ej. generación de UUIDs nativos).
- [ ] **[Cleanup Shared]** Eliminar `SharedCommonTest.kt` (test de demostración vacío).
- [ ] **[Cleanup Backend]** Reemplazar la ruta `GET /` de demostración en `Application.kt` por la estructura de plugins y routing definitiva. Eliminar `ApplicationTest.kt` de demostración.
- [ ] **[Cleanup Desktop]** Reemplazar `composeApp/App.kt` de demo por el Composable raíz de ZenTrack. Eliminar `compose-multiplatform.xml` y `ComposeAppDesktopTest.kt`.
- [ ] **[Cleanup Web]** Eliminar `webApp/src/components/Greeting/` y `webApp/src/components/JSLogo/` (demos). Actualizar `index.tsx` para montar la app ZenTrack.

---

### Backend (server/)

- [x] **[Hecho]** Servidor Ktor con Netty en puerto 8080 operativo (`Application.kt` + `build.gradle.kts` configurados).
- [ ] **[Config Backend]** Añadir dependencias Ktor en `server/build.gradle.kts`: `ktor-server-content-negotiation`, `ktor-serialization-kotlinx-json`, `ktor-server-auth-jwt`, `ktor-server-status-pages`, `ktor-server-cors`, `ktor-server-call-logging`. Añadir `kotlinx-serialization-json` y el driver PostgreSQL.
- [ ] **[Config Backend]** Instalar plugins en `Application.module()`: `ContentNegotiation` (kotlinx.serialization JSON), `StatusPages` (manejo global de errores), `CORS` y `CallLogging`. Crear la estructura de carpetas `api/`, `core/`, `db/`, `integrations/`.
- [ ] **[Config Backend]** Configurar la conexión a PostgreSQL con Exposed/Ktorm + HikariCP (pool de conexiones). Externalizar credenciales a `application.conf` (excluido de git vía `.gitignore`).
- [ ] **[Config Backend]** Implementar el sistema base de Autenticación JWT: plugin `Authentication`, generación de tokens en login, validación en rutas protegidas, separación entre rutas públicas y autenticadas.

### Shared (KMP)

- [x] **[Hecho]** Módulo KMP con targets `jvm` + `js`, patrón `expect/actual` para `Platform` y `@JsExport` operativos (`shared/build.gradle.kts`).
- [x] **[Hecho]** Target `js()` con `generateTypeScriptDefinitions()` configurado. Enlace npm local `"shared": "0.0.0-unspecified"` verificado y funcionando (webApp ya importa desde `'shared'`).
- [ ] **[Config Shared]** Añadir Ktor Client a `commonMain.dependencies`: `ktor-client-core` + engine `ktor-client-cio` en `jvmMain` + engine `ktor-client-js` en `jsMain`. Añadir `ktor-client-content-negotiation` y `kotlinx-serialization-json`.
- [ ] **[Config Shared]** Crear la estructura de carpetas en `commonMain`: `model/`, `dto/`, `network/`, `repository/`, `di/`. Configurar el cliente HTTP base (baseUrl desde `expect/actual`, headers comunes, manejo de errores).

### composeApp/ (Desktop)

- [x] **[Hecho]** App Compose Multiplatform JVM operativa con `MaterialTheme` y componentes M3 base (`App.kt` + `main.kt`).
- [ ] **[Config Desktop]** Extender `App.kt`: definir `ZenTrackTheme` con `colorScheme`, `typography` y `shapes` propios de M3. Establecer el sistema de navegación raíz entre pantallas (Workspaces → Board).

### webApp/ (React + TypeScript)

- [x] **[Hecho]** Proyecto React 19 + TypeScript 5.8 + Vite 7 operativo. Integración con módulo `shared` funcionando (`Greeting.tsx` importa `Greeting` de `'shared'`).
- [ ] **[Config Web]** Instalar dependencias de producción: `@mui/material`, `@emotion/react`, `@emotion/styled`, `zustand`. Configurar el `ThemeProvider` MUI con el tema base de ZenTrack en `index.tsx`.
- [ ] **[Config Web]** Crear la estructura de carpetas definitiva: `screens/`, `store/`, `services/`, `types/`. Configurar `VITE_API_BASE_URL` en `.env.local` (excluido de git).
    

## Fase 2: Historia 1 - Navegación de Workspaces y Configuración de Proyectos

- [ ] **[Backend]** Crear las migraciones y modelos de BD para `Users`, `Workspaces`, `Workspace_Members`, `Projects` y `Project_Members`.
    
- [ ] **[Backend]** Implementar endpoints CRUD para Workspaces (`GET`, `POST`).
    
- [ ] **[Backend]** Implementar endpoints para Proyectos, incluyendo la validación de que el `project_key` sea único por Workspace.
    
- [ ] **[Shared]** Crear los DTOs `@Serializable` y la lógica de red (Ktor Client) para Workspaces y Proyectos en `commonMain`. Ejecutar `jsBrowserLibraryDistribution` para regenerar los `.d.ts`.
    
- [ ] **[Frontend]** Crear la UI del Login/Registro.
    
- [ ] **[Frontend]** Crear la "Vista Raíz": Panel de selección de Workspaces.
    
- [ ] **[Frontend]** Crear el formulario de creación de Proyecto (validando el input del `project_key`).
    

## Fase 3: Historia 2 - Creación de tareas, subtareas y ramas automatizadas

- [ ] **[Backend]** Crear las migraciones y modelos para `Tasks`, `Task_Assignees` y `Task_Tags`.
    
- [ ] **[Backend]** Implementar la lógica transaccional (`SELECT ... FOR UPDATE`) para generar el `task_number` autoincremental de forma segura al crear una tarea.
    
- [ ] **[Backend]** Integrar el cliente HTTP para la API de GitLab/GitHub (Creación de ramas desde `main`/`develop`).
    
- [ ] **[Backend]** Crear el endpoint `POST /tasks` que guarde la tarea, calcule su ID (ej. `ZTK-25`) y dispare la llamada a Git.
    
- [ ] **[Shared]** Definir los modelos `@Serializable` de Tarea (`Task`, `TaskStatus`, `Tag`) y los repositorios en `commonMain`. Regenerar `.d.ts` con `jsBrowserLibraryDistribution`.
    
- [ ] **[Frontend]** Construir el modal/formulario de "Nueva Tarea" (campos: título, descripción, prioridad, estimación, checklist).
    
- [ ] **[Frontend]** Añadir el selector de prefijo GitFlow y la previsualización editable del nombre de la rama.
    

## Fase 4: Historia 3 - Actualización automática de estado por commit

- [ ] **[Backend]** Crear migraciones y modelos para `Sprints` y `Task_Statuses` (Workflows).
    
- [ ] **[Backend]** Crear endpoints para definir y leer los flujos de estados personalizados de un Workspace.
    
- [ ] **[Backend]** Crear el endpoint público (Webhook) `POST /api/webhooks/git` para recibir eventos de _push_.
    
- [ ] **[Backend]** Implementar la lógica del Webhook: parsear el nombre de la rama del payload, buscar el ID de la tarea (`ZTK-25`) y actualizar su `status_id` a "In Progress".
    
- [ ] **[Frontend]** (Opcional MVP) Implementar un refresco periódico (polling) o WebSockets para que el tablero se actualice cuando el Webhook cambie el estado.
    

## Fase 5: Historia 4 - Visualización flexible, ordenación y filtrado

- [ ] **[Backend]** Optimizar el endpoint `GET /tasks` para aceptar parámetros de filtrado (por sprint, por proyecto, por asignado, por estado).
    
- [ ] **[Shared]** Definir los modelos `@Serializable` de `Sprint` y los filtros activos en `commonMain` y regenerar `.d.ts`.
- [ ] **[Frontend Web]** Implementar los stores Zustand (`useSprintStore`, `useFilterStore`) usando los tipos importados desde el paquete `shared`.
    
- [ ] **[Frontend]** Desarrollar el componente "Tablero Kanban" usando Material 3 (columnas basadas en `Task_Statuses` y drag-and-drop básico).
    
- [ ] **[Frontend]** Desarrollar el componente "Vista de Lista" (DataGrid o tabla simple).
    
- [ ] **[Frontend]** Implementar la barra superior con el selector de contexto (Global, Sprint, Proyecto) y los controles de filtrado/ordenación.