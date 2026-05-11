# Tasks: ZenTrack MVP

> Versión del plan: v2 — alineada con `FUNCTIONAL_DESIGN_v2.md`

## Reglas de trabajo

1. **Una tarea a la vez.** Implementar una única tarea, esperar validación del usuario y no avanzar a la siguiente hasta recibir confirmación.
2. **Sin commits autónomos.** Solo commitear y pushear cuando el usuario indique explícitamente que la tarea está completada.
3. **Sin borrar tareas `[x]`.** Las tareas completadas son histórico permanente. Las nuevas pendientes derivadas de cambios se añaden como `[ ]` por debajo.

---

## Fase 1: Setup / Fundacional ✅

### Limpieza de boilerplate

- [x] **[Cleanup Shared]** Eliminar `shared/Greeting.kt` y `shared/Constants.kt`.
- [x] **[Cleanup Shared]** Reemplazar `Platform.kt` / `Platform.jvm.kt` con utilidades reales de ZenTrack.
- [x] **[Cleanup Shared]** Eliminar `SharedCommonTest.kt`.
- [x] **[Cleanup Backend]** Reemplazar la ruta `GET /` de demostración en `Application.kt`.
- [x] **[Cleanup Android]** Eliminar `composeApp/`. Crear `androidApp/` con la estructura definida en `androidApp/CLAUDE.md`.
- [x] **[Cleanup Web]** Eliminar demos de `webApp/src/components/`. Actualizar `index.tsx`.

### Backend

- [x] **[Config Backend]** Servidor Ktor con Netty en puerto 8080 operativo.
- [x] **[Config Backend]** Dependencias Ktor: content-negotiation, serialization, auth-jwt, status-pages, cors, call-logging.
- [x] **[Config Backend]** Plugins en `Application.module()`: ContentNegotiation, StatusPages, CORS, CallLogging.
- [x] **[Setup Docker]** `docker-compose.yml` con `postgres:16`, volumen nombrado, healthcheck.
- [x] **[Config Backend]** Conexión PostgreSQL con Exposed + HikariCP. Credenciales en `application.conf`.
- [x] **[Config Backend]** Sistema base de autenticación JWT: generación en login, validación en rutas protegidas.

### Shared

- [x] **[Config Shared]** Eliminar target `js()`, añadir `androidTarget()`. Eliminar `jsMain/`.
- [x] **[Config Shared]** Ktor Client en `commonMain` con engines CIO (jvm) y OkHttp (android).
- [x] **[Config Shared]** Estructura de carpetas: `model/`, `dto/`, `network/`, `repository/`, `di/`.

### Android / CLI / Web

- [x] **[Setup Android]** Módulo `androidApp/` con Compose + Material 3. Dependencia `projects.shared`.
- [x] **[Config Android]** `ZenTrackTheme`, `MainActivity`, sistema de navegación Compose.
- [x] **[Setup CLI]** Módulo `cli/` con Clikt. Comandos raíz: `tasks`, `workspaces`, `sprints`.
- [x] **[Config Web]** React 19 + TypeScript + Vite operativo. `openapi-typescript` configurado.
- [x] **[Config Web]** Dependencias: MUI, Zustand. `ThemeProvider` configurado. Estructura `screens/`, `store/`, `services/`, `types/`.

---

## Fase 2: Modelo de Datos v2 — Reset y Migraciones desde Cero

> No hay datos en producción. Se eliminan las migraciones v1 (V001–V006) y se recrea el esquema completo desde cero con el diseño v2.

