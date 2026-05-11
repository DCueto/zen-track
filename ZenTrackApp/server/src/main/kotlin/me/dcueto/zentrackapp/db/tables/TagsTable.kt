package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object TagsTable : LongIdTable("tags") {
    val workspaceId = reference("workspace_id", WorkspacesTable)
    val name        = varchar("name", 100)
    val colorHex    = varchar("color_hex", 7).nullable()
    val createdAt   = timestamp("created_at")
    val createdBy   = reference("created_by", UsersTable).nullable()
    val updatedAt   = timestamp("updated_at")
    val updatedBy   = reference("updated_by", UsersTable).nullable()
}
