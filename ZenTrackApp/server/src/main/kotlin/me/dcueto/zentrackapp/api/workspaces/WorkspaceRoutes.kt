package me.dcueto.zentrackapp.api.workspaces

import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.dcueto.zentrackapp.api.ErrorResponse
import me.dcueto.zentrackapp.core.WorkspaceService
import me.dcueto.zentrackapp.dto.CreateWorkspaceRequest
import me.dcueto.zentrackapp.dto.WorkspaceResponse

fun Route.workspaceRoutes(workspaceService: WorkspaceService) {
    route("/api/workspaces") {
        get({
            tags("Workspaces")
            summary = "Listar workspaces"
            description = "Devuelve los workspaces a los que pertenece el usuario autenticado"
            response {
                code(HttpStatusCode.OK) {
                    body<List<WorkspaceResponse>>()
                }
            }
        }) {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val workspaces = workspaceService.getWorkspacesForUser(userId)
            call.respond(HttpStatusCode.OK, workspaces.map { WorkspaceResponse(it.id, it.orgId, it.name, it.createdAt) })
        }

        post({
            tags("Workspaces")
            summary = "Crear workspace"
            description = "Crea un nuevo workspace dentro de la organización especificada"
            request {
                body<CreateWorkspaceRequest> {
                    description = "ID de la organización y nombre del nuevo workspace"
                }
            }
            response {
                code(HttpStatusCode.Created) {
                    body<WorkspaceResponse>()
                }
                code(HttpStatusCode.BadRequest) {
                    description = "El nombre del workspace es requerido"
                    body<ErrorResponse>()
                }
            }
        }) {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val req = call.receive<CreateWorkspaceRequest>()
            if (req.name.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("name es requerido"))
                return@post
            }
            val workspace = workspaceService.createWorkspace(req.orgId, req.name, userId)
            call.respond(HttpStatusCode.Created, WorkspaceResponse(workspace.id, workspace.orgId, workspace.name, workspace.createdAt))
        }
    }
}
