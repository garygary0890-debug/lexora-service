package com.lexora.service.core.data

import com.lexora.service.core.model.SlaEvaluation
import com.lexora.service.core.model.SlaRule
import com.lexora.service.core.model.ServiceRequest

/** SRV-000090 — advanced SLA and service contract evaluation. */
interface SlaRepository {
    suspend fun rules(organizationId: String, includeInactive: Boolean = false): List<SlaRule>
    suspend fun saveRule(rule: SlaRule): SlaRule
    suspend fun evaluate(request: ServiceRequest, nowEpochMs: Long): SlaEvaluation
    suspend fun atRiskRequests(organizationId: String, nowEpochMs: Long): List<SlaEvaluation>
}
