package me.dcueto.zentrackapp.api

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import me.dcueto.zentrackapp.api.auth.authRoutes
import me.dcueto.zentrackapp.core.JwtService

fun Application.configureRouting(jwtService: JwtService) {
    routing {
        // Rutas públicas: no requieren JWT
        authRoutes(jwtService)

        // Rutas protegidas: requieren JWT válido en Authorization: Bearer <token>
        authenticate("jwt") {
            // Fase 2+: workspaceRoutes(), projectRoutes(), taskRoutes(), etc.
        }
    }
}
