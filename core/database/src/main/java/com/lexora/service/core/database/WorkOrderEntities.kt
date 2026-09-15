package com.lexora.service.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "work_order_items",
    indices = [
        Index("documentId"),
        Index("serviceCatalogItemId"),
        Index("approvalStatus"),
    ],
)
data class WorkOrderItemEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val serviceCatalogItemId: String?,
    val title: String,
    val quantity: Double,
    val unit: String,
    val unitPriceMinor: Long,
    val totalMinor: Long,
    val additional: Boolean,
    val approvalStatus: String,
    val approvalComment: String?,
    val approvedAtEpochMs: Long?,
    val syncState: String,
    val updatedAtEpochMs: Long,
)
