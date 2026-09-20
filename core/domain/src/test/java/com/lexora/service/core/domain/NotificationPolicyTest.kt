package com.lexora.service.core.domain

import com.lexora.service.core.model.DoNotDisturbPolicy
import com.lexora.service.core.model.ServiceNotificationPriority
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPolicyTest {
    private val overnight = DoNotDisturbPolicy(
        userId = "u1",
        enabled = true,
        startMinuteOfDay = 22 * 60,
        endMinuteOfDay = 7 * 60,
        allowCritical = true,
    )

    @Test
    fun `cross midnight quiet hours include late evening and early morning`() {
        assertTrue(NotificationPolicy.isQuietMinute(overnight, 23 * 60 + 30))
        assertTrue(NotificationPolicy.isQuietMinute(overnight, 6 * 60 + 59))
        assertFalse(NotificationPolicy.isQuietMinute(overnight, 12 * 60))
    }

    @Test
    fun `critical notification bypasses quiet hours only when configured`() {
        assertFalse(NotificationPolicy.shouldDefer(overnight, 23 * 60, ServiceNotificationPriority.CRITICAL))
        assertTrue(NotificationPolicy.shouldDefer(overnight, 23 * 60, ServiceNotificationPriority.WARNING))
        assertTrue(
            NotificationPolicy.shouldDefer(
                overnight.copy(allowCritical = false),
                23 * 60,
                ServiceNotificationPriority.CRITICAL,
            ),
        )
    }

    @Test
    fun `disabled or invalid quiet hours never defer`() {
        assertFalse(NotificationPolicy.isQuietMinute(overnight.copy(enabled = false), 23 * 60))
        assertFalse(NotificationPolicy.isQuietMinute(overnight.copy(startMinuteOfDay = null), 23 * 60))
        assertFalse(NotificationPolicy.isQuietMinute(overnight.copy(endMinuteOfDay = null), 23 * 60))
    }
}
