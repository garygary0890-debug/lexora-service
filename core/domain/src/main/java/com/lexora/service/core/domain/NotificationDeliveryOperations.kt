package com.lexora.service.core.domain

import com.lexora.service.core.model.NotificationChannel
import com.lexora.service.core.model.NotificationDelivery
import com.lexora.service.core.model.NotificationPreferences
import com.lexora.service.core.model.ServiceNotificationPriority

interface NotificationDeliveryOperations {
    suspend fun preferences(organizationId: String, userId: String): NotificationPreferences
    suspend fun savePreferences(value: NotificationPreferences)
    suspend fun deliveries(organizationId: String, userId: String, limit: Int = 200): List<NotificationDelivery>
    suspend fun enqueue(
        organizationId: String,
        userId: String,
        channel: NotificationChannel,
        idempotencyKey: String,
        eventCode: String,
        title: String,
        body: String,
        priority: ServiceNotificationPriority,
        entityType: String?,
        entityId: String?,
        scheduledAtEpochMs: Long?,
    ): NotificationDelivery

    suspend fun enqueueLocal(
        organizationId: String,
        userId: String,
        idempotencyKey: String,
        eventCode: String,
        title: String,
        body: String,
        priority: ServiceNotificationPriority,
        entityType: String?,
        entityId: String?,
        scheduledAtEpochMs: Long?,
    ): NotificationDelivery = enqueue(
        organizationId = organizationId,
        userId = userId,
        channel = NotificationChannel.LOCAL,
        idempotencyKey = idempotencyKey,
        eventCode = eventCode,
        title = title,
        body = body,
        priority = priority,
        entityType = entityType,
        entityId = entityId,
        scheduledAtEpochMs = scheduledAtEpochMs,
    )

    suspend fun due(nowEpochMs: Long, limit: Int = 100): List<NotificationDelivery>
    suspend fun defer(id: String, nextAttemptAtEpochMs: Long)
    suspend fun markSent(id: String, sentAtEpochMs: Long)
    suspend fun markDelivered(id: String, deliveredAtEpochMs: Long)
    suspend fun markSuppressed(id: String, reason: String)
    suspend fun markFailed(id: String, error: String, nextAttemptAtEpochMs: Long?)
}
