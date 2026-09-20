package com.lexora.service.core.domain

import com.lexora.service.core.model.DoNotDisturbPolicy
import com.lexora.service.core.model.ServiceNotificationPriority

object NotificationPolicy {
    fun isQuietMinute(policy: DoNotDisturbPolicy, minuteOfDay: Int): Boolean {
        if (!policy.enabled || minuteOfDay !in 0..1439) return false
        val start = policy.startMinuteOfDay ?: return false
        val end = policy.endMinuteOfDay ?: return false
        if (start !in 0..1439 || end !in 0..1439) return false
        if (start == end) return true
        return if (start < end) {
            minuteOfDay >= start && minuteOfDay < end
        } else {
            minuteOfDay >= start || minuteOfDay < end
        }
    }

    fun shouldDefer(
        policy: DoNotDisturbPolicy,
        minuteOfDay: Int,
        priority: ServiceNotificationPriority,
    ): Boolean {
        if (!isQuietMinute(policy, minuteOfDay)) return false
        return priority != ServiceNotificationPriority.CRITICAL || !policy.allowCritical
    }
}
