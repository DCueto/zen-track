package me.dcueto.zentrackapp.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateProjectRequest(val projectKey: String, val name: String, val description: String? = null)

@Serializable
data class ProjectResponse(
    val id: Long,
    val workspaceId: Long,
    val projectKey: String,
    val name: String,
    val description: String?,
    val createdAt: String
)
