package me.dcueto.zentrackapp.api.users

import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.dcueto.zentrackapp.api.ErrorResponse
import me.dcueto.zentrackapp.core.GoogleOAuthService
import me.dcueto.zentrackapp.core.LinkResult
import me.dcueto.zentrackapp.core.UnlinkResult
import me.dcueto.zentrackapp.core.UserService
import me.dcueto.zentrackapp.dto.LinkGoogleAccountRequest
import me.dcueto.zentrackapp.dto.OAuthAccountResponse

fun Route.userRoutes(userService: UserService, googleOAuthService: GoogleOAuthService) {
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

        post("/oauth/google", {
            tags("Users")
            summary = "Vincular cuenta Google"
            description = "Vincula una cuenta Google al usuario autenticado usando un authorization code obtenido en el flujo OAuth"
            request {
                body<LinkGoogleAccountRequest> {
                    description = "Authorization code devuelto por Google al completar el flujo OAuth"
                }
            }
            response {
                code(HttpStatusCode.NoContent) {
                    description = "Cuenta Google vinculada correctamente"
                }
                code(HttpStatusCode.Conflict) {
                    description = "La cuenta Google ya está vinculada a otro usuario de ZenTrack"
                    body<ErrorResponse>()
                }
                code(HttpStatusCode.BadRequest) {
                    description = "Código inválido o error al comunicarse con Google"
                    body<ErrorResponse>()
                }
            }
        }) {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val req = call.receive<LinkGoogleAccountRequest>()
            when (googleOAuthService.linkAccount(userId, req.code)) {
                is LinkResult.Success ->
                    call.respond(HttpStatusCode.NoContent)
                is LinkResult.AlreadyLinkedToOtherUser ->
                    call.respond(HttpStatusCode.Conflict, ErrorResponse("La cuenta Google ya está vinculada a otro usuario de ZenTrack"))
                is LinkResult.OAuthError ->
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("No se pudo verificar la cuenta Google. El código puede haber expirado"))
            }
        }

        delete("/oauth/{id}", {
            tags("Users")
            summary = "Desvincular cuenta OAuth"
            description = "Elimina el vínculo con la cuenta OAuth indicada. Rechaza la operación si el usuario no tiene contraseña configurada (evita dejar la cuenta sin método de acceso)"
            request {
                pathParameter<Long>("id") {
                    description = "ID de la cuenta OAuth a desvincular"
                }
            }
            response {
                code(HttpStatusCode.NoContent) {
                    description = "Cuenta OAuth desvinculada"
                }
                code(HttpStatusCode.NotFound) {
                    description = "Cuenta OAuth no encontrada o no pertenece al usuario"
                    body<ErrorResponse>()
                }
                code(HttpStatusCode.Conflict) {
                    description = "No se puede desvincular: la cuenta OAuth es el único método de acceso (sin contraseña)"
                    body<ErrorResponse>()
                }
            }
        }) {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val oAuthAccountId = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("ID inválido"))
            when (userService.unlinkOAuthAccount(userId, oAuthAccountId)) {
                is UnlinkResult.Success ->
                    call.respond(HttpStatusCode.NoContent)
                is UnlinkResult.NotFound ->
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Cuenta OAuth no encontrada"))
                is UnlinkResult.LastLoginMethod ->
                    call.respond(HttpStatusCode.Conflict, ErrorResponse("No puedes desvincular esta cuenta: no tienes contraseña configurada. Añade una contraseña antes de desvincular"))
            }
        }
    }
}
