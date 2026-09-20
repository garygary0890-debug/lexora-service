package com.lexora.service.core.domain

import com.lexora.service.core.model.DoNotDisturbPolicy
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object QuietHoursPolicy {
    fun nextAllowedDelivery(policy: DoNotDisturbPolicy, nowEpochMs: Long): Long {
        if (!policy.enabled) return nowEpochMs
        val start = policy.startMinuteOfDay ?: return nowEpochMs
        val end = policy.endMinuteOfDay ?: return nowEpochMs
        require(start in 0..1439) { "Quiet-hours start must be in 0..1439" }
        require(end in 0..1439) { "Quiet-hours end must be in 0..1439" }
        if (start == end) return nowEpochMs

        val zone = ZoneId.of(policy.timeZoneId?.takeIf { it.isNotBlank() } ?: "UTC")
        val now = Instant.ofEpochMilli(nowEpochMs).atZone(zone)
        val minute = now.hour * 60 + now.minute
        if (!NotificationPolicy.isQuietMinute(policy, minute)) return nowEpochMs

        val endHour = end / 60
        val endMinute = end % 60
        val endToday = now.toLocalDate().atTime(endHour, endMinute).atZone(zone)
        val boundary: ZonedDateTime = if (start < end) {
            endToday
        } else if (minute >= start) {
            endToday.plusDays(1)
        } else {
            endToday
        }
        return boundary.toInstant().toEpochMilli()
    }
}
