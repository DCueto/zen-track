package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp

object SprintsTable : LongIdTable("sprints") {
    val workspaceId = reference("workspace_id", WorkspacesTable)
    val name        = varchar("name", 255)
    val startDate   = date("start_date").nullable()
    val endDate     = date("end_date").nullable()
    val status      = varchar("status", 50)
    val createdAt   = timestamp("created_at")
    val createdBy   = reference("created_by", UsersTable).nullable()
    val updatedAt   = timestamp("updated_at")
    val updatedBy   = reference("updated_by", UsersTable).nullable()
}
