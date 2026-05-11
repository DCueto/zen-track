package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object TaskStatusesTable : LongIdTable("task_statuses") {
    val workspaceId = reference("workspace_id", WorkspacesTable)
    val name        = varchar("name", 100)
    val orderIndex  = integer("order_index")
    val isDefault   = bool("is_default")
    val createdAt   = timestamp("created_at")
    val createdBy   = reference("created_by", UsersTable).nullable()
    val updatedAt   = timestamp("updated_at")
    val updatedBy   = reference("updated_by", UsersTable).nullable()
}
