package com.lexora.service.core.data

import com.lexora.service.core.model.IntegrationOperation
import com.lexora.service.core.model.IntegrationOperationStatus

/** SRV-000092 — resilient integration operation tracking for external adapters. */
interface IntegrationOperationRepository {
    suspend fun operations(organizationId: String, status: IntegrationOperationStatus? = null): List<IntegrationOperation>
    suspend fun enqueue(operation: IntegrationOperation): IntegrationOperation
    suspend fun update(operation: IntegrationOperation): IntegrationOperation
    suspend fun findByIdempotencyKey(organizationId: String, idempotencyKey: String): IntegrationOperation?
}
