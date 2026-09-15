package com.lexora.service.core.model

data class AuditRecord(
    val id: String,
    val organizationId: String,
    val userId: String,
    val entityType: String,
    val entityId: String?,
    val action: String,
    val summary: String,
    val occurredAtEpochMs: Long,
)

data class AuditFilter(
    val query: String = "",
    val entityType: String? = null,
    val action: String? = null,
    val userId: String? = null,
)
