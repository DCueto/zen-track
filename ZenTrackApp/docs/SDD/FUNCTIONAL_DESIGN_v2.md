# Diseño Funcional v2 — ZenTrack

> **Estado:** Borrador para validación  
> **Fecha:** 2026-05-11  
> **Motivo del rediseño:** El modelo original (Workspace como techo del multi-tenancy) no soportaba la estructura organizativa real de una empresa con departamentos/equipos ni el escalado a múltiples clientes/empresas externas.

---

## 1. Visión General

ZenTrack es una plataforma de gestión de proyectos ágil, multi-organización, con integración profunda en GitFlow. Su modelo de datos refleja la estructura real de una empresa de servicios: una organización tiene departamentos (teams), cada departamento gestiona clientes (workspaces), y dentro de cada cliente hay proyectos con tareas.

La plataforma soporta además **usuarios externos de tipo cliente**, que acceden de forma limitada a proyectos concretos de un workspace sin pertenecer a la organización.

---

## 2. Jerarquía de Entidades

```
Usuario (regular | client)
│
├── Organización Personal [auto-creada al registrarse]
│   └── Workspaces personales → Proyectos → Tareas
│
└── Organización(es) Empresariales [puede pertenecer a N]
    ├── Teams (departamentos)
    │   ├── Miembros del team (admin / manager / member)
    │   └── Asignaciones N:M a Workspaces
    └── Workspaces (clientes)
        ├── Miembros del workspace (admin / manager / member / client)
        └── Proyectos
            ├── Miembros del proyecto (admin / manager / member / viewer / client)
            └── Tareas
                ├── Subtareas
                ├── Sprints (nivel workspace)
                ├── Estados personalizados (nivel workspace)
                └── Etiquetas (nivel workspace)
```

---

## 3. Tipos de Usuario

| Tipo | Descripción | Puede estar en |
|---|---|---|
| `regular` | Usuario interno. Empleado de una organización | Orgs, Teams, Workspaces, Proyectos |
| `client` | Usuario externo. Cliente de un workspace | Workspaces (rol `client`) + Proyectos (rol `client`) |

### Regla de coexistencia
Un usuario `regular` puede recibir acceso a un proyecto como `viewer` o `client` si alguien quiere darle visibilidad sin permisos de edición. El `user_type` define su perfil de registro, no limita la asignación de roles.

Un usuario `client` **nunca** puede pertenecer a `organization_members` ni `team_members`. Su único punto de entrada es `workspace_members` con rol `client`.

---

## 4. Modelo de Datos Completo

Todos los PKs son `Long` (autoincremental). Sin excepciones.

### 4.0 Convención de Auditoría

**Todas las tablas del sistema son auditables.** Cada tabla incluye las siguientes columnas de auditoría:

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `created_at` | TIMESTAMP | NOT NULL | Cuándo se creó el registro |
| `created_by` | Long | FK → users, nullable | Quién lo creó (null solo en auto-creación del sistema, ej. org personal) |
| `updated_at` | TIMESTAMP | NOT NULL | Última actualización (igual a `created_at` en el momento de inserción) |
| `updated_by` | Long | FK → users, nullable | Quién realizó la última modificación |

> Las tablas de relación pura N:M (ej. `workspace_teams`, `task_tags`) solo incluyen `created_at` y `created_by` ya que no tienen campos mutables propios.

En las definiciones de tabla que siguen **no se repiten estas columnas** para evitar redundancia, pero se entiende que están presentes en todas.

---

### 4.1 Usuarios

**`users`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | Long | PK |
| `email` | VARCHAR | UNIQUE NOT NULL |
| `password_hash` | VARCHAR | nullable — null si el usuario se registró solo por OAuth |
| `name` | VARCHAR | NOT NULL |
| `avatar_url` | VARCHAR | nullable — URL de avatar de Google u otro proveedor OAuth |
| `user_type` | ENUM(`regular`, `client`) | NOT NULL DEFAULT `regular` |
| `created_at` | TIMESTAMP | NOT NULL (columna de auditoría, aquí explícita) |

> `password_hash` puede ser null para usuarios que se registraron exclusivamente vía OAuth (Google). En ese caso, el login por contraseña no está disponible para esa cuenta.

**`oauth_accounts`** — Cuentas OAuth vinculadas a un usuario

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | Long | PK |
| `user_id` | Long | FK → users NOT NULL |
| `provider` | ENUM(`google`) | NOT NULL — extensible a otros proveedores |
| `provider_user_id` | VARCHAR | NOT NULL — ID del usuario en el proveedor OAuth |
| `email` | VARCHAR | NOT NULL — email registrado en el proveedor |
| `access_token` | VARCHAR | nullable — token de acceso actual (cifrado en BD) |
| `refresh_token` | VARCHAR | nullable — refresh token (cifrado en BD) |
| `token_expires_at` | TIMESTAMP | nullable |

Constraint: `UNIQUE(provider, provider_user_id)` — un mismo proveedor+ID no puede vincularse a dos usuarios.

---

### 4.2 Organizaciones

