package com.lexora.service.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PushDeliveryPolicyTest {
    @Test
    fun `transient failure schedules bounded exponential retry`() {
        val first = PushDeliveryPolicy.afterFailure(attemptCount = 0, nowEpochMs = 1_000L, result = PushProviderResult.TransientFailure("timeout"))
        val third = PushDeliveryPolicy.afterFailure(attemptCount = 2, nowEpochMs = 1_000L, result = PushProviderResult.TransientFailure("timeout"))
        assertEquals(PushDeliveryDecision.RETRY, first.decision)
        assertEquals(61_000L, first.nextAttemptAtEpochMs)
        assertEquals(241_000L, third.nextAttemptAtEpochMs)
    }

    @Test
    fun `permanent provider failure is terminal`() {
        val result = PushDeliveryPolicy.afterFailure(0, 1_000L, PushProviderResult.PermanentFailure("bad payload"))
        assertEquals(PushDeliveryDecision.TERMINAL_FAILURE, result.decision)
        assertEquals(null, result.nextAttemptAtEpochMs)
    }

    @Test
    fun `invalid token disables token and does not retry`() {
        val result = PushDeliveryPolicy.afterFailure(0, 1_000L, PushProviderResult.InvalidToken("gone"))
        assertEquals(PushDeliveryDecision.DISABLE_TOKEN, result.decision)
        assertTrue(result.disableToken)
        assertFalse(result.nextAttemptAtEpochMs != null)
    }

    @Test
    fun `retry stops at maximum attempts`() {
        val result = PushDeliveryPolicy.afterFailure(PushDeliveryPolicy.MAX_ATTEMPTS - 1, 1_000L, PushProviderResult.TransientFailure("timeout"))
        assertEquals(PushDeliveryDecision.TERMINAL_FAILURE, result.decision)
    }
}
