
# Plan Técnico: ZenTrack

## 1. Arquitectura de Alto Nivel

ZenTrack utiliza una arquitectura cliente-servidor basada en el ecosistema Kotlin:

- **Backend (Ktor):** API RESTful con Ktor, encargada de la lógica de negocio, integración con las APIs de GitLab/GitHub, autenticación JWT + OAuth 2.0 Google.

- **Base de Datos (PostgreSQL):** Base de datos relacional para persistir todas las entidades. Se comunica con Ktor mediante el ORM Exposed + HikariCP.

- **Shared / Core (KMP):** Módulo KMP con dos targets: `jvm` (usado por el Backend y el CLI) y `androidTarget` (usado por la app Android). Contiene modelos `@Serializable`, DTOs y Ktor Client. Sin compilación JS.

- **App Android (Jetpack Compose):** UI nativa Android implementando Material 3. Arquitectura MVI con ViewModel + StateFlow. Depende de `:shared` vía `androidTarget`.

- **CLI (Kotlin/JVM + Clikt):** Herramienta de terminal para gestionar tareas desde la línea de comandos. Depende de `:shared` vía `jvm` target.

- **Frontend Web (React + TS + Zustand):** Aplicación TypeScript pura (React 19 + Zustand + MUI). Consume la API Ktor directamente vía `fetch` nativo. Los tipos TypeScript se generan desde la spec OpenAPI del servidor con `openapi-typescript`. **No hay dependencia de módulos Kotlin en `webApp/`**.

### Flujo de Tipos: Ktor → OpenAPI → webApp

```
shared/commonMain  (@Serializable data class Task, Workspace, Project…)
        ↓
server/  (Ktor expone la spec vía plugin OpenAPI)
        ↓
http://localhost:8080/api.json
        ↓
npx openapi-typescript http://localhost:8080/api.json -o webApp/src/types/api.ts
        ↓
webApp/src/  →  import type { Task } from './types/api'  (tipado estricto)
```

**Regla operativa**: cuando se modifique un modelo en `shared/commonMain` o un endpoint en `server/`, regenerar los tipos del frontend ejecutando `openapi-typescript`.

## 2. Infraestructura Local (Docker)

El entorno de desarrollo usa Docker Compose para levantar PostgreSQL sin dependencias de instalación local.

### Archivo: `docker-compose.yml` (raíz del monorepo)

| Servicio | Imagen | Puerto | Volumen |
|---|---|---|---|
| `postgres` | `postgres:17` | `5433:5432` | `zentrack_postgres_data` |

**Credenciales de desarrollo** (solo local, nunca se commitean):

| Variable | Valor |
|---|---|
| `POSTGRES_DB` | `zentrack_db` |
| `POSTGRES_USER` | `zentrack` |
| `POSTGRES_PASSWORD` | `zentrack_dev` |

```bash
docker compose up -d     # Levanta la BD en background
docker compose down      # Para (datos persisten en volumen)
docker compose down -v   # Reset completo del volumen
```

La configuración de conexión vive en `server/src/main/resources/application.conf` (excluido de git vía `.gitignore`).

## 3. Esquema de Base de Datos (PostgreSQL)

Todos los PKs son `Long` (BIGSERIAL). Sin excepciones.

### Convención de Auditoría

Todas las tablas incluyen estas columnas (no se repiten en cada tabla):

| Columna | Tipo | Notas |
|---|---|---|
| `created_at` | TIMESTAMP NOT NULL | Auto-set en INSERT |
| `created_by` | BIGINT FK → users | Nullable solo en auto-creación del sistema |
| `updated_at` | TIMESTAMP NOT NULL | Auto-set en INSERT y UPDATE |
| `updated_by` | BIGINT FK → users | Nullable hasta primera edición |

Las tablas de relación N:M puras solo incluyen `created_at` y `created_by`.

---

### Autenticación

**`users`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `email` | VARCHAR | UNIQUE NOT NULL |
| `password_hash` | VARCHAR | nullable — null si el usuario usa solo OAuth |
| `name` | VARCHAR | NOT NULL |
| `avatar_url` | VARCHAR | nullable — avatar de Google u otro proveedor |
| `user_type` | ENUM(`regular`, `client`) | NOT NULL DEFAULT `regular` |

