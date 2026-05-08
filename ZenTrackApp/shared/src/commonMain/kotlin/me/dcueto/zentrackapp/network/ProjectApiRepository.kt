package me.dcueto.zentrackapp.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import me.dcueto.zentrackapp.dto.CreateProjectRequest
import me.dcueto.zentrackapp.dto.ProjectResponse
import me.dcueto.zentrackapp.model.Project
import me.dcueto.zentrackapp.repository.ProjectRepository

class ProjectApiRepository(private val client: HttpClient) : ProjectRepository {

    override suspend fun findAllByWorkspace(workspaceId: Long, userId: Long): List<Project> =
        client.get("$apiBaseUrl/api/workspaces/$workspaceId/projects") { withAuth() }
            .body<List<ProjectResponse>>()
            .map { it.toDomain() }

    override suspend fun create(workspaceId: Long, projectKey: String, name: String, userId: Long): Project =
        client.post("$apiBaseUrl/api/workspaces/$workspaceId/projects") {
            withAuth()
            setBody(CreateProjectRequest(projectKey, name))
        }.body<ProjectResponse>().toDomain()

    private fun ProjectResponse.toDomain() =
        Project(id = id, workspaceId = workspaceId, projectKey = projectKey, name = name, taskCounter = taskCounter, createdAt = createdAt)
}