**`organizations`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | Long | PK |
| `name` | VARCHAR | NOT NULL |
| `slug` | VARCHAR | UNIQUE NOT NULL — identificador de URL (ej. `basetis`) |
| `plan` | VARCHAR | DEFAULT `free` |
| `is_personal` | BOOLEAN | NOT NULL DEFAULT FALSE — TRUE solo para org personal |
| `created_at` | TIMESTAMP | NOT NULL |

**`organization_members`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `org_id` | Long | FK → organizations, PK compuesta |
| `user_id` | Long | FK → users, PK compuesta |
| `role` | ENUM(`owner`, `admin`, `member`) | NOT NULL |
| `joined_at` | TIMESTAMP | NOT NULL |

Constraint: solo usuarios `regular` pueden estar en esta tabla.

---

### 4.3 Teams

**`teams`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | Long | PK |
| `org_id` | Long | FK → organizations NOT NULL |
| `name` | VARCHAR | NOT NULL |
| `color_hex` | VARCHAR | nullable — color para identificación visual |
| `created_at` | TIMESTAMP | NOT NULL |

**`team_members`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `team_id` | Long | FK → teams, PK compuesta |
| `user_id` | Long | FK → users, PK compuesta |
| `role` | ENUM(`admin`, `manager`, `member`) | NOT NULL |
| `joined_at` | TIMESTAMP | NOT NULL |

Constraint: `user_id` debe existir previamente en `organization_members` para la org del team.

---

### 4.4 Workspaces

**`workspaces`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | Long | PK |
| `org_id` | Long | FK → organizations NOT NULL |
| `name` | VARCHAR | NOT NULL |
| `created_by` | Long | FK → users (informativo, no implica propiedad) |
| `created_at` | TIMESTAMP | NOT NULL |

**`workspace_teams`** — Relación N:M workspace ↔ team

| Columna | Tipo | Restricciones |
|---|---|---|
| `workspace_id` | Long | FK → workspaces, PK compuesta |
| `team_id` | Long | FK → teams, PK compuesta |
| `assigned_at` | TIMESTAMP | NOT NULL |

Constraint: el team y el workspace deben pertenecer a la misma organización.

**`workspace_members`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `workspace_id` | Long | FK → workspaces, PK compuesta |
| `user_id` | Long | FK → users, PK compuesta |
| `role` | ENUM(`admin`, `manager`, `member`, `client`) | NOT NULL |
| `joined_at` | TIMESTAMP | NOT NULL |

Constraint para rol `client`: el `user_id` debe tener `user_type = client` en `users`.  
Constraint para roles internos: el `user_id` debe tener `user_type = regular`.

---

### 4.5 Proyectos

**`projects`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | Long | PK |
| `workspace_id` | Long | FK → workspaces NOT NULL |
| `project_key` | VARCHAR | UNIQUE dentro del mismo `workspace_id` |
| `name` | VARCHAR | NOT NULL |
| `description` | VARCHAR | nullable |
| `created_at` | TIMESTAMP | NOT NULL |

**`project_members`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `project_id` | Long | FK → projects, PK compuesta |
| `user_id` | Long | FK → users, PK compuesta |
| `role` | ENUM(`admin`, `manager`, `member`, `viewer`, `client`) | NOT NULL |
| `joined_at` | TIMESTAMP | NOT NULL |

Constraints:
- Para roles `admin/manager/member/viewer`: el `user_id` debe existir en `workspace_members` del workspace del proyecto con rol interno (`admin/manager/member`).
- Para rol `client`: el `user_id` debe existir en `workspace_members` del mismo workspace con rol `client`.

---

### 4.6 Solicitudes de Membresía

**`membership_requests`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | Long | PK |
| `requester_id` | Long | FK → users NOT NULL |
| `target_type` | ENUM(`organization`, `team`, `workspace`) | NOT NULL |
| `target_id` | Long | ID de la organización, team o workspace según `target_type` |
| `status` | ENUM(`pending`, `approved`, `rejected`) | NOT NULL DEFAULT `pending` |
| `reviewed_by` | Long | FK → users nullable |
| `created_at` | TIMESTAMP | NOT NULL |
| `reviewed_at` | TIMESTAMP | nullable |

Nota: la asignación a **proyectos** es siempre directa por workspace admin/manager. No existe `membership_request` de tipo `project`.

---

### 4.7 Entidades Ágiles (sin cambios estructurales)

**`sprints`** — Nivel workspace

| Columna | Tipo |
|---|---|
| `id` | Long PK |
| `workspace_id` | FK → workspaces |
| `name` | VARCHAR |
| `start_date` | DATE |
| `end_date` | DATE |
| `status` | ENUM(`planning`, `active`, `closed`) |

**`tags`** — Nivel workspace

| Columna | Tipo |
|---|---|
| `id` | Long PK |
| `workspace_id` | FK → workspaces |
| `name` | VARCHAR |
| `color_hex` | VARCHAR |

**`task_statuses`** — Workflows configurables por workspace

| Columna | Tipo |
|---|---|
| `id` | Long PK |
| `workspace_id` | FK → workspaces |
| `name` | VARCHAR |
| `order_index` | Int |
| `is_default` | BOOLEAN |

