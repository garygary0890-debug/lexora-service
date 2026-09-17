package com.lexora.service.core.model

enum class CriticalActionType {
    ROLE_CHANGE, PERMISSION_CHANGE, MODULE_LICENSE_CHANGE, DATA_EXPORT,
    PAYMENT_CHANGE, PRICE_OVERRIDE, ARCHIVE, RESTORE, DELETE, LOGIN_CONTROL
}

data class CriticalActionAudit(
    val id: String,
    val organizationId: String,
    val branchId: String? = null,
    val actorUserId: String,
    val actionType: CriticalActionType,
    val entityType: String?,
    val entityId: String?,
    val occurredAtEpochMs: Long,
    val reason: String? = null,
    val beforeDigest: String? = null,
    val afterDigest: String? = null,
)

data class AuditRetentionPolicy(
    val organizationId: String,
    val retentionDays: Int,
    val immutableCriticalEvents: Boolean = true,
    val allowAdministrativeExport: Boolean = true,
)
