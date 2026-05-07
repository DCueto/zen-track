package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

object ProjectsTable : UUIDTable("projects") {
    val workspaceId  = reference("workspace_id", WorkspacesTable)
    val projectKey   = varchar("project_key", 10)
    val name         = text("name")
    val taskCounter  = integer("task_counter").default(0)
    val createdAt    = timestamp("created_at")

    init {
        uniqueIndex(workspaceId, projectKey)
    }
}
