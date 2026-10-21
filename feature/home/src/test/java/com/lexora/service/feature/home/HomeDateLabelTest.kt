package com.lexora.service.feature.home

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeDateLabelTest {
    @Test
    fun formatsDateWithRussianWeekday() {
        assertEquals("09.10.2026, пт", formatCurrentDateLabel(LocalDate.of(2026, 10, 9)))
    }
}
