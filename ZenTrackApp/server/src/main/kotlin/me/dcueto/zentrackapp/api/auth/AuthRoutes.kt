package me.dcueto.zentrackapp.api.auth

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import me.dcueto.zentrackapp.core.JwtService

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String)

@Serializable
private data class ErrorResponse(val error: String)

fun Route.authRoutes(jwtService: JwtService) {
    route("/api/auth") {
        // POST /api/auth/login — pública, sin JWT
        post("/login") {
            val request = call.receive<LoginRequest>()
            // Fase 2: validar credenciales contra la tabla Users
            val token = jwtService.generateToken(userId = request.email)
            call.respond(HttpStatusCode.OK, AuthResponse(token = token))
        }

        // POST /api/auth/register — pública, sin JWT
        post("/register") {
            // Fase 2: crear usuario en la tabla Users
            call.respond(HttpStatusCode.NotImplemented, ErrorResponse("Pendiente implementación en Fase 2"))
        }
    }
}
