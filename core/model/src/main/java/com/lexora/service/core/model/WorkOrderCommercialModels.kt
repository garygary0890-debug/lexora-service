package com.lexora.service.core.model

enum class AdditionalWorkApprovalDecision { APPROVED, REJECTED }

data class AdditionalWorkApprovalEvent(
    val id: String,
    val organizationId: String,
    val workOrderItemId: String,
    val actorUserId: String,
    val channel: String,
    val payloadHash: String,
    val decision: AdditionalWorkApprovalDecision,
    val sentAtEpochMs: Long,
    val decidedAtEpochMs: Long,
)
