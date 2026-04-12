# CLAUDE.md — server/ (Ktor + PostgreSQL)

## QUÉ hace este módulo
API RESTful construida con Ktor 3.3.3 sobre JVM. Gestiona autenticación JWT, lógica de negocio de ZenTrack (Workspaces, Projects, Tasks, Sprints), integración con APIs de GitLab/GitHub y recepción de webhooks Git.

## POR QUÉ estas reglas existen
Ktor no impone una arquitectura; sin límites explícitos, la lógica de negocio migra hacia las rutas, los repositorios devuelven entidades de presentación y el esquema de BD se vuelve inconsistente. RLS y los índices son la última línea de defensa ante fugas de datos multi-tenant.

## CÓMO estructurar el código

### Clean Architecture — Capas y Límites

```
src/main/kotlin/me/dcueto/zentrackapp/
├── api/           → Routes (presentación): parsean request, llaman Use Cases, serializan response
├── core/          → Use Cases (dominio): lógica de negocio pura, sin referencias a Ktor ni Exposed
├── db/            → Repositories + Tablas Exposed + Migraciones
├── integrations/  → Clientes HTTP externos (GitLab, GitHub)
└── Application.kt → Wiring: instala plugins, registra rutas, configura Koin/DI
```

**PROHIBIDO** incluir lógica de negocio o consultas SQL dentro de las definiciones de `routing { }`.
**PROHIBIDO** que los Use Cases importen `io.ktor.*` o `org.jetbrains.exposed.*` directamente.
**PROHIBIDO** que los Repositories devuelvan entidades de respuesta HTTP (response DTOs); devuelven modelos de dominio.

### Autenticación

- **SIEMPRE** valida el JWT en un plugin de autenticación global (`install(Authentication)`), no inline en cada ruta.
- El `userId` extraído del JWT es la fuente de verdad para el tenant actual. **NUNCA** confíes en un `user_id` proveniente del body de la request.
- Rutas públicas (solo `POST /api/auth/register` y `POST /api/auth/login`): declara explícitamente con `.authenticate` omitido y documenta el motivo.

### PostgreSQL y Row Level Security (RLS)

- **SIEMPRE** activa RLS en cada tabla nueva: `ALTER TABLE <tabla> ENABLE ROW LEVEL SECURITY;`
- **SIEMPRE** crea políticas que restrinjan acceso por `workspace_id` o `user_id` según corresponda.
- **PROHIBIDO** escribir consultas que evadan las políticas RLS mediante superusuario o `SET LOCAL role`.
- **PROHIBIDO** ejecutar queries sin filtro de tenant en tablas multi-tenant (`Tasks`, `Projects`, `Sprints`, `Tags`, `Task_Statuses`).

### Rendimiento de Base de Datos

- **SIEMPRE** declara índices en claves foráneas (`workspace_id`, `project_id`, `user_id`, `sprint_id`, `parent_id`, `status_id`).
- **PROHIBIDO** generar consultas sin cláusula `WHERE` sobre tablas con datos de múltiples tenants (Full Table Scan).
- Para el autoincremento de `task_number`: usa `SELECT ... FOR UPDATE` en la fila del `Project` dentro de una transacción. Esta es la única implementación aceptable para garantizar unicidad bajo concurrencia.
- **PROHIBIDO** implementar el autoincremento de `task_number` con secuencias globales de BD o contadores en memoria.

### Migraciones de Base de Datos

- Las migraciones son **deterministas**: el mismo script ejecutado N veces produce el mismo estado final.
- Usa `IF NOT EXISTS` en `CREATE TABLE` y `CREATE INDEX`. **PROHIBIDO** scripts de migración que fallen si la tabla ya existe.
- **NUNCA** modifiques una migración ya aplicada en producción; crea una nueva migración.
- Orden de ejecución: numérico ascendente. Ejemplo: `V001__init_users.sql`, `V002__workspaces.sql`.

### Integración Git (GitLab/GitHub)

- El cliente HTTP de integración vive en `integrations/`. **PROHIBIDO** hacer llamadas HTTP a APIs externas desde `api/` o `core/`.
- Si la API de Git falla al crear la rama, la tarea se persiste con `git_branch_name = null`. **NUNCA** hagas rollback de la tarea completa por un fallo de la API Git.
- El webhook `POST /api/webhooks/git` es el único endpoint público sin JWT. **SIEMPRE** valida el `X-Hub-Signature` (GitHub) o `X-Gitlab-Token` antes de procesar el payload.

### Webhook de Estado Automático

Flujo obligatorio al recibir un evento `push`:
1. Extrae `branch_name` del payload.
2. Busca la tarea por `git_branch_name` (ÍNDICE requerido en esta columna).
3. Resuelve el `status_id` equivalente a "In Progress" en el workspace de la tarea.
4. Actualiza `Tasks.status_id` en una transacción.
5. **PROHIBIDO** actualizar el estado si la tarea está en status `Done` o `Closed` (regla de negocio: estados terminales son inmutables vía webhook).

### Serialización y DTOs

- Usa `kotlinx.serialization`. **PROHIBIDO** Jackson o Gson en este módulo.
- Los DTOs de request y response son `@Serializable data class` en `shared/commonMain`, no en `server/`. Así el cliente Desktop y Web reutilizan las mismas clases.
- **NUNCA** expongas campos de BD internos (`password_hash`, `rls_policy_id`) en los DTOs de response.