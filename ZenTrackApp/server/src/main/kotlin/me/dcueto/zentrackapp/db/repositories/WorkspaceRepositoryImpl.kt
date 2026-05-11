package me.dcueto.zentrackapp.db.repositories

import kotlinx.coroutines.Dispatchers
import me.dcueto.zentrackapp.db.tables.OrganizationsTable
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

class WorkspaceRepositoryImpl : WorkspaceRepository {

    override suspend fun findAllByUser(userId: Long): List<Workspace> =
        newSuspendedTransaction(Dispatchers.IO) {
            exec("SET LOCAL app.user_id = '$userId'")
            WorkspacesTable.selectAll().map { it.toWorkspace() }
        }

    override suspend fun create(orgId: Long, name: String, userId: Long): Workspace =
        newSuspendedTransaction(Dispatchers.IO) {
            exec("SET LOCAL app.user_id = '$userId'")
            val workspaceId = WorkspacesTable.insertAndGetId {
                it[WorkspacesTable.orgId] = EntityID(orgId, OrganizationsTable)
                it[WorkspacesTable.name] = name
                it[WorkspacesTable.createdAt] = Instant.now()
                it[WorkspacesTable.updatedAt] = Instant.now()
            }
            WorkspaceMembersTable.insert {
                it[WorkspaceMembersTable.workspaceId] = workspaceId
                it[WorkspaceMembersTable.userId] = EntityID(userId, UsersTable)
                it[WorkspaceMembersTable.role] = "admin"
            }
            WorkspacesTable.selectAll()
                .where { WorkspacesTable.id eq workspaceId }
                .single()
                .toWorkspace()
        }

    private fun ResultRow.toWorkspace() = Workspace(
        id = this[WorkspacesTable.id].value,
        orgId = this[WorkspacesTable.orgId].value,
        name = this[WorkspacesTable.name],
        createdAt = this[WorkspacesTable.createdAt].toString()
    )
}