---

### 4.8 Tareas (sin cambios estructurales)

**`tasks`**

| Columna | Tipo | Notas |
|---|---|---|
| `id` | Long | PK |
| `project_id` | Long | FK → projects |
| `sprint_id` | Long nullable | FK → sprints |
| `parent_id` | Long nullable | FK → tasks (subtareas) |
| `task_number` | Int | Autoincremental por proyecto, generado con SELECT FOR UPDATE |
| `display_id` | VARCHAR | Generado: `PROJECT_KEY-task_number` (ej. `ZTK-25`) — inmutable |
| `title` | VARCHAR | NOT NULL |
| `description` | TEXT | nullable |
| `status_id` | Long | FK → task_statuses |
| `priority` | ENUM | (low/medium/high/critical) |
| `estimate` | Int nullable | Puntos de historia o horas |
| `start_date` | DATE nullable | |
| `due_date` | DATE nullable | |
| `git_branch_name` | VARCHAR nullable | null = borrador o fallo API Git |

**`task_assignees`** — N:M tareas ↔ usuarios

| Columna | Tipo |
|---|---|
| `task_id` | Long FK |
| `user_id` | Long FK |

**`task_tags`** — N:M tareas ↔ etiquetas

| Columna | Tipo |
|---|---|
| `task_id` | Long FK |
| `tag_id` | Long FK |

---

## 5. Sistema de Roles y Permisos

### 5.1 Roles por nivel

| Nivel | Roles disponibles |
|---|---|
| Organización | `owner` · `admin` · `member` |
| Team | `admin` · `manager` · `member` |
| Workspace | `admin` · `manager` · `member` · `client` |
| Proyecto | `admin` · `manager` · `member` · `viewer` · `client` |

### 5.2 Permisos por rol — Organización

| Acción | owner | admin | member |
|---|---|---|---|
| Eliminar la organización | ✅ | ❌ | ❌ |
| Crear / eliminar teams | ✅ | ✅ | ❌ |
| Crear / eliminar workspaces | ✅ | ✅ | ❌ |
| Gestionar miembros de la org | ✅ | ✅ | ❌ |
| Auto-asignarse a cualquier team/workspace sin solicitud | ✅ | ✅ | ❌ |
| Ver estructura de la org (teams, workspaces) | ✅ | ✅ | Solo los asignados |

### 5.3 Permisos por rol — Team

| Acción | admin | manager | member |
|---|---|---|---|
| Eliminar el team | ✅ | ❌ | ❌ |
| Gestionar miembros del team | ✅ | ✅ | ❌ |
| Aceptar solicitudes de entrada al team | ✅ | ✅ | ❌ |
| Asignar el team a un workspace | ✅ | ✅ | ❌ |
| Aceptar solicitudes de entrada a workspaces donde el team está asignado | ✅ | ✅ | ❌ |
| Acceder a los workspaces del team | ✅ | ✅ | ✅ |

### 5.4 Permisos por rol — Workspace

| Acción | admin | manager | member | client |
|---|---|---|---|---|
| Configuración del workspace | ✅ | ❌ | ❌ | ❌ |
| Gestionar miembros del workspace | ✅ | ✅ | ❌ | ❌ |
| Aceptar solicitudes de entrada al workspace | ✅ | ✅ | ❌ | ❌ |
| Asignar usuarios a proyectos | ✅ | ✅ | ❌ | ❌ |
| Ver todos los proyectos del workspace | ✅ | ✅ | ✅ | ❌ |
| Ver sprints, estados y tags del workspace | ✅ | ✅ | ✅ | ❌ |
| Ver solo proyectos asignados en `project_members` | — | — | — | ✅ |

### 5.5 Permisos por rol — Proyecto

| Acción | admin | manager | member | viewer | client |
|---|---|---|---|---|---|
| Configurar el proyecto (nombre, key) | ✅ | ❌ | ❌ | ❌ | ❌ |
| Gestionar miembros del proyecto | ✅ | ✅ | ❌ | ❌ | ❌ |
| Ver todas las tareas | ✅ | ✅ | ✅ | ✅ | ✅ |
| Crear tareas | ✅ | ✅ | ✅ | ❌ | ✅ |
| Editar cualquier tarea | ✅ | ✅ | ❌ | ❌ | ❌ |
| Editar tareas propias o asignadas | ✅ | ✅ | ✅ | ❌ | ❌ |
| Mover tareas entre estados (kanban) | ✅ | ✅ | ✅ | ❌ | ❌ |
| Eliminar tareas | ✅ | ✅ | ❌ | ❌ | ❌ |

### 5.6 Reglas de autorización (lógica de negocio)

