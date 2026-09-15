package com.lexora.service.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "public_bookings",
    indices = [
        Index("organizationId"),
        Index("branchId"),
        Index("clientId"),
        Index("vehicleId"),
        Index("serviceCatalogItemId"),
        Index("status"),
        Index("desiredAtEpochMs"),
        Index("convertedRequestId"),
    ],
)
data class PublicBookingEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val branchId: String?,
    val clientId: String?,
    val vehicleId: String?,
    val serviceCatalogItemId: String?,
    val contactName: String,
    val phone: String?,
    val email: String?,
    val desiredAtEpochMs: Long?,
    val comment: String?,
    val status: String,
    val convertedRequestId: String?,
    val source: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val syncState: String,
)

@Entity(
    tableName = "portal_access_grants",
    indices = [
        Index("organizationId"),
        Index("clientId"),
        Index("status"),
        Index("expiresAtEpochMs"),
        Index(value = ["organizationId", "tokenHash"], unique = true),
    ],
)
data class PortalAccessGrantEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val clientId: String,
    val tokenHash: String,
    val status: String,
    val expiresAtEpochMs: Long,
    val revokedAtEpochMs: Long?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val syncState: String,
)
