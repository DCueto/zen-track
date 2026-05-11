package me.dcueto.zentrackapp.model

import kotlinx.serialization.Serializable

@Serializable
data class Workspace(
    val id: Long,
    val orgId: Long,
    val name: String,
    val createdAt: String
)