```
canManageTeamMembers(userId, teamId):
  team_members[userId, teamId].role IN ('admin', 'manager')

canManageWorkspaceMembers(userId, workspaceId):
  (A) workspace_members[userId, workspaceId].role IN ('admin', 'manager')
  OR
  (B) EXISTS team t WHERE:
        workspace_teams[t.id, workspaceId] EXISTS
        AND team_members[userId, t.id].role IN ('admin', 'manager')

canManageProjectMembers(userId, projectId):
  LET workspaceId = projects[projectId].workspace_id
  workspace_members[userId, workspaceId].role IN ('admin', 'manager')
  -- Solo vía directa. Team admin/manager NO tiene este permiso.

canSelfJoinWithoutRequest(userId, targetId, targetType):
  LET orgId = organización del target
  organization_members[userId, orgId].role IN ('owner', 'admin')
  -- Permite auto-asignarse pero NO otorga acceso hasta hacerlo explícitamente.
```

---

## 6. Flujos de Negocio

### 6.1 Registro de usuario regular

**Vía email/contraseña:**

1. Usuario introduce: nombre, email, contraseña
2. *(Opcional)* Busca organizaciones por nombre/slug → selecciona una → se envía `membership_request` de tipo `organization` (estado `pending`)
3. El sistema crea automáticamente:
   - Fila en `users` con `user_type = regular`, `password_hash` relleno, `avatar_url = null`
   - Fila en `organizations` con `is_personal = true` y slug derivado del email
   - Fila en `organization_members` con rol `owner` para esa org personal
4. El usuario accede a la app con su org personal activa
5. Si envió solicitud: queda en `pending` hasta que un org admin/owner la apruebe

**Vía Google OAuth 2.0:**

1. Usuario pulsa "Continuar con Google" → se abre el flujo de autorización OAuth 2.0 de Google
2. Google devuelve el `authorization_code` al backend
3. El backend intercambia el código por `access_token` y `refresh_token`
4. El backend llama a la API de Google con el token para obtener: `email`, `name`, `avatar_url`, `provider_user_id` (sub)
5. Si el email no existe en `users`: se crea automáticamente (mismo flujo que el paso 3 del path email, con `password_hash = null` y `avatar_url` de Google)
6. Si el email ya existe: se vincula la cuenta OAuth añadiendo fila en `oauth_accounts`
7. Se crea fila en `oauth_accounts` con `provider = google` y se almacenan los tokens cifrados
8. El backend emite un JWT interno de ZenTrack para la sesión (el cliente nunca maneja los tokens de Google directamente)

> **Regla de seguridad:** Los tokens de Google (`access_token`, `refresh_token`) se almacenan cifrados (AES-256) en la columna correspondiente de `oauth_accounts`. Nunca se exponen al cliente.

### 6.2 Registro de usuario cliente

**Vía email/contraseña:**

1. Usuario introduce: nombre, email, contraseña
2. El sistema crea:
   - Fila en `users` con `user_type = client`, `password_hash` relleno
   - **No** se crea org personal ni se puede unir a organizaciones
3. El usuario ve una pantalla de espera hasta que un workspace admin/manager lo añada a un workspace

**Vía Google OAuth 2.0:**

1. Mismo flujo OAuth que en 6.1, pero al final el sistema detecta que `user_type = client` (o lo infiere por el contexto del enlace de invitación que trajo al usuario)
2. Se crea `users` con `user_type = client` y `password_hash = null`
3. No se crea org personal

### 6.3 Flujo de solicitud para unirse a una organización

1. Usuario busca org por nombre o slug
2. Envía `membership_request` (target_type: `organization`, target_id: org_id)
3. Cualquier `organization_members[reviewer, org_id].role IN ('owner', 'admin')` recibe notificación
4. Aprueba → se crea `organization_members[user_id, org_id]` con rol `member`
5. Rechaza → `membership_request.status = rejected`

### 6.4 Flujo de solicitud para unirse a un team

1. Usuario (ya miembro de la org) solicita unirse a un team visible
2. Envía `membership_request` (target_type: `team`, target_id: team_id)
3. Cualquier `team_members[reviewer, team_id].role IN ('admin', 'manager')` puede aprobar
4. Aprueba → `team_members[user_id, team_id]` con rol `member`

### 6.5 Flujo de solicitud para unirse a un workspace

1. Usuario solicita acceso a un workspace
2. Envía `membership_request` (target_type: `workspace`, target_id: workspace_id)
3. Puede aprobar cualquiera que cumpla `canManageWorkspaceMembers`:
   - Un `workspace_members` con rol admin/manager, **o**
   - Un `team_members` con rol admin/manager de un team asignado a ese workspace
4. Aprueba → `workspace_members[user_id, workspace_id]` con rol `member` (siempre `member`, nunca otro)

### 6.6 Auto-asignación de org owner/admin

1. Org owner/admin navega a un team o workspace de su org
2. Pulsa "Unirme" → se crea directamente la fila en `team_members` o `workspace_members` (sin pasar por `membership_requests`)
3. Rol inicial: `admin`
4. Hasta que no se auto-asigne, **no tiene acceso** al contenido ni a las acciones de gestión

### 6.7 Incorporación de usuario cliente a un workspace y proyecto

1. Workspace admin/manager busca al usuario cliente por email
2. Lo añade a `workspace_members` con rol `client`
3. El cliente puede iniciar sesión y ver el workspace (sin proyectos visibles aún)
4. Workspace admin/manager lo añade a `project_members` con rol `client` para los proyectos pertinentes
5. El cliente ya ve las tareas de esos proyectos y puede crear nuevas

