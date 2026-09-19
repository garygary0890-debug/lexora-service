package com.lexora.service.core.data

import com.lexora.service.core.database.ServiceDao
import com.lexora.service.core.database.SyncConflictEntity
import com.lexora.service.core.database.SyncOperationEntity
import com.lexora.service.core.model.SyncConflict
import com.lexora.service.core.model.SyncConflictResolution
import com.lexora.service.core.model.SyncOperation
import com.lexora.service.core.model.SyncOperationStatus
import com.lexora.service.core.model.SyncOperationType
import com.lexora.service.core.model.SyncIssue
import java.util.UUID
import kotlin.math.min

class SyncQueueRepository(
    private val dao: ServiceDao,
    private val onWorkQueued: (Long) -> Unit = {},
) {
    suspend fun enqueue(
        organizationId: String,
        entityType: String,
        entityId: String,
        operationType: SyncOperationType,
        payloadJson: String?,
        idempotencyKey: String = "$organizationId:$entityType:$entityId:${operationType.name}:${UUID.randomUUID()}",
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        val inserted = dao.enqueueSyncOperation(
            SyncOperationEntity(
                id = UUID.randomUUID().toString(),
                organizationId = organizationId,
                entityType = entityType,
                entityId = entityId,
                operationType = operationType.name,
                idempotencyKey = idempotencyKey,
                payloadJson = payloadJson,
                status = SyncOperationStatus.PENDING.name,
                attemptCount = 0,
                nextAttemptAtEpochMs = null,
                lastError = null,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )
        if (inserted != -1L) onWorkQueued(0L)
        return inserted != -1L
    }

    suspend fun ready(organizationId: String, now: Long = System.currentTimeMillis(), limit: Int = 50): List<SyncOperation> =
        dao.readySyncOperations(organizationId, now, limit).map { it.toModel() }

    suspend fun markInProgress(operation: SyncOperation, now: Long = System.currentTimeMillis()): Boolean =
        dao.markSyncOperationInProgress(operation.id, operation.organizationId, now) == 1

    suspend fun markSucceeded(operation: SyncOperation, now: Long = System.currentTimeMillis()) {
        dao.markSyncOperationSucceeded(operation.id, operation.organizationId, now)
    }

    suspend fun markRetry(operation: SyncOperation, error: String?, now: Long = System.currentTimeMillis()) {
        val delay = retryDelayMs(operation.attemptCount + 1)
        dao.markSyncOperationRetry(
            operation.id,
            operation.organizationId,
            error?.take(MAX_ERROR_LENGTH),
            now + delay,
            now,
        )
        onWorkQueued(delay)
    }

    suspend fun markFailed(operation: SyncOperation, error: String?, now: Long = System.currentTimeMillis()) {
        dao.markSyncOperationFailed(operation.id, operation.organizationId, error?.take(MAX_ERROR_LENGTH), now)
    }

    suspend fun recoverStaleInProgress(
        organizationId: String,
        now: Long = System.currentTimeMillis(),
        staleAfterMs: Long = STALE_IN_PROGRESS_MS,
    ): Int {
        val stale = dao.syncOperations(organizationId)
            .map { it.toModel() }
            .filter { it.status == SyncOperationStatus.IN_PROGRESS && now - it.updatedAtEpochMs >= staleAfterMs }
        stale.forEach { markRetry(it, "Recovered after interrupted sync", now) }
        return stale.size
    }

    suspend fun nextRetryDelayMs(organizationId: String, now: Long = System.currentTimeMillis()): Long? =
        dao.syncOperations(organizationId)
            .filter { it.status == SyncOperationStatus.PENDING.name || it.status == SyncOperationStatus.RETRY_WAIT.name }
            .map { ((it.nextAttemptAtEpochMs ?: now) - now).coerceAtLeast(0L) }
            .minOrNull()

    suspend fun recordConflict(
        operation: SyncOperation,
        localVersionJson: String?,
        remoteVersionJson: String?,
        error: String? = "Version conflict",
        now: Long = System.currentTimeMillis(),
    ): String {
        dao.markSyncOperationConflict(operation.id, operation.organizationId, error?.take(MAX_ERROR_LENGTH), now)
        val conflictId = UUID.randomUUID().toString()
        dao.upsertSyncConflict(
            SyncConflictEntity(
                id = conflictId,
                organizationId = operation.organizationId,
                entityType = operation.entityType,
                entityId = operation.entityId,
                localVersionJson = localVersionJson,
                remoteVersionJson = remoteVersionJson,
                resolution = SyncConflictResolution.UNRESOLVED.name,
                detectedAtEpochMs = now,
                resolvedAtEpochMs = null,
            ),
        )
        return conflictId
    }

    suspend fun syncIssue(organizationId: String): SyncIssue {
        val operations = dao.syncOperations(organizationId)
        val pending = operations.count { it.status == SyncOperationStatus.PENDING.name || it.status == SyncOperationStatus.IN_PROGRESS.name }
        val retry = operations.count { it.status == SyncOperationStatus.RETRY_WAIT.name }
        val failed = operations.count { it.status == SyncOperationStatus.FAILED.name }
        val conflicts = operations.count { it.status == SyncOperationStatus.CONFLICT.name }
        val lastError = operations.asReversed().firstOrNull { !it.lastError.isNullOrBlank() }?.lastError
        return SyncIssue(pending, retry, failed, conflicts, lastError)
    }

    suspend fun unresolvedConflicts(organizationId: String): List<SyncConflict> =
        dao.unresolvedSyncConflicts(organizationId).map { it.toModel() }

    suspend fun resolveConflict(
        organizationId: String,
        id: String,
        resolution: SyncConflictResolution,
        mergedPayloadJson: String? = null,
        now: Long = System.currentTimeMillis(),
    ) {
        require(resolution != SyncConflictResolution.UNRESOLVED)
        val conflict = dao.unresolvedSyncConflicts(organizationId).firstOrNull { it.id == id }
            ?: return
        val original = dao.syncOperations(organizationId)
            .filter {
                it.entityType == conflict.entityType &&
                    it.entityId == conflict.entityId &&
                    it.status == SyncOperationStatus.CONFLICT.name
            }
            .maxByOrNull { it.updatedAtEpochMs }
            ?.toModel()

        when (resolution) {
            SyncConflictResolution.KEEP_REMOTE -> {
                original?.let { markFailed(it, "Conflict resolved: server version kept", now) }
            }
            SyncConflictResolution.KEEP_LOCAL,
            SyncConflictResolution.MERGED -> {
                val source = requireNotNull(original) { "Conflicting operation not found" }
                val payload = if (resolution == SyncConflictResolution.MERGED) {
                    requireNotNull(mergedPayloadJson) { "Merged payload is required" }
                } else {
                    source.payloadJson
                }
                markFailed(source, "Conflict superseded by resolved mutation", now)
                enqueue(
                    organizationId = source.organizationId,
                    entityType = source.entityType,
                    entityId = source.entityId,
                    operationType = source.operationType,
                    payloadJson = payload,
                    now = now,
                )
            }
            SyncConflictResolution.UNRESOLVED -> error("Unreachable")
        }
        dao.resolveSyncConflict(id, organizationId, resolution.name, now)
    }

    suspend fun cleanupSucceeded(organizationId: String, now: Long = System.currentTimeMillis(), retentionDays: Int = 14) {
        val retentionMs = retentionDays.coerceAtLeast(1).toLong() * 24L * 60L * 60L * 1000L
        dao.deleteOldSucceededSyncOperations(organizationId, now - retentionMs)
    }

    private fun retryDelayMs(attempt: Int): Long {
        val power = min(attempt.coerceAtLeast(1) - 1, 8)
        return min(BASE_RETRY_MS * (1L shl power), MAX_RETRY_MS)
    }

    private companion object {
        const val BASE_RETRY_MS = 30_000L
        const val MAX_RETRY_MS = 6L * 60L * 60L * 1000L
        const val STALE_IN_PROGRESS_MS = 5L * 60L * 1000L
        const val MAX_ERROR_LENGTH = 2_000
    }
}

private fun SyncOperationEntity.toModel() = SyncOperation(
    id = id,
    organizationId = organizationId,
    entityType = entityType,
    entityId = entityId,
    operationType = SyncOperationType.valueOf(operationType),
    idempotencyKey = idempotencyKey,
    payloadJson = payloadJson,
    status = SyncOperationStatus.valueOf(status),
    attemptCount = attemptCount,
    nextAttemptAtEpochMs = nextAttemptAtEpochMs,
    lastError = lastError,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
)

private fun SyncConflictEntity.toModel() = SyncConflict(
    id = id,
    organizationId = organizationId,
    entityType = entityType,
    entityId = entityId,
    localVersionJson = localVersionJson,
    remoteVersionJson = remoteVersionJson,
    resolution = SyncConflictResolution.valueOf(resolution),
    detectedAtEpochMs = detectedAtEpochMs,
    resolvedAtEpochMs = resolvedAtEpochMs,
)
