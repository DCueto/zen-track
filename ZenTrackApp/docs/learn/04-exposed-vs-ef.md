# 04 — Exposed ORM para devs Entity Framework

> Cómo entender Exposed (JetBrains) si conoces EF Core.

---

## ¿Qué es Exposed?

**Exposed** es el ORM oficial de JetBrains para Kotlin. Tiene dos APIs:

1. **DAO API** — Define entidades con clases y accede a la BD como objetos. Es el approach principal de ZenTrack porque se parece más a EF Core.
2. **DSL API** — Escribe queries con una DSL typesafe similar a SQL. Solo se usa en ZenTrack cuando la DAO API no es suficiente: `SELECT FOR UPDATE`, queries de agregación, joins complejos.

---

## HikariCP — El connection pool

Antes de entender Exposed, necesitas entender **HikariCP**. Es una librería de **connection pooling** para JDBC (la API de bajo nivel de Java para bases de datos).

En .NET, Entity Framework Core incluye el connection pool **integrado** — no lo ves. En el ecosistema JVM, el pool es una capa separada que configuras tú.

```
.NET:  EF Core  →  (pool incluido internamente)  →  PostgreSQL driver
JVM:   Exposed  →  HikariCP (pool explícito)      →  PostgreSQL JDBC driver
```

**¿Qué es un connection pool?**  
Conectarse a una base de datos es caro (TCP handshake, autenticación). Un pool mantiene un número de conexiones abiertas y las reutiliza. HikariCP es el pool más rápido del ecosistema JVM.

```kotlin
// DatabaseFactory.kt — nuestro equivalente al DbContext de EF
object DatabaseFactory {
    fun init(application: Application) {
        val cfg = application.environment.config
        val hikariConfig = HikariConfig().apply {
            jdbcUrl          = cfg.property("database.url").getString()
            driverClassName  = cfg.property("database.driver").getString()
            username         = cfg.property("database.user").getString()
            password         = cfg.property("database.password").getString()
            maximumPoolSize  = 10
        }
        Database.connect(HikariDataSource(hikariConfig))
    }
}
```

```csharp
// Program.cs — equivalente .NET
builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseNpgsql(builder.Configuration.GetConnectionString("DefaultConnection")));
```

---

## Definir entidades — DAO API vs EF Core

### EF Core

```csharp
// Entidad
public class User
{
    public Guid Id { get; set; }
    public string Email { get; set; } = "";
    public string PasswordHash { get; set; } = "";
    public DateTime CreatedAt { get; set; }
}

// DbContext
public class AppDbContext : DbContext
{
    public DbSet<User> Users { get; set; }
    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<User>().HasIndex(u => u.Email).IsUnique();
    }
}
```

### Exposed DAO API

En Exposed, la tabla y la entidad son dos clases separadas. La tabla describe el esquema; la entidad es el objeto con el que trabajas en el código.

```kotlin
// db/UsersTable.kt

// 1. La tabla — describe columnas y tipos (equivale a OnModelCreating + DbSet)
object UsersTable : LongIdTable("users") {         // LongIdTable = PK BIGINT autoincremental (ver sección siguiente)
    val email        = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val createdAt    = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

// 2. La entidad — el objeto con el que trabajas (equivale al POCO en EF)
class UserEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<UserEntity>(UsersTable)  // el "DbSet" — da acceso a find(), all(), etc.

    var email        by UsersTable.email
    var passwordHash by UsersTable.passwordHash
    var createdAt    by UsersTable.createdAt

    fun toUser() = User(                           // convierte a modelo de dominio (NUNCA devuelvas la entidad fuera del transaction)
        id        = id.value,
        email     = email,
        createdAt = createdAt.toString()
    )
}
```

| EF Core | Exposed DAO | Notas |
|---|---|---|
| `public class User { }` (POCO) | `class UserEntity(...) : LongEntity(id)` | El objeto con el que trabajas |
| `DbSet<User> Users` | `companion object : LongEntityClass<UserEntity>` | Punto de entrada para queries |
| `DbContext` (configura la BD) | `object UsersTable : LongIdTable("users")` | Define el esquema de la tabla |
| `modelBuilder.Entity<User>()` | Columnas declaradas en el `object` | Configuración de columnas |

