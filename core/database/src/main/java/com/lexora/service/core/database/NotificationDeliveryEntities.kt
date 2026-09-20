package com.lexora.service.core.database

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "notification_preferences",
    primaryKeys = ["organizationId", "userId"],
    indices = [Index("organizationId"), Index("userId")],
)
data class NotificationPreferenceEntity(
    val organizationId: String,
    val userId: String,
    val dndEnabled: Boolean,
    val dndStartMinuteOfDay: Int?,
    val dndEndMinuteOfDay: Int?,
    val timeZoneId: String?,
    val allowCritical: Boolean,
    val localEnabled: Boolean,
    val pushEnabled: Boolean,
    val taskRemindersEnabled: Boolean,
    val slaEnabled: Boolean,
    val contractsEnabled: Boolean,
    val tireStorageEnabled: Boolean,
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "notification_deliveries",
    indices = [
        Index("organizationId"), Index("userId"), Index("status"),
        Index("nextAttemptAtEpochMs"), Index(value = ["organizationId", "idempotencyKey", "channel"], unique = true),
    ],
)
data class NotificationDeliveryEntity(
    @androidx.room.PrimaryKey val id: String,
    val organizationId: String,
    val userId: String,
    val channel: String,
    val idempotencyKey: String,
    val eventCode: String,
    val title: String,
    val body: String,
    val priority: String,
    val entityType: String?,
    val entityId: String?,
    val scheduledAtEpochMs: Long?,
    val nextAttemptAtEpochMs: Long?,
    val deliveredAtEpochMs: Long?,
    val status: String,
    val attemptCount: Int,
    val lastError: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
