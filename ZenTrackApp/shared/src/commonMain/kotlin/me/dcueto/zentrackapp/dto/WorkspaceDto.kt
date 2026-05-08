package me.dcueto.zentrackapp.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateWorkspaceRequest(val name: String)

@Serializable
data class WorkspaceResponse(
    val id: Long,
    val name: String,
    val ownerId: Long,
    val createdAt: String
)
