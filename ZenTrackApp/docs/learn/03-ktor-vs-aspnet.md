# 03 — Ktor para devs ASP.NET Core

> Cómo pensar en Ktor si conoces el pipeline de ASP.NET Core.

---

## Filosofía: minimal y explícito

ASP.NET Core usa un modelo de **middleware pipeline** más implícito — mucho está configurado por defecto con `AddControllers()`, `UseRouting()`, etc. Ktor es lo contrario: **nada viene activado por defecto**. Todo se instala explícitamente mediante **plugins**.

```
ASP.NET Core:  builder.Services.AddControllers() + app.MapControllers()
Ktor:          install(ContentNegotiation) { json() } + routing { get("/") { ... } }
```

Esto significa más código inicial, pero el comportamiento es totalmente predecible: si no instalas el plugin, la funcionalidad no existe.

---

## La pipeline: Middleware vs Plugins

En ASP.NET Core, el pipeline es una cadena de middleware:

```csharp
// Program.cs (ASP.NET Core)
var builder = WebApplication.CreateBuilder(args);
builder.Services.AddControllers();
builder.Services.AddAuthentication().AddJwtBearer(...);

var app = builder.Build();
app.UseAuthentication();
app.UseAuthorization();
app.UseRouting();
app.MapControllers();
app.Run();
```

En Ktor, el equivalente son los **plugins** instalados en `Application.module()`:

```kotlin
// Application.kt (Ktor)
fun Application.module() {
    install(ContentNegotiation) { json() }   // AddControllers + JSON serializer
    install(StatusPages) { ... }             // UseExceptionHandler
    install(CORS) { ... }                    // UseCors
    install(CallLogging) { ... }             // UseHttpLogging
    install(Authentication) { jwt("jwt") { ... } }  // AddAuthentication + AddJwtBearer
    configureRouting()
}
```

La diferencia conceptual: en .NET el orden de los `Use*()` es importante (son middleware que se encadenan). En Ktor los plugins son más independientes entre sí, aunque el orden de instalación también puede importar (especialmente `Authentication` antes que las rutas protegidas).

---

## ContentNegotiation — El serializador JSON

```csharp
// ASP.NET Core — activa JSON por defecto con System.Text.Json
builder.Services.AddControllers()
    .AddJsonOptions(opts => opts.JsonSerializerOptions.PropertyNamingPolicy = null);
```

```kotlin
// Ktor — activa JSON con kotlinx.serialization
install(ContentNegotiation) {
    json(Json {
        ignoreUnknownKeys = true
        isLenient = false
    })
}
```

Ktor no usa Jackson ni System.Text.Json — usa **kotlinx.serialization**, la librería oficial de JetBrains. Para que una clase sea serializable debes anotarla:

```kotlin
// Kotlin — necesitas anotar explícitamente
@Serializable
data class UserDto(val id: String, val email: String)

// C# — System.Text.Json serializa cualquier clase pública por defecto
record UserDto(string Id, string Email);
```

---

## StatusPages — El ExceptionHandler global

```csharp
// ASP.NET Core
app.UseExceptionHandler("/error");
// o con ProblemDetails:
builder.Services.AddProblemDetails();
```

```kotlin
// Ktor
install(StatusPages) {
    exception<Throwable> { call, cause ->
        call.application.log.error("Unhandled exception", cause)
        call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
    }
    // También puedes manejar status codes específicos:
    status(HttpStatusCode.NotFound) { call, _ ->
        call.respond(HttpStatusCode.NotFound, ErrorResponse("Not found"))
    }
}
```

El plugin `StatusPages` intercepta cualquier excepción no capturada en las rutas y devuelve una respuesta estructurada. En ZenTrack, la `ErrorResponse` es un `@Serializable data class` que se serializa como JSON gracias a que `ContentNegotiation` ya está instalado.

---

## CORS

```csharp
// ASP.NET Core
builder.Services.AddCors(opts => opts.AddPolicy("AllowAll", p =>
    p.AllowAnyOrigin().AllowAnyHeader().AllowAnyMethod()));
app.UseCors("AllowAll");
```

```kotlin
// Ktor
install(CORS) {
    anyHost()
    allowHeader(HttpHeaders.ContentType)
    allowHeader(HttpHeaders.Authorization)
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    allowMethod(HttpMethod.Patch)
}
```

---

## CallLogging — El HttpLogging

```csharp
// ASP.NET Core
builder.Services.AddHttpLogging(logging => {
    logging.LoggingFields = HttpLoggingFields.RequestPath | HttpLoggingFields.Duration;
});
app.UseHttpLogging();
```

```kotlin
// Ktor
install(CallLogging) {
    level = Level.INFO  // loggea método HTTP, ruta y status code de cada request
}
```

---

## Routing — De Controllers a functions

Este es el cambio conceptual más grande. ASP.NET Core usa **Controllers** con atributos. Ktor usa **funciones** dentro de un bloque `routing { }`. Es más parecido a Minimal APIs de .NET 6+.

### ASP.NET Core (Controller-based)

```csharp
[ApiController]
[Route("api/[controller]")]
public class AuthController : ControllerBase
{
    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] LoginRequest request)
    {
        var token = _authService.GenerateToken(request.Email);
        return Ok(new { token });
    }
}
```

### ASP.NET Core (Minimal APIs — más parecido a Ktor)

```csharp
app.MapPost("/api/auth/login", async (LoginRequest request, IAuthService authService) =>
{
    var token = authService.GenerateToken(request.Email);
    return Results.Ok(new { token });
});
```

### Ktor

