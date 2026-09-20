package com.lexora.service.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lexora.service.core.database.NotificationRuntimeDatabase
import com.lexora.service.core.domain.NotificationPolicy
import com.lexora.service.core.domain.QuietHoursPolicy
import com.lexora.service.core.model.DoNotDisturbPolicy
import com.lexora.service.core.model.NotificationChannel
import com.lexora.service.core.model.NotificationDelivery
import com.lexora.service.core.model.NotificationDeliveryStatus
import com.lexora.service.core.model.ServiceNotificationPriority
import java.time.Instant
import java.time.ZoneId

class NotificationDeliveryWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_DELIVERY_ID) ?: return Result.failure()
        val dao = NotificationRuntimeDatabase.create(applicationContext).notificationDeliveryDao()
        val entity = dao.delivery(id) ?: return Result.success()
        if (entity.status in setOf(
                NotificationDeliveryStatus.DELIVERED.name,
                NotificationDeliveryStatus.SUPPRESSED.name,
                NotificationDeliveryStatus.SENT.name,
            )
        ) return Result.success()

        val now = System.currentTimeMillis()
        if (entity.channel != NotificationChannel.LOCAL.name) return Result.success()
        val prefs = dao.preferences(entity.organizationId, entity.userId)
        if (prefs != null && !prefs.localEnabled) {
            dao.updateState(id, NotificationDeliveryStatus.SUPPRESSED.name, null, null, entity.attemptCount, "local_notifications_disabled", now)
            return Result.success()
        }

        val policy = DoNotDisturbPolicy(
            userId = entity.userId,
            enabled = prefs?.dndEnabled ?: false,
            startMinuteOfDay = prefs?.dndStartMinuteOfDay,
            endMinuteOfDay = prefs?.dndEndMinuteOfDay,
            timeZoneId = prefs?.timeZoneId,
            allowCritical = prefs?.allowCritical ?: true,
        )
        val zone = runCatching { ZoneId.of(policy.timeZoneId ?: "UTC") }.getOrDefault(ZoneId.of("UTC"))
        val local = Instant.ofEpochMilli(now).atZone(zone)
        val priority = ServiceNotificationPriority.valueOf(entity.priority)
        if (NotificationPolicy.shouldDefer(policy, local.hour * 60 + local.minute, priority)) {
            val next = QuietHoursPolicy.nextAllowedDelivery(policy, now)
            dao.updateState(id, NotificationDeliveryStatus.DEFERRED.name, next, null, entity.attemptCount, null, now)
            NotificationScheduler.schedule(applicationContext, entity.toModel(nextAttemptOverride = next))
            return Result.success()
        }

        val publisher = AndroidNotificationPublisher(applicationContext)
        if (!publisher.publish(entity.toModel())) {
            dao.updateState(id, NotificationDeliveryStatus.FAILED.name, null, null, entity.attemptCount + 1, "notification_permission_denied", now)
            return Result.failure()
        }
        dao.updateState(id, NotificationDeliveryStatus.DELIVERED.name, null, now, entity.attemptCount, null, now)
        return Result.success()
    }

    companion object {
        const val KEY_DELIVERY_ID = "deliveryId"
    }
}

private fun com.lexora.service.core.database.NotificationDeliveryEntity.toModel(
    nextAttemptOverride: Long? = nextAttemptAtEpochMs,
) = NotificationDelivery(
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
    nextAttemptAtEpochMs = nextAttemptOverride,
    deliveredAtEpochMs = deliveredAtEpochMs,
    status = NotificationDeliveryStatus.valueOf(status),
    attemptCount = attemptCount,
    lastError = lastError,
)
