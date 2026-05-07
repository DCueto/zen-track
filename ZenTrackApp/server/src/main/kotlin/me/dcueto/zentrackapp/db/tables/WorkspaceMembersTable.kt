package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.sql.Table

object WorkspaceMembersTable : Table("workspace_members") {
    val workspaceId = reference("workspace_id", WorkspacesTable)
    val userId      = reference("user_id", UsersTable)
    val role        = varchar("role", 50)
    override val primaryKey = PrimaryKey(workspaceId, userId)
}
