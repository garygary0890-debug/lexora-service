package com.lexora.service.core.model

data class ServiceBranch(
    val id: String,
    val organizationId: String,
    val name: String,
    val address: String,
    val timeZoneId: String,
    val phone: String? = null,
    val email: String? = null,
    val workScheduleCode: String? = null,
    val active: Boolean = true,
)

data class UserBranchAccess(
    val userId: String,
    val organizationId: String,
    val branchIds: Set<String>,
    val consolidatedReportingAllowed: Boolean = false,
)

data class BranchTransfer(
    val id: String,
    val organizationId: String,
    val entityType: String,
    val entityId: String,
    val sourceBranchId: String?,
    val targetBranchId: String,
    val actorUserId: String,
    val reason: String,
    val transferredAtEpochMs: Long,
)
