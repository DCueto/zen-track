package me.dcueto.zentrackapp.core

import me.dcueto.zentrackapp.model.Workspace
import me.dcueto.zentrackapp.repository.WorkspaceRepository

class WorkspaceService(private val repository: WorkspaceRepository) {
    suspend fun getWorkspacesForUser(userId: Long): List<Workspace> =
        repository.findAllByUser(userId)

    suspend fun createWorkspace(orgId: Long, name: String, userId: Long): Workspace =
        repository.create(orgId, name, userId)
}