**`oauth_accounts`** — Cuentas OAuth vinculadas a un usuario

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `user_id` | BIGINT | FK → users NOT NULL |
| `provider` | ENUM(`google`) | NOT NULL |
| `provider_user_id` | VARCHAR | NOT NULL |
| `email` | VARCHAR | NOT NULL |
| `access_token` | VARCHAR | nullable — cifrado AES-256 |
| `refresh_token` | VARCHAR | nullable — cifrado AES-256 |
| `token_expires_at` | TIMESTAMP | nullable |

Constraint: `UNIQUE(provider, provider_user_id)`.

**`refresh_tokens`** — Tokens de refresco internos de ZenTrack

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `user_id` | BIGINT | FK → users NOT NULL |
| `token_hash` | VARCHAR | NOT NULL — SHA-256 del token |
| `expires_at` | TIMESTAMP | NOT NULL |
| `revoked_at` | TIMESTAMP | nullable |

---

### Organizaciones y Teams

**`organizations`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `name` | VARCHAR | NOT NULL |
| `slug` | VARCHAR | UNIQUE NOT NULL |
| `plan` | VARCHAR | DEFAULT `free` |
| `is_personal` | BOOLEAN | NOT NULL DEFAULT FALSE |

**`organization_members`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `org_id` | BIGINT | FK → organizations, PK compuesta |
| `user_id` | BIGINT | FK → users, PK compuesta |
| `role` | ENUM(`owner`, `admin`, `member`) | NOT NULL |
| `joined_at` | TIMESTAMP | NOT NULL |

Constraint: solo usuarios `regular` pueden estar en esta tabla.

**`teams`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `org_id` | BIGINT | FK → organizations NOT NULL |
| `name` | VARCHAR | NOT NULL |
| `color_hex` | VARCHAR | nullable |

**`team_members`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `team_id` | BIGINT | FK → teams, PK compuesta |
| `user_id` | BIGINT | FK → users, PK compuesta |
| `role` | ENUM(`admin`, `manager`, `member`) | NOT NULL |
| `joined_at` | TIMESTAMP | NOT NULL |

Constraint: `user_id` debe existir en `organization_members` para la org del team.

---

### Workspaces

**`workspaces`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `org_id` | BIGINT | FK → organizations NOT NULL |
| `name` | VARCHAR | NOT NULL |
| `created_by` | BIGINT | FK → users (informativo) |

**`workspace_teams`** — N:M workspace ↔ team

| Columna | Tipo | Restricciones |
|---|---|---|
| `workspace_id` | BIGINT | FK → workspaces, PK compuesta |
| `team_id` | BIGINT | FK → teams, PK compuesta |
| `assigned_at` | TIMESTAMP | NOT NULL |

Constraint: team y workspace deben pertenecer a la misma org.

**`workspace_members`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `workspace_id` | BIGINT | FK → workspaces, PK compuesta |
| `user_id` | BIGINT | FK → users, PK compuesta |
| `role` | ENUM(`admin`, `manager`, `member`, `client`) | NOT NULL |
| `joined_at` | TIMESTAMP | NOT NULL |

Constraints: rol `client` → `user_type = client`; roles internos → `user_type = regular`.

---

### Proyectos

**`projects`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `workspace_id` | BIGINT | FK → workspaces NOT NULL |
| `project_key` | VARCHAR | UNIQUE dentro del mismo `workspace_id` |
| `name` | VARCHAR | NOT NULL |
| `description` | VARCHAR | nullable |

**`project_members`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `project_id` | BIGINT | FK → projects, PK compuesta |
| `user_id` | BIGINT | FK → users, PK compuesta |
| `role` | ENUM(`admin`, `manager`, `member`, `viewer`, `client`) | NOT NULL |
| `joined_at` | TIMESTAMP | NOT NULL |

---

### Solicitudes de Membresía

**`membership_requests`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `requester_id` | BIGINT | FK → users NOT NULL |
| `target_type` | ENUM(`organization`, `team`, `workspace`) | NOT NULL |
| `target_id` | BIGINT | ID del target según `target_type` |
| `status` | ENUM(`pending`, `approved`, `rejected`) | NOT NULL DEFAULT `pending` |
| `reviewed_by` | BIGINT | FK → users nullable |
| `reviewed_at` | TIMESTAMP | nullable |

---

### Entidades Ágiles

**`sprints`** — Nivel workspace

| Columna | Tipo |
|---|---|
| `id` | BIGSERIAL PK |
| `workspace_id` | FK → workspaces |
| `name` | VARCHAR |
| `start_date` | DATE |
| `end_date` | DATE |
| `status` | ENUM(`planning`, `active`, `closed`) |

