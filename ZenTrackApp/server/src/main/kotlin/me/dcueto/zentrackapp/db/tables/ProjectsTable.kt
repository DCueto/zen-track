package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object ProjectsTable : LongIdTable("projects") {
    val workspaceId  = reference("workspace_id", WorkspacesTable)
    val projectKey   = varchar("project_key", 20)
    val name         = varchar("name", 255)
    val description  = text("description").nullable()
    val createdAt    = timestamp("created_at")
    val createdBy    = reference("created_by", UsersTable).nullable()
    val updatedAt    = timestamp("updated_at")
    val updatedBy    = reference("updated_by", UsersTable).nullable()

    init {
        uniqueIndex(workspaceId, projectKey)
    }
}
