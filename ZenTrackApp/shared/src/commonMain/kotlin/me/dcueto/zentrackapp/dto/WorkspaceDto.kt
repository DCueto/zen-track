package me.dcueto.zentrackapp.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateWorkspaceRequest(val orgId: Long, val name: String)

@Serializable
data class WorkspaceResponse(
    val id: Long,
    val orgId: Long,
    val name: String,
    val createdAt: String
)
