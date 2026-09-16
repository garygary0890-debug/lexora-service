package com.lexora.service.core.model

enum class SyncConflictResolution { AUTO_MERGED, KEEP_LOCAL, KEEP_REMOTE, MANUAL_REQUIRED, RESOLVED }

data class SyncConflict(
    val id: String,
    val organizationId: String,
    val entityType: String,
    val entityId: String,
    val localVersion: Long,
    val remoteVersion: Long,
    val conflictingFields: Set<String>,
    val detectedAtEpochMs: Long,
    val resolution: SyncConflictResolution = SyncConflictResolution.MANUAL_REQUIRED,
    val resolvedByUserId: String? = null,
    val resolvedAtEpochMs: Long? = null,
)
