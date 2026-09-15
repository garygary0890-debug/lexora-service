package com.lexora.service.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "loyalty_accounts",
    indices = [Index("organizationId"), Index("clientId"), Index(value = ["organizationId", "clientId"], unique = true)],
)
data class LoyaltyAccountEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val clientId: String,
    val pointsBalance: Long,
    val active: Boolean,
    val syncState: String,
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "loyalty_transactions",
    indices = [Index("organizationId"), Index("clientId"), Index("accountId"), Index("requestId"), Index("paymentId"), Index("occurredAtEpochMs")],
)
data class LoyaltyTransactionEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val clientId: String,
    val accountId: String,
    val requestId: String?,
    val paymentId: String?,
    val type: String,
    val pointsDelta: Long,
    val comment: String?,
    val occurredAtEpochMs: Long,
    val syncState: String,
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "quality_control",
    indices = [Index("organizationId"), Index("requestId"), Index("clientId"), Index("vehicleId"), Index("status"), Index("controlledAtEpochMs")],
)
data class QualityControlRecordEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val requestId: String,
    val clientId: String?,
    val vehicleId: String?,
    val status: String,
    val rating: Int?,
    val checklistResult: String?,
    val issueDescription: String?,
    val resolutionNote: String?,
    val controlledAtEpochMs: Long?,
    val syncState: String,
    val updatedAtEpochMs: Long,
)
