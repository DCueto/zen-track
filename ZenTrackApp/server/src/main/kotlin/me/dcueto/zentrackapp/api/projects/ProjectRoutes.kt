package me.dcueto.zentrackapp.api.projects

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import me.dcueto.zentrackapp.core.DuplicateProjectKeyException
import me.dcueto.zentrackapp.core.ProjectService
import me.dcueto.zentrackapp.dto.CreateProjectRequest
import me.dcueto.zentrackapp.dto.ProjectResponse

private val PROJECT_KEY_REGEX = Regex("^[A-Z][A-Z0-9]{0,9}$")

@Serializable
private data class ErrorResponse(val error: String)

fun Route.projectRoutes(projectService: ProjectService) {
    route("/api/workspaces/{workspaceId}/projects") {
        get {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
            val workspaceId = call.parameters["workspaceId"]!!
            val projects = projectService.getProjectsForWorkspace(workspaceId, userId)
            call.respond(HttpStatusCode.OK, projects.map {
                ProjectResponse(it.id, it.workspaceId, it.projectKey, it.name, it.taskCounter, it.createdAt)
            })
        }

        post {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
            val workspaceId = call.parameters["workspaceId"]!!
            val req = call.receive<CreateProjectRequest>()

            val key = req.projectKey.trim().uppercase()
            if (key.isBlank() || req.name.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("projectKey y name son requeridos"))
                return@post
            }
            if (!PROJECT_KEY_REGEX.matches(key)) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("projectKey debe tener 1-10 caracteres, empezar por letra y contener solo letras y números (ej: ZTK, MYPROJ)"))
                return@post
            }

            try {
                val project = projectService.createProject(workspaceId, key, req.name, userId)
                call.respond(HttpStatusCode.Created,
                    ProjectResponse(project.id, project.workspaceId, project.projectKey, project.name, project.taskCounter, project.createdAt))
            } catch (e: DuplicateProjectKeyException) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse(e.message!!))
            }
        }
    }
}
