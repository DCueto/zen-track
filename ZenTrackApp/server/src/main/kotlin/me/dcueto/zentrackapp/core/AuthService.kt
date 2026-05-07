package me.dcueto.zentrackapp.core

import at.favre.lib.crypto.bcrypt.BCrypt
import me.dcueto.zentrackapp.db.repositories.UserRecord
import me.dcueto.zentrackapp.db.repositories.UserRepositoryImpl

class AuthService(
    private val userRepository: UserRepositoryImpl,
    private val jwtService: JwtService
) {
    suspend fun register(email: String, password: String, name: String): String {
        val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val user = userRepository.create(email, passwordHash, name)
        return jwtService.generateToken(userId = user.id)
    }

    suspend fun login(email: String, password: String): String? {
        val user: UserRecord = userRepository.findByEmail(email) ?: return null
        val verified = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash).verified
        if (!verified) return null
        return jwtService.generateToken(userId = user.id)
    }
}
