package me.dcueto.zentrackapp.db.repositories

import kotlinx.coroutines.Dispatchers
import me.dcueto.zentrackapp.db.tables.UsersTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant

data class UserRecord(
    val id: Long,
    val email: String,
    val passwordHash: String?,
    val name: String
)

class UserRepositoryImpl {

    suspend fun findByEmail(email: String): UserRecord? =
        newSuspendedTransaction(Dispatchers.IO) {
            UsersTable.selectAll()
                .where { UsersTable.email eq email }
                .singleOrNull()
                ?.toUserRecord()
        }

    suspend fun create(email: String, passwordHash: String, name: String): UserRecord =
        newSuspendedTransaction(Dispatchers.IO) {
            UsersTable.insertAndGetId {
                it[UsersTable.email] = email
                it[UsersTable.passwordHash] = passwordHash
                it[UsersTable.name] = name
                it[UsersTable.createdAt] = Instant.now()
                it[UsersTable.updatedAt] = Instant.now()
            }
            UsersTable.selectAll()
                .where { UsersTable.email eq email }
                .single()
                .toUserRecord()
        }

    private fun ResultRow.toUserRecord() = UserRecord(
        id = this[UsersTable.id].value,
        email = this[UsersTable.email],
        passwordHash = this[UsersTable.passwordHash],
        name = this[UsersTable.name]
    )
}
