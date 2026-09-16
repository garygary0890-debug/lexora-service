package com.lexora.service.core.model

enum class ExchangeDirection { IMPORT, EXPORT, WEBHOOK_OUT, WEBHOOK_IN }
enum class ExchangeStatus { PENDING, PROCESSING, SUCCEEDED, PARTIAL, FAILED, RETRY_WAIT }

data class IntegrationEnvelope(
    val id: String,
    val organizationId: String,
    val providerCode: String,
    val direction: ExchangeDirection,
    val contractVersion: String,
    val entityType: String? = null,
    val entityId: String? = null,
    val idempotencyKey: String,
    val status: ExchangeStatus = ExchangeStatus.PENDING,
    val attemptCount: Int = 0,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val lastError: String? = null,
)

data class TabularExchangeRequest(
    val id: String,
    val organizationId: String,
    val entityType: String,
    val fileName: String,
    val mimeType: String,
    val requestedByUserId: String,
    val requestedAtEpochMs: Long,
    val direction: ExchangeDirection,
    val branchId: String? = null,
)
