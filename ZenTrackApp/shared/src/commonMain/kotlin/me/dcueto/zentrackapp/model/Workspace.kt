package me.dcueto.zentrackapp.model

import kotlinx.serialization.Serializable

@Serializable
data class Workspace(
    val id: String,
    val name: String,
    val ownerId: String,
    val createdAt: String
)
