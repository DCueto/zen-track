# Tasks: ZenTrack MVP

## Fase 1: Setup / Fundacional

### Limpieza de boilerplate de plantilla (prerrequisito de la Fase 1)

- [x] **[Cleanup Shared]** Eliminar `shared/Greeting.kt` y `shared/Constants.kt` (boilerplate del template KMP, sin valor para ZenTrack). El `SERVER_PORT` de `Constants.kt` se moverá a `server/application.conf`.
- [x] **[Cleanup Shared]** Reemplazar el contenido de `Platform.kt` / `Platform.jvm.kt` / `Platform.js.kt` — el patrón `expect/actual` es válido, pero la implementación actual solo devuelve un nombre de plataforma. Redefinir la interfaz con utilidades reales de ZenTrack (ej. generación de UUIDs nativos).
- [x] **[Cleanup Shared]** Eliminar `SharedCommonTest.kt` (test de demostración vacío).
- [x] **[Cleanup Backend]** Reemplazar la ruta `GET /` de demostración en `Application.kt` por la estructura de plugins y routing definitiva. Eliminar `ApplicationTest.kt` de demostración.
- [ ] **[Cleanup Android]** Eliminar el módulo `composeApp/` del monorepo Gradle. Crear el módulo `androidApp/` con la estructura definida en `androidApp/CLAUDE.md`.
- [x] **[Cleanup Web]** Eliminar `webApp/src/components/Greeting/` y `webApp/src/components/JSLogo/` (demos). Actualizar `index.tsx` para montar la app ZenTrack.

---

### Backend (server/)

- [x] **[Hecho]** Servidor Ktor con Netty en puerto 8080 operativo (`Application.kt` + `build.gradle.kts` configurados).
- [x] **[Config Backend]** Añadir dependencias Ktor en `server/build.gradle.kts`: `ktor-server-content-negotiation`, `ktor-serialization-kotlinx-json`, `ktor-server-auth-jwt`, `ktor-server-status-pages`, `ktor-server-cors`, `ktor-server-call-logging`. Añadir `kotlinx-serialization-json` y el driver PostgreSQL.
- [ ] **[Config Backend]** Instalar plugins en `Application.module()`: `ContentNegotiation` (kotlinx.serialization JSON), `StatusPages` (manejo global de errores), `CORS` y `CallLogging`. Crear la estructura de carpetas `api/`, `core/`, `db/`, `integrations/`.
- [ ] **[Config Backend]** Configurar la conexión a PostgreSQL con Exposed/Ktorm + HikariCP (pool de conexiones). Externalizar credenciales a `application.conf` (excluido de git vía `.gitignore`).
- [ ] **[Config Backend]** Implementar el sistema base de Autenticación JWT: plugin `Authentication`, generación de tokens en login, validación en rutas protegidas, separación entre rutas públicas y autenticadas.

### Shared (KMP)

- [ ] **[Config Shared]** Reconfigurar `shared/build.gradle.kts`: eliminar target `js()`, añadir `androidTarget()`. Eliminar `jsMain/` source set y `generateTypeScriptDefinitions()`.
- [ ] **[Config Shared]** Añadir Ktor Client a `commonMain.dependencies`: `ktor-client-core` + engine `ktor-client-cio` en `jvmMain` + engine `ktor-client-okhttp` en `androidMain`. Añadir `ktor-client-content-negotiation` y `kotlinx-serialization-json`.
- [ ] **[Config Shared]** Crear la estructura de carpetas en `commonMain`: `model/`, `dto/`, `network/`, `repository/`, `di/`. Configurar el cliente HTTP base (baseUrl desde `expect/actual`, headers comunes, manejo de errores).
- [ ] **[Cleanup Shared]** Eliminar `Platform.js.kt` y cualquier archivo en `jsMain/`. Adaptar `Platform.kt` / `Platform.jvm.kt` con el nuevo `androidMain/Platform.android.kt`.

### androidApp/ (Jetpack Compose Android)

