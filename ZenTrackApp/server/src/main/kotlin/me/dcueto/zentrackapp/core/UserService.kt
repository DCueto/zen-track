package me.dcueto.zentrackapp.core

import me.dcueto.zentrackapp.db.repositories.OAuthAccountRecord
import me.dcueto.zentrackapp.db.repositories.OAuthAccountRepositoryImpl
import me.dcueto.zentrackapp.db.repositories.UserRepositoryImpl

sealed class UnlinkResult {
    object Success : UnlinkResult()
    object NotFound : UnlinkResult()
    object LastLoginMethod : UnlinkResult()
}


class UserService(
    private val oAuthAccountRepository: OAuthAccountRepositoryImpl,
    private val userRepository: UserRepositoryImpl
) {
    suspend fun getOAuthAccounts(userId: Long): List<OAuthAccountRecord> =
        oAuthAccountRepository.findByUserId(userId)

    suspend fun unlinkOAuthAccount(userId: Long, oAuthAccountId: Long): UnlinkResult {
        val account = oAuthAccountRepository.findById(oAuthAccountId) ?: return UnlinkResult.NotFound
        if (account.userId != userId) return UnlinkResult.NotFound  // ownership — no revelar existencia

        val user = userRepository.findById(userId) ?: return UnlinkResult.NotFound
        if (user.passwordHash == null) return UnlinkResult.LastLoginMethod

        oAuthAccountRepository.deleteById(oAuthAccountId)
        return UnlinkResult.Success
    }
}