### 6.8 Asignación de un team a un workspace

1. Org admin/owner, o workspace admin/manager, selecciona un team para asignar al workspace
2. Se crea fila en `workspace_teams`
3. Los miembros del team NO reciben acceso automáticamente a `workspace_members` — el team solo habilita que sus admin/manager puedan gestionar el workspace
4. Para tener acceso al workspace, cada miembro del team debe ser añadido individualmente a `workspace_members` (o enviar solicitud)

---

## 7. Historias de Usuario

### Historia 0 — Registro e Incorporación

**H0.1 — Registro básico**
Como nuevo usuario, quiero registrarme con nombre, email y contraseña para acceder a ZenTrack.
- Al completar el registro se crea mi organización personal automáticamente.
- No necesito pertenecer a ninguna organización empresarial para empezar a usar la app.

**H0.2 — Búsqueda de organización durante el registro**
Como nuevo usuario, durante el proceso de registro quiero buscar organizaciones existentes por nombre o slug para enviar una solicitud de adhesión junto con mi registro.
- La solicitud queda en estado `pending` hasta que un admin/owner la apruebe.
- Puedo completar el registro sin seleccionar ninguna organización.

**H0.3 — Multi-organización**
Como usuario regular, quiero poder pertenecer a varias organizaciones simultáneamente para gestionar proyectos de diferentes empresas desde la misma cuenta.
- En la app puedo cambiar de contexto de organización.
- Cada organización muestra sus propios teams y workspaces.

**H0.4 — Organización personal**
Como usuario regular, quiero disponer de una organización personal donde guardar workspaces y proyectos que no pertenecen a ninguna empresa, para gestionar trabajo personal o freelance.
- La org personal se crea automáticamente al registrarme.
- Puedo invitar a otros usuarios a mi org personal para colaborar.
- La UI distingue visualmente la org personal de las empresariales.

---

### Historia 0b — Gestión de Organización

**H0b.1 — Crear organización empresarial**
Como usuario regular, quiero crear una organización empresarial con nombre y slug único para que otros miembros de mi empresa puedan unirse.

**H0b.2 — Gestionar miembros de la organización**
Como org owner/admin, quiero ver las solicitudes de entrada pendientes y aprobarlas o rechazarlas para controlar quién forma parte de mi organización.

**H0b.3 — Crear y configurar teams**
Como org owner/admin, quiero crear departamentos/teams dentro de la organización y asignarles un nombre y color identificativo.

**H0b.4 — Auto-asignación sin solicitud**
Como org owner/admin, quiero poder unirme directamente a cualquier team o workspace de mi organización sin necesitar aprobación, para poder actuar cuando haya un problema o nadie disponible para aprobar.
- Al unirme, recibo rol `admin` en ese team o workspace.
- Hasta que me auto-asigne, no tengo acceso a su contenido.

---

### Historia 0c — Gestión de Teams

**H0c.1 — Gestionar miembros del team**
Como team admin/manager, quiero ver las solicitudes de entrada a mi team y aprobarlas o rechazarlas.
- Los usuarios aprobados reciben rol `member` en el team.

**H0c.2 — Asignar team a workspace**
Como team admin/manager (o org admin/owner), quiero asignar mi team a un workspace para que mis admin/managers puedan gestionar ese workspace.
- La asignación no da acceso automático al workspace a todos los miembros del team.

**H0c.3 — Gestionar miembros de workspaces asignados al team**
Como team admin/manager de un team asignado a un workspace, quiero poder aceptar solicitudes de entrada a ese workspace y añadir miembros directamente.
- Los usuarios añadidos por esta vía siempre reciben rol `member` en el workspace.

---

### Historia 1 — Navegación de Workspaces

**H1.1 — Panel raíz de workspaces**
Como miembro, al iniciar sesión quiero ver todos los workspaces a los que tengo acceso para seleccionar el contexto de trabajo.
- Un workspace aparece si tengo fila en `workspace_members` (directa) o si pertenezco a un team asignado a ese workspace.
- Los workspaces de mi org personal aparecen siempre en una sección separada.

**H1.2 — Crear workspace**
Como org owner/admin, quiero crear un nuevo workspace dentro de una organización y opcionalmente asignarlo a uno o varios teams desde el momento de la creación.

**H1.3 — Gestionar miembros del workspace**
Como workspace admin/manager, quiero añadir miembros al workspace, ver la lista de miembros actuales y cambiar o revocar sus roles.

**H1.4 — Configurar proyecto dentro del workspace**
Como workspace admin/manager, quiero crear proyectos dentro del workspace definiendo un nombre y un `project_key` único (ej. `ZTK`), de forma que todas las tareas del proyecto hereden ese identificador.
- El `project_key` debe ser único dentro del mismo workspace.
- Las tareas adoptan el formato `[PROJECT_KEY]-[N]` (ej. `ZTK-1`).

---

### Historia 2 — Gestión de Proyectos y Miembros

