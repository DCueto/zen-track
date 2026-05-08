package me.dcueto.zentrackapp.model

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: Long,
    val workspaceId: Long,
    val projectKey: String,
    val name: String,
    val taskCounter: Int,
    val createdAt: String
)
