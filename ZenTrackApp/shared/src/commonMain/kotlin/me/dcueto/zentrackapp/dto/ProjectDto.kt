package me.dcueto.zentrackapp.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateProjectRequest(val projectKey: String, val name: String)

@Serializable
data class ProjectResponse(
    val id: String,
    val workspaceId: String,
    val projectKey: String,
    val name: String,
    val taskCounter: Int,
    val createdAt: String
)