**H2.1 — Ver proyectos del workspace**
Como miembro del workspace (rol `member`, `admin` o `manager`), quiero ver todos los proyectos del workspace para navegar al que necesito.
- Los usuarios con rol `client` en el workspace solo ven los proyectos donde están en `project_members`.

**H2.2 — Asignar miembros a un proyecto**
Como workspace admin/manager, quiero asignar miembros del workspace a proyectos concretos con un rol específico.
- Solo pueden ser asignados usuarios que ya estén en `workspace_members`.
- Un usuario `client` del workspace puede ser asignado a proyectos con rol `client`.

**H2.3 — Gestionar roles de proyecto**
Como project admin, quiero cambiar el rol de un miembro dentro del proyecto o eliminarlo del proyecto sin afectar su membresía en el workspace.

---

### Historia 3 — Acceso de Usuario Cliente

**H3.1 — Registro como cliente**
Como usuario externo (cliente de una empresa), quiero registrarme en ZenTrack como usuario tipo cliente para acceder a los proyectos que me han asignado.
- Al registrarme no se crea una org personal ni tengo acceso a organizaciones empresariales.
- Veo una pantalla de espera hasta recibir acceso a un workspace.

**H3.2 — Acceso al workspace como cliente**
Como usuario cliente, una vez que el workspace admin/manager me ha añadido, quiero ver el workspace y navegar únicamente a los proyectos donde tengo acceso asignado.
- No veo la lista completa de proyectos del workspace.
- No veo información sobre la organización, teams ni miembros.

**H3.3 — Crear tareas en proyectos asignados**
Como usuario cliente en un proyecto con rol `client`, quiero crear nuevas tareas para reportar incidencias o solicitar trabajo.
- Puedo ver todas las tareas del proyecto.
- No puedo editar tareas de otros miembros.
- No puedo cambiar configuración del proyecto.

---

### Historia 4 — Creación de Tareas y Ramas Git

**H4.1 — Crear tarea con rama automatizada**
Como miembro del proyecto (rol `member`, `manager` o `admin`), quiero crear una tarea con título, descripción, prioridad, estimación y asignados, y que el sistema genere automáticamente una rama en GitLab/GitHub siguiendo la nomenclatura GitFlow.
- La tarea recibe su ID correlativo (ej. `ZTK-25`), inmutable desde la creación.
- Puedo elegir el prefijo GitFlow (feature, bug, hotfix…) y editar el nombre de rama antes de confirmar.
- El formato por defecto de la rama es: `[prefijo]/[TASK_ID]/[descripcion-breve]`.

**H4.2 — Subtareas**
Como miembro, quiero crear subtareas vinculadas a una tarea padre.
- Las subtareas generan su propia rama configurada para hacer merge a la rama padre.

**H4.3 — Guardar como borrador**
Como miembro, quiero guardar una tarea como borrador sin generar la rama Git, para completar los detalles más tarde.
- `git_branch_name = null` indica estado de borrador o fallo de la API Git.

---

### Historia 5 — Actualización Automática por Commit

**H5.1 — Estados personalizados por workspace**
Como workspace admin/manager, quiero definir los estados del kanban para mi workspace (ej. Backlog, In Progress, Testing, Done) para adaptar el flujo a nuestra metodología.

**H5.2 — Webhook Git**
Como equipo, queremos que al hacer push del primer commit a la rama de una tarea, esta pase automáticamente al estado equivalente a "In Progress".
- El webhook recibe el evento de push, extrae el ID de tarea del nombre de rama y actualiza `status_id`.
- El endpoint del webhook es público pero protegido por token secreto del proveedor Git.

---

### Historia 6 — Visualización, Filtrado y Ordenación

**H6.1 — Tablero Kanban**
Como miembro, quiero ver las tareas del contexto activo (global, sprint o proyecto) en un tablero Kanban con columnas basadas en los estados configurados del workspace.

**H6.2 — Vista de Lista**
Como miembro, quiero alternar a una vista de lista para ver más tareas de forma compacta con sus atributos clave.

**H6.3 — Selector de contexto**
Como miembro, quiero cambiar el contexto de visualización entre:
- Tablero Global (todas las tareas del workspace)
- Tablero de Sprint (tareas del sprint activo)
- Tablero de Proyecto (tareas de un proyecto específico)

**H6.4 — Filtrado y ordenación**
Como miembro, quiero filtrar y ordenar la vista actual por cualquier atributo de la tarea (estado, prioridad, asignado, etiqueta, sprint, proyecto).

---

## 8. API — Estructura de Endpoints (actualizada)

Todas las rutas excepto auth requieren `Authorization: Bearer <JWT>`.

### Autenticación

