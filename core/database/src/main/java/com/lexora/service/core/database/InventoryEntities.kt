package com.lexora.service.core.database

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "inventory_locations",
    primaryKeys = ["id"],
    indices = [Index("organizationId"), Index("branchId"), Index(value = ["organizationId", "name"], unique = true)],
)
data class InventoryLocationEntity(
    val id: String,
    val organizationId: String,
    val branchId: String?,
    val name: String,
    val active: Boolean,
)

@Entity(
    tableName = "inventory_items",
    primaryKeys = ["id"],
    indices = [Index("organizationId"), Index(value = ["organizationId", "sku"], unique = true), Index("active")],
)
data class InventoryItemEntity(
    val id: String,
    val organizationId: String,
    val sku: String,
    val name: String,
    val unit: String,
    val minimumStock: Double?,
    val active: Boolean,
)

@Entity(
    tableName = "inventory_balances",
    primaryKeys = ["organizationId", "locationId", "itemId"],
    indices = [Index("locationId"), Index("itemId")],
)
data class InventoryBalanceEntity(
    val organizationId: String,
    val locationId: String,
    val itemId: String,
    val quantity: Double,
    val reservedQuantity: Double,
)

@Entity(
    tableName = "inventory_movements",
    primaryKeys = ["id"],
    indices = [Index("organizationId"), Index("locationId"), Index("targetLocationId"), Index("itemId"), Index("requestId"), Index("workOrderDocumentId"), Index("occurredAtEpochMs")],
)
data class InventoryMovementEntity(
    val id: String,
    val organizationId: String,
    val locationId: String,
    val targetLocationId: String?,
    val itemId: String,
    val requestId: String?,
    val workOrderDocumentId: String?,
    val type: String,
    val quantity: Double,
    val occurredAtEpochMs: Long,
    val actorUserId: String,
    val note: String?,
)
