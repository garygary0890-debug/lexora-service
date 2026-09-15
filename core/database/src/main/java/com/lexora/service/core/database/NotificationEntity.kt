package com.lexora.service.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "service_notifications",
    indices = [
        Index("organizationId"),
        Index("type"),
        Index("priority"),
        Index("entityType"),
        Index("entityId"),
        Index("scheduledAtEpochMs"),
        Index("occurredAtEpochMs"),
        Index("readAtEpochMs"),
        Index("archived"),
    ],
)
data class ServiceNotificationEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val type: String,
    val priority: String,
    val title: String,
    val message: String,
    val entityType: String?,
    val entityId: String?,
    val scheduledAtEpochMs: Long?,
    val occurredAtEpochMs: Long,
    val readAtEpochMs: Long?,
    val archived: Boolean,
    val syncState: String,
    val updatedAtEpochMs: Long,
)