**`tags`** — Nivel workspace

| Columna | Tipo |
|---|---|
| `id` | BIGSERIAL PK |
| `workspace_id` | FK → workspaces |
| `name` | VARCHAR |
| `color_hex` | VARCHAR |

**`task_statuses`** — Workflows configurables por workspace

| Columna | Tipo |
|---|---|
| `id` | BIGSERIAL PK |
| `workspace_id` | FK → workspaces |
| `name` | VARCHAR |
| `order_index` | INT |
| `is_default` | BOOLEAN |

---

### Tareas

**`tasks`**

| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `project_id` | BIGINT | FK → projects |
| `sprint_id` | BIGINT nullable | FK → sprints |
| `parent_id` | BIGINT nullable | FK → tasks (subtareas) |
| `task_number` | INT | Autoincremental por proyecto — `SELECT FOR UPDATE` |
| `display_id` | VARCHAR | Generado: `PROJECT_KEY-task_number` — inmutable |
| `title` | VARCHAR | NOT NULL |
| `description` | TEXT | nullable |
| `status_id` | BIGINT | FK → task_statuses |
| `priority` | ENUM(`low`, `medium`, `high`, `critical`) | |
| `estimate` | INT nullable | Puntos de historia o horas |
| `start_date` | DATE nullable | |
| `due_date` | DATE nullable | |
| `git_branch_name` | VARCHAR nullable | null = borrador o fallo API Git |

**`task_assignees`** — N:M tareas ↔ usuarios

| Columna | Tipo |
|---|---|
| `task_id` | BIGINT FK |
| `user_id` | BIGINT FK |

**`task_tags`** — N:M tareas ↔ etiquetas

| Columna | Tipo |
|---|---|
| `task_id` | BIGINT FK |
| `tag_id` | BIGINT FK |

---

## 4. Estructura de Endpoints (API Ktor)

Todas las rutas excepto auth requieren `Authorization: Bearer <JWT>`.

### Autenticación

```
# Email / contraseña
POST /api/auth/register
POST /api/auth/login

# OAuth 2.0 — Google (Authorization Code Flow)
GET  /api/auth/google                         Inicia flujo OAuth → redirect a Google
GET  /api/auth/google/callback                Callback — intercambia code, emite JWT
POST /api/auth/refresh                        Renueva JWT con refresh token interno
POST /api/auth/logout                         Invalida JWT

# Perfil y cuentas OAuth
GET    /api/users/me
PUT    /api/users/me
GET    /api/users/me/oauth                    Lista cuentas OAuth vinculadas
POST   /api/users/me/oauth/google             Vincula cuenta Google a usuario existente
DELETE /api/users/me/oauth/{id}              Desvincula cuenta OAuth
```

### Organizaciones

```
GET    /api/organizations
POST   /api/organizations
GET    /api/organizations/{org_id}
GET    /api/organizations/{org_id}/members
POST   /api/organizations/{org_id}/members
DELETE /api/organizations/{org_id}/members/{uid}
GET    /api/organizations/{org_id}/requests
POST   /api/organizations/{org_id}/requests/{id}/approve
POST   /api/organizations/{org_id}/requests/{id}/reject
```

### Teams

```
GET    /api/organizations/{org_id}/teams
POST   /api/organizations/{org_id}/teams
GET    /api/teams/{team_id}/members
POST   /api/teams/{team_id}/members
DELETE /api/teams/{team_id}/members/{uid}
GET    /api/teams/{team_id}/requests
POST   /api/teams/{team_id}/requests/{id}/approve
POST   /api/teams/{team_id}/requests/{id}/reject
```

### Workspaces

```
GET    /api/organizations/{org_id}/workspaces
POST   /api/organizations/{org_id}/workspaces
GET    /api/workspaces/{w_id}
GET    /api/workspaces/{w_id}/members
POST   /api/workspaces/{w_id}/members
DELETE /api/workspaces/{w_id}/members/{uid}
GET    /api/workspaces/{w_id}/requests
POST   /api/workspaces/{w_id}/requests/{id}/approve
POST   /api/workspaces/{w_id}/requests/{id}/reject
GET    /api/workspaces/{w_id}/teams
POST   /api/workspaces/{w_id}/teams
DELETE /api/workspaces/{w_id}/teams/{team_id}
```

### Solicitudes (usuario solicitante)

```
POST /api/membership-requests
GET  /api/membership-requests
```

### Proyectos

