# 04 — Exposed ORM para devs Entity Framework

> Cómo entender Exposed (JetBrains) si conoces EF Core.

---

## ¿Qué es Exposed?

**Exposed** es el ORM oficial de JetBrains para Kotlin. Permite trabajar con bases de datos relacionales de dos formas:

1. **DSL API** — Define tablas como objetos Kotlin y escribe queries con una DSL typesafe. Similar al Query Builder de EF Core.
2. **DAO API** — Define entidades con clases que tienen métodos `save()`, `findById()`, etc. Más parecido a los `DbSet<T>` de EF Core.

En ZenTrack usamos la **DSL API** porque da más control sobre las queries y es más explícita — importante en un sistema multi-tenant con políticas RLS en PostgreSQL.

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
            maximumPoolSize  = 10  // máximo 10 conexiones simultáneas
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

## Definir tablas — Table Objects vs DbContext

### EF Core — Entidades y DbContext

```csharp
// Modelo
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

### Exposed (DSL API) — Table Objects

```kotlin
// En Exposed defines la tabla como un object singleton
object Users : Table("users") {
    val id            = uuid("id").autoGenerate()
    val email         = varchar("email", 255).uniqueIndex()
    val passwordHash  = varchar("password_hash", 255)
    val createdAt     = datetime("created_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}
```

La diferencia clave: en EF Core tienes una clase para el modelo (POCO) y otra para la configuración (DbContext). En Exposed DSL, la tabla es el punto central — las queries van directamente sobre ella.

---

## Queries — LINQ vs Exposed DSL

### EF Core (LINQ)

```csharp
// SELECT
var users = await ctx.Users
    .Where(u => u.Email == email)
    .FirstOrDefaultAsync();

// INSERT
var user = new User { Email = "test@example.com", PasswordHash = hash };
ctx.Users.Add(user);
await ctx.SaveChangesAsync();

// UPDATE
user.PasswordHash = newHash;
await ctx.SaveChangesAsync();

// DELETE
ctx.Users.Remove(user);
await ctx.SaveChangesAsync();
```

### Exposed DSL

```kotlin
// SELECT — dentro de una transaction { }
val user = Users.selectAll()
    .where { Users.email eq email }
    .firstOrNull()
    ?.let { row ->
        User(id = row[Users.id], email = row[Users.email])
    }

// INSERT
Users.insert {
    it[email] = "test@example.com"
    it[passwordHash] = hash
}

// UPDATE
Users.update({ Users.id eq userId }) {
    it[passwordHash] = newHash
}

// DELETE
Users.deleteWhere { Users.id eq userId }
```

---

## Transacciones

En EF Core, `SaveChangesAsync()` es implícitamente transaccional. En Exposed, **toda operación debe estar dentro de una `transaction { }`**:

```kotlin
// Exposed — transacción explícita (blocking I/O)
transaction {
    Users.insert {
        it[email] = "test@example.com"
        it[passwordHash] = hash
    }
}

// Para coroutines (suspending), usa suspendTransaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun createUser(email: String, hash: String) {
    newSuspendedTransaction {
        Users.insert {
            it[Users.email] = email
            it[passwordHash] = hash
        }
    }
}
```

Esto equivale a:

```csharp
// EF Core — transacción explícita (raramente necesaria)
using var tx = await ctx.Database.BeginTransactionAsync();
try {
    ctx.Users.Add(user);
    await ctx.SaveChangesAsync();
    await tx.CommitAsync();
} catch {
    await tx.RollbackAsync();
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

El equivalente en .NET sería usar **Flyway** o **Liquibase** en lugar de EF Migrations — herramientas que aplican SQL scripts en orden y registran cuáles ya se ejecutaron.

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

El formato de la connection string JDBC (`jdbc:postgresql://host:port/database`) es diferente al formato de Npgsql de .NET, pero la información es la misma.

El **driver** (`org.postgresql.Driver`) es el equivalente a tener instalado el paquete NuGet `Npgsql` — es el adaptador específico de PostgreSQL para la API JDBC de Java.

---

## Resumen: tabla de equivalencias

| Entity Framework Core / .NET | Exposed / JVM | Notas |
|---|---|---|
| `DbContext` | `DatabaseFactory` (singleton) | Gestiona la conexión a BD |
| Connection pool (integrado en EF) | HikariCP (explícito) | Pool de conexiones |
| `DbSet<User>` | `object Users : Table(...)` | Definición de tabla |
| POCO / Record | `ResultRow` + data class manual | Resultado de query |
| LINQ `.Where().FirstOrDefault()` | `.selectAll().where { }.firstOrNull()` | Queries |
| `SaveChangesAsync()` | `transaction { }` | Persistir cambios |
| `BeginTransactionAsync()` | `newSuspendedTransaction { }` | Transacción explícita |
| EF Migrations | Scripts SQL numerados (V001__.sql) | Evolución del esquema |
| `dotnet ef database update` | Aplicar scripts manualmente / Flyway | Ejecutar migraciones |
| `Npgsql` NuGet package | `org.postgresql:postgresql` JAR | Driver de PostgreSQL |
| `appsettings.json` | `application.conf` (HOCON) | Configuración |
| Connection string Npgsql format | `jdbc:postgresql://` format | Formato de URL de BD |
