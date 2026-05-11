# 11 — OAuth 2.0 con Google en ZenTrack

> Flujo completo de autenticación OAuth 2.0 con Google: backend Ktor + SPA React. Comparativa con el enfoque ASP.NET Core.

---

## ¿Qué problema resuelve OAuth?

Sin OAuth, el usuario tendría que dar su contraseña de Google a ZenTrack para que ZenTrack acceda a su cuenta. OAuth permite que Google autentique al usuario y le comunique a ZenTrack "este usuario es quien dice ser" sin que ZenTrack toque nunca la contraseña de Google.

El resultado para ZenTrack es un JWT propio (no un token de Google) — Google es solo el mecanismo de verificación de identidad.

---

## Flujo implementado en ZenTrack (Authorization Code + PKCE-ready)

```
1. Usuario pulsa "Continuar con Google"
   → Frontend: window.location.href = GET /api/auth/google

2. Backend genera state UUID (CSRF token), almacena en memoria
   → Backend redirige 302 a accounts.google.com/o/oauth2/auth?
       client_id=...&redirect_uri=http://localhost:3000/auth/callback
       &scope=openid email profile&state=<uuid>

3. Google muestra pantalla de autorización
   → Usuario aprueba

4. Google redirige al frontend:
   http://localhost:3000/auth/callback?code=<auth_code>&state=<uuid>

5. OAuthCallbackScreen.tsx lee code y state de la URL
   → Llama POST /api/auth/google/exchange { code, state }

6. Backend valida state (¿lo generó este servidor?), intercambia code con Google
   → Google Token Endpoint devuelve access_token + id_token
   → Backend llama Google UserInfo API → obtiene email, nombre
   → Crea o encuentra usuario en BD
   → Guarda tokens de Google cifrados en oauth_accounts
   → Emite JWT propio de ZenTrack

7. Frontend recibe AuthResponse { token, refreshToken }
   → Guarda token en localStorage
   → Navega al panel principal
```

**¿Por qué el `redirect_uri` apunta al frontend y no al backend?**

Opciones evaluadas:

| Opción | JWT en URL | Seguridad | Complejidad |
|--------|-----------|-----------|-------------|
| Backend redirige con `?token=XXX` | Sí — query param | ❌ historial, logs | Baja |
| Backend redirige con `#token=XXX` | Sí — fragment | ⚠️ historial browser | Baja |
| **Frontend como redirectUri (elegida)** | **No** | **✅ JWT nunca en URL** | **Media** |
| HttpOnly cookie | No | ✅✅ | Alta |

El `code` que aparece en la URL del frontend es efímero (válido ~1 min, un solo uso) y sin valor una vez usado — no es un secreto. El JWT nunca aparece en ninguna URL.

---

## Equivalencia con ASP.NET Core

En ASP.NET Core usarías:

```csharp
// Program.cs
builder.Services.AddAuthentication()
    .AddGoogle(options => {
        options.ClientId = config["Google:ClientId"];
        options.ClientSecret = config["Google:ClientSecret"];
    });
```

ASP.NET maneja el flujo OAuth internamente (genera el state, valida el callback) y lo integra con su middleware de cookies/sesión.

En Ktor **no existe ese middleware automático**. El flujo se implementa manualmente:
- `GET /api/auth/google` → genera state y URL
- `GoogleApiClient` → hace las llamadas HTTP al Token Endpoint y UserInfo API de Google
- `GoogleOAuthService` → orquesta la lógica: validar state, intercambiar code, crear usuario

Es más código, pero también más control y transparencia sobre lo que ocurre.

---

## Archivos clave en ZenTrack

| Archivo | Responsabilidad |
|---------|----------------|
| `server/api/auth/GoogleOAuthRoutes.kt` | Endpoints `GET /google` y `POST /google/exchange` |
| `server/core/GoogleOAuthService.kt` | Lógica: `buildAuthorizationUrl()`, `handleCallback()`, `linkAccount()` |
| `server/integrations/google/GoogleApiClient.kt` | HTTP hacia Google (Token Endpoint, UserInfo API) |
| `server/core/TokenEncryptionService.kt` | Cifra tokens de Google (AES-256) antes de guardarlos en BD |
| `webApp/screens/OAuthCallbackScreen.tsx` | Lee `code`/`state` de la URL, llama al exchange endpoint |
| `webApp/services/authService.ts` | `exchangeGoogleCode()` — POST al backend |
| `shared/dto/AuthDto.kt` | `GoogleExchangeRequest` — DTO compartido backend/frontend |

---

## Sealed classes como resultado de operaciones OAuth

Las operaciones OAuth pueden tener múltiples resultados — más que un simple éxito/error. En C# modelarías esto con una clase abstracta o un `OneOf<T1,T2,T3>` de librería. Kotlin tiene **sealed classes** como discriminated unions nativas:

```kotlin
// Equivalent to F# discriminated union / C# OneOf
sealed class LinkResult {
    object Success : LinkResult()
    object AlreadyLinkedToOtherUser : LinkResult()
    object OAuthError : LinkResult()
}
```

El compilador garantiza exhaustividad en el `when` — si añades un caso y olvidas manejarlo, falla en compilación:

```kotlin
when (googleOAuthService.linkAccount(userId, req.code)) {
    is LinkResult.Success              -> call.respond(HttpStatusCode.NoContent)
    is LinkResult.AlreadyLinkedToOtherUser -> call.respond(HttpStatusCode.Conflict, ...)
    is LinkResult.OAuthError           -> call.respond(HttpStatusCode.BadRequest, ...)
    // Olvidar un caso → error de compilación, no runtime
}
```

Usado también en `UnlinkResult` para el endpoint `DELETE /api/users/me/oauth/{id}`:

```kotlin
sealed class UnlinkResult {
    object Success : UnlinkResult()
    object NotFound : UnlinkResult()
    object LastLoginMethod : UnlinkResult()  // sin contraseña → no permitir desvincular
}
```

---

## Seguridad: ¿qué valida cada capa?

| Validación | Dónde |
|------------|-------|
| `state` CSRF | `GoogleOAuthService.handleCallback()` — compara UUID con el generado en `buildAuthorizationUrl()` |
| Ownership de cuenta OAuth | `UserService.unlinkOAuthAccount()` — verifica `account.userId == userId` |
| Último método de login | `UserService.unlinkOAuthAccount()` — rechaza si `password_hash == null` |
| Tokens de Google cifrados | `TokenEncryptionService` — AES-256-GCM antes de guardar en `oauth_accounts` |

---

## Configuración en `application.conf`

```hocon
google {
    clientId     = "..."           // De Google Cloud Console
    clientSecret = "..."           // Nunca en git
    redirectUri  = "http://localhost:3000/auth/callback"  // Frontend, no backend
    encryptionKey = "..."          // Base64, 32 bytes — para cifrar tokens de Google
}
```

Para producción, los valores secretos se inyectan como variables de entorno.
