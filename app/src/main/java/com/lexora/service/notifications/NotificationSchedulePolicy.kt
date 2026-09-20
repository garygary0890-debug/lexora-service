package com.lexora.service.notifications

import com.lexora.service.core.model.NotificationDelivery

object NotificationSchedulePolicy {
    fun uniqueWorkName(deliveryId: String): String = "notification:$deliveryId"

    fun initialDelayMillis(delivery: NotificationDelivery, nowEpochMs: Long): Long {
        val target = delivery.nextAttemptAtEpochMs ?: delivery.scheduledAtEpochMs ?: nowEpochMs
        return (target - nowEpochMs).coerceAtLeast(0L)
    }
}
