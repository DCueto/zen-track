package me.dcueto.zentrackapp.core

import at.favre.lib.crypto.bcrypt.BCrypt
import me.dcueto.zentrackapp.db.repositories.RefreshTokenRepositoryImpl
import me.dcueto.zentrackapp.db.repositories.UserRecord
import me.dcueto.zentrackapp.db.repositories.UserRepositoryImpl
import me.dcueto.zentrackapp.dto.AuthResponse
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

private val REFRESH_TOKEN_TTL_SECONDS = 30L * 24 * 3600   // 30 días

class AuthService(
    private val userRepository: UserRepositoryImpl,
    private val jwtService: JwtService,
    private val refreshTokenRepository: RefreshTokenRepositoryImpl
) {
    suspend fun register(email: String, password: String, name: String): AuthResponse {
        val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val user = userRepository.create(email, passwordHash, name)
        return issueTokenPair(user.id)
    }

    suspend fun login(email: String, password: String): AuthResponse? {
        val user: UserRecord = userRepository.findByEmail(email) ?: return null
        val verified = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash ?: return null).verified
        if (!verified) return null
        return issueTokenPair(user.id)
    }

    suspend fun refresh(rawRefreshToken: String): AuthResponse? {
        val hash = hashToken(rawRefreshToken)
        val record = refreshTokenRepository.findByTokenHash(hash) ?: return null
        if (record.revokedAt != null || Instant.now().isAfter(record.expiresAt)) return null

        // Rotate: revoke old, issue new pair
        refreshTokenRepository.revoke(record.id)
        return issueTokenPair(record.userId)
    }

    suspend fun logout(rawRefreshToken: String) {
        val hash = hashToken(rawRefreshToken)
        val record = refreshTokenRepository.findByTokenHash(hash) ?: return
        if (record.revokedAt == null) refreshTokenRepository.revoke(record.id)
    }

    suspend fun issueTokenPair(userId: Long): AuthResponse {
        val jwt = jwtService.generateToken(userId)
        val rawToken = generateRawToken()
        val expiresAt = Instant.now().plusSeconds(REFRESH_TOKEN_TTL_SECONDS)
        refreshTokenRepository.create(userId, hashToken(rawToken), expiresAt)
        return AuthResponse(token = jwt, refreshToken = rawToken)
    }

    private fun generateRawToken(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