```
# Email / contraseña
POST /api/auth/register          Crea usuario (regular o client) + org personal si regular
POST /api/auth/login             Valida credenciales y devuelve JWT

# OAuth 2.0 — Google
GET  /api/auth/google            Inicia flujo OAuth: redirige al consentimiento de Google
GET  /api/auth/google/callback   Callback de Google — intercambia code, crea/vincula usuario, devuelve JWT
POST /api/auth/refresh           Renueva el JWT de ZenTrack usando refresh token interno

# Sesión y perfil
POST /api/auth/logout            Invalida el JWT (lista negra en Redis o similar)
GET  /api/users/me               Datos del usuario autenticado (incluye proveedor OAuth si aplica)
PUT  /api/users/me               Actualiza nombre o avatar
GET  /api/users/me/oauth         Lista las cuentas OAuth vinculadas a la sesión actual
POST /api/users/me/oauth/google  Vincula una cuenta Google a un usuario ya autenticado por email
DELETE /api/users/me/oauth/{id}  Desvincula una cuenta OAuth (solo si tiene password_hash como alternativa)
```

### Organizaciones

```
GET    /api/organizations                         Orgs del usuario autenticado
POST   /api/organizations                         Crear nueva organización empresarial
GET    /api/organizations/{org_id}                Detalle de la org
GET    /api/organizations/{org_id}/members        Listar miembros
POST   /api/organizations/{org_id}/members        Añadir miembro directamente (owner/admin)
DELETE /api/organizations/{org_id}/members/{uid}  Eliminar miembro
GET    /api/organizations/{org_id}/requests       Ver solicitudes pendientes de la org
POST   /api/organizations/{org_id}/requests/{id}/approve
POST   /api/organizations/{org_id}/requests/{id}/reject
```

### Teams

```
GET    /api/organizations/{org_id}/teams             Listar teams de la org
POST   /api/organizations/{org_id}/teams             Crear team
GET    /api/teams/{team_id}/members                  Listar miembros del team
POST   /api/teams/{team_id}/members                  Añadir miembro directamente
DELETE /api/teams/{team_id}/members/{uid}            Eliminar miembro
GET    /api/teams/{team_id}/requests                 Solicitudes pendientes del team
POST   /api/teams/{team_id}/requests/{id}/approve
POST   /api/teams/{team_id}/requests/{id}/reject
```

### Workspaces

```
GET    /api/organizations/{org_id}/workspaces        Workspaces accesibles de la org
POST   /api/organizations/{org_id}/workspaces        Crear workspace
GET    /api/workspaces/{w_id}                        Detalle del workspace
GET    /api/workspaces/{w_id}/members                Miembros del workspace
POST   /api/workspaces/{w_id}/members                Añadir miembro directamente
DELETE /api/workspaces/{w_id}/members/{uid}          Eliminar miembro
GET    /api/workspaces/{w_id}/requests               Solicitudes pendientes del workspace
POST   /api/workspaces/{w_id}/requests/{id}/approve
POST   /api/workspaces/{w_id}/requests/{id}/reject
GET    /api/workspaces/{w_id}/teams                  Teams asignados al workspace
POST   /api/workspaces/{w_id}/teams                  Asignar team al workspace
DELETE /api/workspaces/{w_id}/teams/{team_id}        Desasignar team
```

### Solicitudes (usuario que las envía)

```
POST /api/membership-requests    Enviar solicitud (org/team/workspace)
GET  /api/membership-requests    Ver mis solicitudes enviadas
```

### Proyectos

```
GET    /api/workspaces/{w_id}/projects               Proyectos del workspace
POST   /api/workspaces/{w_id}/projects               Crear proyecto
GET    /api/projects/{p_id}                          Detalle del proyecto
GET    /api/projects/{p_id}/members                  Miembros del proyecto
POST   /api/projects/{p_id}/members                  Asignar miembro
DELETE /api/projects/{p_id}/members/{uid}            Eliminar miembro del proyecto
```

### Tareas

```
GET    /api/workspaces/{w_id}/tasks                  Tareas del workspace (filtros: project, sprint, assignee, status…)
POST   /api/projects/{p_id}/tasks                    Crear tarea (genera ID + rama Git)
GET    /api/tasks/{t_id}                             Detalle de la tarea
PUT    /api/tasks/{t_id}                             Actualizar tarea
POST   /api/tasks/{t_id}/assignees                   Asignar usuario a la tarea
POST   /api/tasks/{t_id}/subtasks                    Crear subtarea
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
POST   /api/webhooks/git         Público, protegido por secret del proveedor
```

---

## 9. Estrategia de Autenticación

### 9.1 Métodos soportados

| Método | Disponible para | Notas |
|---|---|---|
| Email + contraseña | `regular` y `client` | `password_hash` almacenado con bcrypt |
| Google OAuth 2.0 | `regular` y `client` | Prioridad para usuarios con Google Workspace |

Un mismo usuario puede tener **ambos métodos activos simultáneamente** (email+contraseña + Google vinculado).

### 9.2 Flujo OAuth 2.0 (Authorization Code Flow)

```
Cliente (web/app)
    │
    ├─[1]─► GET /api/auth/google
    │            └─► Redirect 302 → accounts.google.com/o/oauth2/auth
    │                              (client_id, redirect_uri, scope: openid email profile)
    │
    ├─[2]── Usuario aprueba en Google ──────────────────────────────────────────┐
    │                                                                           │
    └─[3]─◄── GET /api/auth/google/callback?code=...&state=...  ◄──────────────┘
               │
               ├─ Intercambia code por tokens en Google Token Endpoint
               ├─ Llama a Google UserInfo API para obtener email, name, picture, sub
               ├─ Crea o vincula usuario en BD
               ├─ Emite JWT interno de ZenTrack (no expone tokens de Google)
               └─► Redirect al frontend con JWT en query param seguro o cookie HttpOnly
```

