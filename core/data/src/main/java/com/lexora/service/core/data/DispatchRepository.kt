package com.lexora.service.core.data

import com.lexora.service.core.model.DispatchCandidate
import com.lexora.service.core.model.DispatchDecision

/** SRV-000088 — automatic dispatch foundation. */
interface DispatchRepository {
    suspend fun candidates(organizationId: String, requestId: String): List<DispatchCandidate>
    suspend fun evaluate(organizationId: String, requestId: String, candidates: List<DispatchCandidate>): List<DispatchDecision>
    suspend fun assign(organizationId: String, decision: DispatchDecision): DispatchDecision
}
