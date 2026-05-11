package me.dcueto.zentrackapp.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import me.dcueto.zentrackapp.cli.CredentialStore
import me.dcueto.zentrackapp.cli.PersistedCredentials
import me.dcueto.zentrackapp.cli.ReplSession
import me.dcueto.zentrackapp.dto.AuthResponse
import me.dcueto.zentrackapp.dto.LoginRequest
import me.dcueto.zentrackapp.network.ApiException
import me.dcueto.zentrackapp.network.createHttpClient

class AuthCommands : CliktCommand(name = "auth") {
    override fun help(context: Context) = "Gestiona la autenticación"
    override fun run() = Unit
}

class AuthLoginCommand(private val session: ReplSession) : CliktCommand(name = "login") {
    override fun help(context: Context) = "Iniciar sesión con email y contraseña"

    private val emailOpt by option("--email", "-e", help = "Email")
    private val passwordOpt by option("--password", "-p", help = "Contraseña")

    private val client by lazy { createHttpClient() }

    override fun run() {
        val email = emailOpt ?: promptEmail()
        val password = passwordOpt ?: promptPassword()

        val response = try {
            runBlocking {
                client.post("${session.apiUrl}/api/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(email, password))
                }.body<AuthResponse>()
            }
        } catch (e: ApiException.Unauthorized) {
            echo("Credenciales inválidas", err = true)
            return
        } catch (e: ApiException) {
            echo("Error: ${e.message}", err = true)
            return
        } catch (e: Exception) {
            echo("No se pudo conectar con el servidor: ${e.message}", err = true)
            return
        }

        session.token = response.token
        session.refreshToken = response.refreshToken ?: ""
        session.userEmail = email
        CredentialStore.save(
            PersistedCredentials(
                token = response.token,
                refreshToken = response.refreshToken ?: "",
                email = email
            )
        )
        echo("Sesión iniciada como $email")
    }

    private fun promptEmail(): String {
        echo("Email: ", trailingNewline = false)
        return readLine()?.trim() ?: ""
    }

    private fun promptPassword(): String =
        System.console()?.let { console ->
            String(console.readPassword("Contraseña: "))
        } ?: run {
            echo("Contraseña: ", trailingNewline = false)
            readLine() ?: ""
        }
}
