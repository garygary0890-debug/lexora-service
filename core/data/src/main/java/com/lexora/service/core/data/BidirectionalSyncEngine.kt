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
    suspend fun apply(organizationId: String, change: RemoteSyncChange): Boolean
}

interface SyncMutationMapper {
    fun map(operation: SyncOperation): SyncMutation?
}

class ServiceFoundationMutationMapper : SyncMutationMapper {
    override fun map(operation: SyncOperation): SyncMutation? {
        val mutationType = when (operation.entityType.trim().uppercase()) {
            "CLIENT", "SERVICECLIENT", "SERVICE_CLIENT" -> "service.client.upsert"
            "VEHICLE", "SERVICEASSET", "SERVICE_ASSET", "ASSET" -> "service.asset.upsert"
            "SERVICEDOCUMENT", "SERVICEWORKORDER", "SERVICE_WORK_ORDER", "WORK_ORDER" -> "service.work-order.upsert"
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
    private val metadata: ServiceSyncMetadataStore? = cursorStore as? ServiceSyncMetadataStore

    suspend fun runOnce(
        organizationId: String,
        pushLimit: Int = 100,
        pullPageSize: Int = 200,
        maxPullPages: Int = 10,
    ): BidirectionalSyncSummary {
        require(organizationId.isNotBlank())
        require(pushLimit in 1..200)
        require(pullPageSize in 1..500)
        require(maxPullPages in 1..100)

        queue.recoverStaleInProgress(organizationId)

        var pushed = 0
        var conflicts = 0
        var retried = 0
        var failed = 0
        var skippedUnsupported = 0
        var pullApplied = 0

        val ready = queue.ready(organizationId, limit = pushLimit)
        val operationsByMutationId = linkedMapOf<String, Pair<SyncOperation, SyncMutation>>()
        ready.forEach { operation ->
            if (operation.organizationId != organizationId) return@forEach
            val mutation = mutationMapper.map(operation)
            if (mutation == null) {
                skippedUnsupported++
                return@forEach
            }
            if (queue.markInProgress(operation)) operationsByMutationId[mutation.clientMutationId] = operation to mutation
        }

        if (operationsByMutationId.isNotEmpty()) {
            val results = try {
                api.push(organizationId, operationsByMutationId.values.map { it.second })
            } catch (error: Exception) {
                operationsByMutationId.values.forEach { (operation, _) ->
                    queue.markRetry(operation, error.message ?: error.javaClass.simpleName)
                    retried++
                }
                emptyList()
            }
            val seen = mutableSetOf<String>()
            results.forEach { result ->
                val pair = operationsByMutationId[result.clientMutationId] ?: return@forEach
                val (operation, mutation) = pair
                seen += result.clientMutationId
                when (normalizeStatus(result)) {
                    "APPLIED", "DUPLICATE", "AUTO_RESOLVED" -> {
                        queue.markSucceeded(operation)
                        (result.resultingVersion ?: result.currentVersion)?.let { version ->
                            metadata?.setVersion(organizationId, mutation.entityType, mutation.entityId, version)
                        }
                        pushed++
                    }
                    "RESOLVED_SERVER_WINS" -> {
                        queue.markSucceeded(operation)
                        result.currentVersion?.let { version ->
                            metadata?.setVersion(organizationId, mutation.entityType, mutation.entityId, version)
                        }
                    }
                    "CONFLICT", "USER_RESOLUTION_REQUIRED" -> {
                        queue.recordConflict(
                            operation = operation,
                            localVersionJson = operation.payloadJson,
                            remoteVersionJson = result.currentVersion?.let { "{\"serverVersion\":$it}" },
                            error = result.resultCode ?: "USER_RESOLUTION_REQUIRED",
                        )
                        result.currentVersion?.let { version ->
                            metadata?.setVersion(organizationId, mutation.entityType, mutation.entityId, version)
                        }
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
            operationsByMutationId.forEach { (mutationId, pair) ->
                if (mutationId !in seen && results.isNotEmpty()) {
                    queue.markRetry(pair.first, "SYNC_RESULT_MISSING")
                    retried++
                }
            }
        }

        var cursor = cursorStore.cursor(organizationId).coerceAtLeast(0L)
        var pageNo = 0
        while (pageNo < maxPullPages) {
            val page = api.pull(organizationId, cursor, pullPageSize)
            if (page.items.isEmpty()) {
                if (page.nextCursor > cursor) {
                    cursor = page.nextCursor
                    cursorStore.saveCursor(organizationId, cursor)
                }
                break
            }
            for (change in page.items.sortedBy { it.cursor }) {
                if (change.cursor <= cursor) continue
                if (!remoteChangeApplier.apply(organizationId, change)) {
                    return BidirectionalSyncSummary(pushed, pullApplied, conflicts + 1, retried, failed, skippedUnsupported, cursor)
                }
                metadata?.setVersion(organizationId, change.entityType, change.entityId, change.entityVersion)
                cursor = change.cursor
                cursorStore.saveCursor(organizationId, cursor)
                pullApplied++
            }
            if (page.nextCursor > cursor) {
                cursor = page.nextCursor
                cursorStore.saveCursor(organizationId, cursor)
            }
            pageNo++
            if (page.items.size < pullPageSize) break
        }

        queue.cleanupSucceeded(organizationId)
        return BidirectionalSyncSummary(pushed, pullApplied, conflicts, retried, failed, skippedUnsupported, cursor)
    }

    private fun normalizeStatus(result: SyncMutationResult): String = result.status.trim().uppercase()
}