### 9.3 Tokens y sesión

| Token | Origen | Almacenamiento | Expiración |
|---|---|---|---|
| JWT ZenTrack | Ktor genera | Cliente (memoria / cookie HttpOnly) | 1 hora |
| Refresh Token ZenTrack | Ktor genera | BD `refresh_tokens` o cookie HttpOnly | 30 días |
| Google access_token | Google | BD `oauth_accounts` (cifrado AES-256) | ~1 hora |
| Google refresh_token | Google | BD `oauth_accounts` (cifrado AES-256) | Sin expiración fija |

> El cliente de ZenTrack (web/app) **solo maneja el JWT interno**. Los tokens de Google son transparentes para el cliente.

### 9.4 Reglas de seguridad OAuth

1. El `state` parameter del flujo OAuth se genera como UUID aleatorio y se valida en el callback para prevenir CSRF.
2. La `redirect_uri` debe estar registrada en Google Cloud Console y coincidir exactamente.
3. Scope mínimo: `openid email profile` — no se solicitan permisos de Drive, Calendar ni otros.
4. Si Google devuelve un email diferente al del usuario autenticado que intenta vincular una cuenta OAuth adicional, se rechaza con 409.
5. No se puede desvincular una cuenta OAuth si es el único método de login (sin `password_hash`).

### 9.5 Tabla auxiliar para refresh tokens internos

**`refresh_tokens`**

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | Long | PK |
| `user_id` | Long | FK → users NOT NULL |
| `token_hash` | VARCHAR | NOT NULL — hash SHA-256 del token |
| `expires_at` | TIMESTAMP | NOT NULL |
| `revoked_at` | TIMESTAMP | nullable — si se hizo logout |

---

## 10. Casos Borde y Reglas de Integridad

1. **Team sin workspace:** Un team puede existir sin tener workspaces asignados. Sus miembros no tienen acceso a ningún workspace hasta que se les asigne uno.

2. **Workspace compartido entre teams:** Si un workspace está asignado a 3 teams, los admin/manager de cualquiera de los 3 teams pueden gestionar miembros de ese workspace.

3. **Eliminar un team:** Si un team se elimina, las filas en `workspace_teams` se eliminan en cascada. Los `workspace_members` que entraron vía ese team permanecen — perdieron el vínculo de team pero conservan su membresía directa.

4. **Usuario cliente en múltiples workspaces:** Un usuario `client` puede estar en `workspace_members` de varios workspaces (de distintas organizaciones), y en `project_members` de proyectos en cada uno de ellos.

5. **Concurrencia en task_number:** El `task_number` por proyecto se genera con `SELECT ... FOR UPDATE` sobre la tabla `projects` para garantizar unicidad absoluta. El `display_id` resultante es inmutable.

6. **Fallo de API Git:** Si la llamada a GitLab/GitHub falla al crear la tarea, esta se guarda con `git_branch_name = null`. El usuario puede reintentar la creación de rama manualmente desde el detalle de la tarea.

7. **project_key único por workspace:** Dos proyectos en el mismo workspace no pueden tener el mismo `project_key`. El constraint es a nivel de BD: `UNIQUE(workspace_id, project_key)`.

8. **Org personal no aparece en búsquedas:** Las organizaciones con `is_personal = true` no son visibles en el buscador de organizaciones durante el registro ni en ningún listado público.

---

## 11. Cambios Respecto al Diseño v1

| Elemento | v1 | v2 |
|---|---|---|
| Techo del multi-tenancy | Workspace | Organization |
| Departamentos | No existía | Teams (N:M con workspaces) |
| PKs | UUID | Long (autoincremental) |
| Usuario externo | No existía | `user_type = client` con acceso limitado |
| Solicitudes de membresía | No existía | `membership_requests` (org/team/workspace) |
| Org personal | No existía | Auto-creada al registrarse (is_personal) |
| Multi-org por usuario | No soportado | Soportado (N orgs por usuario) |
| Roles de proyecto | Solo `role` genérico | admin / manager / member / viewer / client |
| Roles de workspace | Solo `role` genérico | admin / manager / member / client |
| `workspaces.owner_id` | FK propietario | Renombrado a `created_by` (informativo) |
| Auditoría de tablas | No existía | Columnas `created_at/by`, `updated_at/by` en todas las tablas |
| Autenticación | Solo email+contraseña | OAuth 2.0 Google + email+contraseña (ambos coexisten) |
| `users.password_hash` | NOT NULL | nullable (null = solo OAuth) |
| `users.avatar_url` | No existía | VARCHAR nullable — avatar de Google |
| `oauth_accounts` | No existía | Tabla para vincular cuentas OAuth por proveedor |
| `refresh_tokens` | No existía | Tabla para tokens de refresco internos |
