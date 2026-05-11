package me.dcueto.zentrackapp.api.auth

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.dcueto.zentrackapp.core.GoogleOAuthService

fun Route.googleOAuthRoutes(googleOAuthService: GoogleOAuthService) {
    route("/api/auth") {
        get("/google", {
            tags("Auth")
            summary = "Iniciar OAuth con Google"
            description = "Genera un state CSRF, construye la URL de autorización de Google con scope openid email profile y redirige (302)"
            response {
                code(HttpStatusCode.Found) {
                    description = "Redirect al authorization endpoint de Google"
                }
            }
        }) {
            val url = googleOAuthService.buildAuthorizationUrl()
            call.respondRedirect(url, permanent = false)
        }
    }
}
