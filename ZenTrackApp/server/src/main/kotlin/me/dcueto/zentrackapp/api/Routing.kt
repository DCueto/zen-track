package me.dcueto.zentrackapp.api

import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import me.dcueto.zentrackapp.api.auth.authRoutes
import me.dcueto.zentrackapp.api.auth.googleOAuthRoutes
import me.dcueto.zentrackapp.api.projects.projectRoutes
import me.dcueto.zentrackapp.api.users.userRoutes
import me.dcueto.zentrackapp.api.workspaces.workspaceRoutes
import me.dcueto.zentrackapp.core.AuthService
import me.dcueto.zentrackapp.core.GoogleOAuthService
import me.dcueto.zentrackapp.core.ProjectService
import me.dcueto.zentrackapp.core.UserService
import me.dcueto.zentrackapp.core.WorkspaceService

fun Application.configureRouting(authService: AuthService, workspaceService: WorkspaceService, projectService: ProjectService, googleOAuthService: GoogleOAuthService, userService: UserService) {
    routing {
        route("api.json") { openApi() }
        route("swagger") { swaggerUI("/api.json") }

        authRoutes(authService)
        googleOAuthRoutes(googleOAuthService)

        authenticate("jwt") {
            workspaceRoutes(workspaceService)
            projectRoutes(projectService)
            userRoutes(userService)
        }
    }
}
