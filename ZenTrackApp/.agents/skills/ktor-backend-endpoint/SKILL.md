---
name: ktor-backend-endpoint
description: >
  Guía operativa estricta para crear o modificar endpoints REST en el módulo
  `server/` de ZenTrack (Ktor 3.3.3 + PostgreSQL + Exposed). Carga esta habilidad
  siempre que la tarea implique: declarar una nueva ruta Ktor, escribir un Use
  Case de dominio, añadir o modificar una tabla Exposed, crear una migración SQL,
  implementar un Repository, o tocar cualquier parte del stack backend. También
  aplica cuando el usuario mencione "endpoint", "ruta", "API REST", "handler",
  "Use Case", "repositorio backend", "migración", "tabla Exposed", "RLS",
  "Row Level Security", "query PostgreSQL", "índice BD" o "lógica de negocio
  backend". Actívate proactivamente ante cualquier tarea backend aunque no se
  mencione Ktor explícitamente.
---

# Ktor Backend Endpoint — Procedimiento Operativo para server/

## Contexto de Arquitectura

`server/` implementa la API RESTful de ZenTrack con Ktor 3.3.3 sobre JVM.
La arquitectura sigue Clean Architecture estricta con tres capas y fronteras
infranqueables entre ellas.

```
src/main/kotlin/me/dcueto/zentrackapp/
├── api/           → Routes: parsean request, llaman Use Cases, serializan response
├── core/          → Use Cases: lógica de negocio pura, SIN referencias a Ktor ni Exposed
├── db/            → Repositories, Tablas Exposed, Migraciones SQL
├── integrations/  → Clientes HTTP externos (GitLab, GitHub) — aislados del resto
└── Application.kt → Wiring: instala plugins, registra rutas, configura Koin
```

**Por qué importan estas fronteras:** Ktor no impone estructura. Sin límites
explícitos la lógica de negocio migra hacia las rutas y los tests se vuelven
imposibles. Los Use Cases deben ser testeables sin levantar un servidor.

---

## Reglas Absolutas (no negociables)

Estas reglas están codificadas en `server/CLAUDE.md`. Violarlas introduce
vulnerabilidades de seguridad o degradación de rendimiento que no serán
evidentes hasta producción.

### Límites de Capa

- **PROHIBIDO** poner lógica de negocio o consultas SQL dentro de `routing { }`.
- **PROHIBIDO** que los Use Cases importen `io.ktor.*` o `org.jetbrains.exposed.*`.
- **PROHIBIDO** que los Repositories devuelvan DTOs de respuesta HTTP — devuelven modelos de dominio.
- **PROHIBIDO** hacer llamadas HTTP a APIs externas desde `api/` o `core/` — solo desde `integrations/`.

### Seguridad — JWT y Tenant

- **SIEMPRE** valida JWT en el plugin global `install(Authentication)`, nunca inline en la ruta.
- El `userId` del JWT es la **única** fuente de verdad para el tenant. **NUNCA** confíes en `user_id` del body.
- Rutas públicas (solo `/api/auth/register` y `/api/auth/login`): documenta explícitamente por qué omiten `.authenticate`.

### Seguridad — Row Level Security

- **SIEMPRE** habilita RLS en cada tabla nueva: `ALTER TABLE <tabla> ENABLE ROW LEVEL SECURITY;`
- **SIEMPRE** crea política de acceso por `workspace_id` o `user_id` según corresponda.
- **PROHIBIDO** evadir RLS con superusuario o `SET LOCAL role`.
- **PROHIBIDO** consultas sin filtro de tenant en tablas multi-tenant (`Tasks`, `Projects`, `Sprints`, `Tags`, `Task_Statuses`).

### Rendimiento — Prevención de Full Table Scans

- **SIEMPRE** declara índices en claves foráneas: `workspace_id`, `project_id`, `user_id`, `sprint_id`, `parent_id`, `status_id`, `git_branch_name`.
- **PROHIBIDO** ejecutar queries sin `WHERE` sobre tablas con datos de múltiples tenants.
- Para `task_number`: usa `SELECT ... FOR UPDATE` en la fila del `Project` dentro de transacción. Es la **única** implementación aceptable.
- **PROHIBIDO** usar secuencias globales de BD o contadores en memoria para `task_number`.

### Serialización

- Usa `kotlinx.serialization`. **PROHIBIDO** Jackson o Gson.
- Los DTOs de request/response son `@Serializable data class` definidos en `shared/commonMain`, no en `server/`.
- **NUNCA** expongas `password_hash` ni campos internos de BD en los DTOs de response.