---

## Elegir el tipo de PK: UUIDTable vs IntIdTable vs LongIdTable

Exposed tiene tres clases base para tablas con PK autogestionada. La elección equivale a elegir el tipo de `Id` en EF Core.

### La equivalencia .NET

| C# / EF Core | Exposed | PostgreSQL DDL generado |
|---|---|---|
| `[Key] public Guid Id { get; set; }` | `UUIDTable` | `id UUID DEFAULT gen_random_uuid()` |
| `[Key] public int Id { get; set; }` | `IntIdTable` | `id SERIAL` (INT4 + secuencia) |
| `[Key] public long Id { get; set; }` | `LongIdTable` | `id BIGSERIAL` (INT8 + secuencia) |

### Por qué ZenTrack usa `LongIdTable`

El análisis práctico para elegir:

| Criterio | UUID | Int (SERIAL) | Long (BIGSERIAL) |
|---|---|---|---|
| Tamaño en disco | 16 bytes | 4 bytes | 8 bytes |
| Máximo de filas | Ilimitado | ~2.100 millones | ~9,2 × 10¹⁸ |
| IDs en URLs | `/workspaces/0029cd84-…` | `/workspaces/1` | `/workspaces/1` |
| Generado por | Cliente o BD | BD (secuencia) | BD (secuencia) |
| Útil en sistemas distribuidos | Sí | No | No |
| Dificultad en el código | Alta (conversiones UUID↔String) | Baja | Baja |

UUID tiene sentido cuando tienes varios servicios generando IDs de forma independiente sin coordinarse. Para una app monolítica como ZenTrack, añade complejidad sin beneficio.

`Long` sobre `Int`: ambos son igual de simples en el código. `Long` elimina el riesgo de overflow en tablas grandes (como `Tasks`) sin coste adicional.

### Cómo se declara

```kotlin
// ZenTrack — tablas reales
object UsersTable : LongIdTable("users") {
    val email = text("email")
    // id se hereda: Column<EntityID<Long>>, auto-generado por PostgreSQL
}

object WorkspacesTable : LongIdTable("workspaces") {
    val ownerId = reference("owner_id", UsersTable)  // Column<EntityID<Long>>
    val name    = text("name")
}
```

Las FKs con `reference()` heredan el tipo automáticamente: si `UsersTable : LongIdTable`, entonces `reference("owner_id", UsersTable)` crea una columna `BIGINT NOT NULL REFERENCES users(id)`.

### El SQL que genera Flyway (escrito a mano, no por Exposed)

```sql
-- ZenTrack usa Flyway para gestionar el esquema (Flyway no lo genera Exposed)
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email TEXT NOT NULL,
    ...
);

CREATE TABLE workspaces (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ...
);
```

`GENERATED ALWAYS AS IDENTITY` es el estándar SQL moderno para auto-increment. PostgreSQL lo usa internamente con una secuencia, igual que `BIGSERIAL`, pero la sintaxis IDENTITY es más portátil.

### La RLS con IDs numéricos

El cambio de UUID a Long afecta todos los casts en las políticas RLS:

```sql
-- Antes (UUID)
USING (id = current_setting('app.user_id', true)::uuid)

-- Ahora (Long)
USING (id = current_setting('app.user_id', true)::bigint)
```

El patrón `SET LOCAL app.user_id = '$userId'` sigue siendo el mismo — el string `'42'` se castea a `BIGINT 42` en PostgreSQL sin problema.

**Caso especial en registro:** con UUID, el ID se generaba en el cliente antes del INSERT, lo que permitía `SET LOCAL app.user_id = newId` y luego verificar `id = app.user_id::uuid` en el `WITH CHECK`. Con BIGINT generado por la BD, el ID no existe hasta que el INSERT ocurre. La solución fue simplificar esa política a `WITH CHECK (true)` — válido porque el `UNIQUE` en `email` ya impide crear filas duplicadas maliciosas.

---

