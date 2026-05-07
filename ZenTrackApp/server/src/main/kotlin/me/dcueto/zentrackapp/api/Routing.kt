package me.dcueto.zentrackapp.api

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import me.dcueto.zentrackapp.api.auth.authRoutes
import me.dcueto.zentrackapp.api.workspaces.workspaceRoutes
import me.dcueto.zentrackapp.core.AuthService
import me.dcueto.zentrackapp.core.WorkspaceService

fun Application.configureRouting(authService: AuthService, workspaceService: WorkspaceService) {
    routing {
        // Rutas públicas: no requieren JWT
        authRoutes(authService)

        // Rutas protegidas: requieren JWT válido en Authorization: Bearer <token>
        authenticate("jwt") {
            workspaceRoutes(workspaceService)
        }
    }
}