---

## Procedimiento: Crear un Nuevo Endpoint (paso a paso)

### Paso 0 — Lectura Obligatoria Antes de Escribir Código

Antes de tocar cualquier fichero, lee:
1. `docs/SDD/PLAN.md` sección 3 — confirma que el endpoint está documentado.
   Si no lo está, añádelo al plan antes de implementarlo.
2. `docs/SDD/PLAN.md` sección 2 — entiende el esquema BD y las relaciones.
3. El archivo de la tabla Exposed relacionada en `server/src/.../db/` si ya existe.

### Paso 1 — Migración SQL (si la tabla no existe)

Crea el fichero en `server/src/main/resources/migrations/` con nombre
`V[N+1]__descripcion.sql` donde N es el número de la última migración existente.

**Plantilla de migración:**

```sql
-- V003__create_tasks.sql
-- Invariante: idempotente. Puede ejecutarse N veces con el mismo resultado.

CREATE TABLE IF NOT EXISTS tasks (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    workspace_id UUID       NOT NULL REFERENCES workspaces(id),  -- desnormalizado para RLS
    task_number INT         NOT NULL,
    display_id  TEXT        NOT NULL,           -- inmutable: "[KEY]-[N]"
    title       TEXT        NOT NULL,
    description TEXT,
    status_id   UUID        NOT NULL REFERENCES task_statuses(id),
    priority    TEXT        NOT NULL DEFAULT 'MEDIUM',
    sprint_id   UUID        REFERENCES sprints(id),
    parent_id   UUID        REFERENCES tasks(id),
    git_branch_name TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (project_id, task_number)
);

-- Índices obligatorios en FK (previenen Full Table Scans en JOINs y filtros)
CREATE INDEX IF NOT EXISTS idx_tasks_project_id    ON tasks(project_id);
CREATE INDEX IF NOT EXISTS idx_tasks_workspace_id  ON tasks(workspace_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status_id     ON tasks(status_id);
CREATE INDEX IF NOT EXISTS idx_tasks_sprint_id     ON tasks(sprint_id);
CREATE INDEX IF NOT EXISTS idx_tasks_parent_id     ON tasks(parent_id);
CREATE INDEX IF NOT EXISTS idx_tasks_git_branch    ON tasks(git_branch_name)
    WHERE git_branch_name IS NOT NULL;  -- índice parcial: solo filas con rama asignada

-- RLS obligatorio — sin esto cualquier usuario puede leer tareas de otros tenants
ALTER TABLE tasks ENABLE ROW LEVEL SECURITY;

CREATE POLICY tasks_workspace_isolation ON tasks
    USING (workspace_id = current_setting('app.current_workspace_id')::UUID);

-- Si el webhook necesita buscar por rama sin contexto de workspace:
CREATE POLICY tasks_git_webhook_read ON tasks
    FOR SELECT
    USING (true);  -- solo si el endpoint webhook lo requiere explícitamente
```

**Reglas de migración:**
- Usa `IF NOT EXISTS` en `CREATE TABLE` y `CREATE INDEX` — las migraciones son idempotentes.
- **NUNCA** modifiques una migración ya aplicada en producción; crea una nueva.
- El número `V[N]` es el único mecanismo de orden. No hay fechas en el nombre.
- Incluye el índice en `git_branch_name` si la tabla tiene ese campo (el webhook lo necesita).

---

### Paso 2 — Tabla Exposed

Crea o modifica el objeto Exposed en `server/src/.../db/`:

```kotlin
// db/TasksTable.kt
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object TasksTable : Table("tasks") {
    val id           = uuid("id").autoGenerate()
    val projectId    = uuid("project_id").references(ProjectsTable.id)
    val workspaceId  = uuid("workspace_id").references(WorkspacesTable.id)
    val taskNumber   = integer("task_number")
    val displayId    = text("display_id")
    val title        = text("title")
    val description  = text("description").nullable()
    val statusId     = uuid("status_id").references(TaskStatusesTable.id)
    val priority     = enumerationByName<TaskPriority>("priority", 10)
    val sprintId     = uuid("sprint_id").references(SprintsTable.id).nullable()
    val parentId     = uuid("parent_id").references(TasksTable.id).nullable()
    val gitBranchName = text("git_branch_name").nullable()
    val createdAt    = timestamp("created_at")
    val updatedAt    = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
```