## Queries — LINQ vs Exposed DAO

```csharp
// EF Core — LINQ
var user = await ctx.Users
    .FirstOrDefaultAsync(u => u.Email == email);
```

```kotlin
// Exposed DAO — dentro de transaction { }
val user = UserEntity.find { UsersTable.email eq email }.firstOrNull()
```

### CRUD completo

```csharp
// INSERT
var user = new User { Email = "a@b.com", PasswordHash = hash };
ctx.Users.Add(user);
await ctx.SaveChangesAsync();

// UPDATE
user.PasswordHash = newHash;
await ctx.SaveChangesAsync();  // EF detecta el cambio automáticamente (change tracker)

// DELETE
ctx.Users.Remove(user);
await ctx.SaveChangesAsync();
```

```kotlin
// INSERT — dentro de transaction { }
val user = UserEntity.new {
    email        = "a@b.com"
    passwordHash = hash
}
// No hay SaveChanges() — se persiste al cerrar el transaction { }

// UPDATE — la entidad es mutable mientras estés en el transaction
user.passwordHash = newHash
// Se persiste automáticamente al cerrar el transaction (igual que el change tracker de EF)

// DELETE
user.delete()
```

---

## Relaciones entre tablas — cómo se definen y consumen

En EF Core defines las relaciones con **navigation properties** en el modelo y las configuras en `OnModelCreating`. En Exposed DAO usas dos piezas: una columna `reference()` en la tabla (FK) y un delegado especial en la entidad (la navigation property).

### Many-to-One: Task pertenece a Workspace

El caso más común — una FK de N hacia 1.

```csharp
// EF Core
public class Task
{
    public Guid WorkspaceId { get; set; }
    public Workspace Workspace { get; set; }  // navigation property
}
public class Workspace
{
    public List<Task> Tasks { get; set; }     // navigation property inversa
}
// OnModelCreating:
modelBuilder.Entity<Task>()
    .HasOne(t => t.Workspace)
    .WithMany(w => w.Tasks)
    .HasForeignKey(t => t.WorkspaceId);
```

```kotlin
// Exposed DAO

// 1. En la tabla — declara la FK con reference() en lugar de uuid()
object TasksTable : UUIDTable("tasks") {
    val workspaceId = reference("workspace_id", WorkspacesTable)   // FK — NOT NULL
    val sprintId    = optionalReference("sprint_id", SprintsTable) // FK nullable
}

// 2. En la entidad — los delegados de navegación
class TaskEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<TaskEntity>(TasksTable)

    var workspace by WorkspaceEntity referencedOn TasksTable.workspaceId         // Many-to-One (NOT NULL)
    var sprint    by SprintEntity    optionalReferencedOn TasksTable.sprintId     // Many-to-One nullable
}

class WorkspaceEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<WorkspaceEntity>(WorkspacesTable)

    val tasks by TaskEntity referrersOn TasksTable.workspaceId                   // One-to-Many (colección inversa)
}
```

| EF Core | Exposed DAO | Cuándo usarlo |
|---|---|---|
| `HasOne(...).WithMany(...)` + nav property | `referencedOn` en la entidad hijo | FK obligatoria (NOT NULL) |
| `HasOne(...).WithMany(...)` + nav property nullable | `optionalReferencedOn` en la entidad hijo | FK nullable |
| Colección `List<Task> Tasks` | `referrersOn` en la entidad padre | Acceder a la colección desde el padre |

La diferencia clave de sintaxis: en EF defines la relación **una sola vez** en `OnModelCreating`. En Exposed la defines **dos veces** — `reference()` en la tabla y el delegado en la entidad — pero cada pieza tiene una responsabilidad distinta (esquema vs navegación).

---

### Many-to-Many: Task tiene muchos Tags

Para relaciones N:M necesitas una tabla intermedia, igual que en EF con `HasMany().WithMany()` o una entidad join explícita.

```csharp
// EF Core — Many-to-Many implícito (EF crea la tabla join automáticamente)
public class Task   { public List<Tag> Tags { get; set; } }
public class Tag    { public List<Task> Tasks { get; set; } }
// OnModelCreating:
modelBuilder.Entity<Task>().HasMany(t => t.Tags).WithMany(t => t.Tasks);
```

