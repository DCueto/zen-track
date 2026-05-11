package me.dcueto.zentrackapp.dto

import kotlinx.serialization.Serializable

@Serializable
data class OAuthAccountResponse(
    val id: Long,
    val provider: String,
    val email: String,
    val createdAt: String
)
