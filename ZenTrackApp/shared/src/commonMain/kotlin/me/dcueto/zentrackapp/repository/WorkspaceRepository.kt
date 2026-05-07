package me.dcueto.zentrackapp.repository

import me.dcueto.zentrackapp.model.Workspace

interface WorkspaceRepository {
    suspend fun findAllByUser(userId: String): List<Workspace>
    suspend fun create(name: String, ownerId: String): Workspace
}