**Reglas Exposed:**
- Usa `enumerationByName` para enums — guarda el nombre como texto, no ordinal.
- Los campos `nullable()` deben tener valor `null` por defecto en el modelo de dominio.
- No definas `autoGenerate()` si la BD ya lo hace con `DEFAULT gen_random_uuid()` — elige uno.
- La tabla Exposed es solo un descriptor de esquema. **No** pongas consultas aquí.

---

### Paso 3 — Repository (capa `db/`)

El Repository accede a la BD y devuelve **modelos de dominio** (no DTOs HTTP).
Trabaja exclusivamente con `org.jetbrains.exposed.sql.*`.

```kotlin
// db/TaskRepository.kt
class TaskRepository {

    // CORRECTO: filtra por workspaceId — nunca un full table scan en tabla multi-tenant
    suspend fun findByWorkspace(workspaceId: UUID): List<Task> = dbQuery {
        TasksTable
            .select { TasksTable.workspaceId eq workspaceId }
            .map { it.toTask() }
    }

    // CORRECTO: task_number con SELECT FOR UPDATE — única implementación válida
    suspend fun create(command: CreateTaskCommand): Task = dbQuery {
        transaction {
            // 1. Obtener y bloquear la fila del Project para incremento atómico
            val project = ProjectsTable
                .select { ProjectsTable.id eq command.projectId }
                .forUpdate()                          // ← SELECT FOR UPDATE
                .singleOrNull() ?: error("Project ${command.projectId} not found")

            val nextNumber = project[ProjectsTable.lastTaskNumber] + 1
            val projectKey = project[ProjectsTable.projectKey]

            // 2. Incrementar el contador en Projects
            ProjectsTable.update({ ProjectsTable.id eq command.projectId }) {
                it[lastTaskNumber] = nextNumber
            }

            // 3. Insertar la tarea con el número bloqueado
            val taskId = TasksTable.insertAndGetId {
                it[projectId]    = command.projectId
                it[workspaceId]  = command.workspaceId
                it[taskNumber]   = nextNumber
                it[displayId]    = "$projectKey-$nextNumber"  // inmutable desde aquí
                it[title]        = command.title
                it[description]  = command.description
                it[statusId]     = command.statusId
                it[priority]     = command.priority
                it[sprintId]     = command.sprintId
                it[createdAt]    = Instant.now()
                it[updatedAt]    = Instant.now()
            }

            findById(taskId.value) ?: error("Insert failed")
        }
    }

    // CORRECTO: siempre filtra por workspaceId además del id — defensa en profundidad
    suspend fun findById(id: UUID, workspaceId: UUID? = null): Task? = dbQuery {
        TasksTable
            .select {
                (TasksTable.id eq id).let { cond ->
                    if (workspaceId != null) cond and (TasksTable.workspaceId eq workspaceId)
                    else cond
                }
            }
            .singleOrNull()
            ?.toTask()
    }

    private fun ResultRow.toTask(): Task = Task(
        id            = this[TasksTable.id].value.toString(),
        projectId     = this[TasksTable.projectId].value.toString(),
        taskNumber    = this[TasksTable.taskNumber],
        displayId     = this[TasksTable.displayId],
        title         = this[TasksTable.title],
        description   = this[TasksTable.description],
        statusId      = this[TasksTable.statusId].value.toString(),
        priority      = this[TasksTable.priority],
        sprintId      = this[TasksTable.sprintId]?.value?.toString(),
        gitBranchName = this[TasksTable.gitBranchName],
        createdAt     = this[TasksTable.createdAt].toString(),
        updatedAt     = this[TasksTable.updatedAt].toString()
    )
}

// Helper de coroutines para Exposed (adapta transacciones bloqueantes a suspending)
suspend fun <T> dbQuery(block: () -> T): T =
    withContext(Dispatchers.IO) { transaction { block() } }
```

**Reglas de Repository:**
- Toda función es `suspend` — usa `dbQuery` o equivalente para salir del hilo principal.
- **PROHIBIDO** devolver `ResultRow` fuera del Repository. Mapea siempre a modelo de dominio.
- **PROHIBIDO** consultas sin `WHERE` sobre tablas multi-tenant.
- El mapeo a modelo de dominio (`toTask()`) vive como extensión privada dentro del Repository.

---

### Paso 4 — Use Case (capa `core/`)

El Use Case orquesta la lógica de negocio. No sabe nada de Ktor ni de Exposed.
Sus dependencias llegan por constructor (Koin las inyecta).