```kotlin
// Exposed DAO — la tabla join siempre es explícita

// 1. Tabla join (no extiende UUIDTable — no tiene PK propia, la PK es compuesta)
object TaskTagsTable : Table("task_tags") {
    val taskId = reference("task_id", TasksTable)
    val tagId  = reference("tag_id",  TagsTable)
    override val primaryKey = PrimaryKey(taskId, tagId)
}

// 2. En la entidad — delegado via
class TaskEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<TaskEntity>(TasksTable)

    var tags by TagEntity via TaskTagsTable   // Many-to-Many
}

class TagEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<TagEntity>(TagsTable)

    var tasks by TaskEntity via TaskTagsTable  // Many-to-Many inversa
}
```

Asignar y leer tags:

```kotlin
transaction {
    // Leer
    val task = TaskEntity.findById(taskId)!!
    val tagNames = task.tags.map { it.name }

    // Asignar (reemplaza la colección entera)
    task.tags = SizedCollection(listOf(tag1, tag2))
}
```

---

### Cómo consumir relaciones — lazy vs eager

Por defecto todas las relaciones son **lazy**: se cargan en el momento en que accedes a la propiedad, disparando una query adicional. El comportamiento es idéntico al de EF Core sin `.Include()`.

```kotlin
// LAZY — Exposed dispara una query adicional cuando accedes a .workspace
val task = TaskEntity.findById(id)!!
println(task.workspace.name)  // ← query extra aquí
```

Para evitar N+1, carga las relaciones que vayas a usar con **`.with()`**:

```kotlin
// EAGER — una sola query carga tasks y workspaces juntos
TaskEntity
    .find { TasksTable.workspaceId eq workspaceId }
    .with(TaskEntity::workspace)               // carga workspace de cada task
    .map { it.toTask() }

// Varias relaciones a la vez
TaskEntity
    .find { TasksTable.projectId eq projectId }
    .with(TaskEntity::workspace, TaskEntity::sprint, TaskEntity::tags)
    .map { it.toTask() }
```

Equivalencia exacta con EF Core:

```csharp
// EF Core — Include
ctx.Tasks
    .Where(t => t.WorkspaceId == workspaceId)
    .Include(t => t.Workspace)
    .Include(t => t.Sprint)
    .Include(t => t.Tags)
    .ToList();
```

**Regla en ZenTrack:** si en `toTask()` (o `toDto()`) accedes a una propiedad de una relación, esa relación debe estar en `.with()` en la query que la llama.

---

## N+1 queries — el mismo problema que EF sin `.Include()`

En DAO API, las relaciones se cargan **lazy** por defecto. Si iteras una lista sin cargar la relación explícitamente, cada elemento dispara una query extra (N+1).

```kotlin
// MALO — N+1: una query por cada task para cargar el workspace
TaskEntity.all().forEach { println(it.workspace.name) }

// BIEN — eager load con with(): una sola query
TaskEntity.all().with(TaskEntity::workspace).forEach { println(it.workspace.name) }
```

El equivalente exacto en EF Core:

```csharp
// MALO — N+1 sin Include
ctx.Tasks.ToList().ForEach(t => Console.WriteLine(t.Workspace.Name));

// BIEN — Include
ctx.Tasks.Include(t => t.Workspace).ToList().ForEach(t => Console.WriteLine(t.Workspace.Name));
```

**Regla en ZenTrack:** siempre que accedas a una relación en un loop, usa `.with()`.

---

## Las entidades NO son thread-safe fuera de `transaction {}`

Este es el gotcha más importante del DAO API. Las instancias de `Entity` **solo son válidas dentro del `transaction {}` donde fueron creadas**. Si las devuelves directamente desde un Repository, obtienes errores en runtime cuando se acceden fuera del contexto de transacción.

