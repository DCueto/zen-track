package me.dcueto.zentrackapp.api.auth

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import me.dcueto.zentrackapp.core.AuthService
import me.dcueto.zentrackapp.dto.AuthResponse
import me.dcueto.zentrackapp.dto.LoginRequest
import me.dcueto.zentrackapp.dto.RegisterRequest

@Serializable
private data class ErrorResponse(val error: String)

fun Route.authRoutes(authService: AuthService) {
    route("/api/auth") {
        post("/register") {
            val req = call.receive<RegisterRequest>()
            if (req.email.isBlank() || req.password.isBlank() || req.name.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("email, password y name son requeridos"))
                return@post
            }
            val token = authService.register(req.email, req.password, req.name)
            call.respond(HttpStatusCode.Created, AuthResponse(token = token))
        }

        post("/login") {
            val req = call.receive<LoginRequest>()
            val token = authService.login(req.email, req.password)
                ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Credenciales incorrectas"))
            call.respond(HttpStatusCode.OK, AuthResponse(token = token))
        }
    }
}
