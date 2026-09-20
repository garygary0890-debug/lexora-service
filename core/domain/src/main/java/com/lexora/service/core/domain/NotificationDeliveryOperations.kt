package com.lexora.service.core.domain

import com.lexora.service.core.model.NotificationDelivery
import com.lexora.service.core.model.NotificationPreferences
import com.lexora.service.core.model.ServiceNotificationPriority

interface NotificationDeliveryOperations {
    suspend fun preferences(organizationId: String, userId: String): NotificationPreferences
    suspend fun savePreferences(value: NotificationPreferences)
    suspend fun deliveries(organizationId: String, userId: String, limit: Int = 200): List<NotificationDelivery>
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
    ): NotificationDelivery
    suspend fun due(nowEpochMs: Long, limit: Int = 100): List<NotificationDelivery>
    suspend fun defer(id: String, nextAttemptAtEpochMs: Long)
    suspend fun markDelivered(id: String, deliveredAtEpochMs: Long)
    suspend fun markFailed(id: String, error: String, nextAttemptAtEpochMs: Long?)
}
