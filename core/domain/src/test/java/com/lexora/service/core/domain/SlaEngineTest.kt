package com.lexora.service.core.domain

import com.lexora.service.core.model.RequestPriority
import com.lexora.service.core.model.ServiceRequest
import com.lexora.service.core.model.SlaRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlaEngineTest {
    private val engine = SlaEngine()
    private val created = 1_000_000L
    private val request = ServiceRequest(
        id = "request-1",
        organizationId = "org-1",
        number = "REQ-1",
        clientId = null,
        title = "Test",
        priority = RequestPriority.NORMAL,
        createdAtEpochMs = created,
    )
    private val rule = SlaRule(
        id = "sla-1",
        organizationId = "org-1",
        reactionMinutes = 30,
        resolutionMinutes = 120,
        businessHoursOnly = false,
    )

    @Test
    fun calculatesReactionAndResolutionDeadlines() {
        val result = engine.evaluate(request, rule, created, null, created + 10 * 60_000L, 15)
        assertFalse(result.breached)
        assertFalse(result.atRisk)
        assertTrue(result.reactionDeadlineEpochMs == created + 30 * 60_000L)
        assertTrue(result.resolutionDeadlineEpochMs == created + 120 * 60_000L)
    }

    @Test
    fun warnsBeforeReactionDeadline() {
        val result = engine.evaluate(request, rule, created, null, created + 20 * 60_000L, 15)
        assertTrue(result.atRisk)
        assertTrue(result.reactionAtRisk)
        assertFalse(result.reactionBreached)
    }

    @Test
    fun completedReactionStopsReactionWarning() {
        val result = engine.evaluate(request, rule, created, created + 5 * 60_000L, created + 25 * 60_000L, 15)
        assertFalse(result.reactionAtRisk)
        assertFalse(result.reactionBreached)
    }

    @Test
    fun detectsResolutionBreach() {
        val result = engine.evaluate(request, rule, created, created + 5 * 60_000L, created + 121 * 60_000L, 15)
        assertTrue(result.breached)
        assertTrue(result.resolutionBreached)
    }
    @Test
    fun detectsReactionBreachBeforeResolutionDeadline() {
        val result = engine.evaluate(request, rule, created, null, created + 31 * 60_000L, 15)
        assertTrue(result.breached)
        assertTrue(result.reactionBreached)
        assertFalse(result.resolutionBreached)
    }

    @Test
    fun closedOnTimeDoesNotBreachResolution() {
        val closed = request.copy(closedAtEpochMs = created + 90 * 60_000L)
        val result = engine.evaluate(closed, rule, created, created + 5 * 60_000L, created + 150 * 60_000L, 15)
        assertFalse(result.resolutionBreached)
    }

}
