package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object UsersTable : LongIdTable("users") {
    val email        = varchar("email", 255)
    val passwordHash = varchar("password_hash", 255).nullable()
    val name         = varchar("name", 255)
    val avatarUrl    = text("avatar_url").nullable()
    val userType     = varchar("user_type", 50)
    val createdAt    = timestamp("created_at")
    val createdBy    = reference("created_by", UsersTable).nullable()
    val updatedAt    = timestamp("updated_at")
    val updatedBy    = reference("updated_by", UsersTable).nullable()
}
