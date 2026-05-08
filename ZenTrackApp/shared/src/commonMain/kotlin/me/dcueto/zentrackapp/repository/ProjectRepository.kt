package me.dcueto.zentrackapp.repository

import me.dcueto.zentrackapp.model.Project

interface ProjectRepository {
    suspend fun findAllByWorkspace(workspaceId: Long, userId: Long): List<Project>
    suspend fun create(workspaceId: Long, projectKey: String, name: String, userId: Long): Project
}
