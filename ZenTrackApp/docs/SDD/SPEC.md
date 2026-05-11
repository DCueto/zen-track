# Spec: ZenTrack - Gestión Ágil Multi-Organización con Integración GitFlow

**Descripción General**: ZenTrack es una plataforma minimalista de gestión de proyectos estructurada en **Organizaciones**. Cada Organización agrupa Teams (departamentos) y Workspaces (clientes). Los Teams se asignan N:M a Workspaces para reflejar que varios departamentos pueden trabajar con el mismo cliente. La herramienta automatiza el flujo de trabajo mediante una integración profunda con GitLab/GitHub, generando ramas basadas en GitFlow con IDs de proyecto legibles (ej. ZTK-1), y sienta las bases para una futura asistencia integral por Inteligencia Artificial.

**Arquitectura de Frontend Web**: La aplicación web es TypeScript puro (React 19 + Zustand + MUI). Los tipos se generan automáticamente desde la spec OpenAPI del servidor con `openapi-typescript`. No hay dependencia de módulos Kotlin en `webApp/`; toda la lógica del frontend es TypeScript estándar.

## Historias de Usuario: MVP (Fase 1)

### Historia 0 — Registro e Incorporación

**H0.1 — Registro básico**
Como nuevo usuario, quiero registrarme con nombre, email y contraseña, o mediante Google OAuth 2.0, para acceder a ZenTrack.
- Al completar el registro se crea mi organización personal automáticamente.
- No necesito pertenecer a ninguna organización empresarial para empezar.

**H0.2 — Búsqueda de organización durante el registro**
Como nuevo usuario, durante el proceso de registro quiero buscar organizaciones existentes por nombre o slug para enviar una solicitud de adhesión junto con mi registro.
- La solicitud queda en estado `pending` hasta que un admin/owner la apruebe.
- Puedo completar el registro sin seleccionar ninguna organización.

**H0.3 — Multi-organización**
Como usuario regular, quiero poder pertenecer a varias organizaciones simultáneamente para gestionar proyectos de diferentes empresas desde la misma cuenta.

**H0.4 — Organización personal**
Como usuario regular, quiero disponer de una organización personal donde guardar workspaces y proyectos que no pertenecen a ninguna empresa.
- La org personal se crea automáticamente al registrarme.
- Puedo invitar a otros usuarios a colaborar en mi org personal.

### Historia 0b — Gestión de Organización

**H0b.1 — Crear organización empresarial**
Como usuario regular, quiero crear una organización con nombre y slug único para que otros miembros de mi empresa puedan unirse.

**H0b.2 — Gestionar miembros de la organización**
Como org owner/admin, quiero ver las solicitudes de entrada pendientes y aprobarlas o rechazarlas.

**H0b.3 — Crear y configurar teams**
Como org owner/admin, quiero crear teams dentro de la organización con nombre y color identificativo.

**H0b.4 — Auto-asignación sin solicitud**
Como org owner/admin, quiero poder unirme directamente a cualquier team o workspace de mi organización sin necesitar aprobación.
- Al unirme recibo rol `admin`. Hasta que me auto-asigne no tengo acceso al contenido.

### Historia 0c — Gestión de Teams

**H0c.1 — Gestionar miembros del team**
Como team admin/manager, quiero ver las solicitudes de entrada a mi team y aprobarlas o rechazarlas.

**H0c.2 — Asignar team a workspace**
Como team admin/manager, quiero asignar mi team a un workspace para que mis admin/managers puedan gestionar ese workspace.
- La asignación no da acceso automático al workspace a todos los miembros del team.

**H0c.3 — Gestionar miembros de workspaces asignados al team**
Como team admin/manager de un team asignado a un workspace, quiero poder aceptar solicitudes de entrada a ese workspace.

### Historia 1 — Navegación de Workspaces y Configuración de Proyectos

**H1.1 — Panel raíz de workspaces**
Como miembro, al iniciar sesión quiero ver todos los workspaces a los que tengo acceso.

**H1.2 — Crear workspace**
Como org owner/admin, quiero crear un nuevo workspace dentro de una organización y opcionalmente asignarlo a teams desde el momento de la creación.

**H1.3 — Gestionar miembros del workspace**
Como workspace admin/manager, quiero añadir miembros, ver la lista actual y cambiar o revocar roles.

**H1.4 — Configurar proyecto dentro del workspace**
Como workspace admin/manager, quiero crear proyectos definiendo nombre y `project_key` único (ej. `ZTK`).
- El `project_key` debe ser único dentro del mismo workspace.
- Las tareas adoptan el formato `[PROJECT_KEY]-[N]` (ej. `ZTK-1`).

### Historia 2 — Gestión de Proyectos y Miembros

**H2.1 — Ver proyectos del workspace**
Como miembro del workspace, quiero ver todos los proyectos del workspace.
- Los usuarios con rol `client` solo ven los proyectos donde están en `project_members`.

**H2.2 — Asignar miembros a un proyecto**
Como workspace admin/manager, quiero asignar miembros del workspace a proyectos con un rol específico.