```kotlin
fun Route.authRoutes(jwtService: JwtService) {
    route("/api/auth") {
        post("/login") {
            val request = call.receive<LoginRequest>()
            val token = jwtService.generateToken(userId = request.email)
            call.respond(HttpStatusCode.OK, AuthResponse(token = token))
        }
    }
}
```

El `call` de Ktor es el equivalente al `HttpContext` de ASP.NET Core:

| ASP.NET Core | Ktor | Descripción |
|---|---|---|
| `Request.Body` → `FromBody` | `call.receive<T>()` | Deserializar el body de la request |
| `Ok(data)` / `Results.Ok(data)` | `call.respond(HttpStatusCode.OK, data)` | Responder con status + body |
| `Request.Headers["Authorization"]` | `call.request.headers["Authorization"]` | Leer un header |
| `HttpContext.User` | `call.principal<JWTPrincipal>()` | Obtener el usuario autenticado |

---

## Arquitectura de rutas — la organización en ZenTrack

En ASP.NET Core los Controllers se registran automáticamente con `MapControllers()`. En Ktor hay que registrar cada grupo de rutas manualmente en `configureRouting()`:

```kotlin
// api/Routing.kt
fun Application.configureRouting(jwtService: JwtService) {
    routing {
        // Rutas públicas — sin autenticación
        authRoutes(jwtService)           // POST /api/auth/login, /register

        // Rutas protegidas — requieren JWT válido
        authenticate("jwt") {
            // Fase 2: workspaceRoutes()
            // Fase 3: taskRoutes()
        }
    }
}
```

El patrón de extraer rutas a funciones de extensión sobre `Route` (`fun Route.authRoutes(...)`) es el equivalente a tener Controllers separados por dominio.

---

## Extension functions — `fun Application.configureSerialization()`

El prefijo `Application.` no es una clase anidada ni un namespace — es una **extension function** de Kotlin. Permite añadir métodos a una clase existente sin modificarla ni heredar de ella.

El equivalente exacto en C# son los **métodos de extensión**:

```csharp
// C# — método de extensión (requiere clase estática wrapper)
public static class ApplicationExtensions
{
    public static void ConfigureSerialization(this WebApplication app)
    {
        app.Services.AddControllers();
        // "app" es el WebApplication — equivale a "this" en Kotlin
    }
}
```

```kotlin
// Kotlin — extension function (sintaxis directa, sin clase wrapper)
fun Application.configureSerialization() {
    install(ContentNegotiation) { json() }
    // "this" es la instancia de Application — implícito
}
```

`Application` es una clase de Ktor (`io.ktor.server.application.Application`) que representa el servidor en ejecución. Contiene `environment` (config), `log` y la función `install()`. Ctrl+click en IntelliJ para ver su código fuente.

El patrón completo: cuando arrancas el servidor con `embeddedServer(module = Application::module)`, Ktor crea la instancia de `Application` y la pasa como receiver a `module()`. Dentro de `module()` tienes `this` = el servidor, y desde ahí puedes llamar a las demás extension functions que configuran los plugins.

```kotlin
// Sin extension function — parámetro explícito
fun configureModule(app: Application) {
    app.install(ContentNegotiation) { ... }
}

// Con extension function — this implícito, más idiomático en Kotlin
fun Application.module() {
    install(ContentNegotiation) { ... }   // this.install(...) implícito
}
```

## KDoc — documentación al hacer hover

El equivalente de los XML docs de C# es **KDoc**. Sintaxis casi idéntica:

```csharp
// C# — XML docs (aparecen en IntelliJ/VS al hacer hover)
/// <summary>Instala ContentNegotiation con System.Text.Json.</summary>
public void ConfigureSerialization() { }
```

```kotlin
// Kotlin — KDoc (aparece en IntelliJ al hacer hover)
/**
 * Instala ContentNegotiation con kotlinx.serialization JSON.
 * ignoreUnknownKeys permite recibir payloads con campos extra sin error.
 */
fun Application.configureSerialization() { }
```

Las funciones `configure*()` de ZenTrack no tienen KDoc porque sus nombres ya dicen lo que hacen. Si al hacer hover no ves documentación, es porque son funciones propias sin comentarios — no un error de IntelliJ. Para las funciones de Ktor (`install`, `routing`, etc.) sí aparece su documentación porque JetBrains las documentó.

## Resumen: tabla de equivalencias

| ASP.NET Core | Ktor | Notas |
|---|---|---|
| `Program.cs` | `Application.kt` | Punto de entrada y configuración |
| `builder.Services.Add*()` | `install(Plugin)` | Registrar servicios/plugins |
| `app.Use*()` | También `install(Plugin)` | Activar middleware |
| Middleware Pipeline | Plugin System | Mismo concepto, diferente API |
| Controller + `[ApiController]` | `fun Route.myRoutes()` | Agrupación de endpoints |
| `[HttpGet]`, `[HttpPost]`, etc. | `get { }`, `post { }` | Definir un endpoint |
| `[FromBody]` / `Request.Body` | `call.receive<T>()` | Deserializar request body |
| `Ok(data)` / `Results.Ok(data)` | `call.respond(OK, data)` | Responder con datos |
| `HttpContext` | `call` (ApplicationCall) | Contexto de la request actual |
| `ILogger<T>` | `call.application.log` | Logger (Logback en JVM) |
| `System.Text.Json` | `kotlinx.serialization` | Librería de serialización JSON |
| `appsettings.json` | `application.conf` (HOCON) | Configuración |
| `UseExceptionHandler` | `StatusPages` plugin | Manejo global de errores |
| `UseCors` | `CORS` plugin | Cross-Origin Resource Sharing |
| `UseHttpLogging` | `CallLogging` plugin | Log de requests HTTP |
| `[Authorize]` | `authenticate("jwt") { }` | Proteger rutas con JWT |