- [ ] **[Backend]** Eliminar todas las migraciones Flyway existentes (`server/src/main/resources/db/migration/V001` … `V006`).
- [ ] **[Infra]** Reset completo de la BD del contenedor: `docker compose down -v && docker compose up -d`.
- [ ] **[Backend]** Crear `V001__create_users_and_auth.sql`: tablas `users` (password_hash nullable, avatar_url, user_type), `oauth_accounts` (provider, provider_user_id, tokens cifrados), `refresh_tokens`. Todas con columnas de auditoría (`created_at`, `created_by`, `updated_at`, `updated_by`).
- [ ] **[Backend]** Crear `V002__create_organizations.sql`: tablas `organizations` (name, slug UNIQUE, plan, is_personal) y `organization_members` (role ENUM owner/admin/member). Con columnas de auditoría.
- [ ] **[Backend]** Crear `V003__create_teams.sql`: tablas `teams` (org_id, name, color_hex) y `team_members` (role ENUM admin/manager/member). Con columnas de auditoría.
- [ ] **[Backend]** Crear `V004__create_workspaces.sql`: tablas `workspaces` (org_id), `workspace_teams` (N:M) y `workspace_members` (role ENUM admin/manager/member/client). Con columnas de auditoría.
- [ ] **[Backend]** Crear `V005__create_projects.sql`: tablas `projects` (workspace_id, project_key, constraint UNIQUE(workspace_id, project_key)) y `project_members` (role ENUM admin/manager/member/viewer/client). Con columnas de auditoría.
- [ ] **[Backend]** Crear `V006__create_membership_requests.sql`: tabla `membership_requests` (requester_id, target_type ENUM organization/team/workspace, target_id, status ENUM pending/approved/rejected, reviewed_by, reviewed_at). Con columnas de auditoría.
- [ ] **[Backend]** Crear `V007__create_agile_entities.sql`: tablas `sprints`, `tags`, `task_statuses`. Con columnas de auditoría.
- [ ] **[Backend]** Crear `V008__create_tasks.sql`: tablas `tasks` (con todos los campos incluido git_branch_name), `task_assignees`, `task_tags`. Con columnas de auditoría en `tasks`; `task_assignees` y `task_tags` con `created_at` y `created_by`.
- [ ] **[Backend]** Reescribir todas las tablas Exposed desde cero en `db/tables/` para reflejar el esquema v2: `UsersTable`, `OAuthAccountsTable`, `RefreshTokensTable`, `OrganizationsTable`, `OrganizationMembersTable`, `TeamsTable`, `TeamMembersTable`, `WorkspacesTable`, `WorkspaceTeamsTable`, `WorkspaceMembersTable`, `ProjectsTable`, `ProjectMembersTable`, `MembershipRequestsTable`, `SprintsTable`, `TagsTable`, `TaskStatusesTable`, `TasksTable`, `TaskAssigneesTable`, `TaskTagsTable`.

---

## Fase 3: Autenticación — OAuth 2.0 Google

- [ ] **[Backend]** Añadir dependencias en `libs.versions.toml`: cliente HTTP para intercambio de tokens OAuth (Ktor Client o `ktor-client-apache`). Añadir librería de cifrado AES-256 para tokens de Google.
- [ ] **[Backend]** Configurar las credenciales de Google OAuth en `application.conf`: `google.clientId`, `google.clientSecret`, `google.redirectUri` (excluidos de git).
- [ ] **[Backend]** Implementar `GET /api/auth/google`: genera `state` UUID, construye la URL de autorización de Google con scope `openid email profile` y redirige (302).
- [ ] **[Backend]** Implementar `GET /api/auth/google/callback`: valida `state`, intercambia `code` por tokens en el Token Endpoint de Google, llama a UserInfo API, crea o vincula usuario en BD, almacena tokens cifrados en `oauth_accounts`, emite JWT interno de ZenTrack.
- [ ] **[Backend]** Implementar `POST /api/auth/refresh`: valida refresh token interno (tabla `refresh_tokens`), emite nuevo JWT.
- [ ] **[Backend]** Implementar `POST /api/auth/logout`: marca el refresh token como revocado en `refresh_tokens`.
- [ ] **[Backend]** Implementar `GET /api/users/me/oauth`: lista cuentas OAuth vinculadas al usuario autenticado.
- [ ] **[Backend]** Implementar `POST /api/users/me/oauth/google`: vincula cuenta Google a usuario ya autenticado por email/contraseña.
- [ ] **[Backend]** Implementar `DELETE /api/users/me/oauth/{id}`: desvincula cuenta OAuth — rechazar si `password_hash` es null (único método de login).
- [ ] **[Shared]** Añadir DTOs `@Serializable` para respuestas OAuth: `OAuthAccountDto`, `AuthResponseDto` (JWT + refresh token).
- [ ] **[Frontend]** Añadir botón "Continuar con Google" en `AuthScreen`. Al pulsar, redirige a `GET /api/auth/google`.
- [ ] **[Frontend]** Gestionar el callback OAuth en la web: leer JWT del redirect, almacenarlo en `useAuthStore` y navegar al panel principal.

---

## Fase 4: Organizaciones y Teams

- [ ] **[Backend]** Auto-crear organización personal al registrar un usuario `regular`: fila en `organizations` (is_personal=true, slug derivado del email) + fila en `organization_members` con rol `owner`.
- [ ] **[Backend]** Implementar endpoints de organizaciones:
  - `GET /api/organizations` — orgs del usuario autenticado
  - `POST /api/organizations` — crear org empresarial
  - `GET /api/organizations/{org_id}` — detalle
  - `GET /api/organizations/{org_id}/members` — listar miembros
  - `POST /api/organizations/{org_id}/members` — añadir miembro directamente (owner/admin)
  - `DELETE /api/organizations/{org_id}/members/{uid}` — eliminar miembro
