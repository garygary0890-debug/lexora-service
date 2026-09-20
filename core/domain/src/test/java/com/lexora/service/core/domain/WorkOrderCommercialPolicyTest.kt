package com.lexora.service.core.domain

import com.lexora.service.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkOrderCommercialPolicyTest {
    private val policy = WorkOrderCommercialPolicy()

    @Test
    fun `signed or issued work order freezes commercial rows`() {
        assertTrue(policy.isCommerciallyFrozen(ServiceDocumentStatus.ISSUED))
        assertTrue(policy.isCommerciallyFrozen(ServiceDocumentStatus.SIGNED))
        assertFalse(policy.isCommerciallyFrozen(ServiceDocumentStatus.DRAFT))
    }

    @Test
    fun `additional work approval evidence carries immutable acceptance metadata`() {
        val event = policy.approvalEvent(
            id = "a1", organizationId = "org", workOrderItemId = "item", actorUserId = "u",
            channel = "CLIENT_PORTAL", payloadHash = "sha256:x", decision = AdditionalWorkApprovalDecision.APPROVED,
            sentAtEpochMs = 100, decidedAtEpochMs = 200,
        )
        assertEquals(AdditionalWorkApprovalDecision.APPROVED, event.decision)
        assertEquals(200L, event.decidedAtEpochMs)
        assertEquals("sha256:x", event.payloadHash)
    }
}