```kotlin
// core/CreateTaskUseCase.kt
class CreateTaskUseCase(
    private val taskRepository: TaskRepository,
    private val gitIntegration: GitIntegration,      // de integrations/
    private val statusRepository: TaskStatusRepository
) {
    // El comando encapsula todo lo que necesita el caso de uso — sin HttpCall aquí
    suspend fun execute(command: CreateTaskCommand): Task {
        // 1. Validaciones de negocio (no de formato — esas van en la ruta)
        val defaultStatus = statusRepository.findDefault(command.workspaceId)
            ?: error("Workspace ${command.workspaceId} has no default status")

        // 2. Crear la tarea — el repository garantiza la atomicidad del task_number
        val task = taskRepository.create(command.copy(statusId = defaultStatus.id))

        // 3. Llamada a Git — si falla, NO hacemos rollback de la tarea (regla de dominio)
        val branchName = try {
            gitIntegration.createBranch(task.displayId, task.title, command.workspaceId)
        } catch (e: Exception) {
            null  // git_branch_name = null es estado válido, no un error
        }

        // 4. Si obtuvimos rama, persistimos el nombre
        return if (branchName != null) {
            taskRepository.updateBranchName(task.id, branchName)
        } else {
            task
        }
    }
}
```

**Reglas de Use Case:**
- Sin imports de `io.ktor.*` ni `org.jetbrains.exposed.*`.
- Los comandos de entrada (`CreateTaskCommand`) son `data class` simples en `core/`.
- Los errores de negocio se lanzan como excepciones con mensaje claro — la ruta los captura.
- No dependas de `HttpCall`, `ApplicationCall` ni ningún tipo de Ktor en este paquete.

---

### Paso 5 — Ruta Ktor (capa `api/`)

La ruta es la capa más delgada posible: parsea el request, extrae el tenant del JWT,
llama al Use Case y serializa el response.

```kotlin
// api/TaskRoutes.kt
fun Route.taskRoutes(createTaskUseCase: CreateTaskUseCase) {

    // Todas las rutas aquí están bajo authenticate { } en Application.kt
    route("/api/projects/{projectId}/tasks") {

        post {
            // 1. Extraer tenant del JWT — NUNCA del body
            val userId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asString()
                ?: return@post call.respond(HttpStatusCode.Unauthorized)

            val workspaceId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("workspaceId")?.asString()
                ?: return@post call.respond(HttpStatusCode.Unauthorized)

            // 2. Parsear y validar el request (formato, campos requeridos)
            val projectId = call.parameters["projectId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing projectId")

            val request = call.receive<CreateTaskRequest>()  // DTO de shared/

            // 3. Construir el comando de dominio
            val command = CreateTaskCommand(
                projectId   = UUID.fromString(projectId),
                workspaceId = UUID.fromString(workspaceId),
                requestedBy = UUID.fromString(userId),
                title       = request.title,
                description = request.description,
                priority    = request.priority ?: TaskPriority.MEDIUM,
                sprintId    = request.sprintId?.let { UUID.fromString(it) }
            )

            // 4. Ejecutar Use Case — los errores de negocio se propagan como excepciones
            val task = createTaskUseCase.execute(command)

            // 5. Responder con el DTO de shared/ (nunca el modelo de dominio directo)
            call.respond(HttpStatusCode.Created, TaskResponse(task = task, projectKey = ""))
        }
    }
}
```

**Plugin de manejo de errores en Application.kt** (evita try/catch en cada ruta):

```kotlin
install(StatusPages) {
    exception<IllegalArgumentException> { call, cause ->
        call.respond(HttpStatusCode.BadRequest, mapOf("error" to cause.message))
    }
    exception<NoSuchElementException> { call, cause ->
        call.respond(HttpStatusCode.NotFound, mapOf("error" to cause.message))
    }
    exception<Exception> { call, cause ->
        application.log.error("Unhandled error", cause)
        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal error"))
    }
}
```

**Reglas de ruta:**
- **PROHIBIDO** poner lógica de negocio aquí. Si hay un `if` que no sea de parsing/validación, muévelo al Use Case.
- **PROHIBIDO** llamar directamente al Repository desde la ruta.
- El `userId` y `workspaceId` del JWT son la única fuente del tenant.
- Usa `call.receive<T>()` con el DTO de `shared/`. No definas clases de request en `server/`.

---

### Paso 6 — Registrar en Koin y Application.kt

Añade los bindings en el módulo Koin del servidor:

```kotlin
// Application.kt (o el módulo Koin correspondiente)
val serverModule = module {
    // Repositories
    single { TaskRepository() }
    single { TaskStatusRepository() }

    // Use Cases
    single { CreateTaskUseCase(get(), get(), get()) }
}

// Registro de rutas (dentro de routing { })
fun Application.configureRouting() {
    routing {
        authenticate("jwt") {
            taskRoutes(createTaskUseCase = get())
            // ... otras rutas autenticadas
        }
        // Rutas públicas — documenta aquí por qué no llevan authenticate
        post("/api/auth/login") { /* ... */ }
        post("/api/auth/register") { /* ... */ }
    }
}
```

