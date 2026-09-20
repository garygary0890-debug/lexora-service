package com.lexora.service.core.domain

import com.lexora.service.core.model.WashBooking
import com.lexora.service.core.model.WashBookingStatus
import com.lexora.service.core.model.WashPostStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WashBookingPolicyTest {
    private val policy = WashBookingPolicy()

    @Test
    fun `overlapping active booking on same post is rejected`() {
        val existing = listOf(WashBooking("b1", "org", "p1", "r1", 100, 200, WashBookingStatus.CONFIRMED))
        assertFalse(policy.canBook("p1", 150, 250, existing))
        assertTrue(policy.canBook("p1", 200, 300, existing))
        assertTrue(policy.canBook("p2", 150, 250, existing))
    }

    @Test
    fun `post lifecycle supports cleaning and maintenance without reopening directly`() {
        assertTrue(policy.canTransitionPost(WashPostStatus.OCCUPIED, WashPostStatus.CLEANING))
        assertTrue(policy.canTransitionPost(WashPostStatus.CLEANING, WashPostStatus.AVAILABLE))
        assertFalse(policy.canTransitionPost(WashPostStatus.MAINTENANCE, WashPostStatus.OCCUPIED))
    }
}
