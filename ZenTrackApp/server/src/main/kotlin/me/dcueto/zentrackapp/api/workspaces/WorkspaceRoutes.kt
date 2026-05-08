package me.dcueto.zentrackapp.api.workspaces

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import me.dcueto.zentrackapp.core.WorkspaceService
import me.dcueto.zentrackapp.dto.CreateWorkspaceRequest
import me.dcueto.zentrackapp.dto.WorkspaceResponse

@Serializable
private data class ErrorResponse(val error: String)

fun Route.workspaceRoutes(workspaceService: WorkspaceService) {
    route("/api/workspaces") {
        get {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val workspaces = workspaceService.getWorkspacesForUser(userId)
            call.respond(HttpStatusCode.OK, workspaces.map { WorkspaceResponse(it.id, it.name, it.ownerId, it.createdAt) })
        }

        post {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val req = call.receive<CreateWorkspaceRequest>()
            if (req.name.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("name es requerido"))
                return@post
            }
            val workspace = workspaceService.createWorkspace(req.name, ownerId = userId)
            call.respond(HttpStatusCode.Created, WorkspaceResponse(workspace.id, workspace.name, workspace.ownerId, workspace.createdAt))
        }
    }
}
