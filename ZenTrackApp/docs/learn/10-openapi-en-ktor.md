# 10 — OpenAPI / Swagger UI en Ktor

> Equivalente a Swashbuckle en ASP.NET Core, usando `ktor-openapi` (smiley4).

---

## El problema: Ktor no tiene autodetección de tipos

En ASP.NET Core, Swashbuckle/NSwag leen los atributos de los Controllers y generan la especificación OpenAPI automáticamente:

```csharp
// ASP.NET Core — Swashbuckle lee esto solo
[HttpPost]
[ProducesResponseType(typeof(AuthResponse), 201)]
[ProducesResponseType(typeof(ErrorResponse), 400)]
public IActionResult Register([FromBody] RegisterRequest req) { ... }
```

En Ktor, el routing es DSL puro sin reflexión de atributos. La librería más madura para generación automática de spec es **`io.github.smiley4:ktor-openapi`**, que extiende el DSL de routing con un bloque de documentación opcional.

---

## La librería — `ktor-openapi` vs oficial Ktor

| | `io.ktor:ktor-server-swagger` (oficial) | `io.github.smiley4:ktor-openapi` (smiley4) |
|---|---|---|
| Generación de spec | Manual (YAML estático) | Automática desde código |
| DSL de rutas | No modifica rutas | Envuelve cada handler |
| Tipos de respuesta | No infiere | Inferidos con `body<T>()` |
| Mantenimiento | JetBrains | Comunidad (activo, >1k stars) |
| Compatibilidad Ktor 3.x | Sí | Desde v5.x |

Para ZenTrack se eligió `ktor-openapi` porque el usuario viene de Swashbuckle y espera que los tipos se reflejen en la UI.

---

## Instalación

### `gradle/libs.versions.toml`

```toml
[versions]
smiley4 = "5.4.0"   # compatible con Ktor 3.3.x

[libraries]
smiley4-ktorOpenapi = { module = "io.github.smiley4:ktor-openapi", version.ref = "smiley4" }
smiley4-swaggerUi   = { module = "io.github.smiley4:ktor-swagger-ui", version.ref = "smiley4" }
```

### `server/build.gradle.kts`

```kotlin
implementation(libs.smiley4.ktorOpenapi)
implementation(libs.smiley4.swaggerUi)
```

Los módulos internos necesarios (schema-kenerator, swagger-parser, jackson) se incluyen como dependencias transitivas — no se declaran manualmente.

---

## Configuración del plugin

```kotlin
// api/Plugins.kt
fun Application.configureOpenApi() {
    install(OpenApi) {
        info {
            title   = "ZenTrack API"
            version = "1.0.0"
            description = "API multi-tenant de ZenTrack — gestión ágil de proyectos y tareas"
        }
        server {
            url         = "http://localhost:8080"
            description = "Servidor de desarrollo"
        }
        security {
            securityScheme("bearerAuth") {
                type         = AuthType.HTTP
                scheme       = AuthScheme.BEARER
                bearerFormat = "JWT"
                description  = "JWT generado en POST /api/auth/login o /api/auth/register"
            }
            defaultSecuritySchemeNames("bearerAuth")
            defaultUnauthorizedResponse {
                description = "Token JWT ausente o inválido"
                body<ErrorResponse>()
            }
        }
    }
}
```

**Analogía ASP.NET Core:**

```csharp
// Program.cs
builder.Services.AddSwaggerGen(c => {
    c.SwaggerDoc("v1", new OpenApiInfo { Title = "ZenTrack API", Version = "v1" });
    c.AddSecurityDefinition("bearerAuth", new OpenApiSecurityScheme {
        Type   = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT"
    });
    c.AddSecurityRequirement(new OpenApiSecurityRequirement { ... });
});
```

---

## Servir la spec y la UI

```kotlin
// api/Routing.kt
fun Application.configureRouting(...) {
    routing {
        // Rutas de documentación — excluidas de la spec automáticamente
        route("api.json") { openApi() }     // GET /api.json → especificación JSON
        route("swagger")  { swaggerUI("/api.json") }  // GET /swagger → Swagger UI

        authRoutes(authService)             // públicas, fuera de authenticate

        authenticate("jwt") {
            workspaceRoutes(workspaceService)
            projectRoutes(projectService)
        }
    }
}
```

Importaciones necesarias:

```kotlin
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktorswaggerui.swaggerUI
```

---

## Documentar rutas — el DSL

La diferencia respecto a Ktor estándar es el segundo argumento (lambda de documentación) que se añade **antes** del handler:

```kotlin
// Ktor estándar
get("/users") { /* handler */ }

// ktor-openapi — se añade el bloque de docs como segundo argumento
get("/users", {
    description = "..."
    response { code(HttpStatusCode.OK) { body<List<UserResponse>>() } }
}) { /* mismo handler, sin cambios */ }
```

Dentro del bloque `route { }`, se usa sin path:

```kotlin
route("/api/workspaces") {
    get({ /* docs */ }) { /* handler */ }
    post({ /* docs */ }) { /* handler */ }
}
```

Con path explícito (para rutas como `/api/auth/register`):

```kotlin
route("/api/auth") {
    post("/register", { /* docs */ }) { /* handler */ }
    post("/login",    { /* docs */ }) { /* handler */ }
}
```

### Imports necesarios en cada archivo de rutas

```kotlin
import io.github.smiley4.ktoropenapi.get   // reemplaza la versión Ktor
import io.github.smiley4.ktoropenapi.post  // solo para los casos documentados
```