---

## Caso Especial: Webhook Git (endpoint público sin JWT)

El webhook es el **único** endpoint que no lleva JWT. Su seguridad viene de la
validación del header de firma del proveedor.

```kotlin
// api/WebhookRoutes.kt
fun Route.webhookRoutes(webhookUseCase: ProcessGitWebhookUseCase) {
    post("/api/webhooks/git") {
        // 1. Validar firma ANTES de procesar el payload
        val signature = call.request.headers["X-Hub-Signature-256"]
            ?: call.request.headers["X-Gitlab-Token"]
            ?: return@post call.respond(HttpStatusCode.Unauthorized)

        val rawBody = call.receiveText()
        if (!webhookUseCase.validateSignature(signature, rawBody)) {
            return@post call.respond(HttpStatusCode.Unauthorized)
        }

        // 2. Procesar
        webhookUseCase.execute(rawBody)
        call.respond(HttpStatusCode.OK)
    }
}
```

**Regla de webhook:** La búsqueda de tarea por `git_branch_name` **requiere** el
índice `idx_tasks_git_branch` creado en la migración. Sin él, cada push hace
un Full Table Scan sobre `tasks`.

**Regla de estado terminal:** El Use Case del webhook **no** actualiza tareas
en estado `Done` o `Closed`. Estos son estados terminales inmutables vía webhook.

---

## Checklist de Validación

Antes de marcar cualquier endpoint como completado, verifica cada punto:

```
SEGURIDAD
[ ] JWT validado en plugin global, no inline en la ruta
[ ] userId y workspaceId extraídos del JWT, NUNCA del request body
[ ] RLS habilitado en cada tabla nueva con política de aislamiento por workspace_id
[ ] Endpoint público documentado con razón explícita de la ausencia de authenticate
[ ] Webhook: firma del proveedor validada antes de procesar payload
[ ] Sin campos internos (password_hash, rls_policy_id) en DTOs de response

ARQUITECTURA
[ ] Cero imports de io.ktor.* en Use Cases
[ ] Cero imports de org.jetbrains.exposed.* en Use Cases
[ ] Repository devuelve modelos de dominio, no DTOs HTTP
[ ] Llamadas HTTP a Git/GitHub únicamente desde integrations/

RENDIMIENTO / BASE DE DATOS
[ ] Índice creado en cada FK nueva (workspace_id, project_id, status_id, git_branch_name…)
[ ] Toda query sobre tabla multi-tenant tiene cláusula WHERE con filtro de tenant
[ ] task_number generado con SELECT FOR UPDATE sobre Projects (no secuencias globales)
[ ] Migración usa IF NOT EXISTS (idempotente)
[ ] No se ha modificado ninguna migración ya aplicada

CALIDAD
[ ] DTOs de request/response definidos en shared/commonMain, no en server/
[ ] Serialización con kotlinx.serialization (sin Jackson/Gson)
[ ] La ruta del endpoint coincide con docs/SDD/PLAN.md sección 3
[ ] Use Case registrado en el módulo Koin del servidor
```

Comandos de verificación:

```bash
./gradlew :server:test           # Tests unitarios del backend
./gradlew :server:buildFatJar    # Verifica compilación completa
```

---

## Referencia Rápida de Estructura de Ficheros

```
server/src/main/kotlin/me/dcueto/zentrackapp/
├── api/
│   ├── TaskRoutes.kt              → fun Route.taskRoutes(...)
│   ├── WorkspaceRoutes.kt
│   └── WebhookRoutes.kt           → sin JWT; valida firma del proveedor
├── core/
│   ├── CreateTaskUseCase.kt       → orquesta, sin Ktor ni Exposed
│   ├── ProcessGitWebhookUseCase.kt
│   └── commands/
│       └── CreateTaskCommand.kt   → data class simple, sin anotaciones de framework
├── db/
│   ├── TasksTable.kt              → object extends Table("tasks")
│   ├── TaskRepository.kt          → solo Exposed; devuelve modelos de dominio
│   └── migrations/
│       ├── V001__init_users.sql
│       └── V002__workspaces.sql
├── integrations/
│   └── GitIntegration.kt          → HttpClient a GitLab/GitHub
└── Application.kt                 → wiring global
```
