package com.lexora.service.core.data

import com.lexora.service.core.model.SyncOperation
import com.lexora.service.core.model.SyncOperationType
import com.lexora.service.core.network.LexoraBackendSyncApi
import com.lexora.service.core.network.RemoteSyncChange
import com.lexora.service.core.network.SyncMutation
import com.lexora.service.core.network.SyncMutationResult

interface SyncCursorStore {
    suspend fun cursor(organizationId: String): Long
    suspend fun saveCursor(organizationId: String, cursor: Long)
}

interface RemoteChangeApplier {
    /** Apply one remote change atomically. Return false to stop without advancing the cursor. */
    suspend fun apply(organizationId: String, change: RemoteSyncChange): Boolean
}

interface SyncMutationMapper {
    fun map(operation: SyncOperation): SyncMutation?
}

class ServiceFoundationMutationMapper : SyncMutationMapper {
    override fun map(operation: SyncOperation): SyncMutation? {
        val mutationType = when (operation.entityType.trim().uppercase()) {
            "CLIENT", "SERVICE_CLIENT" -> "service.client.upsert"
            "VEHICLE", "SERVICE_ASSET", "ASSET" -> "service.asset.upsert"
            "WORK_ORDER", "SERVICE_WORK_ORDER" -> "service.work-order.upsert"
            else -> return null
        }
        if (operation.operationType == SyncOperationType.DELETE) return null
        return SyncMutation(
            clientMutationId = operation.idempotencyKey,
            mutationType = mutationType,
            entityType = operation.entityType,
            entityId = operation.entityId,
            payloadJson = operation.payloadJson,
        )
    }
}

data class BidirectionalSyncSummary(
    val pushed: Int,
    val pullApplied: Int,
    val conflicts: Int,
    val retried: Int,
    val failed: Int,
    val skippedUnsupported: Int,
    val finalCursor: Long,
)

class BidirectionalSyncEngine(
    private val queue: SyncQueueRepository,
    private val api: LexoraBackendSyncApi,
    private val cursorStore: SyncCursorStore,
    private val remoteChangeApplier: RemoteChangeApplier,
    private val mutationMapper: SyncMutationMapper = ServiceFoundationMutationMapper(),
) {
    suspend fun runOnce(
        organizationId: String,
        pushLimit: Int = 100,
        pullPageSize: Int = 200,
        maxPullPages: Int = 10,
    ): BidirectionalSyncSummary {
        require(pushLimit in 1..500)
        require(pullPageSize in 1..500)
        require(maxPullPages in 1..100)

        var pushed = 0
        var conflicts = 0
        var retried = 0
        var failed = 0
        var skippedUnsupported = 0
        var pullApplied = 0

        val ready = queue.ready(organizationId, limit = pushLimit)
        val operationsByMutationId = linkedMapOf<String, SyncOperation>()
        val mutations = mutableListOf<SyncMutation>()
        ready.forEach { operation ->
            if (operation.organizationId != organizationId) return@forEach
            val mutation = mutationMapper.map(operation)
            if (mutation == null) {
                skippedUnsupported++
                return@forEach
            }
            if (!queue.markInProgress(operation)) return@forEach
            operationsByMutationId[mutation.clientMutationId] = operation
            mutations += mutation
        }

        if (mutations.isNotEmpty()) {
            val results = try {
                api.push(organizationId, mutations)
            } catch (error: Exception) {
                operationsByMutationId.values.forEach {
                    queue.markRetry(it, error.message ?: error.javaClass.simpleName)
                    retried++
                }
                emptyList()
            }
            val seen = mutableSetOf<String>()
            results.forEach { result ->
                val operation = operationsByMutationId[result.clientMutationId] ?: return@forEach
                seen += result.clientMutationId
                when (normalizeStatus(result)) {
                    "APPLIED", "DUPLICATE" -> {
                        queue.markSucceeded(operation)
                        pushed++
                    }
                    "CONFLICT" -> {
                        queue.recordConflict(
                            operation = operation,
                            localVersionJson = operation.payloadJson,
                            remoteVersionJson = result.currentVersion?.let { "{\"serverVersion\":$it}" },
                            error = result.resultCode ?: "SYNC_VERSION_CONFLICT",
                        )
                        conflicts++
                    }
                    "REJECTED", "FAILED" -> {
                        queue.markFailed(operation, result.resultCode ?: "SYNC_REJECTED")
                        failed++
                    }
                    else -> {
                        queue.markRetry(operation, result.resultCode ?: "UNKNOWN_SYNC_RESULT")
                        retried++
                    }
                }
            }
            operationsByMutationId.forEach { (mutationId, operation) ->
                if (mutationId !in seen && mutations.isNotEmpty()) {
                    queue.markRetry(operation, "SYNC_RESULT_MISSING")
                    retried++
                }
            }
        }

        var cursor = cursorStore.cursor(organizationId).coerceAtLeast(0L)
        repeat(maxPullPages) {
            val page = api.pull(organizationId, cursor, pullPageSize)
            if (page.items.isEmpty()) {
                if (page.nextCursor > cursor) {
                    cursor = page.nextCursor
                    cursorStore.saveCursor(organizationId, cursor)
                }
                return@repeat
            }
            for (change in page.items.sortedBy { it.cursor }) {
                if (change.cursor <= cursor) continue
                val applied = remoteChangeApplier.apply(organizationId, change)
                if (!applied) {
                    return BidirectionalSyncSummary(
                        pushed, pullApplied, conflicts + 1, retried, failed, skippedUnsupported, cursor,
                    )
                }
                cursor = change.cursor
                cursorStore.saveCursor(organizationId, cursor)
                pullApplied++
            }
            if (page.nextCursor > cursor) {
                cursor = page.nextCursor
                cursorStore.saveCursor(organizationId, cursor)
            }
            if (page.items.size < pullPageSize) return@repeat
        }

        queue.cleanupSucceeded(organizationId)
        return BidirectionalSyncSummary(
            pushed = pushed,
            pullApplied = pullApplied,
            conflicts = conflicts,
            retried = retried,
            failed = failed,
            skippedUnsupported = skippedUnsupported,
            finalCursor = cursor,
        )
    }

    private fun normalizeStatus(result: SyncMutationResult): String = result.status.trim().uppercase()
}
