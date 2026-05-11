package me.dcueto.zentrackapp.integrations.google

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GoogleTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String
)

@Serializable
data class GoogleUserInfo(
    val sub: String,
    val email: String,
    val name: String,
    val picture: String? = null
)

class GoogleApiClient(
    private val clientId: String,
    private val clientSecret: String,
    private val redirectUri: String
) {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun exchangeCodeForTokens(code: String): GoogleTokenResponse =
        httpClient.submitForm(
            url = "https://oauth2.googleapis.com/token",
            formParameters = parameters {
                append("code", code)
                append("client_id", clientId)
                append("client_secret", clientSecret)
                append("redirect_uri", redirectUri)
                append("grant_type", "authorization_code")
            }
        ).body()

    suspend fun getUserInfo(accessToken: String): GoogleUserInfo =
        httpClient.get("https://www.googleapis.com/oauth2/v3/userinfo") {
            bearerAuth(accessToken)
        }.body()
}
