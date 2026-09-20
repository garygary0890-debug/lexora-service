package com.lexora.service.notifications

object NotificationPermissionPolicy {
    fun requiresRuntimePermission(sdkInt: Int, granted: Boolean): Boolean =
        sdkInt >= 33 && !granted
}
