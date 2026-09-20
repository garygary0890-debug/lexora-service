package com.lexora.service.core.data

import com.lexora.service.core.database.NotificationDeliveryDao
import com.lexora.service.core.database.NotificationDeliveryEntity
import com.lexora.service.core.database.NotificationPreferenceEntity
import com.lexora.service.core.domain.NotificationDeliveryOperations
import com.lexora.service.core.model.DoNotDisturbPolicy
import com.lexora.service.core.model.NotificationChannel
import com.lexora.service.core.model.NotificationDelivery
import com.lexora.service.core.model.NotificationDeliveryStatus
import com.lexora.service.core.model.NotificationPreferences
import com.lexora.service.core.model.ServiceNotificationPriority
import java.util.UUID

class PersistentNotificationDeliveryRepository(
    private val dao: NotificationDeliveryDao,
) : NotificationDeliveryOperations {
    override suspend fun preferences(organizationId: String, userId: String): NotificationPreferences =
        dao.preferences(organizationId, userId)?.toModel()
            ?: NotificationPreferences(organizationId = organizationId, userId = userId)

    override suspend fun savePreferences(value: NotificationPreferences) {
        dao.upsertPreferences(value.toEntity(System.currentTimeMillis()))
    }

    override suspend fun deliveries(organizationId: String, userId: String, limit: Int): List<NotificationDelivery> =
        dao.deliveries(organizationId, userId, limit).map(NotificationDeliveryEntity::toModel)

    override suspend fun enqueueLocal(
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
    ): NotificationDelivery {
        val now = System.currentTimeMillis()
        val entity = NotificationDeliveryEntity(
            id = UUID.randomUUID().toString(),
            organizationId = organizationId,
            userId = userId,
            channel = NotificationChannel.LOCAL.name,
            idempotencyKey = idempotencyKey,
            eventCode = eventCode,
            title = title,
            body = body,
            priority = priority.name,
            entityType = entityType,
            entityId = entityId,
            scheduledAtEpochMs = scheduledAtEpochMs,
            nextAttemptAtEpochMs = scheduledAtEpochMs,
            deliveredAtEpochMs = null,
            status = NotificationDeliveryStatus.PENDING.name,
            attemptCount = 0,
            lastError = null,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
        )
        val inserted = dao.enqueue(entity)
        if (inserted != -1L) return entity.toModel()
        return dao.deliveries(organizationId, userId, 500)
            .firstOrNull { it.idempotencyKey == idempotencyKey && it.channel == NotificationChannel.LOCAL.name }
            ?.toModel() ?: error("Notification delivery idempotency lookup failed")
    }

    override suspend fun due(nowEpochMs: Long, limit: Int): List<NotificationDelivery> =
        dao.due(nowEpochMs, limit).map(NotificationDeliveryEntity::toModel)

    override suspend fun defer(id: String, nextAttemptAtEpochMs: Long) = update(
        id, NotificationDeliveryStatus.DEFERRED, nextAttemptAtEpochMs, null, null,
    )

    override suspend fun markDelivered(id: String, deliveredAtEpochMs: Long) = update(
        id, NotificationDeliveryStatus.DELIVERED, null, deliveredAtEpochMs, null,
    )

    override suspend fun markFailed(id: String, error: String, nextAttemptAtEpochMs: Long?) = update(
        id, NotificationDeliveryStatus.FAILED, nextAttemptAtEpochMs, null, error,
    )

    private suspend fun update(
        id: String,
        status: NotificationDeliveryStatus,
        nextAttemptAt: Long?,
        deliveredAt: Long?,
        error: String?,
    ) {
        val current = dao.delivery(id) ?: return
        dao.updateState(
            id = id,
            status = status.name,
            nextAttemptAt = nextAttemptAt,
            deliveredAt = deliveredAt,
            attemptCount = current.attemptCount + if (status == NotificationDeliveryStatus.FAILED) 1 else 0,
            lastError = error,
            updatedAt = System.currentTimeMillis(),
        )
    }
}

private fun NotificationPreferenceEntity.toModel() = NotificationPreferences(
    organizationId = organizationId,
    userId = userId,
    doNotDisturb = DoNotDisturbPolicy(
        userId = userId,
        enabled = dndEnabled,
        startMinuteOfDay = dndStartMinuteOfDay,
        endMinuteOfDay = dndEndMinuteOfDay,
        timeZoneId = timeZoneId,
        allowCritical = allowCritical,
    ),
    localEnabled = localEnabled,
    pushEnabled = pushEnabled,
    taskRemindersEnabled = taskRemindersEnabled,
    slaEnabled = slaEnabled,
    contractsEnabled = contractsEnabled,
    tireStorageEnabled = tireStorageEnabled,
)

private fun NotificationPreferences.toEntity(now: Long) = NotificationPreferenceEntity(
    organizationId = organizationId,
    userId = userId,
    dndEnabled = doNotDisturb.enabled,
    dndStartMinuteOfDay = doNotDisturb.startMinuteOfDay,
    dndEndMinuteOfDay = doNotDisturb.endMinuteOfDay,
    timeZoneId = doNotDisturb.timeZoneId,
    allowCritical = doNotDisturb.allowCritical,
    localEnabled = localEnabled,
    pushEnabled = pushEnabled,
    taskRemindersEnabled = taskRemindersEnabled,
    slaEnabled = slaEnabled,
    contractsEnabled = contractsEnabled,
    tireStorageEnabled = tireStorageEnabled,
    updatedAtEpochMs = now,
)

private fun NotificationDeliveryEntity.toModel() = NotificationDelivery(
    id = id,
    organizationId = organizationId,
    userId = userId,
    channel = NotificationChannel.valueOf(channel),
    eventCode = eventCode,
    title = title,
    body = body,
    priority = ServiceNotificationPriority.valueOf(priority),
    entityType = entityType,
    entityId = entityId,
    scheduledAtEpochMs = scheduledAtEpochMs,
    nextAttemptAtEpochMs = nextAttemptAtEpochMs,
    deliveredAtEpochMs = deliveredAtEpochMs,
    status = NotificationDeliveryStatus.valueOf(status),
    attemptCount = attemptCount,
    lastError = lastError,
)
