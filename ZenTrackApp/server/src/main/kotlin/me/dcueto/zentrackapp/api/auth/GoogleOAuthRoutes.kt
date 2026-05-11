package me.dcueto.zentrackapp.api.auth

import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.dcueto.zentrackapp.api.ErrorResponse
import me.dcueto.zentrackapp.core.GoogleOAuthService
import me.dcueto.zentrackapp.dto.AuthResponse
import me.dcueto.zentrackapp.dto.GoogleExchangeRequest

fun Route.googleOAuthRoutes(googleOAuthService: GoogleOAuthService) {
    route("/api/auth") {
        get("/google", {
            tags("Auth")
            summary = "Iniciar OAuth con Google"
            description = "Genera un state CSRF, construye la URL de autorización de Google con scope openid email profile y redirige (302). El redirectUri apunta al frontend (/auth/callback), que luego llama a POST /api/auth/google/exchange para completar el flujo."
            response {
                code(HttpStatusCode.Found) {
                    description = "Redirect al authorization endpoint de Google"
                }
            }
        }) {
            val url = googleOAuthService.buildAuthorizationUrl()
            call.respondRedirect(url, permanent = false)
        }

        post("/google/exchange", {
            tags("Auth")
            summary = "Intercambiar code OAuth por JWT"
            description = "El frontend recibe code y state de Google en /auth/callback y los envía aquí. Valida el state CSRF, intercambia el code por tokens con Google, crea o vincula el usuario y devuelve un JWT de ZenTrack."
            request {
                body<GoogleExchangeRequest> {
                    description = "Authorization code y state CSRF recibidos por el frontend desde Google"
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Autenticación exitosa"
                    body<AuthResponse>()
                }
                code(HttpStatusCode.BadRequest) {
                    description = "State inválido/expirado o error al comunicarse con Google"
                    body<ErrorResponse>()
                }
            }
        }) {
            val req = call.receive<GoogleExchangeRequest>()
            val response = googleOAuthService.handleCallback(req.code, req.state)
                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("State inválido o expirado"))
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
