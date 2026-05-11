package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object MembershipRequestsTable : LongIdTable("membership_requests") {
    val requesterId = reference("requester_id", UsersTable)
    val targetType  = varchar("target_type", 50)
    val targetId    = long("target_id")
    val status      = varchar("status", 50)
    val reviewedBy  = reference("reviewed_by", UsersTable).nullable()
    val reviewedAt  = timestamp("reviewed_at").nullable()
    val createdAt   = timestamp("created_at")
    val createdBy   = reference("created_by", UsersTable).nullable()
    val updatedAt   = timestamp("updated_at")
    val updatedBy   = reference("updated_by", UsersTable).nullable()
}
