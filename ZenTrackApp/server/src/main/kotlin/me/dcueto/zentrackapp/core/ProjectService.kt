package me.dcueto.zentrackapp.core

import me.dcueto.zentrackapp.model.Project
import me.dcueto.zentrackapp.repository.ProjectRepository

class DuplicateProjectKeyException(key: String) : Exception("project_key '$key' ya existe en este workspace")

class ProjectService(private val repository: ProjectRepository) {
    suspend fun getProjectsForWorkspace(workspaceId: Long, userId: Long): List<Project> =
        repository.findAllByWorkspace(workspaceId, userId)

    suspend fun createProject(workspaceId: Long, projectKey: String, name: String, userId: Long): Project =
        repository.create(workspaceId, projectKey, name, userId)
}
