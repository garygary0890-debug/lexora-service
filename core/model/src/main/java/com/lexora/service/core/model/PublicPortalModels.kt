package com.lexora.service.core.model

enum class PublicBookingStatus {
    NEW,
    CONFIRMED,
    CONVERTED_TO_REQUEST,
    CANCELLED,
    REJECTED,
}

enum class PortalAccessStatus {
    ACTIVE,
    REVOKED,
    EXPIRED,
}

data class PublicBooking(
    val id: String,
    val organizationId: String,
    val branchId: String? = null,
    val clientId: String? = null,
    val vehicleId: String? = null,
    val serviceCatalogItemId: String? = null,
    val contactName: String,
    val phone: String? = null,
    val email: String? = null,
    val desiredAtEpochMs: Long? = null,
    val comment: String? = null,
    val status: PublicBookingStatus = PublicBookingStatus.NEW,
    val convertedRequestId: String? = null,
    val source: String = "WEB",
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val syncState: SyncState = SyncState.PENDING_CREATE,
)

data class PortalAccessGrant(
    val id: String,
    val organizationId: String,
    val clientId: String,
    val tokenHash: String,
    val status: PortalAccessStatus = PortalAccessStatus.ACTIVE,
    val expiresAtEpochMs: Long,
    val revokedAtEpochMs: Long? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val syncState: SyncState = SyncState.PENDING_CREATE,
)
