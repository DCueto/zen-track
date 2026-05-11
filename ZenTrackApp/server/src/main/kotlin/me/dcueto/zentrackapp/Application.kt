package me.dcueto.zentrackapp

import io.ktor.server.application.*
import io.ktor.server.netty.*
import me.dcueto.zentrackapp.api.configureAuthentication
import me.dcueto.zentrackapp.api.configureCallLogging
import me.dcueto.zentrackapp.api.configureCors
import me.dcueto.zentrackapp.api.configureOpenApi
import me.dcueto.zentrackapp.api.configureRouting
import me.dcueto.zentrackapp.api.configureSerialization
import me.dcueto.zentrackapp.api.configureStatusPages
import me.dcueto.zentrackapp.core.AuthService
import me.dcueto.zentrackapp.core.GoogleOAuthService
import me.dcueto.zentrackapp.core.JwtService
import me.dcueto.zentrackapp.core.ProjectService
import me.dcueto.zentrackapp.core.TokenEncryptionService
import me.dcueto.zentrackapp.core.WorkspaceService
import me.dcueto.zentrackapp.db.DatabaseFactory
import me.dcueto.zentrackapp.db.repositories.OAuthAccountRepositoryImpl
import me.dcueto.zentrackapp.db.repositories.ProjectRepositoryImpl
import me.dcueto.zentrackapp.db.repositories.UserRepositoryImpl
import me.dcueto.zentrackapp.db.repositories.WorkspaceRepositoryImpl
import me.dcueto.zentrackapp.integrations.google.GoogleApiClient

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    // Serialization primero: StatusPages usa call.respond(ErrorResponse(...)),
    // que necesita ContentNegotiation ya instalado para serializar a JSON.
    configureSerialization()
    configureOpenApi()
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

    val userRepository = UserRepositoryImpl()
    val authService = AuthService(userRepository, jwtService)
    val workspaceService = WorkspaceService(WorkspaceRepositoryImpl())
    val projectService = ProjectService(ProjectRepositoryImpl())

    val googleApiClient = GoogleApiClient(
        clientId = cfg.property("google.clientId").getString(),
        clientSecret = cfg.property("google.clientSecret").getString(),
        redirectUri = cfg.property("google.redirectUri").getString()
    )
    val tokenEncryptionService = TokenEncryptionService(
        keyBase64 = cfg.property("google.encryptionKey").getString()
    )
    val googleOAuthService = GoogleOAuthService(
        clientId = cfg.property("google.clientId").getString(),
        redirectUri = cfg.property("google.redirectUri").getString(),
        googleApiClient = googleApiClient,
        userRepository = userRepository,
        oAuthAccountRepository = OAuthAccountRepositoryImpl(),
        jwtService = jwtService,
        tokenEncryptionService = tokenEncryptionService
    )

    configureRouting(authService, workspaceService, projectService, googleOAuthService)
}
