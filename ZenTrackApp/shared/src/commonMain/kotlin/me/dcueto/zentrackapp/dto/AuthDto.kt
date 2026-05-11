package me.dcueto.zentrackapp.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val email: String, val password: String, val name: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val refreshToken: String? = null)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
data class UserResponse(val id: String, val email: String, val name: String)
