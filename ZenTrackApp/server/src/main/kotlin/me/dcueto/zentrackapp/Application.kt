package me.dcueto.zentrackapp

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import me.dcueto.zentrackapp.api.configureCallLogging
import me.dcueto.zentrackapp.api.configureCors
import me.dcueto.zentrackapp.api.configureRouting
import me.dcueto.zentrackapp.api.configureSerialization
import me.dcueto.zentrackapp.api.configureStatusPages

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureStatusPages()
    configureCors()
    configureCallLogging()
    configureRouting()
}
