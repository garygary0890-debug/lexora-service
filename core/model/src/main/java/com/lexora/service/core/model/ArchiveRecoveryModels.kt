package com.lexora.service.core.model

enum class ArchiveAction { ARCHIVE, RESTORE, HARD_DELETE }

data class ArchiveEvent(
    val id: String,
    val organizationId: String,
    val entityType: String,
    val entityId: String,
    val action: ArchiveAction,
    val actorUserId: String,
    val reason: String,
    val occurredAtEpochMs: Long,
    val branchId: String? = null,
)

data class RecoverableEntityState(
    val entityType: String,
    val entityId: String,
    val archived: Boolean,
    val archivedAtEpochMs: Long? = null,
    val archivedByUserId: String? = null,
    val restoreAllowed: Boolean = true,
    val hardDeleteAllowed: Boolean = false,
)

data class RecoveryResult(
    val entityType: String,
    val entityId: String,
    val restored: Boolean,
    val preservedRelationCount: Int,
    val auditEventId: String,
)
