package me.dcueto.zentrackapp.db.repositories

import kotlinx.coroutines.Dispatchers
import me.dcueto.zentrackapp.db.tables.RefreshTokensTable
import me.dcueto.zentrackapp.db.tables.UsersTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

data class RefreshTokenRecord(
    val id: Long,
    val userId: Long,
    val tokenHash: String,
    val expiresAt: Instant,
    val revokedAt: Instant?
)

class RefreshTokenRepositoryImpl {

    suspend fun create(userId: Long, tokenHash: String, expiresAt: Instant) =
        newSuspendedTransaction(Dispatchers.IO) {
            RefreshTokensTable.insert {
                it[RefreshTokensTable.userId] = EntityID(userId, UsersTable)
                it[RefreshTokensTable.tokenHash] = tokenHash
                it[RefreshTokensTable.expiresAt] = expiresAt
                it[RefreshTokensTable.createdAt] = Instant.now()
                it[RefreshTokensTable.updatedAt] = Instant.now()
            }
        }

    suspend fun findByTokenHash(tokenHash: String): RefreshTokenRecord? =
        newSuspendedTransaction(Dispatchers.IO) {
            RefreshTokensTable.selectAll()
                .where { RefreshTokensTable.tokenHash eq tokenHash }
                .singleOrNull()
                ?.toRecord()
        }

    suspend fun revoke(id: Long) =
        newSuspendedTransaction(Dispatchers.IO) {
            RefreshTokensTable.update({ RefreshTokensTable.id eq id }) {
                it[RefreshTokensTable.revokedAt] = Instant.now()
                it[RefreshTokensTable.updatedAt] = Instant.now()
            }
        }

    private fun ResultRow.toRecord() = RefreshTokenRecord(
        id = this[RefreshTokensTable.id].value,
        userId = this[RefreshTokensTable.userId].value,
        tokenHash = this[RefreshTokensTable.tokenHash],
        expiresAt = this[RefreshTokensTable.expiresAt],
        revokedAt = this[RefreshTokensTable.revokedAt]
    )
}