```kotlin
// MALO — la entidad escapa del transaction
fun getUser(id: UUID): UserEntity = transaction { UserEntity.findById(id)!! }
// → LazyInitializationException al acceder a campos fuera del transaction

// BIEN — conviertes a data class dentro del transaction
fun getUser(id: UUID): User = transaction { UserEntity.findById(id)!!.toUser() }
```

En EF Core esto no pasa porque los POCOs son clases normales sin dependencias del framework. En Exposed DAO, la entidad tiene un "hilo" a la transacción activa.

---

## Transacciones

En EF Core, `SaveChangesAsync()` es implícitamente transaccional. En Exposed, **toda operación debe estar dentro de una `transaction {}`**:

```kotlin
// Exposed — transacción bloqueante (síncrona)
transaction {
    UserEntity.new {
        email        = "a@b.com"
        passwordHash = hash
    }
}

// Para coroutines (Ktor usa coroutines), usa newSuspendedTransaction
suspend fun createUser(email: String, hash: String): User =
    newSuspendedTransaction {
        UserEntity.new {
            this.email        = email
            this.passwordHash = hash
        }.toUser()
    }
```

El helper `dbQuery` encapsula esto para no repetirlo en cada Repository:

```kotlin
// Helper reutilizable en db/DatabaseFactory.kt o db/DbQuery.kt
suspend fun <T> dbQuery(block: () -> T): T =
    withContext(Dispatchers.IO) { transaction { block() } }

// Uso en Repository
suspend fun findById(id: UUID): User? = dbQuery {
    UserEntity.findById(id)?.toUser()
}
```

---

## Cuándo usar DSL API (excepciones a la regla)

Usa DSL API únicamente cuando DAO no sea suficiente:

| Caso | Por qué necesita DSL |
|---|---|
| `SELECT ... FOR UPDATE` (task_number atómico) | DAO no expone `forUpdate()` |
| Queries de agregación (`COUNT`, `SUM`, `GROUP BY`) | DAO no soporta agregaciones |
| Joins complejos entre 3+ tablas | DAO genera SQL menos eficiente |
| Bulk updates/deletes | DAO no tiene equivalente directo |

Ejemplo del único caso en ZenTrack donde mezclas DAO + DSL (incremento atómico de `task_number`):

```kotlin
suspend fun create(command: CreateTaskCommand): Task = dbQuery {
    // SELECT FOR UPDATE — necesita DSL: no hay equivalente en DAO API
    val project = ProjectsTable
        .select { ProjectsTable.id eq command.projectId }
        .forUpdate()
        .singleOrNull() ?: error("Project not found")

    val nextNumber = project[ProjectsTable.lastTaskNumber] + 1

    ProjectsTable.update({ ProjectsTable.id eq command.projectId }) {
        it[ProjectsTable.lastTaskNumber] = nextNumber
    }

    // El resto de la creación sí usa DAO
    TaskEntity.new {
        projectId   = command.projectId
        taskNumber  = nextNumber
        displayId   = "${project[ProjectsTable.projectKey]}-$nextNumber"
        title       = command.title
        createdAt   = Instant.now()
        updatedAt   = Instant.now()
    }.toTask()
}
```

---

## Migraciones — Flyway vs EF Migrations

EF Core genera migraciones automáticamente (`dotnet ef migrations add`). **Exposed no tiene un sistema de migraciones incorporado** equivalente. En ZenTrack usamos **Flyway**: gestiona scripts SQL numerados y lleva un registro de cuáles ya se aplicaron.

### Convención de nombres (idéntica al `0001_Description.cs` de EF)

```
server/src/main/resources/db/migration/
├── V001__create_users.sql
├── V002__create_workspaces.sql
├── V003__create_workspace_members.sql
├── V004__create_projects.sql
└── V005__create_project_members.sql
```

El prefijo `V###__` es obligatorio (dos guiones bajos). Flyway ejecuta los scripts en orden ascendente y **nunca los vuelve a ejecutar** una vez aplicados (como `__EFMigrationsHistory` en EF).

### Configurar Flyway en `DatabaseFactory.kt`

