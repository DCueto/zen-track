package me.dcueto.zentrackapp.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object TaskTagsTable : Table("task_tags") {
    val taskId    = reference("task_id", TasksTable)
    val tagId     = reference("tag_id", TagsTable)
    val createdAt = timestamp("created_at")
    val createdBy = reference("created_by", UsersTable).nullable()
    override val primaryKey = PrimaryKey(taskId, tagId)
}
