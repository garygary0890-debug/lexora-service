package com.lexora.service.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lexora.service.core.database.NotificationRuntimeDatabase
import com.lexora.service.core.domain.NotificationPolicy
import com.lexora.service.core.domain.PushDeliveryDecision
import com.lexora.service.core.domain.PushDeliveryPolicy
import com.lexora.service.core.domain.PushTokenPolicy
import com.lexora.service.core.domain.QuietHoursPolicy
import com.lexora.service.core.model.DoNotDisturbPolicy
import com.lexora.service.core.model.NotificationChannel
import com.lexora.service.core.model.NotificationDelivery
import com.lexora.service.core.model.NotificationDeliveryStatus
import com.lexora.service.core.model.PushDeviceToken
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
        val channel = NotificationChannel.valueOf(entity.channel)
        val prefs = dao.preferences(entity.organizationId, entity.userId)
        if (channel == NotificationChannel.LOCAL && prefs != null && !prefs.localEnabled) {
            dao.updateState(id, NotificationDeliveryStatus.SUPPRESSED.name, null, null, entity.attemptCount, "local_notifications_disabled", now)
            return Result.success()
        }
        if (channel == NotificationChannel.PUSH && prefs != null && !prefs.pushEnabled) {
            dao.updateState(id, NotificationDeliveryStatus.SUPPRESSED.name, null, null, entity.attemptCount, "push_notifications_disabled", now)
            return Result.success()
        }
        if (channel !in setOf(NotificationChannel.LOCAL, NotificationChannel.PUSH)) return Result.success()

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

        return when (channel) {
            NotificationChannel.LOCAL -> deliverLocal(entity.toModel(), entity.attemptCount, id, now)
            NotificationChannel.PUSH -> deliverPush(entity.toModel(), entity.attemptCount, id, now)
            else -> Result.success()
        }
    }

    private suspend fun deliverLocal(delivery: NotificationDelivery, attempts: Int, id: String, now: Long): Result {
        val dao = NotificationRuntimeDatabase.create(applicationContext).notificationDeliveryDao()
        val publisher = AndroidNotificationPublisher(applicationContext)
        if (!publisher.publish(delivery)) {
            dao.updateState(id, NotificationDeliveryStatus.FAILED.name, null, null, attempts + 1, "notification_permission_denied", now)
            return Result.failure()
        }
        dao.updateState(id, NotificationDeliveryStatus.DELIVERED.name, null, now, attempts, null, now)
        return Result.success()
    }

    private suspend fun deliverPush(delivery: NotificationDelivery, attempts: Int, id: String, now: Long): Result {
        val dao = NotificationRuntimeDatabase.create(applicationContext).notificationDeliveryDao()
        val tokenEntities = dao.pushTokens(delivery.organizationId, delivery.userId)
        val token = PushTokenPolicy().select(tokenEntities.map { it.toModel() })
        if (token == null) {
            dao.updateState(id, NotificationDeliveryStatus.SUPPRESSED.name, null, null, attempts, "push_token_missing", now)
            return Result.success()
        }
        val result = PushProviderRuntime.current().send(delivery, token)
        val outcome = PushDeliveryPolicy.afterFailure(attempts, now, result)
        return when (outcome.decision) {
            PushDeliveryDecision.DELIVERED -> {
                dao.updateState(id, NotificationDeliveryStatus.DELIVERED.name, null, now, attempts, null, now)
                Result.success()
            }
            PushDeliveryDecision.RETRY -> {
                val next = requireNotNull(outcome.nextAttemptAtEpochMs)
                dao.updateState(id, NotificationDeliveryStatus.FAILED.name, next, null, attempts + 1, "push_transient_failure", now)
                NotificationScheduler.schedule(applicationContext, delivery.copy(nextAttemptAtEpochMs = next, attemptCount = attempts + 1))
                Result.success()
            }
            PushDeliveryDecision.DISABLE_TOKEN -> {
                dao.deactivatePushToken(token.id, now)
                dao.updateState(id, NotificationDeliveryStatus.SUPPRESSED.name, null, null, attempts + 1, "push_invalid_token", now)
                Result.success()
            }
            PushDeliveryDecision.TERMINAL_FAILURE -> {
                dao.updateState(id, NotificationDeliveryStatus.FAILED.name, null, null, attempts + 1, "push_terminal_failure", now)
                Result.success()
            }
        }
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

private fun com.lexora.service.core.database.PushDeviceTokenEntity.toModel() = PushDeviceToken(
    id = id,
    organizationId = organizationId,
    userId = userId,
    deviceId = deviceId,
    provider = provider,
    token = token,
    active = active,
    updatedAtEpochMs = updatedAtEpochMs,
)
