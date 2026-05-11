package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object OAuthAccountsTable : LongIdTable("oauth_accounts") {
    val userId          = reference("user_id", UsersTable)
    val provider        = varchar("provider", 50)
    val providerUserId  = varchar("provider_user_id", 255)
    val email           = varchar("email", 255)
    val accessToken     = text("access_token").nullable()
    val refreshToken    = text("refresh_token").nullable()
    val tokenExpiresAt  = timestamp("token_expires_at").nullable()
    val createdAt       = timestamp("created_at")
    val createdBy       = reference("created_by", UsersTable).nullable()
    val updatedAt       = timestamp("updated_at")
    val updatedBy       = reference("updated_by", UsersTable).nullable()

    init {
        uniqueIndex(provider, providerUserId)
    }
}
