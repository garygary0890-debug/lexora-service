package com.lexora.service.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "additional_work_approval_events",
    indices = [Index("organizationId"), Index("workOrderItemId"), Index("decidedAtEpochMs")],
)
data class AdditionalWorkApprovalEventEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val workOrderItemId: String,
    val actorUserId: String,
    val channel: String,
    val payloadHash: String,
    val decision: String,
    val sentAtEpochMs: Long,
    val decidedAtEpochMs: Long,
)
