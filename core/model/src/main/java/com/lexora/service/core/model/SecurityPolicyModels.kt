package com.lexora.service.core.model

enum class SecurityEventType { LOGIN_SUCCESS, LOGIN_FAILURE, TOKEN_REFRESH, TOKEN_REVOKE, PERMISSION_DENIED, EXPORT, ADMIN_CHANGE }

data class SecurityPolicy(
    val organizationId: String,
    val sessionTimeoutMinutes: Int,
    val refreshTokenLifetimeDays: Int,
    val requireDeviceLock: Boolean = false,
    val allowSensitiveOfflineData: Boolean = true,
    val maxFailedLoginAttempts: Int = 5,
)

data class SecurityEvent(
    val id: String,
    val organizationId: String?,
    val userId: String?,
    val type: SecurityEventType,
    val occurredAtEpochMs: Long,
    val deviceId: String? = null,
    val ipAddressMasked: String? = null,
    val reasonCode: String? = null,
)

data class SensitiveDataRule(
    val dataCategory: String,
    val encryptAtRest: Boolean,
    val allowLogging: Boolean = false,
    val allowExport: Boolean = false,
)
