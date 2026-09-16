package com.lexora.service.core.data

import com.lexora.service.core.model.SyncOperation

interface SyncTransport {
    suspend fun push(operation: SyncOperation): SyncPushResult
}

sealed interface SyncPushResult {
    data object Success : SyncPushResult
    data class RetryableError(val message: String?) : SyncPushResult
    data class PermanentError(val message: String?) : SyncPushResult
    data class Conflict(
        val message: String? = null,
        val localVersionJson: String? = null,
        val remoteVersionJson: String? = null,
    ) : SyncPushResult
}

class SyncEngine(
    private val queue: SyncQueueRepository,
    private val transport: SyncTransport,
) {
    suspend fun runOnce(organizationId: String, limit: Int = 50): SyncRunSummary {
        var succeeded = 0
        var retried = 0
        var failed = 0
        var conflicts = 0

        val ready = queue.ready(organizationId, limit = limit)
        for (operation in ready) {
            if (operation.organizationId != organizationId) continue
            if (!queue.markInProgress(operation)) continue
            when (val result = transport.push(operation.copy(attemptCount = operation.attemptCount + 1))) {
                SyncPushResult.Success -> {
                    queue.markSucceeded(operation)
                    succeeded++
                }
                is SyncPushResult.RetryableError -> {
                    queue.markRetry(operation, result.message)
                    retried++
                }
                is SyncPushResult.PermanentError -> {
                    queue.markFailed(operation, result.message)
                    failed++
                }
                is SyncPushResult.Conflict -> {
                    queue.recordConflict(operation, result.localVersionJson, result.remoteVersionJson, result.message)
                    conflicts++
                }
            }
        }

        return SyncRunSummary(
            inspected = ready.size,
            succeeded = succeeded,
            retried = retried,
            failed = failed,
            conflicts = conflicts,
        )
    }
}

data class SyncRunSummary(
    val inspected: Int,
    val succeeded: Int,
    val retried: Int,
    val failed: Int,
    val conflicts: Int,
)
