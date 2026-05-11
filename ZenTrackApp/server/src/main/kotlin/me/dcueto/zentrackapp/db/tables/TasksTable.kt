package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp

object TasksTable : LongIdTable("tasks") {
    val projectId      = reference("project_id", ProjectsTable)
    val sprintId       = reference("sprint_id", SprintsTable).nullable()
    val parentId       = reference("parent_id", TasksTable).nullable()
    val taskNumber     = integer("task_number")
    val displayId      = varchar("display_id", 50)
    val title          = varchar("title", 255)
    val description    = text("description").nullable()
    val statusId       = reference("status_id", TaskStatusesTable).nullable()
    val priority       = varchar("priority", 50)
    val estimate       = integer("estimate").nullable()
    val startDate      = date("start_date").nullable()
    val dueDate        = date("due_date").nullable()
    val gitBranchName  = varchar("git_branch_name", 255).nullable()
    val createdAt      = timestamp("created_at")
    val createdBy      = reference("created_by", UsersTable).nullable()
    val updatedAt      = timestamp("updated_at")
    val updatedBy      = reference("updated_by", UsersTable).nullable()

    init {
        uniqueIndex(projectId, taskNumber)
        uniqueIndex(projectId, displayId)
    }
}
