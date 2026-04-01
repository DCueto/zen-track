# Tasks: ZenTrack MVP

## Fase 1: Setup / Fundacional

- [ ] **[Setup]** Inicializar el monorepo `zentrackapp` con la estructura de carpetas (backend, shared, composeApp, webApp).
    
- [ ] **[Setup Backend]** Configurar el proyecto Ktor con la conexión a PostgreSQL y el ORM (Exposed/Ktorm).
    
- [ ] **[Setup Backend]** Implementar el sistema base de Autenticación (generación y validación de JWT).
    
- [ ] **[Setup Shared]** Configurar el módulo KMP (`shared`) con Ktor Client para las peticiones HTTP.
    
- [ ] **[Setup Frontend]** Inicializar la app de Compose Multiplatform (Escritorio) con el tema base de Material 3.
    
- [ ] **[Setup Frontend]** Inicializar el proyecto web (React + TS + Zustand) configurando llamadas al módulo KMP o API.
    

## Fase 2: Historia 1 - Navegación de Workspaces y Configuración de Proyectos

- [ ] **[Backend]** Crear las migraciones y modelos de BD para `Users`, `Workspaces`, `Workspace_Members`, `Projects` y `Project_Members`.
    
- [ ] **[Backend]** Implementar endpoints CRUD para Workspaces (`GET`, `POST`).
    
- [ ] **[Backend]** Implementar endpoints para Proyectos, incluyendo la validación de que el `project_key` sea único por Workspace.
    
- [ ] **[Shared]** Crear los DTOs y la lógica de red (Ktor Client) para interactuar con Workspaces y Proyectos.
    
- [ ] **[Frontend]** Crear la UI del Login/Registro.
    
- [ ] **[Frontend]** Crear la "Vista Raíz": Panel de selección de Workspaces.
    
- [ ] **[Frontend]** Crear el formulario de creación de Proyecto (validando el input del `project_key`).
    

## Fase 3: Historia 2 - Creación de tareas, subtareas y ramas automatizadas

- [ ] **[Backend]** Crear las migraciones y modelos para `Tasks`, `Task_Assignees` y `Task_Tags`.
    
- [ ] **[Backend]** Implementar la lógica transaccional (`SELECT ... FOR UPDATE`) para generar el `task_number` autoincremental de forma segura al crear una tarea.
    
- [ ] **[Backend]** Integrar el cliente HTTP para la API de GitLab/GitHub (Creación de ramas desde `main`/`develop`).
    
- [ ] **[Backend]** Crear el endpoint `POST /tasks` que guarde la tarea, calcule su ID (ej. `ZTK-25`) y dispare la llamada a Git.
    
- [ ] **[Shared]** Definir los modelos de Tarea y los repositorios en el módulo común.
    
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
    
- [ ] **[Shared]** Implementar la gestión del estado global (ej. Sprints activos, filtros seleccionados).
    
- [ ] **[Frontend]** Desarrollar el componente "Tablero Kanban" usando Material 3 (columnas basadas en `Task_Statuses` y drag-and-drop básico).
    
- [ ] **[Frontend]** Desarrollar el componente "Vista de Lista" (DataGrid o tabla simple).
    
- [ ] **[Frontend]** Implementar la barra superior con el selector de contexto (Global, Sprint, Proyecto) y los controles de filtrado/ordenación.