package me.dcueto.zentrackapp.core

import me.dcueto.zentrackapp.db.repositories.OAuthAccountRecord
import me.dcueto.zentrackapp.db.repositories.OAuthAccountRepositoryImpl

class UserService(
    private val oAuthAccountRepository: OAuthAccountRepositoryImpl
) {
    suspend fun getOAuthAccounts(userId: Long): List<OAuthAccountRecord> =
        oAuthAccountRepository.findByUserId(userId)
}
