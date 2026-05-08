# 05 — JWT en Ktor

> Autenticación JWT comparada con ASP.NET Core — lo que cambia, lo que es igual.

---

## JWT es JWT — el concepto no cambia

Un JWT (JSON Web Token) funciona igual independientemente del framework:

1. El cliente manda credenciales a `POST /api/auth/login`
2. El servidor valida y devuelve un token firmado
3. El cliente incluye el token en requests posteriores: `Authorization: Bearer <token>`
4. El servidor valida la firma del token en cada request protegida

Lo que cambia entre ASP.NET Core y Ktor es **la API de configuración**, no el concepto.

---

## La librería JWT — java-jwt vs Microsoft.AspNetCore.Authentication.JwtBearer

### .NET

```csharp
// NuGet packages
// Microsoft.AspNetCore.Authentication.JwtBearer
// System.IdentityModel.Tokens.Jwt (si generas tokens manualmente)

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options => {
        options.TokenValidationParameters = new TokenValidationParameters {
            ValidateIssuer = true,
            ValidIssuer = "zentrack",
            ValidateAudience = true,
            ValidAudience = "zentrack-users",
            ValidateLifetime = true,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(secret))
        };
    });
```

### Ktor + java-jwt

`ktor-server-auth-jwt` usa internamente la librería **`com.auth0:java-jwt`** (la librería JWT más popular del ecosistema JVM, equivalente a `System.IdentityModel.Tokens.Jwt`).

```kotlin
// Ktor — configureAuthentication() en Plugins.kt
install(Authentication) {
    jwt("jwt") {                        // "jwt" es el nombre del scheme
        realm = "ZenTrack API"          // aparece en el header WWW-Authenticate
        verifier(
            JWT.require(Algorithm.HMAC256(secret))
                .withAudience(audience)
                .withIssuer(issuer)
                .build()
        )
        validate { credential ->
            // credential.payload contiene los claims del token
            if (credential.payload.getClaim("userId").asString() != null) {
                JWTPrincipal(credential.payload)  // token válido
            } else null                            // token inválido → 401
        }
        challenge { _, _ ->
            // respuesta cuando el token es inválido o falta
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Token inválido o expirado"))
        }
    }
}
```

---

## JwtService — Generación de tokens

### .NET (JwtSecurityTokenHandler)

```csharp
public class JwtService
{
    private readonly string _secret;
    private readonly string _issuer;
    private readonly string _audience;

    public string GenerateToken(string userId)
    {
        var claims = new[] { new Claim("userId", userId) };
        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_secret));
        var credentials = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

        var token = new JwtSecurityToken(
            issuer: _issuer,
            audience: _audience,
            claims: claims,
            expires: DateTime.UtcNow.AddDays(1),
            signingCredentials: credentials
        );

        return new JwtSecurityTokenHandler().WriteToken(token);
    }
}
```

### Kotlin (com.auth0:java-jwt)

```kotlin
// core/JwtService.kt
class JwtService(
    private val secret: String,
    private val issuer: String,
    private val audience: String,
    private val expirationMs: Long = 86_400_000L  // 24h en milisegundos
) {
    fun generateToken(userId: Long): String =   // Long, no String — ver sección "Claims numéricos"
        JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", userId)        // se guarda como número JSON, no como string
            .withExpiresAt(Date(System.currentTimeMillis() + expirationMs))
            .sign(Algorithm.HMAC256(secret))
}
```

La API de `com.auth0:java-jwt` es un **builder fluido** muy parecido al de `JwtSecurityToken`. El resultado es idéntico: un string JWT firmado.

---

## Proteger rutas — `[Authorize]` vs `authenticate("jwt")`

### ASP.NET Core

```csharp
// Atributo en Controller — protege todos los endpoints del controller
[Authorize]
[ApiController]
[Route("api/[controller]")]
public class WorkspacesController : ControllerBase { ... }

// O en un endpoint específico (Minimal APIs)
app.MapGet("/api/workspaces", [Authorize] async (AppDbContext ctx) => { ... });

// Rutas públicas — sin atributo o con [AllowAnonymous]
app.MapPost("/api/auth/login", async (...) => { ... });
```

### Ktor

```kotlin
// Routing.kt — separación explícita en el grafo de rutas
fun Application.configureRouting(jwtService: JwtService) {
    routing {
        // Rutas PÚBLICAS — fuera del bloque authenticate
        authRoutes(jwtService)          // POST /api/auth/login, /register

        // Rutas PROTEGIDAS — dentro del bloque authenticate
        authenticate("jwt") {
            // workspaceRoutes()        // GET/POST /api/workspaces
            // taskRoutes()             // GET/POST /api/tasks
        }
    }
}
```

