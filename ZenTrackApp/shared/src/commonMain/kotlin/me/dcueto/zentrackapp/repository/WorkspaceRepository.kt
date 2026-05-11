package me.dcueto.zentrackapp.repository

import me.dcueto.zentrackapp.model.Workspace

interface WorkspaceRepository {
    suspend fun findAllByUser(userId: Long): List<Workspace>
    suspend fun create(orgId: Long, name: String, userId: Long): Workspace
}
