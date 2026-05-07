package me.dcueto.zentrackapp.db.repositories

import kotlinx.coroutines.Dispatchers
import me.dcueto.zentrackapp.core.DuplicateProjectKeyException
import me.dcueto.zentrackapp.db.tables.ProjectsTable
import me.dcueto.zentrackapp.db.tables.WorkspacesTable
import me.dcueto.zentrackapp.model.Project
import me.dcueto.zentrackapp.repository.ProjectRepository
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.UUID

class ProjectRepositoryImpl : ProjectRepository {

    override suspend fun findAllByWorkspace(workspaceId: String, userId: String): List<Project> {
        val userUuid = UUID.fromString(userId)
        val wsUuid = UUID.fromString(workspaceId)
        return newSuspendedTransaction(Dispatchers.IO) {
            exec("SET LOCAL app.user_id = '$userUuid'")
            ProjectsTable.selectAll()
                .where { ProjectsTable.workspaceId eq EntityID(wsUuid, WorkspacesTable) }
                .map { it.toProject() }
        }
    }

    override suspend fun create(workspaceId: String, projectKey: String, name: String, userId: String): Project {
        val userUuid = UUID.fromString(userId)
        val wsUuid = UUID.fromString(workspaceId)
        return newSuspendedTransaction(Dispatchers.IO) {
            exec("SET LOCAL app.user_id = '$userUuid'")
            try {
                val projectId = ProjectsTable.insertAndGetId {
                    it[ProjectsTable.workspaceId] = EntityID(wsUuid, WorkspacesTable)
                    it[ProjectsTable.projectKey] = projectKey.uppercase()
                    it[ProjectsTable.name] = name
                    it[ProjectsTable.taskCounter] = 0
                    it[ProjectsTable.createdAt] = Instant.now()
                }
                ProjectsTable.selectAll()
                    .where { ProjectsTable.id eq projectId }
                    .single()
                    .toProject()
            } catch (e: ExposedSQLException) {
                if (e.sqlState == "23505") throw DuplicateProjectKeyException(projectKey)
                throw e
            }
        }
    }

    private fun ResultRow.toProject() = Project(
        id = this[ProjectsTable.id].value.toString(),
        workspaceId = this[ProjectsTable.workspaceId].value.toString(),
        projectKey = this[ProjectsTable.projectKey],
        name = this[ProjectsTable.name],
        taskCounter = this[ProjectsTable.taskCounter],
        createdAt = this[ProjectsTable.createdAt].toString()
    )
}
