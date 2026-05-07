package me.dcueto.zentrackapp.core

import me.dcueto.zentrackapp.model.Workspace
import me.dcueto.zentrackapp.repository.WorkspaceRepository

class WorkspaceService(private val repository: WorkspaceRepository) {
    suspend fun getWorkspacesForUser(userId: String): List<Workspace> =
        repository.findAllByUser(userId)

    suspend fun createWorkspace(name: String, ownerId: String): Workspace =
        repository.create(name, ownerId)
}
