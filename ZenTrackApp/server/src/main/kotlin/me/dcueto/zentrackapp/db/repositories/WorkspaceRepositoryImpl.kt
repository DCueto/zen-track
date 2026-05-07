package me.dcueto.zentrackapp.db.repositories

import kotlinx.coroutines.Dispatchers
import me.dcueto.zentrackapp.db.tables.UsersTable
import me.dcueto.zentrackapp.db.tables.WorkspaceMembersTable
import me.dcueto.zentrackapp.db.tables.WorkspacesTable
import me.dcueto.zentrackapp.model.Workspace
import me.dcueto.zentrackapp.repository.WorkspaceRepository
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.UUID

class WorkspaceRepositoryImpl : WorkspaceRepository {

    override suspend fun findAllByUser(userId: String): List<Workspace> {
        val uuid = UUID.fromString(userId)
        return newSuspendedTransaction(Dispatchers.IO) {
            exec("SET LOCAL app.user_id = '$uuid'")
            WorkspacesTable.selectAll().map { it.toWorkspace() }
        }
    }

    override suspend fun create(name: String, ownerId: String): Workspace {
        val ownerUuid = UUID.fromString(ownerId)
        return newSuspendedTransaction(Dispatchers.IO) {
            exec("SET LOCAL app.user_id = '$ownerUuid'")
            val workspaceId = WorkspacesTable.insertAndGetId {
                it[WorkspacesTable.name] = name
                it[WorkspacesTable.ownerId] = EntityID(ownerUuid, UsersTable)
                it[WorkspacesTable.createdAt] = Instant.now()
            }
            WorkspaceMembersTable.insert {
                it[WorkspaceMembersTable.workspaceId] = workspaceId
                it[WorkspaceMembersTable.userId] = EntityID(ownerUuid, UsersTable)
                it[WorkspaceMembersTable.role] = "OWNER"
            }
            WorkspacesTable.selectAll()
                .where { WorkspacesTable.id eq workspaceId }
                .single()
                .toWorkspace()
        }
    }

    private fun ResultRow.toWorkspace() = Workspace(
        id = this[WorkspacesTable.id].value.toString(),
        name = this[WorkspacesTable.name],
        ownerId = this[WorkspacesTable.ownerId].value.toString(),
        createdAt = this[WorkspacesTable.createdAt].toString()
    )
}
