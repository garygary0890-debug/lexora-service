package com.lexora.service.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionPolicyTest {
    @Test
    fun `android 13 and newer requests runtime permission when not granted`() {
        assertTrue(NotificationPermissionPolicy.requiresRuntimePermission(sdkInt = 33, granted = false))
        assertTrue(NotificationPermissionPolicy.requiresRuntimePermission(sdkInt = 36, granted = false))
    }

    @Test
    fun `older android and already granted states do not request permission`() {
        assertFalse(NotificationPermissionPolicy.requiresRuntimePermission(sdkInt = 32, granted = false))
        assertFalse(NotificationPermissionPolicy.requiresRuntimePermission(sdkInt = 36, granted = true))
    }
}
