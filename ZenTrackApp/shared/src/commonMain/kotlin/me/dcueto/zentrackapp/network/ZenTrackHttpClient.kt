package me.dcueto.zentrackapp.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun createHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = false
        })
    }
    HttpResponseValidator {
        validateResponse { response ->
            when (response.status.value) {
                401 -> throw ApiException.Unauthorized()
                404 -> throw ApiException.NotFound(response.request.url.encodedPath)
                in 400..499 -> throw ApiException.ServerError(response.status.value, response.bodyAsText())
                in 500..599 -> throw ApiException.ServerError(response.status.value, response.bodyAsText())
            }
        }
        handleResponseExceptionWithRequest { cause, _ ->
            if (cause !is ApiException) throw ApiException.NetworkError(cause)
        }
    }
}

// Añade el Bearer token de TokenStore al request si hay sesión activa
fun HttpRequestBuilder.withAuth() {
    TokenStore.token?.let { bearerAuth(it) }
}