```kotlin
// DatabaseFactory.kt
object DatabaseFactory {
    fun init(application: Application) {
        val dataSource = HikariDataSource(hikariConfig)

        // Ejecuta las migraciones pendientes antes de conectar Exposed
        // Equivale a: dotnet ef database update
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()

        Database.connect(dataSource)
    }
}
```

| EF Core | Flyway | Notas |
|---|---|---|
| `dotnet ef migrations add NombreMigración` | Crear `V006__descripcion.sql` a mano | Flyway no genera DDL — lo escribes tú |
| `dotnet ef database update` | `Flyway.configure()...migrate()` | Flyway se llama en el arranque del servidor |
| `__EFMigrationsHistory` | `flyway_schema_history` | Tabla interna que registra qué migraciones se aplicaron |
| `migration.Up()` / `migration.Down()` | Solo `.sql` ascendente | Flyway no soporta rollbacks automáticos — se crea un nuevo script de rollback |

> Por qué no EF-style migrations: los scripts SQL son explícitos y deterministas. No hay "magia" que genera DDL incorrecto para esquemas complejos. En un sistema multi-tenant con RLS, necesitas control total sobre el DDL.

### Reglas de escritura de migraciones (reflejan `server/CLAUDE.md`)

- `CREATE TABLE IF NOT EXISTS` — idempotente (ejecutar N veces da el mismo resultado).
- `CREATE INDEX IF NOT EXISTS` — ídem.
- **NUNCA** modifiques un script ya aplicado en producción. Crea un `V007__` nuevo.
- Las dependencias entre tablas dictan el orden: `users` antes de `workspaces`, `workspaces` antes de `workspace_members`.

---

## Row Level Security (RLS) — sin equivalente directo en .NET

PostgreSQL permite definir políticas que filtran filas **a nivel de base de datos**, no en la aplicación. Es una capa de defensa extra que garantiza el aislamiento multi-tenant incluso si hay un bug en el código.

### El patrón: `app.user_id` como variable de sesión

Antes de ejecutar cualquier query con restricción de tenant, la aplicación establece quién es el usuario actual:

```kotlin
// En el repositorio — dentro de newSuspendedTransaction
newSuspendedTransaction {
    exec("SET LOCAL app.user_id = '$userId'")  // solo para esta transacción
    // ... queries normales con Exposed
}
```

Las políticas RLS leen esta variable:

```sql
-- V001__create_users.sql
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;  -- aplica también al owner de la tabla

-- Users solo ven su propia fila
CREATE POLICY users_self
    ON users
    USING      (id = current_setting('app.user_id', true)::uuid)
    WITH CHECK (id = current_setting('app.user_id', true)::uuid);
```

`current_setting('app.user_id', true)` devuelve `NULL` si no está definida (segundo arg = `missing_ok`). `NULL = uuid` es siempre `NULL` (evaluado como falso en el `USING`), así que si el servidor olvida establecer la variable, el usuario no ve nada — comportamiento seguro por defecto.

### `FORCE ROW LEVEL SECURITY` — por qué es necesario

En PostgreSQL, el owner de una tabla **no está sujeto a RLS por defecto**. Como la app usa el mismo usuario que crea las tablas (`zentrack`), añadimos `FORCE` para que las políticas también apliquen al owner:

```sql
ALTER TABLE workspaces FORCE ROW LEVEL SECURITY;  -- aplica RLS incluso al owner
```

Sin `FORCE`, el usuario `zentrack` saltaría todas las políticas.

### Políticas `USING` vs `WITH CHECK`

| Cláusula | Cuándo aplica | Analogía EF |
|---|---|---|
| `USING` | SELECT, DELETE, UPDATE WHERE | Filtro del `Where()` en la query |
| `WITH CHECK` | INSERT, UPDATE SET | Validación en `SaveChanges()` antes de escribir |

Ejemplo: política de workspaces (definida en `V003__` una vez existe `workspace_members`):

```sql
-- Los miembros pueden ver workspaces donde tienen membresía
CREATE POLICY workspaces_member_access
    ON workspaces
    USING (
        id IN (
            SELECT workspace_id FROM workspace_members
            WHERE user_id = current_setting('app.user_id', true)::uuid
        )
    )
    -- Solo el owner puede crear workspaces (owner_id = usuario actual)
    WITH CHECK (
        owner_id = current_setting('app.user_id', true)::uuid
    );
```

