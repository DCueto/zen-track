package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object OrganizationsTable : LongIdTable("organizations") {
    val name        = varchar("name", 255)
    val slug        = varchar("slug", 100)
    val plan        = varchar("plan", 50)
    val isPersonal  = bool("is_personal")
    val createdAt   = timestamp("created_at")
    val createdBy   = reference("created_by", UsersTable).nullable()
    val updatedAt   = timestamp("updated_at")
    val updatedBy   = reference("updated_by", UsersTable).nullable()

    init {
        uniqueIndex(slug)
    }
}
