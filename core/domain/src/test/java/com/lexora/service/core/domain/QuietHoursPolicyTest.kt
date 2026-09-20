package com.lexora.service.core.domain

import com.lexora.service.core.model.DoNotDisturbPolicy
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class QuietHoursPolicyTest {
    @Test
    fun `cross midnight quiet hours defer until local end`() {
        val policy = DoNotDisturbPolicy(
            userId = "u1",
            enabled = true,
            startMinuteOfDay = 22 * 60,
            endMinuteOfDay = 7 * 60,
            timeZoneId = "UTC",
            allowCritical = true,
        )
        val now = Instant.parse("2026-09-20T23:30:00Z").toEpochMilli()
        val expected = Instant.parse("2026-09-21T07:00:00Z").toEpochMilli()
        assertEquals(expected, QuietHoursPolicy.nextAllowedDelivery(policy, now))
    }

    @Test
    fun `daytime quiet hours defer until same day end`() {
        val policy = DoNotDisturbPolicy(
            userId = "u1",
            enabled = true,
            startMinuteOfDay = 9 * 60,
            endMinuteOfDay = 17 * 60,
            timeZoneId = "UTC",
            allowCritical = true,
        )
        val now = Instant.parse("2026-09-20T12:00:00Z").toEpochMilli()
        val expected = Instant.parse("2026-09-20T17:00:00Z").toEpochMilli()
        assertEquals(expected, QuietHoursPolicy.nextAllowedDelivery(policy, now))
    }

    @Test
    fun `outside quiet hours returns current instant`() {
        val policy = DoNotDisturbPolicy(
            userId = "u1",
            enabled = true,
            startMinuteOfDay = 22 * 60,
            endMinuteOfDay = 7 * 60,
            timeZoneId = "Europe/Berlin",
            allowCritical = true,
        )
        val now = Instant.parse("2026-09-20T12:00:00Z").toEpochMilli()
        assertEquals(now, QuietHoursPolicy.nextAllowedDelivery(policy, now))
    }
}
