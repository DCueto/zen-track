package me.dcueto.zentrackapp.repository

import me.dcueto.zentrackapp.model.Project

interface ProjectRepository {
    suspend fun findAllByWorkspace(workspaceId: String, userId: String): List<Project>
    suspend fun create(workspaceId: String, projectKey: String, name: String, userId: String): Project
}