La diferencia de filosofía: en ASP.NET Core, las rutas son públicas por defecto y `[Authorize]` las protege. En Ktor, dentro del bloque `authenticate("jwt") { }` **todas** las rutas son protegidas — las públicas están fuera del bloque.

Ambos enfoques son correctos; Ktor simplemente hace la separación más visual en la estructura del código.

---

## Leer el usuario autenticado

### ASP.NET Core

```csharp
// En un Controller
var userId = User.FindFirst("userId")?.Value;

// En Minimal APIs
app.MapGet("/api/me", (ClaimsPrincipal user) =>
{
    var userId = user.FindFirst("userId")?.Value;
    return Results.Ok(new { userId });
}).RequireAuthorization();
```

### Ktor

```kotlin
// En cualquier ruta dentro de authenticate("jwt") { }
get("/api/me") {
    val principal = call.principal<JWTPrincipal>()
    val userId = principal?.payload?.getClaim("userId")?.asLong()  // asLong(), no asString()
    call.respond(HttpStatusCode.OK, mapOf("userId" to userId))
}
```

`JWTPrincipal` es el equivalente de `ClaimsPrincipal` en .NET. Contiene el payload del JWT con todos los claims que pusiste al generar el token.

---

## Claims numéricos — la trampa de `.asString()`

Este es uno de los bugs más silenciosos que puedes encontrar al cambiar el tipo de ID.

### El problema

`.withClaim("userId", userId)` en `java-jwt` guarda el claim con el **tipo nativo** del valor:

```kotlin
.withClaim("userId", "abc-123")   // → JSON: "userId": "abc-123"  (string)
.withClaim("userId", 42L)         // → JSON: "userId": 42         (number)
```

Cuando lees el claim de vuelta, el método importa:

```kotlin
payload.getClaim("userId").asString()  // → null  si el claim es un número
payload.getClaim("userId").asLong()    // → 42L   ✓
payload.getClaim("userId").asInt()     // → 42    ✓
```

`.asString()` devuelve `null` para claims de tipo numérico — sin excepción, sin warning. El bug es que la validación del token (en `Plugins.kt`) también usa `.asString()`, así que cada token generado con un Long falla silenciosamente:

```kotlin
// Plugins.kt — validación del JWT
validate { credential ->
    // BUG: si userId se guardó como Long, asString() devuelve null → token rechazado
    if (credential.payload.getClaim("userId").asString() != null) {
        JWTPrincipal(credential.payload)
    } else null   // ← siempre llega aquí con IDs numéricos
}
```

El síntoma: el `POST /api/auth/register` devuelve un token, pero cualquier ruta protegida responde con `401 Token inválido o expirado`. El token es válido — la validación está mal.

### La solución

Alinear el tipo en generación y en lectura:

```kotlin
// Generación (JwtService.kt)
.withClaim("userId", userId)   // userId: Long → se guarda como número

// Validación (Plugins.kt)
validate { credential ->
    if (credential.payload.getClaim("userId").asLong() != null) {  // ← asLong()
        JWTPrincipal(credential.payload)
    } else null
}

// Lectura en rutas
val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
```

### Analogía .NET

En ASP.NET Core, los claims siempre son strings (`Claim.Value: string`), así que este problema no existe. En `java-jwt`, el tipo del claim refleja el tipo JSON real — la misma diferencia que hay entre un campo `string` y un campo `number` en JSON. Si lo guardas como número, tienes que leerlo como número.

---

## Configuración — application.conf

```hocon
# application.conf
jwt {
    secret = "dev-secret-change-in-production-min-32-chars!!"
    issuer = "zentrack"
    audience = "zentrack-users"
    realm = "ZenTrack API"
    expirationMs = 86400000
}
```

```json
// appsettings.json (.NET equivalente)
{
  "JwtSettings": {
    "Secret": "dev-secret-change-in-production-min-32-chars!!",
    "Issuer": "zentrack",
    "Audience": "zentrack-users",
    "ExpirationMinutes": 1440
  }
}
```

El `realm` es el valor que aparece en el header `WWW-Authenticate: Bearer realm="ZenTrack API"` cuando una request no incluye token. En .NET esto es configurado automáticamente por `JwtBearerDefaults`.

---

## Flujo completo en ZenTrack

