package com.lexora.service.notifications

import com.lexora.service.core.model.NotificationChannel
import com.lexora.service.core.model.NotificationDelivery
import com.lexora.service.core.model.NotificationDeliveryStatus
import com.lexora.service.core.model.ServiceNotificationPriority
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationSchedulerTest {
    @Test
    fun `due item schedules immediately`() {
        val now = 1_000L
        val item = delivery(nextAttempt = null)
        assertEquals(0L, NotificationSchedulePolicy.initialDelayMillis(item, now))
    }

    @Test
    fun `deferred item schedules at next attempt`() {
        val now = 1_000L
        val item = delivery(nextAttempt = 7_000L)
        assertEquals(6_000L, NotificationSchedulePolicy.initialDelayMillis(item, now))
    }

    @Test
    fun `unique work name is stable per delivery`() {
        assertEquals("notification:d1", NotificationSchedulePolicy.uniqueWorkName("d1"))
    }

    private fun delivery(nextAttempt: Long?) = NotificationDelivery(
        id = "d1",
        organizationId = "o1",
        userId = "u1",
        channel = NotificationChannel.LOCAL,
        eventCode = "TASK_REMINDER",
        title = "Напоминание",
        body = "Задача",
        priority = ServiceNotificationPriority.WARNING,
        nextAttemptAtEpochMs = nextAttempt,
        status = NotificationDeliveryStatus.PENDING,
    )
}
