package me.dcueto.zentrackapp.db.repositories

import kotlinx.coroutines.Dispatchers
import me.dcueto.zentrackapp.db.tables.OAuthAccountsTable
import me.dcueto.zentrackapp.db.tables.UsersTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

data class OAuthAccountRecord(
    val id: Long,
    val userId: Long,
    val provider: String,
    val providerUserId: String,
    val email: String,
    val createdAt: java.time.Instant
)

class OAuthAccountRepositoryImpl {

    suspend fun upsert(
        userId: Long,
        provider: String,
        providerUserId: String,
        email: String,
        encryptedAccessToken: String?,
        encryptedRefreshToken: String?,
        tokenExpiresAt: Instant?
    ) = newSuspendedTransaction(Dispatchers.IO) {
        val existing = OAuthAccountsTable.selectAll()
            .where {
                (OAuthAccountsTable.provider eq provider) and
                (OAuthAccountsTable.providerUserId eq providerUserId)
            }
            .singleOrNull()

        if (existing == null) {
            OAuthAccountsTable.insert {
                it[OAuthAccountsTable.userId] = EntityID(userId, UsersTable)
                it[OAuthAccountsTable.provider] = provider
                it[OAuthAccountsTable.providerUserId] = providerUserId
                it[OAuthAccountsTable.email] = email
                it[OAuthAccountsTable.accessToken] = encryptedAccessToken
                it[OAuthAccountsTable.refreshToken] = encryptedRefreshToken
                it[OAuthAccountsTable.tokenExpiresAt] = tokenExpiresAt
                it[OAuthAccountsTable.createdAt] = Instant.now()
                it[OAuthAccountsTable.updatedAt] = Instant.now()
            }
        } else {
            OAuthAccountsTable.update({
                (OAuthAccountsTable.provider eq provider) and
                (OAuthAccountsTable.providerUserId eq providerUserId)
            }) {
                it[OAuthAccountsTable.accessToken] = encryptedAccessToken
                // Only update refresh_token if Google returned a new one (only on first auth)
                if (encryptedRefreshToken != null) {
                    it[OAuthAccountsTable.refreshToken] = encryptedRefreshToken
                }
                if (tokenExpiresAt != null) {
                    it[OAuthAccountsTable.tokenExpiresAt] = tokenExpiresAt
                }
                it[OAuthAccountsTable.updatedAt] = Instant.now()
            }
        }
    }

    suspend fun findByUserId(userId: Long): List<OAuthAccountRecord> =
        newSuspendedTransaction(Dispatchers.IO) {
            OAuthAccountsTable.selectAll()
                .where { OAuthAccountsTable.userId eq EntityID(userId, UsersTable) }
                .map { it.toRecord() }
        }

    private fun ResultRow.toRecord() = OAuthAccountRecord(
        id = this[OAuthAccountsTable.id].value,
        userId = this[OAuthAccountsTable.userId].value,
        provider = this[OAuthAccountsTable.provider],
        providerUserId = this[OAuthAccountsTable.providerUserId],
        email = this[OAuthAccountsTable.email],
        createdAt = this[OAuthAccountsTable.createdAt]
    )
}
