package me.dcueto.zentrackapp.api.auth

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.dcueto.zentrackapp.api.ErrorResponse
import me.dcueto.zentrackapp.core.GoogleOAuthService
import me.dcueto.zentrackapp.dto.AuthResponse

fun Route.googleOAuthRoutes(googleOAuthService: GoogleOAuthService) {
    route("/api/auth") {
        get("/google", {
            tags("Auth")
            summary = "Iniciar OAuth con Google"
            description = "Genera un state CSRF, construye la URL de autorización de Google con scope openid email profile y redirige (302)"
            response {
                code(HttpStatusCode.Found) {
                    description = "Redirect al authorization endpoint de Google"
                }
            }
        }) {
            val url = googleOAuthService.buildAuthorizationUrl()
            call.respondRedirect(url, permanent = false)
        }

        get("/google/callback", {
            tags("Auth")
            summary = "Callback OAuth de Google"
            description = "Valida el state CSRF, intercambia el code por tokens en Google, crea o vincula el usuario y emite un JWT de ZenTrack"
            request {
                queryParameter<String>("code") { description = "Authorization code devuelto por Google" }
                queryParameter<String>("state") { description = "CSRF state generado en GET /api/auth/google" }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Autenticación exitosa"
                    body<AuthResponse>()
                }
                code(HttpStatusCode.BadRequest) {
                    description = "Parámetro ausente o state inválido/expirado"
                    body<ErrorResponse>()
                }
            }
        }) {
            val code = call.request.queryParameters["code"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Falta el parámetro code"))
            val state = call.request.queryParameters["state"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Falta el parámetro state"))

            val token = googleOAuthService.handleCallback(code, state)
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("State inválido o expirado"))

            call.respond(HttpStatusCode.OK, AuthResponse(token = token))
        }
    }
}
