package me.dcueto.zentrackapp

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import me.dcueto.zentrackapp.api.configureAuthentication
import me.dcueto.zentrackapp.api.configureCallLogging
import me.dcueto.zentrackapp.api.configureCors
import me.dcueto.zentrackapp.api.configureRouting
import me.dcueto.zentrackapp.api.configureSerialization
import me.dcueto.zentrackapp.api.configureStatusPages
import me.dcueto.zentrackapp.core.JwtService
import me.dcueto.zentrackapp.db.DatabaseFactory

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Serialization primero: StatusPages usa call.respond(ErrorResponse(...)),
    // que necesita ContentNegotiation ya instalado para serializar a JSON.
    configureSerialization()
    configureStatusPages()
    configureCors()
    configureCallLogging()
    configureAuthentication()
    DatabaseFactory.init(this)

    val cfg = environment.config
    val jwtService = JwtService(
        secret = cfg.property("jwt.secret").getString(),
        issuer = cfg.property("jwt.issuer").getString(),
        audience = cfg.property("jwt.audience").getString(),
        expirationMs = cfg.propertyOrNull("jwt.expirationMs")?.getString()?.toLong() ?: 86_400_000L
    )
    configureRouting(jwtService)
}
