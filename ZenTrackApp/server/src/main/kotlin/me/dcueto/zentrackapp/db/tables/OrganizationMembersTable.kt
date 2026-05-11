package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object OrganizationMembersTable : Table("organization_members") {
    val orgId     = reference("org_id", OrganizationsTable)
    val userId    = reference("user_id", UsersTable)
    val role      = varchar("role", 50)
    val joinedAt  = timestamp("joined_at")
    val createdAt = timestamp("created_at")
    val createdBy = reference("created_by", UsersTable).nullable()
    val updatedAt = timestamp("updated_at")
    val updatedBy = reference("updated_by", UsersTable).nullable()
    override val primaryKey = PrimaryKey(orgId, userId)
}
