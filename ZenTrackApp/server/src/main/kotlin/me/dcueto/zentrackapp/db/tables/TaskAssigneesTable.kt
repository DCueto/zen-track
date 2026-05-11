package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object TaskAssigneesTable : Table("task_assignees") {
    val taskId    = reference("task_id", TasksTable)
    val userId    = reference("user_id", UsersTable)
    val createdAt = timestamp("created_at")
    val createdBy = reference("created_by", UsersTable).nullable()
    override val primaryKey = PrimaryKey(taskId, userId)
}
