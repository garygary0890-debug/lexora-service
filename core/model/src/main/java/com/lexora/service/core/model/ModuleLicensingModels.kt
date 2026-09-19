package com.lexora.service.core.model

enum class ModuleEntitlementStatus { ACTIVE, TRIAL, GRACE, EXPIRED, SUSPENDED, REVOKED }

data class ModuleEntitlement(
    val organizationId: String,
    val moduleCode: String,
    val status: ModuleEntitlementStatus,
    val planCode: String? = null,
    val startsAtEpochMs: Long? = null,
    val expiresAtEpochMs: Long? = null,
    val limits: Map<String, Long> = emptyMap(),
    val source: String? = null,
)

data class ModuleAccessDecision(
    val moduleCode: String,
    val allowed: Boolean,
    val reasonCode: String? = null,
    val readOnly: Boolean = false,
)
