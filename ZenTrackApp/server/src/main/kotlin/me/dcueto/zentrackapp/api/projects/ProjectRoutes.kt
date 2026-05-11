package me.dcueto.zentrackapp.api.projects

import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.dcueto.zentrackapp.api.ErrorResponse
import me.dcueto.zentrackapp.core.DuplicateProjectKeyException
import me.dcueto.zentrackapp.core.ProjectService
import me.dcueto.zentrackapp.dto.CreateProjectRequest
import me.dcueto.zentrackapp.dto.ProjectResponse

private val PROJECT_KEY_REGEX = Regex("^[A-Z][A-Z0-9]{0,9}$")

fun Route.projectRoutes(projectService: ProjectService) {
    route("/api/workspaces/{workspaceId}/projects") {
        get({
            tags("Projects")
            summary = "Listar proyectos"
            description = "Devuelve los proyectos del workspace al que pertenece el usuario autenticado"
            request {
                pathParameter<Long>("workspaceId") {
                    description = "ID del workspace"
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    body<List<ProjectResponse>>()
                }
                code(HttpStatusCode.BadRequest) {
                    description = "workspaceId no es un número válido"
                    body<ErrorResponse>()
                }
            }
        }) {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val workspaceId = call.parameters["workspaceId"]!!.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("workspaceId inválido"))
            val projects = projectService.getProjectsForWorkspace(workspaceId, userId)
            call.respond(HttpStatusCode.OK, projects.map {
                ProjectResponse(it.id, it.workspaceId, it.projectKey, it.name, it.description, it.createdAt)
            })
        }

        post({
            tags("Projects")
            summary = "Crear proyecto"
            description = "Crea un nuevo proyecto en el workspace especificado. La clave debe tener 1-10 caracteres alfanuméricos, empezando por letra"
            request {
                pathParameter<Long>("workspaceId") {
                    description = "ID del workspace"
                }
                body<CreateProjectRequest> {
                    description = "Clave y nombre del proyecto"
                }
            }
            response {
                code(HttpStatusCode.Created) {
                    body<ProjectResponse>()
                }
                code(HttpStatusCode.BadRequest) {
                    description = "Parámetros inválidos o formato de clave incorrecto"
                    body<ErrorResponse>()
                }
                code(HttpStatusCode.Conflict) {
                    description = "La clave del proyecto ya existe en el workspace"
                    body<ErrorResponse>()
                }
            }
        }) {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val workspaceId = call.parameters["workspaceId"]!!.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("workspaceId inválido"))
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
                    ProjectResponse(project.id, project.workspaceId, project.projectKey, project.name, project.description, project.createdAt))
            } catch (e: DuplicateProjectKeyException) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse(e.message!!))
            }
        }
    }
}