---

## RLS Bootstrap: el problema del huevo y la gallina

Las políticas RLS parecen sencillas sobre el papel, pero en la práctica hay dos momentos donde la propia política bloquea una operación legítima: el **login** y la **creación del primer workspace**. Ambos casos aparecieron en ZenTrack y se resolvieron en `V006__fix_rls_policies.sql`.

### Problema 1 — El SELECT de login queda bloqueado por RLS

La política inicial de `users` era:

```sql
CREATE POLICY users_self ON users
    USING      (id = current_setting('app.user_id', true)::uuid)
    WITH CHECK (id = current_setting('app.user_id', true)::uuid);
```

El flujo de login es:

1. El usuario envía email + contraseña.
2. El servidor hace `SELECT * FROM users WHERE email = ?` para recuperar el hash y el UUID.
3. **Problema:** en este punto `app.user_id` aún no está establecido (¡es lo que estamos buscando!). `current_setting` devuelve `NULL`. `NULL = uuid` es siempre `NULL`, que PostgreSQL evalúa como falso en el `USING`. Resultado: cero filas devueltas aunque el usuario exista.

En .NET sería como si un middleware de autorización rechazara la petición `/login` antes de que el `AuthController` pudiera leer el email de la base de datos — el guardia impide la operación que necesitas para identificarte.

**Solución en V006:** separar la política única en dos, con reglas distintas para lectura y escritura:

```sql
DROP POLICY IF EXISTS users_self ON users;

CREATE POLICY users_self_read ON users FOR SELECT
    USING (current_setting('app.user_id', true) IS NULL
           OR current_setting('app.user_id', true) = ''
           OR id = current_setting('app.user_id', true)::uuid);

CREATE POLICY users_self_write ON users FOR INSERT
    WITH CHECK (current_setting('app.user_id', true) IS NOT NULL
                AND current_setting('app.user_id', true) <> ''
                AND id = current_setting('app.user_id', true)::uuid);
```

- `users_self_read` (SELECT): permisiva — si la variable no está establecida, deja pasar (necesario para el login). Una vez autenticado, solo devuelve la fila propia.
- `users_self_write` (INSERT): estricta — exige que `app.user_id` esté definido y coincida con el UUID que se está insertando. Sin variable → operación bloqueada.

La idea clave: no toda operación sobre `users` requiere el mismo nivel de restricción. El SELECT de login es legítimamente "anónimo"; el INSERT nunca lo es.

---

### Problema 2 — Bootstrap de `workspace_members`: el OWNER no puede añadirse a sí mismo

Al crear un workspace, el servidor debe insertar automáticamente al creador como primer miembro con rol `OWNER`. La política original de `workspace_members` tenía este `WITH CHECK`:

```sql
WITH CHECK (
    workspace_id IN (
        SELECT wm.workspace_id FROM workspace_members wm
        WHERE wm.user_id = current_setting('app.user_id', true)::uuid
          AND wm.role IN ('OWNER', 'ADMIN')
    )
)
```

La lógica es: "solo puedes insertar filas en un workspace donde ya eres OWNER o ADMIN". Tiene sentido para el caso general (añadir a alguien a un workspace existente), pero falla en el momento de creación:

- Para insertar la fila de membresía, necesitas ser OWNER.
- Para ser OWNER, necesitas que la fila de membresía exista.

Es el problema del huevo y la gallina. En .NET sería análogo a un middleware que valida permisos de recurso antes de que el recurso exista en la base de datos.

**Solución en V006:** añadir una condición OR que permita el bootstrap cuando el workspace recién fue creado por este usuario:

