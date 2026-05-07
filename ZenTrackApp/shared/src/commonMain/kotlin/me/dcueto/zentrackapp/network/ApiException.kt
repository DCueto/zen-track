package me.dcueto.zentrackapp.network

sealed class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Unauthorized : ApiException("Token inválido o expirado")
    class NotFound(resource: String) : ApiException("$resource not found")
    class ServerError(val code: Int, message: String) : ApiException("Server error $code: $message")
    class NetworkError(cause: Throwable) : ApiException(cause.message ?: "Network error", cause)
}
