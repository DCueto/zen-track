package me.dcueto.zentrackapp.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateWorkspaceRequest(val name: String)

@Serializable
data class WorkspaceResponse(
    val id: String,
    val name: String,
    val ownerId: String,
    val createdAt: String
)
