package me.dcueto.zentrackapp.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import me.dcueto.zentrackapp.dto.CreateWorkspaceRequest
import me.dcueto.zentrackapp.dto.WorkspaceResponse
import me.dcueto.zentrackapp.model.Workspace
import me.dcueto.zentrackapp.repository.WorkspaceRepository

class WorkspaceApiRepository(private val client: HttpClient) : WorkspaceRepository {

    override suspend fun findAllByUser(userId: String): List<Workspace> =
        client.get("$apiBaseUrl/api/workspaces") { withAuth() }
            .body<List<WorkspaceResponse>>()
            .map { it.toDomain() }

    override suspend fun create(name: String, ownerId: String): Workspace =
        client.post("$apiBaseUrl/api/workspaces") {
            withAuth()
            setBody(CreateWorkspaceRequest(name))
        }.body<WorkspaceResponse>().toDomain()

    private fun WorkspaceResponse.toDomain() =
        Workspace(id = id, name = name, ownerId = ownerId, createdAt = createdAt)
}