```sql
DROP POLICY IF EXISTS workspace_members_access ON workspace_members;

CREATE POLICY workspace_members_access ON workspace_members
    USING (workspace_id IN (
        SELECT wm.workspace_id FROM workspace_members wm
        WHERE wm.user_id = current_setting('app.user_id', true)::uuid
    ))
    WITH CHECK (
        workspace_id IN (
            SELECT wm.workspace_id FROM workspace_members wm
            WHERE wm.user_id = current_setting('app.user_id', true)::uuid
              AND wm.role IN ('OWNER', 'ADMIN')
        )
        OR (
            user_id = current_setting('app.user_id', true)::uuid
            AND role = 'OWNER'
            AND workspace_id IN (
                SELECT id FROM workspaces
                WHERE owner_id = current_setting('app.user_id', true)::uuid
            )
        )
    );
```

El OR añadido dice: "también puedes insertar una fila de membresía si (a) la fila es para ti mismo, (b) el rol es OWNER, y (c) el workspace tiene `owner_id` igual a tu UUID". La columna `owner_id` en `workspaces` se establece en el mismo `transaction {}` antes del INSERT en `workspace_members`, por lo que la condición (c) ya es verdadera en el momento de la verificación.

---

La clave en ambos casos es que RLS es una red de seguridad de la BD — cuando es demasiado restrictiva bloquea operaciones legítimas. En lugar de desactivarla, ajustas la política con precisión quirúrgica en una nueva migración.

---

## `application.conf` — La connection string

```hocon
# application.conf (HOCON)
database {
    url = "jdbc:postgresql://localhost:5433/zentrack_db"
    driver = "org.postgresql.Driver"
    user = "zentrack"
    password = "zentrack_dev"
    maxPoolSize = 10
}
```

```json
// appsettings.json (.NET con Npgsql)
{
  "ConnectionStrings": {
    "DefaultConnection": "Host=localhost;Port=5433;Database=zentrack_db;Username=zentrack;Password=zentrack_dev"
  }
}
```

El formato JDBC (`jdbc:postgresql://host:port/database`) es diferente al formato Npgsql, pero la información es la misma. El **driver** (`org.postgresql.Driver`) es el equivalente al NuGet `Npgsql`.

---

## Resumen: tabla de equivalencias

| Entity Framework Core / .NET | Exposed DAO / JVM | Notas |
|---|---|---|
| `DbContext` | `DatabaseFactory` (singleton) | Gestiona la conexión a BD |
| Connection pool (integrado en EF) | HikariCP (explícito) | Pool de conexiones |
| POCO / Record | `class UserEntity(...) : UUIDEntity(id)` | Objeto de entidad |
| `DbSet<User>` | `companion object : UUIDEntityClass<UserEntity>` | Acceso a queries |
| `DbContext.OnModelCreating` | `object UsersTable : UUIDTable(...)` | Definición del esquema |
| `.Include(u => u.Workspace)` | `.with(UserEntity::workspace)` | Eager loading (evita N+1) |
| `ctx.Users.Add(entity)` | `UserEntity.new { ... }` | INSERT |
| `entity.Field = value; SaveChanges()` | `entity.field = value` (dentro de transaction) | UPDATE |
| `ctx.Users.Remove(entity)` | `entity.delete()` | DELETE |
| `.Where().FirstOrDefault()` | `.find { }.firstOrNull()` | SELECT con filtro |
| `SaveChangesAsync()` | `transaction { }` / `newSuspendedTransaction { }` | Persistir cambios |
| `BeginTransactionAsync()` | `newSuspendedTransaction { }` | Transacción explícita |
| EF Migrations | Scripts SQL numerados (`V001__.sql`) | Evolución del esquema |
| `dotnet ef database update` | `Flyway.configure()...migrate()` en el arranque | Ejecutar migraciones |
| `__EFMigrationsHistory` | `flyway_schema_history` | Registro de migraciones aplicadas |
| Row-level security (SQL Server) | `ENABLE/FORCE ROW LEVEL SECURITY` + `CREATE POLICY` | PostgreSQL RLS — aislamiento multi-tenant en la BD |
| — | `SET LOCAL app.user_id = '...'` | Variable de sesión por transacción para RLS |
| `Npgsql` NuGet package | `org.postgresql:postgresql` JAR | Driver de PostgreSQL |
| `appsettings.json` | `application.conf` (HOCON) | Configuración |
