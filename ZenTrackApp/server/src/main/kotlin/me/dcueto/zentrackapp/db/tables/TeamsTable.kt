package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object TeamsTable : LongIdTable("teams") {
    val orgId     = reference("org_id", OrganizationsTable)
    val name      = varchar("name", 255)
    val colorHex  = varchar("color_hex", 7).nullable()
    val createdAt = timestamp("created_at")
    val createdBy = reference("created_by", UsersTable).nullable()
    val updatedAt = timestamp("updated_at")
    val updatedBy = reference("updated_by", UsersTable).nullable()
}
