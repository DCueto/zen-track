package me.dcueto.zentrackapp.db.repositories

import kotlinx.coroutines.Dispatchers
import me.dcueto.zentrackapp.db.tables.UsersTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.UUID

data class UserRecord(
    val id: String,
    val email: String,
    val passwordHash: String,
    val name: String
)

class UserRepositoryImpl {

    suspend fun findByEmail(email: String): UserRecord? =
        newSuspendedTransaction(Dispatchers.IO) {
            // Login path: app.user_id is not set yet — the V006 policy allows this SELECT.
            UsersTable.selectAll()
                .where { UsersTable.email eq email }
                .singleOrNull()
                ?.toUserRecord()
        }

    suspend fun create(email: String, passwordHash: String, name: String): UserRecord {
        val newId = UUID.randomUUID()
        return newSuspendedTransaction(Dispatchers.IO) {
            exec("SET LOCAL app.user_id = '$newId'")
            UsersTable.insertAndGetId {
                it[UsersTable.id] = EntityID(newId, UsersTable)
                it[UsersTable.email] = email
                it[UsersTable.passwordHash] = passwordHash
                it[UsersTable.name] = name
                it[UsersTable.createdAt] = Instant.now()
            }
            UsersTable.selectAll()
                .where { UsersTable.email eq email }
                .single()
                .toUserRecord()
        }
    }

    private fun ResultRow.toUserRecord() = UserRecord(
        id = this[UsersTable.id].value.toString(),
        email = this[UsersTable.email],
        passwordHash = this[UsersTable.passwordHash],
        name = this[UsersTable.name]
    )
}
