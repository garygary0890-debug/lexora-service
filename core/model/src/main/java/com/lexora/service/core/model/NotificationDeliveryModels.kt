package com.lexora.service.core.model

enum class NotificationChannel { LOCAL, PUSH, EMAIL, SMS, MESSENGER }
enum class NotificationDeliveryStatus { PENDING, SENT, DELIVERED, FAILED, SUPPRESSED }

data class NotificationDelivery(
    val id: String,
    val organizationId: String,
    val userId: String,
    val channel: NotificationChannel,
    val eventCode: String,
    val entityType: String? = null,
    val entityId: String? = null,
    val scheduledAtEpochMs: Long? = null,
    val status: NotificationDeliveryStatus = NotificationDeliveryStatus.PENDING,
    val attemptCount: Int = 0,
    val lastError: String? = null,
)

data class DoNotDisturbPolicy(
    val userId: String,
    val enabled: Boolean,
    val startMinuteOfDay: Int? = null,
    val endMinuteOfDay: Int? = null,
    val allowCritical: Boolean = true,
)
