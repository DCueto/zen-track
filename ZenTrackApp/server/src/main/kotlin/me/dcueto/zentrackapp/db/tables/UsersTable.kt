package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

object UsersTable : UUIDTable("users") {
    val email        = text("email")
    val passwordHash = text("password_hash")
    val name         = text("name")
    val createdAt    = timestamp("created_at")
}
