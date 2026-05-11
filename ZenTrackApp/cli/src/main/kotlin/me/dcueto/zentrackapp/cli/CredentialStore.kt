package me.dcueto.zentrackapp.cli

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class PersistedCredentials(
    val token: String,
    val refreshToken: String,
    val email: String
)

private val credentialsJson = Json { ignoreUnknownKeys = true }

object CredentialStore {
    private val file = File(System.getProperty("user.home"), ".zentrack/credentials.json")

    fun save(credentials: PersistedCredentials) {
        file.parentFile.mkdirs()
        file.writeText(credentialsJson.encodeToString(credentials))
    }

    fun load(): PersistedCredentials? = runCatching {
        credentialsJson.decodeFromString<PersistedCredentials>(file.readText())
    }.getOrNull()

    fun clear() {
        file.delete()
    }
}
