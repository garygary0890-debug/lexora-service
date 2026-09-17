package com.lexora.service.core.model

enum class SessionState { ACTIVE, EXPIRED, REVOKED, LOCKED }

data class AuthSession(
    val id: String,
    val userId: String,
    val organizationId: String?,
    val accessTokenExpiresAtEpochMs: Long,
    val refreshTokenExpiresAtEpochMs: Long,
    val state: SessionState = SessionState.ACTIVE,
    val createdAtEpochMs: Long,
    val lastSeenAtEpochMs: Long,
    val revokedAtEpochMs: Long? = null,
)

data class TokenRefreshRequest(
    val sessionId: String,
    val refreshTokenFingerprint: String,
    val requestedAtEpochMs: Long,
)

data class TokenRefreshResult(
    val sessionId: String,
    val accessTokenExpiresAtEpochMs: Long,
    val refreshTokenRotated: Boolean,
)

data class SessionRevocation(
    val sessionId: String,
    val actorUserId: String?,
    val reasonCode: String,
    val revokedAtEpochMs: Long,
)
