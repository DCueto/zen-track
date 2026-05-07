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
object UsersTable : UUIDTable("users") {           // UUIDTable = PK de tipo UUID autoincremental
    val email        = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val createdAt    = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

// 2. La entidad — el objeto con el que trabajas (equivale al POCO en EF)
class UserEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserEntity>(UsersTable)  // el "DbSet" — da acceso a find(), all(), etc.

    var email        by UsersTable.email
    var passwordHash by UsersTable.passwordHash
    var createdAt    by UsersTable.createdAt

    fun toUser() = User(                           // convierte a modelo de dominio (NUNCA devuelvas la entidad fuera del transaction)
        id        = id.value.toString(),
        email     = email,
        createdAt = createdAt.toString()
    )
}
```

| EF Core | Exposed DAO | Notas |
|---|---|---|
| `public class User { }` (POCO) | `class UserEntity(...) : UUIDEntity(id)` | El objeto con el que trabajas |
| `DbSet<User> Users` | `companion object : UUIDEntityClass<UserEntity>` | Punto de entrada para queries |
| `DbContext` (configura la BD) | `object UsersTable : UUIDTable("users")` | Define el esquema de la tabla |
| `modelBuilder.Entity<User>()` | Columnas declaradas en el `object` | Configuración de columnas |

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

## Migraciones — Flyway / Liquibase vs EF Migrations

EF Core tiene migraciones generadas automáticamente (`dotnet ef migrations add`). **Exposed no tiene un sistema de migraciones incorporado** equivalente.

En ZenTrack usamos el patrón de **SQL scripts numerados**, ejecutados por una librería separada (o manualmente). Los scripts viven en `server/src/main/resources/db/migrations/`:

```
V001__init_users.sql
V002__workspaces.sql
V003__projects.sql
```

El equivalente en .NET sería usar **Flyway** o **Liquibase** en lugar de EF Migrations.

> Por qué no usar EF-style migrations: Los scripts SQL son explícitos y deterministas. No hay "magia" que genera SQL incorrecto para esquemas complejos. En un sistema multi-tenant con RLS, necesitas control total sobre el DDL.

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
| EF Migrations | Scripts SQL numerados (V001__.sql) | Evolución del esquema |
| `dotnet ef database update` | Aplicar scripts manualmente / Flyway | Ejecutar migraciones |
| `Npgsql` NuGet package | `org.postgresql:postgresql` JAR | Driver de PostgreSQL |
| `appsettings.json` | `application.conf` (HOCON) | Configuración |
