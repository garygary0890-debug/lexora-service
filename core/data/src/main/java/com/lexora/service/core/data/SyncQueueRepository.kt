package com.lexora.service.core.data

import com.lexora.service.core.database.ServiceDao
import com.lexora.service.core.database.SyncConflictEntity
import com.lexora.service.core.database.SyncOperationEntity
import com.lexora.service.core.model.SyncConflict
import com.lexora.service.core.model.SyncConflictResolution
import com.lexora.service.core.model.SyncOperation
import com.lexora.service.core.model.SyncOperationStatus
import com.lexora.service.core.model.SyncOperationType
import java.util.UUID
import kotlin.math.min

class SyncQueueRepository(private val dao: ServiceDao) {
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
            )
        )
        return inserted != -1L
    }

    suspend fun ready(organizationId: String, now: Long = System.currentTimeMillis(), limit: Int = 50): List<SyncOperation> =
        dao.readySyncOperations(organizationId, now, limit).map { it.toModel() }

    suspend fun markInProgress(operation: SyncOperation, now: Long = System.currentTimeMillis()): Boolean =
        dao.markSyncOperationInProgress(operation.id, operation.organizationId, now) == 1

    suspend fun markSucceeded(operation: SyncOperation, now: Long = System.currentTimeMillis()) {
        dao.markSyncOperationSucceeded(operation.id, operation.organizationId, now)
    }

    suspend fun markRetry(
        operation: SyncOperation,
        error: String?,
        now: Long = System.currentTimeMillis(),
    ) {
        val nextAttempt = now + retryDelayMs(operation.attemptCount + 1)
        dao.markSyncOperationRetry(operation.id, operation.organizationId, error?.take(MAX_ERROR_LENGTH), nextAttempt, now)
    }

    suspend fun markFailed(operation: SyncOperation, error: String?, now: Long = System.currentTimeMillis()) {
        dao.markSyncOperationFailed(operation.id, operation.organizationId, error?.take(MAX_ERROR_LENGTH), now)
    }

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
            )
        )
        return conflictId
    }

    suspend fun unresolvedConflicts(organizationId: String): List<SyncConflict> =
        dao.unresolvedSyncConflicts(organizationId).map { it.toModel() }

    suspend fun resolveConflict(
        organizationId: String,
        id: String,
        resolution: SyncConflictResolution,
        now: Long = System.currentTimeMillis(),
    ) {
        require(resolution != SyncConflictResolution.UNRESOLVED)
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