```
GET    /api/workspaces/{w_id}/projects
POST   /api/workspaces/{w_id}/projects
GET    /api/projects/{p_id}
GET    /api/projects/{p_id}/members
POST   /api/projects/{p_id}/members
DELETE /api/projects/{p_id}/members/{uid}
```

### Tareas

```
GET    /api/workspaces/{w_id}/tasks    (filtros: project_id, sprint_id, assignee, status…)
POST   /api/projects/{p_id}/tasks
GET    /api/tasks/{t_id}
PUT    /api/tasks/{t_id}
POST   /api/tasks/{t_id}/assignees
POST   /api/tasks/{t_id}/subtasks
```

### Sprints, Estados y Tags

```
GET    /api/workspaces/{w_id}/sprints
POST   /api/workspaces/{w_id}/sprints
GET    /api/workspaces/{w_id}/statuses
POST   /api/workspaces/{w_id}/statuses
GET    /api/workspaces/{w_id}/tags
POST   /api/workspaces/{w_id}/tags
```

### Webhooks Git

```
POST   /api/webhooks/git    Público, protegido por secret del proveedor
```

## 5. Estrategia de Autenticación

### OAuth 2.0 Google — Authorization Code Flow

```
Cliente
  ├─[1]─► GET /api/auth/google → Redirect 302 → accounts.google.com
  │                              (client_id, redirect_uri, scope: openid email profile, state)
  ├─[2]── Usuario aprueba en Google
  └─[3]─◄── GET /api/auth/google/callback?code=...&state=...
               ├─ Valida state (anti-CSRF)
               ├─ Intercambia code → access_token + refresh_token (Google)
               ├─ Llama Google UserInfo API → email, name, picture, sub
               ├─ Crea o vincula usuario en BD
               ├─ Almacena tokens de Google cifrados (AES-256) en oauth_accounts
               └─► Emite JWT interno de ZenTrack → cliente
```

- Los tokens de Google **nunca se exponen al cliente**; solo se usa el JWT interno.
- El `state` se valida para prevenir CSRF.
- Scope mínimo: `openid email profile`.
- No se puede desvincular Google si es el único método de login.

## 6. Estructura de Directorios (Monorepo)

```
zentrackapp/
├── server/                   # Ktor Server API
│   ├── src/main/kotlin/
│   │   ├── api/              # Controladores (Routes): auth, orgs, teams, workspaces, projects, tasks
│   │   ├── core/             # Lógica de negocio (Services) y Auth JWT + OAuth
│   │   ├── db/               # Tablas Exposed y Migraciones Flyway
│   │   ├── integrations/     # Clientes HTTP para GitLab/GitHub API
│   │   └── Application.kt
├── shared/                   # KMP Module — targets: jvm + androidTarget
│   ├── commonMain/           # Models (@Serializable), DTOs, Ktor Client, Repositories
│   ├── jvmMain/              # Expect/Actual JVM → server + cli
│   └── androidMain/          # Expect/Actual Android → androidApp
├── androidApp/               # Jetpack Compose Android
│   └── src/main/kotlin/
│       ├── ui/screens/       # Orgs, Workspaces, Board, Backlog, TaskDetail
│       ├── ui/components/    # Material 3 UI Components reutilizables
│       └── ui/theme/         # ZenTrackTheme, Color, Type
├── cli/                      # Kotlin/JVM CLI (Clikt)
│   └── src/main/kotlin/
│       └── commands/         # Comandos Clikt (tasks, workspaces, sprints…)
└── webApp/                   # React 19 + TypeScript + Zustand + MUI
    ├── src/
    │   ├── components/       # Componentes MUI reutilizables (presentacionales)
    │   ├── screens/          # Pantallas (Orgs, Workspaces, Board, Backlog, TaskDetail)
    │   ├── store/            # Estado global Zustand (un archivo por dominio)
    │   ├── services/         # Llamadas a la API Ktor vía fetch nativo
    │   └── types/            # api.ts generado por openapi-typescript
    ├── package.json
    └── tsconfig.json         # TypeScript strict mode
```

## 7. Integración con Git y Manejo de Concurrencia

- **Concurrencia de IDs:** Para el incremento de `task_number` por proyecto se usa `SELECT ... FOR UPDATE` en la tabla `projects` para garantizar atomicidad y evitar IDs duplicados bajo alta carga.

- **Flujo Git:** Ktor hace la petición a la API de Git. Si falla, la BD guarda la tarea con `git_branch_name = null` para permitir un reintento posterior desde el detalle de la tarea.
