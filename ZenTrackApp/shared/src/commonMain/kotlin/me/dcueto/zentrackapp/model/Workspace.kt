package me.dcueto.zentrackapp.model

import kotlinx.serialization.Serializable

@Serializable
data class Workspace(
    val id: Long,
    val name: String,
    val ownerId: Long,
    val createdAt: String
)