```
Cliente                    Ktor Server
   |                            |
   |  POST /api/auth/login      |
   |  { email, password }  ──►  |
   |                            |  configureAuthentication() no actúa
   |                            |  (ruta pública, fuera de authenticate)
   |                            |  authRoutes() recibe la request
   |                            |  Register: hash password → insert user → token con UUID real
   |                            |  Login: SELECT user by email → verifica BCrypt hash → token con UUID real
   |  { token: "eyJ..." }  ◄──  |
   |                            |
   |  GET /api/workspaces       |
   |  Authorization: Bearer eyJ |
   |  ───────────────────────►  |
   |                            |  install(Authentication) { jwt("jwt") }
   |                            |  verifica firma HMAC256
   |                            |  valida audience, issuer, expiresAt
   |                            |  validate { } extrae claim "userId"
   |                            |  → JWTPrincipal (usuario autenticado)
   |                            |  la ruta se ejecuta con el usuario
   |  { workspaces: [...] }  ◄─ |
```

---

## BCrypt — hashing de contraseñas

El equivalente en Kotlin de `BCryptPasswordHasher<T>` de ASP.NET Core Identity es la librería **`at.favre.lib:bcrypt`** (declarada en `gradle/libs.versions.toml`).

### Comparativa

```csharp
// ASP.NET Core Identity
var hasher = new PasswordHasher<IdentityUser>();
string hash = hasher.HashPassword(user, password);
PasswordVerificationResult result = hasher.VerifyHashedPassword(user, hash, password);
```

```kotlin
// at.favre.lib:bcrypt
val hash: String = BCrypt.withDefaults().hashToString(12, password.toCharArray())
val verified: Boolean = BCrypt.verifyer().verify(password.toCharArray(), hash).verified
```

El `12` es el **cost factor** (equivalente a `IterationCount` en ASP.NET Core Identity). El valor por defecto de ASP.NET Core Identity es también 10-12 iteraciones.

### AuthService real de ZenTrack

```kotlin
// core/AuthService.kt
class AuthService(
    private val userRepository: UserRepositoryImpl,
    private val jwtService: JwtService
) {
    suspend fun register(email: String, password: String, name: String): String {
        val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val user = userRepository.create(email, passwordHash, name)
        return jwtService.generateToken(userId = user.id)  // user.id es Long (BIGINT)
    }

    suspend fun login(email: String, password: String): String? {
        val user = userRepository.findByEmail(email) ?: return null
        val verified = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash).verified
        if (!verified) return null
        return jwtService.generateToken(userId = user.id)
    }
}
```

> `userRepository.findByEmail()` funciona sin necesidad de establecer `app.user_id` porque la migración V006 dividió la política RLS `users_self` para permitir SELECT cuando `app.user_id` no está establecido. Ver `04-exposed-vs-ef.md` para los detalles.

---

## Estado actual en ZenTrack (Fase 2)

La autenticación está completamente implementada:
- `POST /api/auth/register` crea un usuario en PostgreSQL con la contraseña hasheada con BCrypt y devuelve un JWT
- `POST /api/auth/login` consulta el usuario por email, verifica el hash BCrypt y devuelve un JWT
- El claim `userId` del JWT contiene el `BIGINT` del usuario como número JSON (no string) — se lee con `.asLong()` en todas las rutas y en la validación del plugin

---

## Resumen: tabla de equivalencias

| ASP.NET Core / .NET | Ktor / Kotlin | Notas |
|---|---|---|
| `Microsoft.AspNetCore.Authentication.JwtBearer` | `ktor-server-auth-jwt-jvm` | Paquete de autenticación JWT |
| `System.IdentityModel.Tokens.Jwt` | `com.auth0:java-jwt` | Librería de generación/validación |
| `AddAuthentication().AddJwtBearer(...)` | `install(Authentication) { jwt { } }` | Configurar validación |
| `TokenValidationParameters` | `.withAudience().withIssuer()` en el verifier | Parámetros de validación |
| `JwtSecurityTokenHandler.WriteToken()` | `JWT.create()...sign()` | Generar token |
| `[Authorize]` | `authenticate("jwt") { }` | Proteger rutas |
| `[AllowAnonymous]` | fuera del bloque `authenticate` | Rutas públicas |
| `ClaimsPrincipal` | `JWTPrincipal` | Objeto del usuario autenticado |
| `User.FindFirst("claim")` | `principal?.payload?.getClaim("claim")` | Leer un claim |
| `IssuerSigningKey` (HMAC256) | `Algorithm.HMAC256(secret)` | Algoritmo de firma |
| `options.Challenge` / `OnChallenge` | `challenge { }` block | Respuesta 401 personalizada |
