package me.dcueto.zentrackapp.api.users

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.dcueto.zentrackapp.core.UserService
import me.dcueto.zentrackapp.dto.OAuthAccountResponse

fun Route.userRoutes(userService: UserService) {
    route("/api/users/me") {
        get("/oauth", {
            tags("Users")
            summary = "Listar cuentas OAuth vinculadas"
            description = "Devuelve las cuentas OAuth (e.g. Google) vinculadas al usuario autenticado"
            response {
                code(HttpStatusCode.OK) {
                    body<List<OAuthAccountResponse>>()
                }
            }
        }) {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val accounts = userService.getOAuthAccounts(userId)
            call.respond(
                HttpStatusCode.OK,
                accounts.map { OAuthAccountResponse(it.id, it.provider, it.email, it.createdAt.toString()) }
            )
        }
    }
}
