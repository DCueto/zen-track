package me.dcueto.zentrackapp.api

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.config.AuthScheme
import io.github.smiley4.ktoropenapi.config.AuthType
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = false
        })
    }
}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
        }
    }
}

fun Application.configureCors() {
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
    }
}

fun Application.configureCallLogging() {
    install(CallLogging) {
        level = Level.INFO
    }
}

fun Application.configureOpenApi() {
    install(OpenApi) {
        info {
            title = "ZenTrack API"
            version = "1.0.0"
            description = "API multi-tenant de ZenTrack — gestión ágil de proyectos y tareas"
        }
        server {
            url = "http://localhost:8080"
            description = "Servidor de desarrollo"
        }
        security {
            securityScheme("bearerAuth") {
                type = AuthType.HTTP
                scheme = AuthScheme.BEARER
                bearerFormat = "JWT"
                description = "JWT generado en POST /api/auth/login o /api/auth/register"
            }
            defaultSecuritySchemeNames("bearerAuth")
            defaultUnauthorizedResponse {
                description = "Token JWT ausente o inválido"
                body<ErrorResponse>()
            }
        }
    }
}

fun Application.configureAuthentication() {
    val cfg = environment.config
    val secret = cfg.property("jwt.secret").getString()
    val issuer = cfg.property("jwt.issuer").getString()
    val audience = cfg.property("jwt.audience").getString()
    val realm = cfg.property("jwt.realm").getString()

    install(Authentication) {
        jwt("jwt") {
            this.realm = realm
            verifier(
                JWT.require(Algorithm.HMAC256(secret))
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asLong() != null) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Token inválido o expirado"))
            }
        }
    }
}
