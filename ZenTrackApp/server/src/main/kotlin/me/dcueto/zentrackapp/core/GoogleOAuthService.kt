package me.dcueto.zentrackapp.core

import java.net.URLEncoder
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
private const val STATE_TTL_SECONDS = 600L

class GoogleOAuthService(
    private val clientId: String,
    private val redirectUri: String
) {
    private val pendingStates = ConcurrentHashMap<String, Instant>()

    fun buildAuthorizationUrl(): String {
        val state = UUID.randomUUID().toString()
        pendingStates[state] = Instant.now().plusSeconds(STATE_TTL_SECONDS)
        return buildString {
            append(GOOGLE_AUTH_URL)
            append("?client_id=").append(enc(clientId))
            append("&redirect_uri=").append(enc(redirectUri))
            append("&response_type=code")
            append("&scope=").append(enc("openid email profile"))
            append("&state=").append(state)
            append("&access_type=offline")
            append("&prompt=consent")
        }
    }

    fun validateAndConsumeState(state: String): Boolean {
        val expiry = pendingStates.remove(state) ?: return false
        return Instant.now().isBefore(expiry)
    }

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
}
