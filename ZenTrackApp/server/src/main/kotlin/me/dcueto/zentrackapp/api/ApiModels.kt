package me.dcueto.zentrackapp.api

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val error: String)