- [ ] **[Backend]** Implementar endpoints de teams:
  - `GET /api/organizations/{org_id}/teams` — listar teams
  - `POST /api/organizations/{org_id}/teams` — crear team
  - `GET /api/teams/{team_id}/members` — listar miembros
  - `POST /api/teams/{team_id}/members` — añadir miembro directamente
  - `DELETE /api/teams/{team_id}/members/{uid}` — eliminar miembro
- [ ] **[Backend]** Añadir endpoint de búsqueda pública de orgs (excluye `is_personal=true`): `GET /api/organizations/search?q=...` — usado durante el registro.
- [ ] **[Backend]** Implementar lógica de autorización `canManageTeamMembers(userId, teamId)` en capa de servicios.
- [ ] **[Shared]** Crear DTOs `@Serializable`: `OrganizationDto`, `OrganizationMemberDto`, `TeamDto`, `TeamMemberDto`.
- [ ] **[Frontend]** Actualizar `AuthScreen` (registro): añadir paso opcional de búsqueda y selección de organización para enviar solicitud al registrarse.
- [ ] **[Frontend]** Crear pantalla `OrgSwitcherScreen`: selector de organización activa (empresariales + personal). Visible en el panel raíz.
- [ ] **[Frontend]** Crear pantalla `OrgSettingsScreen`: gestión de miembros y solicitudes pendientes (solo org owner/admin).
- [ ] **[Frontend]** Crear pantalla `TeamsScreen`: lista de teams de la org activa, crear team, ver miembros.

---

## Fase 5: Sistema de Membresías y Usuarios Cliente

- [ ] **[Backend]** Implementar endpoints de solicitudes:
  - `POST /api/membership-requests` — enviar solicitud (org/team/workspace)
  - `GET /api/membership-requests` — ver mis solicitudes enviadas
  - `GET /api/organizations/{org_id}/requests` — solicitudes pendientes de la org
  - `POST /api/organizations/{org_id}/requests/{id}/approve`
  - `POST /api/organizations/{org_id}/requests/{id}/reject`
  - `GET /api/teams/{team_id}/requests` — solicitudes pendientes del team
  - `POST /api/teams/{team_id}/requests/{id}/approve`
  - `POST /api/teams/{team_id}/requests/{id}/reject`
  - `GET /api/workspaces/{w_id}/requests` — solicitudes pendientes del workspace
  - `POST /api/workspaces/{w_id}/requests/{id}/approve`
  - `POST /api/workspaces/{w_id}/requests/{id}/reject`
- [ ] **[Backend]** Implementar lógica de autorización dual para workspace: `canManageWorkspaceMembers(userId, workspaceId)` — path directo (workspace admin/manager) O path vía team (team admin/manager de team asignado al workspace).
- [ ] **[Backend]** Implementar auto-asignación sin solicitud para org owner/admin: `POST /api/teams/{id}/self-join` y `POST /api/workspaces/{id}/self-join` — sin pasar por `membership_requests`, rol inicial `admin`.
- [ ] **[Backend]** Adaptar `POST /api/auth/register` para soportar `user_type = client`: no crear org personal, no poder unirse a orgs empresariales.
- [ ] **[Backend]** Implementar flujo de incorporación de cliente: workspace admin/manager añade cliente a `workspace_members` (rol `client`) y luego a `project_members` (rol `client`).
- [ ] **[Shared]** Añadir DTOs: `MembershipRequestDto`, `MembershipRequestStatusDto`.
- [ ] **[Frontend]** Crear componente de bandeja de solicitudes pendientes (visible para admins/managers de org, team y workspace).
- [ ] **[Frontend]** Crear pantalla de espera para usuarios `client` recién registrados sin workspace asignado.
- [ ] **[Frontend]** Añadir opción en `AuthScreen` para registrarse como usuario cliente (`user_type = client`).

---

## Fase 6: Workspaces y Proyectos (actualizado v2)