- [ ] **[Setup Android]** Crear el módulo `androidApp/` en el monorepo Gradle con `com.android.application` + `kotlin-android` + `compose`. Añadir dependencia `implementation(projects.shared)`.
- [ ] **[Config Android]** Definir `ZenTrackTheme` con `colorScheme`, `typography` y `shapes` propios de M3. Configurar `MainActivity` como punto de entrada con Koin y navegación.
- [ ] **[Config Android]** Establecer el sistema de navegación (Jetpack Navigation Compose) con el grafo de rutas: Workspaces → Board → TaskDetail.

### cli/ (Kotlin/JVM + Clikt)

- [ ] **[Setup CLI]** Crear el módulo `cli/` en el monorepo Gradle con `application` plugin. Añadir dependencia `implementation(projects.shared)` y `implementation(libs.clikt)`.
- [ ] **[Config CLI]** Definir la estructura de comandos raíz con Clikt: `zentrack tasks`, `zentrack workspaces`, `zentrack sprints`.

### webApp/ (React + TypeScript)

- [x] **[Hecho]** Proyecto React 19 + TypeScript 5.8 + Vite 7 operativo.
- [ ] **[Config Web]** Eliminar la dependencia `"shared": "0.0.0-unspecified"` de `package.json`. Instalar `openapi-typescript` como devDependency. Configurar script `npm run types:generate` en `package.json`.
- [ ] **[Config Web]** Instalar dependencias de producción: `@mui/material`, `@emotion/react`, `@emotion/styled`, `zustand`. Configurar el `ThemeProvider` MUI con el tema base de ZenTrack en `index.tsx`.
- [ ] **[Config Web]** Crear la estructura de carpetas definitiva: `screens/`, `store/`, `services/`, `types/`. Configurar `VITE_API_BASE_URL` en `.env.local` (excluido de git). Generar `src/types/api.ts` inicial desde la spec OpenAPI.
    

## Fase 2: Historia 1 - Navegación de Workspaces y Configuración de Proyectos

- [ ] **[Backend]** Crear las migraciones y modelos de BD para `Users`, `Workspaces`, `Workspace_Members`, `Projects` y `Project_Members`.
    
- [ ] **[Backend]** Implementar endpoints CRUD para Workspaces (`GET`, `POST`).
    
- [ ] **[Backend]** Implementar endpoints para Proyectos, incluyendo la validación de que el `project_key` sea único por Workspace.
    
- [ ] **[Shared]** Crear los DTOs `@Serializable` y la lógica de red (Ktor Client) para Workspaces y Proyectos en `commonMain`. Tras cada cambio de modelo, verificar compilación con `./gradlew :shared:jvmJar :shared:testDebugUnitTest`.
    
- [ ] **[Frontend]** Crear la UI del Login/Registro.
    
- [ ] **[Frontend]** Crear la "Vista Raíz": Panel de selección de Workspaces.
    
- [ ] **[Frontend]** Crear el formulario de creación de Proyecto (validando el input del `project_key`).
    

## Fase 3: Historia 2 - Creación de tareas, subtareas y ramas automatizadas

- [ ] **[Backend]** Crear las migraciones y modelos para `Tasks`, `Task_Assignees` y `Task_Tags`.
    
- [ ] **[Backend]** Implementar la lógica transaccional (`SELECT ... FOR UPDATE`) para generar el `task_number` autoincremental de forma segura al crear una tarea.
    
- [ ] **[Backend]** Integrar el cliente HTTP para la API de GitLab/GitHub (Creación de ramas desde `main`/`develop`).
    
- [ ] **[Backend]** Crear el endpoint `POST /tasks` que guarde la tarea, calcule su ID (ej. `ZTK-25`) y dispare la llamada a Git.
    
- [ ] **[Shared]** Definir los modelos `@Serializable` de Tarea (`Task`, `TaskStatus`, `Tag`) y los repositorios en `commonMain`. Verificar compilación con `./gradlew :shared:jvmJar :shared:testDebugUnitTest`.
    
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