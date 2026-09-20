package com.lexora.service.core.domain

import com.lexora.service.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportingCalculatorTest {
    private val calculator = ReportingCalculator()

    @Test
    fun `financial report uses paid revenue minus refunds and separates overdue debt`() {
        val now = 1_000_000L
        val documents = listOf(
            ServiceDocument("d1", "org", "r1", null, "c1", ServiceDocumentType.INVOICE, "1", ServiceDocumentStatus.ISSUED, issuedAtEpochMs = now - 10_000, totalMinor = 10_000),
            ServiceDocument("d2", "org", "r2", null, "c2", ServiceDocumentType.INVOICE, "2", ServiceDocumentStatus.ISSUED, issuedAtEpochMs = now - 5_000, totalMinor = 8_000),
        )
        val payments = listOf(
            Payment("p1", "org", "r1", "d1", "c1", 10_000, status = PaymentStatus.PAID, paidAtEpochMs = now - 1_000),
            Payment("p2", "org", "r1", "d1", "c1", -2_000, status = PaymentStatus.PAID, paidAtEpochMs = now - 500, note = "REFUND"),
        )
        val result = calculator.financial(documents, payments, overdueDocumentIds = setOf("d2"))
        assertEquals(8_000L, result.netRevenueMinor)
        assertEquals(8_000L, result.debtMinor)
        assertEquals(8_000L, result.overdueDebtMinor)
    }

    @Test
    fun `sla report counts breaches and computes averages only from measured requests`() {
        val requests = listOf(
            ServiceRequest("r1", "org", "1", null, title = "A", createdAtEpochMs = 0, firstReactionAtEpochMs = 10 * 60_000L, closedAtEpochMs = 50 * 60_000L, slaReactionMinutes = 5, slaResolutionMinutes = 40),
            ServiceRequest("r2", "org", "2", null, title = "B", createdAtEpochMs = 0, firstReactionAtEpochMs = 4 * 60_000L, closedAtEpochMs = 20 * 60_000L, slaReactionMinutes = 5, slaResolutionMinutes = 40),
            ServiceRequest("r3", "org", "3", null, title = "Open", createdAtEpochMs = 0, slaReactionMinutes = 5, slaResolutionMinutes = 40),
        )
        val result = calculator.sla(requests, nowEpochMs = 60 * 60_000L)
        assertEquals(2, result.reactionBreachedCount)
        assertEquals(2, result.resolutionBreachedCount)
        assertEquals(7.0, result.averageReactionMinutes!!, 0.001)
        assertEquals(35.0, result.averageResolutionMinutes!!, 0.001)
    }

    @Test
    fun `repeat issue report treats configured window boundary as inclusive`() {
        val day = 24L * 60L * 60L * 1000L
        val requests = listOf(
            ServiceRequest("r1", "org", "1", null, equipmentId = "eq", title = "Leak", createdAtEpochMs = 0),
            ServiceRequest("r2", "org", "2", null, equipmentId = "eq", title = "Leak", createdAtEpochMs = 30 * day),
            ServiceRequest("r3", "org", "3", null, equipmentId = "eq", title = "Other", createdAtEpochMs = 31 * day),
        )
        val result = calculator.repeatIssues(requests, windowMs = 30 * day)
        assertEquals(1, result.repeatedIssueCount)
        assertTrue(result.rows.single().requestIds.containsAll(listOf("r1", "r2")))
    }
}