- [x] **[Backend]** Crear las migraciones y modelos de BD para `Users`, `Workspaces`, `Workspace_Members`, `Projects` y `Project_Members`.
- [x] **[Backend]** Implementar endpoints CRUD para Workspaces (`GET`, `POST`).
- [x] **[Backend]** Implementar endpoints para Proyectos, incluyendo la validación de que el `project_key` sea único por Workspace.
- [x] **[Shared]** Crear los DTOs `@Serializable` y la lógica de red (Ktor Client) para Workspaces y Proyectos en `commonMain`.
- [x] **[Frontend]** Crear la UI del Login/Registro (`AuthScreen`).
- [ ] **[Backend]** Tras el reset de Fase 2, adaptar endpoints de Workspaces al nuevo esquema v2 (añadir `org_id`, audit cols, mover ruta a `/api/organizations/{org_id}/workspaces`).
- [ ] **[Backend]** Tras el reset de Fase 2, adaptar endpoints de Proyectos al nuevo esquema v2 (audit cols, constraint `UNIQUE(workspace_id, project_key)` explícito).
- [ ] **[Shared]** Actualizar DTOs de Workspaces y Proyectos con los nuevos campos del esquema v2 (`orgId`, `createdBy`, etc.).
- [ ] **[Backend]** Actualizar endpoints de Workspaces al nuevo contexto: `GET /api/organizations/{org_id}/workspaces` y `POST /api/organizations/{org_id}/workspaces`.
- [ ] **[Backend]** Implementar endpoints de `workspace_teams`:
  - `GET /api/workspaces/{w_id}/teams`
  - `POST /api/workspaces/{w_id}/teams` — asignar team al workspace
  - `DELETE /api/workspaces/{w_id}/teams/{team_id}` — desasignar team
- [ ] **[Backend]** Implementar `canManageProjectMembers(userId, projectId)` — solo workspace admin/manager (path directo, team path NO válido aquí).
- [ ] **[Frontend]** Crear "Vista Raíz": panel de selección de Workspaces dentro del contexto de la org activa. Los workspaces de la org personal aparecen en sección separada.
- [ ] **[Frontend]** Crear formulario de creación de Workspace (selección de org + asignación opcional de teams).
- [ ] **[Frontend]** Crear formulario de creación de Proyecto (con validación del `project_key`).
- [ ] **[Frontend]** Crear pantalla de gestión de miembros del Workspace (añadir, cambiar rol, revocar).

---

## Fase 7: Historia 2 - Creación de Tareas, Subtareas y Ramas Git

- [ ] **[Backend]** Migraciones para `Tasks`, `Task_Assignees`, `Task_Tags`.
- [ ] **[Backend]** Lógica transaccional (`SELECT ... FOR UPDATE`) para `task_number` autoincremental por proyecto.
- [ ] **[Backend]** Integrar cliente HTTP para GitLab/GitHub (creación de ramas desde `main`/`develop`).
- [ ] **[Backend]** Endpoint `POST /api/projects/{p_id}/tasks`: guarda la tarea, calcula `display_id` (ej. `ZTK-25`) y dispara llamada a Git. Si Git falla, guarda con `git_branch_name = null`.
- [ ] **[Backend]** Endpoint `POST /api/tasks/{t_id}/subtasks`: crea subtarea con `parent_id` vinculado.
- [ ] **[Shared]** Modelos `@Serializable`: `Task`, `TaskStatus`, `Tag`. Repositorios en `commonMain`.
- [ ] **[Frontend]** Modal/formulario "Nueva Tarea": título, descripción, prioridad, estimación, asignados, etiquetas.
- [ ] **[Frontend]** Selector de prefijo GitFlow y previsualización editable del nombre de rama.
- [ ] **[Frontend]** Opción "Guardar como borrador" (no genera rama Git).

---

## Fase 8: Historia 3 - Actualización Automática de Estado por Commit

- [ ] **[Backend]** Migraciones para `Sprints` y `Task_Statuses` (workflows configurables por workspace).
- [ ] **[Backend]** Endpoints para definir y leer estados del workspace: `GET/POST /api/workspaces/{w_id}/statuses`.
- [ ] **[Backend]** Endpoint público `POST /api/webhooks/git` para recibir eventos de push (protegido por secret del proveedor).
- [ ] **[Backend]** Lógica del Webhook: parsear nombre de rama del payload → extraer `display_id` → actualizar `status_id` a "In Progress".
- [ ] **[Frontend]** (Opcional MVP) Polling o WebSocket para refrescar el tablero cuando el Webhook cambia estados.

---

## Fase 9: Historia 4 - Visualización, Filtrado y Ordenación

- [ ] **[Backend]** Optimizar `GET /api/workspaces/{w_id}/tasks` con parámetros de filtrado: `project_id`, `sprint_id`, `assignee`, `status`, `priority`, `tag`.
- [ ] **[Shared]** DTOs `@Serializable` de `Sprint` y modelos de filtro activo en `commonMain`.
- [ ] **[Frontend]** Stores Zustand: `useSprintStore`, `useFilterStore`, `useTaskBoardStore`.
- [ ] **[Frontend]** Componente "Tablero Kanban": columnas dinámicas basadas en `Task_Statuses` del workspace, drag-and-drop básico.
- [ ] **[Frontend]** Componente "Vista de Lista": tabla compacta con atributos clave de la tarea.
- [ ] **[Frontend]** Selector de contexto en barra superior: Global / Sprint / Proyecto.
- [ ] **[Frontend]** Controles de filtrado y ordenación por cualquier atributo de la tarea.
