package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object WorkspaceTeamsTable : Table("workspace_teams") {
    val workspaceId = reference("workspace_id", WorkspacesTable)
    val teamId      = reference("team_id", TeamsTable)
    val assignedAt  = timestamp("assigned_at")
    val createdAt   = timestamp("created_at")
    val createdBy   = reference("created_by", UsersTable).nullable()
    override val primaryKey = PrimaryKey(workspaceId, teamId)
}
