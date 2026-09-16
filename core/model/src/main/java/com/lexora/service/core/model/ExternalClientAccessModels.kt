package com.lexora.service.core.model

enum class ExternalAccessPurpose { VIEW_ORDER, APPROVE_ADDITIONAL_WORK, VIEW_DOCUMENT, ONLINE_BOOKING }

data class ExternalClientAccessGrant(
    val id: String,
    val organizationId: String,
    val clientId: String,
    val purpose: ExternalAccessPurpose,
    val entityType: String,
    val entityId: String,
    val tokenHash: String,
    val expiresAtEpochMs: Long,
    val singleUse: Boolean = true,
    val consumedAtEpochMs: Long? = null,
    val revokedAtEpochMs: Long? = null,
)

data class OnlineBookingRequest(
    val id: String,
    val organizationId: String,
    val branchId: String,
    val serviceCode: String,
    val vehicleId: String? = null,
    val vehicleCategoryCode: String? = null,
    val desiredStartAtEpochMs: Long,
    val contactName: String,
    val contactPhone: String,
    val idempotencyKey: String,
    val createdAtEpochMs: Long,
)
