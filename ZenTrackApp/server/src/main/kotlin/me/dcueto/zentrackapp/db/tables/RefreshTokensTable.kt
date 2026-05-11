package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object RefreshTokensTable : LongIdTable("refresh_tokens") {
    val userId     = reference("user_id", UsersTable)
    val tokenHash  = varchar("token_hash", 64)
    val expiresAt  = timestamp("expires_at")
    val revokedAt  = timestamp("revoked_at").nullable()
    val createdAt  = timestamp("created_at")
    val createdBy  = reference("created_by", UsersTable).nullable()
    val updatedAt  = timestamp("updated_at")
    val updatedBy  = reference("updated_by", UsersTable).nullable()
}
