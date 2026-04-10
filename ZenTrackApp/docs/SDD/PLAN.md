
# Plan Técnico: ZenTrack

## 1. Arquitectura de Alto Nivel

ZenTrack utilizará una arquitectura cliente-servidor basada en el ecosistema Kotlin:

- **Backend (Ktor):** API RESTful expuesta con Ktor, encargada de la lógica de negocio, integración con las APIs de GitLab/GitHub, y autenticación (JWT).
    
- **Base de Datos (PostgreSQL):** Base de datos relacional para persistir Workspaces, Proyectos, Sprints, Tareas y Usuarios. Se comunicará con Ktor mediante el ORM Exposed o Ktorm.
    
- **Shared / Core (KMP):** Módulo común de KMP que contendrá los modelos de datos, la lógica de red (Ktor Client) y la gestión de estado general.
    
- **Frontend Escritorio (Compose Multiplatform):** UI nativa para JVM (Windows/Mac/Linux) implementando Material 3.
    
- **Frontend Web (React + TS + Zustand):** Aplicación web que consumirá la API directamente (o utilizará wrappers de Kotlin/JS), gestionando su estado local con Zustand y usando librerías basadas en Material 3.
    

## 2. Esquema de Base de Datos (PostgreSQL) y Relaciones

El modelo de datos garantiza el aislamiento lógico por Workspace y permite Sprints transversales.

### Entidades Core y Acceso

|**Tabla**|**Columnas Principales**|**Relaciones / Notas**|
|---|---|---|
|**Users**|`id` (PK), `email`, `password_hash`, `name`, `created_at`||
|**Workspaces**|`id` (PK), `name`, `owner_id` (FK), `created_at`|`owner_id` -> `Users.id`|
|**Workspace_Members**|`workspace_id` (PK/FK), `user_id` (PK/FK), `role`|Relación N:M entre Users y Workspaces.|
|**Projects**|`id` (PK), `workspace_id` (FK), `project_key` (String), `name`|`project_key` debe ser UNIQUE dentro de un mismo `workspace_id`.|
|**Project_Members**|`project_id` (PK/FK), `user_id` (PK/FK), `role`|Relación N:M. Define quién tiene acceso al proyecto. **Regla:** `user_id` debe existir previamente en `Workspace_Members`.|

### Entidades de Metodología Ágil

|**Tabla**|**Columnas Principales**|**Relaciones / Notas**|
|---|---|---|
|**Sprints**|`id` (PK), `workspace_id` (FK), `name`, `start_date`, `end_date`, `status`|Pertenecen al Workspace (transversales a proyectos). Estados: _Planning, Active, Closed_.|
|**Tags**|`id` (PK), `workspace_id` (FK), `name`, `color_hex`|Etiquetas personalizables a nivel de Workspace.|
|**Task_Statuses**|`id` (PK), `workspace_id` (FK), `name`, `order_index`, `is_default`|Workflows personalizados por Workspace (ToDo, In Progress...).|

### Entidades de Tareas

|**Tabla**|**Columnas Principales**|**Relaciones / Notas**|
|---|---|---|
|**Tasks**|`id` (PK UUID), `project_id` (FK), `sprint_id` (FK nullable), `parent_id` (FK nullable)|`parent_id` apunta a `Tasks.id` (para subtareas).|
||`task_number` (Int), `display_id` (Generado)|`task_number` autoincremental por `project_id`. `display_id` = `project_key` + "-" + `task_number`.|
||`title`, `description`, `status_id` (FK), `priority`, `estimate`||
||`start_date`, `due_date`, `git_branch_name`|`git_branch_name` guarda el link a GitLab/GitHub.|
|**Task_Assignees**|`task_id` (PK/FK), `user_id` (PK/FK)|Relación N:M. **Regla:** `user_id` debe existir en `Project_Members` del proyecto de la tarea.|
|**Task_Tags**|`task_id` (PK/FK), `tag_id` (PK/FK)|Relación N:M.|

## 3. Estructura de Endpoints Extendida (API Ktor)

Todas las rutas (excepto login/registro) requieren cabecera `Authorization: Bearer <JWT>`.