**H2.3 — Gestionar roles de proyecto**
Como project admin, quiero cambiar el rol de un miembro dentro del proyecto sin afectar su membresía en el workspace.

### Historia 3 — Acceso de Usuario Cliente

**H3.1 — Registro como cliente**
Como usuario externo, quiero registrarme como usuario tipo cliente para acceder a proyectos asignados.
- No se crea org personal ni accedo a organizaciones empresariales.
- Veo una pantalla de espera hasta recibir acceso a un workspace.

**H3.2 — Acceso al workspace como cliente**
Como usuario cliente, una vez añadido por un workspace admin, quiero ver el workspace y navegar a los proyectos donde tengo acceso.

**H3.3 — Crear tareas en proyectos asignados**
Como usuario cliente con rol `client` en un proyecto, quiero crear nuevas tareas para reportar incidencias.
- Puedo ver todas las tareas del proyecto pero no editar tareas de otros.

### Historia 4 — Creación de Tareas y Ramas Git

**H4.1 — Crear tarea con rama automatizada**
Como miembro del proyecto (rol `member`, `manager` o `admin`), quiero crear una tarea y que el sistema genere automáticamente una rama en GitLab/GitHub siguiendo la nomenclatura GitFlow.
- La tarea recibe su ID correlativo (ej. `ZTK-25`), inmutable desde la creación.
- Puedo elegir el prefijo GitFlow (feature, bug, hotfix…) y editar el nombre de rama.
- El formato por defecto es: `[prefijo]/[TASK_ID]/[descripcion-breve]`.

**H4.2 — Subtareas**
Como miembro, quiero crear subtareas vinculadas a una tarea padre que generen su propia rama configurada para hacer merge a la rama padre.

**H4.3 — Guardar como borrador**
Como miembro, quiero guardar una tarea como borrador sin generar la rama Git.

### Historia 5 — Actualización Automática por Commit

**H5.1 — Estados personalizados por workspace**
Como workspace admin/manager, quiero definir los estados del kanban para adaptar el flujo a nuestra metodología.

**H5.2 — Webhook Git**
Como equipo, queremos que al hacer push del primer commit a la rama de una tarea, esta pase automáticamente al estado "In Progress".

### Historia 6 — Visualización, Filtrado y Ordenación

**H6.1 — Tablero Kanban**
Como miembro, quiero ver las tareas en un tablero Kanban con columnas basadas en los estados configurados del workspace.

**H6.2 — Vista de Lista**
Como miembro, quiero alternar a una vista de lista para ver más tareas de forma compacta.

**H6.3 — Selector de contexto**
Como miembro, quiero cambiar el contexto entre Tablero Global, Tablero de Sprint y Tablero de Proyecto.

**H6.4 — Filtrado y ordenación**
Como miembro, quiero filtrar y ordenar la vista por cualquier atributo de la tarea (estado, prioridad, asignado, etiqueta, sprint, proyecto).

---

## Historias de Usuario: Inteligencia Artificial (Fase 2 - Post-MVP)

Creación de tareas por lenguaje natural, Magic Breakdown para subtareas automáticas, y generación de descripciones/checklists por IA.

---

## Casos Borde y Escenarios de Error (MVP)

1. **Fallo Git:** Si la API de GitLab/GitHub falla, se crea la tarea con `git_branch_name = null` y se permite reintentar manualmente.
2. **Workspace compartido entre teams:** Si un workspace está asignado a varios teams, los admin/manager de cualquiera de ellos pueden gestionar membresías.
3. **Concurrencia de IDs:** El `task_number` se genera con `SELECT ... FOR UPDATE` sobre la tabla `projects` para garantizar unicidad. El `display_id` resultante es inmutable.
4. **Eliminar un team:** Las filas en `workspace_teams` se eliminan en cascada; los `workspace_members` que entraron vía ese team conservan su membresía directa.
5. **Usuario cliente sin workspace:** El usuario cliente ve una pantalla de espera hasta ser añadido a al menos un workspace.
6. **Desvincular OAuth:** No se puede desvincular una cuenta Google si es el único método de login (sin `password_hash`).
7. **project_key duplicado:** Constraint de BD `UNIQUE(workspace_id, project_key)` previene duplicados.
8. **Org personal en búsquedas:** Las organizaciones con `is_personal = true` no son visibles en el buscador de organizaciones.

## Requisitos Clave (Must-Haves)

- Arquitectura multi-tenant lógica con **Organizaciones** como techo de tenancy.
- Teams (departamentos) con relación N:M a Workspaces para soportar clientes compartidos.
- ID de Proyecto configurable (String único) con autoincremento para las tareas.
- Integración bidireccional con GitLab/GitHub (creación de rama + webhook push).
- Autenticación dual: email/contraseña + **OAuth 2.0 Google** (Authorization Code Flow).
- Usuarios de tipo `client` (externos) con acceso limitado a proyectos específicos.
- Sistema de solicitudes de membresía para orgs, teams y workspaces.
- Todas las tablas con columnas de auditoría (`created_at/by`, `updated_at/by`).
- Todos los PKs de tipo `Long` (autoincremental). Sin UUIDs.