Las versiones smiley4 tienen firmas distintas (parámetro extra `RouteConfig.() -> Unit`) por lo que no generan ambigüedad de compilación con las de Ktor.

---

## Estructura completa del bloque de documentación

```kotlin
post("/register", {
    tags("Auth")                              // agrupación en Swagger UI
    summary     = "Registrar usuario"         // título corto del endpoint
    description = "Crea un usuario y devuelve JWT"

    request {
        // Body del request
        body<RegisterRequest> {
            description = "Credenciales del nuevo usuario"
        }
        // Path parameter
        pathParameter<Long>("workspaceId") {
            description = "ID del workspace"
        }
        // Query parameter
        queryParameter<String>("filter") {
            description = "Filtro opcional"
        }
    }

    response {
        code(HttpStatusCode.Created) {
            description = "Usuario creado"
            body<AuthResponse>()              // tipo de la respuesta
        }
        code(HttpStatusCode.BadRequest) {
            description = "Campos requeridos ausentes"
            body<ErrorResponse>()
        }
        code(HttpStatusCode.Conflict) {
            description = "Email ya registrado"
            body<ErrorResponse>()
        }
    }
}) { /* handler */ }
```

---

## Seguridad JWT — comportamiento automático

La integración con `authenticate("jwt") { }` de Ktor es automática:

```kotlin
authenticate("jwt") {
    // ↑ ktor-openapi detecta este bloque y aplica "bearerAuth" a TODAS las rutas internas
    workspaceRoutes(workspaceService)
    projectRoutes(projectService)
}
```

Resultado en Swagger UI: el candado `🔒` aparece automáticamente en todos los endpoints dentro del bloque, y la respuesta 401 configurada en `defaultUnauthorizedResponse` se añade sin declararla en cada ruta.

**Analogía .NET:**

```csharp
// En .NET, se añade manualmente a cada operación:
[Authorize]
[ProducesResponseType(401)]
public IActionResult GetWorkspaces() { ... }

// En Ktor + ktor-openapi:
// authenticate("jwt") { } lo hace todo en bloque — más explícito estructuralmente
```

---

## Generación de schemas

`ktor-openapi` usa **reflexión JVM** para generar el JSON Schema de cada tipo. Las clases `@Serializable` de kotlinx.serialization son clases Kotlin normales que la reflexión puede inspeccionar — el schema se genera correctamente sin configuración adicional.

Tipos soportados out-of-the-box:
- `data class` (tipos primitivos, anidados)
- `List<T>`, `Map<K,V>`
- `Long`, `Int`, `String`, `Boolean`
- `@Serializable` con kotlinx.serialization

Ejemplo de schema generado para `WorkspaceResponse`:

```json
{
  "WorkspaceResponse": {
    "type": "object",
    "properties": {
      "id":        { "type": "integer", "format": "int64" },
      "name":      { "type": "string" },
      "ownerId":   { "type": "integer", "format": "int64" },
      "createdAt": { "type": "string" }
    }
  }
}
```

---

## ErrorResponse compartido

Antes de añadir la documentación, cada archivo de rutas tenía su propio:

```kotlin
@Serializable
private data class ErrorResponse(val error: String)
```

Para que `ktor-openapi` pueda reflejar el tipo (necesita acceso a la clase desde la librería), se movió a un archivo público:

```kotlin
// api/ApiModels.kt
@Serializable
data class ErrorResponse(val error: String)
```

Cada archivo de rutas importa `me.dcueto.zentrackapp.api.ErrorResponse` en lugar de definirlo localmente.

---

## Estado actual en ZenTrack

- `GET  /api.json` → especificación OpenAPI 3.1 completa en JSON
- `GET  /swagger`  → Swagger UI interactivo con todos los endpoints
- Endpoints documentados: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/workspaces`, `POST /api/workspaces`, `GET /api/workspaces/{workspaceId}/projects`, `POST /api/workspaces/{workspaceId}/projects`
- Autenticación: el botón "Authorize" en Swagger UI acepta el JWT y lo añade al header `Authorization: Bearer <token>` en todas las pruebas

---

## Resumen: tabla de equivalencias

| ASP.NET Core (Swashbuckle) | Ktor (ktor-openapi) | Notas |
|---|---|---|
| `AddSwaggerGen()` | `install(OpenApi) { }` | Configuración del plugin |
| `UseSwagger()` + `UseSwaggerUI()` | `route("api.json") { openApi() }` + `route("swagger") { swaggerUI() }` | Servir la UI |
| `[ProducesResponseType(typeof(T), 200)]` | `code(HttpStatusCode.OK) { body<T>() }` | Tipo de respuesta por código |
| `[Consumes("application/json")]` + body param | `request { body<T>() }` | Request body |
| `[FromRoute]` | `request { pathParameter<T>("name") }` | Path parameter |
| `[FromQuery]` | `request { queryParameter<T>("name") }` | Query parameter |
| `[Authorize]` | `authenticate("jwt") { }` bloque | Proteger rutas |
| `c.AddSecurityDefinition("bearer", ...)` | `securityScheme("bearerAuth") { type = AuthType.HTTP ... }` | Esquema JWT |
| `[ApiExplorerSettings(IgnoreApi = true)]` | `hidden = true` en el bloque de docs | Ocultar endpoint |
| `[Obsolete]` | `deprecated = true` en el bloque de docs | Marcar como obsoleto |
