package me.dcueto.zentrackapp.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import me.dcueto.zentrackapp.dto.CreateWorkspaceRequest
import me.dcueto.zentrackapp.dto.WorkspaceResponse
import me.dcueto.zentrackapp.model.Workspace
import me.dcueto.zentrackapp.repository.WorkspaceRepository

class WorkspaceApiRepository(private val client: HttpClient) : WorkspaceRepository {

    override suspend fun findAllByUser(userId: Long): List<Workspace> =
        client.get("$apiBaseUrl/api/workspaces") { withAuth() }
            .body<List<WorkspaceResponse>>()
            .map { it.toDomain() }

    override suspend fun create(orgId: Long, name: String, userId: Long): Workspace =
        client.post("$apiBaseUrl/api/workspaces") {
            withAuth()
            setBody(CreateWorkspaceRequest(orgId, name))
        }.body<WorkspaceResponse>().toDomain()

    private fun WorkspaceResponse.toDomain() =
        Workspace(id = id, orgId = orgId, name = name, createdAt = createdAt)
}
