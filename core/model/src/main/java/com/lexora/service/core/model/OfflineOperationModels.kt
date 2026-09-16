package com.lexora.service.core.model

enum class OfflineOperationState { LOCAL, QUEUED, SYNCING, SYNCED, CONFLICT, FAILED }

data class OfflineOperation(
    val id: String,
    val organizationId: String,
    val entityType: String,
    val entityId: String,
    val operationType: String,
    val payloadVersion: Int,
    val createdByUserId: String,
    val createdAtEpochMs: Long,
    val idempotencyKey: String,
    val state: OfflineOperationState = OfflineOperationState.LOCAL,
    val retryCount: Int = 0,
    val lastError: String? = null,
)
