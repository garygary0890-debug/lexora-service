package com.lexora.service.core.model

enum class NotificationChannel { LOCAL, PUSH, EMAIL, SMS, MESSENGER }
enum class NotificationDeliveryStatus { PENDING, DEFERRED, SENT, DELIVERED, FAILED, SUPPRESSED }

data class NotificationDelivery(
    val id: String,
    val organizationId: String,
    val userId: String,
    val channel: NotificationChannel,
    val eventCode: String,
    val title: String,
    val body: String,
    val priority: ServiceNotificationPriority,
    val entityType: String? = null,
    val entityId: String? = null,
    val scheduledAtEpochMs: Long? = null,
    val nextAttemptAtEpochMs: Long? = null,
    val deliveredAtEpochMs: Long? = null,
    val status: NotificationDeliveryStatus = NotificationDeliveryStatus.PENDING,
    val attemptCount: Int = 0,
    val lastError: String? = null,
)

data class PushDeviceToken(
    val id: String,
    val organizationId: String,
    val userId: String,
    val deviceId: String,
    val provider: String,
    val token: String,
    val active: Boolean = true,
    val updatedAtEpochMs: Long,
)

data class DoNotDisturbPolicy(
    val userId: String,
    val enabled: Boolean,
    val startMinuteOfDay: Int? = null,
    val endMinuteOfDay: Int? = null,
    val timeZoneId: String? = null,
    val allowCritical: Boolean = true,
)

data class NotificationPreferences(
    val organizationId: String,
    val userId: String,
    val doNotDisturb: DoNotDisturbPolicy = DoNotDisturbPolicy(userId = userId, enabled = false),
    val localEnabled: Boolean = true,
    val pushEnabled: Boolean = true,
    val taskRemindersEnabled: Boolean = true,
    val slaEnabled: Boolean = true,
    val contractsEnabled: Boolean = true,
    val tireStorageEnabled: Boolean = true,
)