### Autenticación & Usuarios

- `POST /api/auth/register` -> Crea un nuevo usuario.
    
- `POST /api/auth/login` -> Valida credenciales y devuelve el token JWT.
    
- `GET /api/users/me` -> Devuelve los datos del usuario logueado.
    

### Workspaces

- `GET /api/workspaces` -> Lista los workspaces del usuario.
    
- `POST /api/workspaces` -> Crea un nuevo workspace.
    
- `GET /api/workspaces/{w_id}/members` -> Lista los miembros del workspace.
    

### Sprints & Workflows (Nivel Workspace)

- `GET /api/workspaces/{w_id}/sprints` -> Lista sprints del workspace.
    
- `POST /api/workspaces/{w_id}/sprints` -> Crea un nuevo sprint.
    
- `GET /api/workspaces/{w_id}/statuses` -> Obtiene el flujo de estados configurado (Kanban columns).
    

### Proyectos

- `GET /api/workspaces/{w_id}/projects` -> Lista los proyectos dentro de un workspace.
    
- `POST /api/workspaces/{w_id}/projects` -> Crea un proyecto (requiere `project_key`).
    
- `GET /api/projects/{p_id}/members` -> Lista usuarios asignados al proyecto.
    
- `POST /api/projects/{p_id}/members` -> Asigna un usuario del workspace al proyecto.
    

### Tareas

- `GET /api/workspaces/{w_id}/tasks` -> Obtiene tareas (permite query params para filtrar por `project_id`, `sprint_id`, `assignee`, etc., y montar los diferentes tableros).
    
- `POST /api/projects/{p_id}/tasks` -> Crea una tarea, genera el ID unívoco (ej. ZTK-1) y opcionalmente dispara la petición a GitLab/GitHub para crear la rama.
    
- `PUT /api/tasks/{t_id}` -> Actualiza campos de la tarea (estado, sprint asignado, descripción).
    
- `POST /api/tasks/{t_id}/assignees` -> Asigna un usuario a la tarea (el backend validará que pertenezca a `Project_Members`).
    
- `POST /api/tasks/{t_id}/subtasks` -> Crea una subtarea vinculando el `parent_id`.
    

### Integración Git

- `POST /api/webhooks/git` -> Endpoint público (protegido por secret/token del proveedor) que recibe eventos _push/merge_. El backend buscará la tarea por el nombre de la rama en el payload y actualizará el `status_id`.
    

## 4. Estructura de Directorios (Monorepo)

Plaintext

```
zentrackapp/
├── backend/                  # Ktor Server API
│   ├── src/main/kotlin/
│   │   ├── api/              # Controladores (Routes)
│   │   ├── core/             # Lógica de negocio (Services) y Auth JWT
│   │   ├── db/               # Tablas Exposed/Ktorm y Migraciones
│   │   ├── integrations/     # Clientes HTTP para GitLab/GitHub API
│   │   └── Application.kt    # Punto de entrada
├── shared/                   # KMP Module (Lógica compartida)
│   ├── commonMain/           # Models, DTOs, Ktor Client, Repositories
│   ├── desktopMain/          # Expect/Actual JVM
│   └── jsMain/               # Expect/Actual JS
├── composeApp/               # Compose Multiplatform (Escritorio)
│   └── src/desktopMain/
│       ├── components/       # Material 3 UI Components
│       └── screens/          # Workspaces, Board, Backlog
└── webApp/                   # React + TS + Zustand
    ├── src/
    │   ├── components/       # Componentes MUI (Material 3)
    │   ├── store/            # Estado global (Zustand)
    │   └── services/         # Llamadas a la API
```

## 5. Integración con Git y Manejo de Concurrencia

- **Concurrencia de IDs:** Para el incremento de `task_number` por proyecto, se utilizará una transacción con `SELECT ... FOR UPDATE` en la tabla `Projects` para garantizar atomicidad y evitar IDs duplicados bajo alta carga.
    
- **Flujo Git:** Ktor hará la petición a la API de Git. Si falla, la BBDD guardará la tarea con `git_branch_name = null` para permitir un reintento posterior.