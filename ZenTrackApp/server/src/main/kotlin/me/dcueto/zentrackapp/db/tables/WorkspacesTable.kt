package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object WorkspacesTable : LongIdTable("workspaces") {
    val name      = text("name")
    val ownerId   = reference("owner_id", UsersTable)
    val createdAt = timestamp("created_at")
}
