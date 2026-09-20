package com.lexora.service.feature.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationTimeInputTest {
    @Test
    fun `valid hh mm parses to minute of day`() {
        assertEquals(22 * 60, NotificationTimeInput.parseMinutes("22:00"))
        assertEquals(7 * 60 + 15, NotificationTimeInput.parseMinutes("07:15"))
    }

    @Test
    fun `invalid time is rejected`() {
        assertNull(NotificationTimeInput.parseMinutes("24:00"))
        assertNull(NotificationTimeInput.parseMinutes("7:5"))
        assertNull(NotificationTimeInput.parseMinutes("abc"))
    }
}
