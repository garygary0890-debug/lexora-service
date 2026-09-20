package com.lexora.service.core.domain

import com.lexora.service.core.model.AdditionalWorkApprovalDecision
import com.lexora.service.core.model.AdditionalWorkApprovalEvent
import com.lexora.service.core.model.ServiceDocumentStatus

class WorkOrderCommercialPolicy {
    fun isCommerciallyFrozen(status: ServiceDocumentStatus): Boolean =
        status != ServiceDocumentStatus.DRAFT

    fun approvalEvent(
        id: String,
        organizationId: String,
        workOrderItemId: String,
        actorUserId: String,
        channel: String,
        payloadHash: String,
        decision: AdditionalWorkApprovalDecision,
        sentAtEpochMs: Long,
        decidedAtEpochMs: Long,
    ): AdditionalWorkApprovalEvent {
        require(id.isNotBlank() && organizationId.isNotBlank() && workOrderItemId.isNotBlank())
        require(actorUserId.isNotBlank() && channel.isNotBlank() && payloadHash.isNotBlank())
        require(sentAtEpochMs >= 0 && decidedAtEpochMs >= sentAtEpochMs)
        return AdditionalWorkApprovalEvent(
            id, organizationId, workOrderItemId, actorUserId, channel, payloadHash,
            decision, sentAtEpochMs, decidedAtEpochMs,
        )
    }
}
