package me.dcueto.zentrackapp.api.auth

import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.dcueto.zentrackapp.api.ErrorResponse
import me.dcueto.zentrackapp.core.AuthService
import me.dcueto.zentrackapp.dto.AuthResponse
import me.dcueto.zentrackapp.dto.LoginRequest
import me.dcueto.zentrackapp.dto.RegisterRequest

fun Route.authRoutes(authService: AuthService) {
    route("/api/auth") {
        post("/register", {
            tags("Auth")
            summary = "Registrar usuario"
            description = "Crea un nuevo usuario y devuelve un JWT válido"
            request {
                body<RegisterRequest> {
                    description = "Credenciales del nuevo usuario"
                }
            }
            response {
                code(HttpStatusCode.Created) {
                    description = "Usuario creado"
                    body<AuthResponse>()
                }
                code(HttpStatusCode.BadRequest) {
                    description = "Campos requeridos ausentes"
                    body<ErrorResponse>()
                }
                code(HttpStatusCode.Conflict) {
                    description = "El email ya está registrado"
                    body<ErrorResponse>()
                }
            }
        }) {
            val req = call.receive<RegisterRequest>()
            if (req.email.isBlank() || req.password.isBlank() || req.name.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("email, password y name son requeridos"))
                return@post
            }
            val token = authService.register(req.email, req.password, req.name)
            call.respond(HttpStatusCode.Created, AuthResponse(token = token))
        }

        post("/login", {
            tags("Auth")
            summary = "Autenticar usuario"
            description = "Verifica las credenciales y devuelve un JWT"
            request {
                body<LoginRequest> {
                    description = "Email y contraseña"
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Autenticación exitosa"
                    body<AuthResponse>()
                }
                code(HttpStatusCode.Unauthorized) {
                    description = "Credenciales incorrectas"
                    body<ErrorResponse>()
                }
            }
        }) {
            val req = call.receive<LoginRequest>()
            val token = authService.login(req.email, req.password)
                ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Credenciales incorrectas"))
            call.respond(HttpStatusCode.OK, AuthResponse(token = token))
        }
    }
}
