package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.sql.Table

object ProjectMembersTable : Table("project_members") {
    val projectId = reference("project_id", ProjectsTable)
    val userId    = reference("user_id", UsersTable)
    val role      = varchar("role", 50)
    override val primaryKey = PrimaryKey(projectId, userId)
}
