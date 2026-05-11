package me.dcueto.zentrackapp.cli

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import me.dcueto.zentrackapp.dto.AuthResponse
import me.dcueto.zentrackapp.dto.RefreshTokenRequest
import me.dcueto.zentrackapp.network.createHttpClient
import java.util.Base64

object TokenManager {
    private val client by lazy { createHttpClient() }

    fun isExpired(token: String): Boolean = try {
        val parts = token.split(".")
        if (parts.size < 2) return true
        val padding = "=".repeat((4 - parts[1].length % 4) % 4)
        val payload = Base64.getUrlDecoder().decode(parts[1] + padding)
        val json = Json.parseToJsonElement(String(payload)).jsonObject
        val exp = json["exp"]?.jsonPrimitive?.longOrNull ?: return true
        System.currentTimeMillis() / 1000 >= exp - 30  // 30s buffer antes de que expire
    } catch (e: Exception) {
        true
    }

    // Renueva el JWT si está expirado. Devuelve true si la sesión sigue activa.
    // Los comandos autenticados deben llamar esto antes de cada petición HTTP.
    fun refreshIfNeeded(session: ReplSession): Boolean {
        val token = session.token ?: return false
        if (!isExpired(token)) return true
        val refreshToken = session.refreshToken ?: return false
        return try {
            val response = runBlocking {
                client.post("${session.apiUrl}/api/auth/refresh") {
                    contentType(ContentType.Application.Json)
                    setBody(RefreshTokenRequest(refreshToken))
                }.body<AuthResponse>()
            }
            session.token = response.token
            val newRefreshToken = response.refreshToken ?: refreshToken
            session.refreshToken = newRefreshToken
            CredentialStore.save(PersistedCredentials(response.token, newRefreshToken, session.userEmail ?: ""))
            true
        } catch (e: Exception) {
            false
        }
    }
}
