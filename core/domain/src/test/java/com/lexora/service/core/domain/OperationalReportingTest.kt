package com.lexora.service.core.domain

import com.lexora.service.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class OperationalReportingTest {
    private val calculator = OperationalReportingCalculator()

    @Test
    fun `workload groups visits by employee and branch inside period`() {
        val visits = listOf(
            ServiceVisit("v1", "org", "r1", "b1", "e1", VisitStatus.COMPLETED, 100, 200),
            ServiceVisit("v2", "org", "r2", "b1", "e1", VisitStatus.COMPLETED, 300, 500),
            ServiceVisit("v3", "org", "r3", "b2", "e2", VisitStatus.CANCELLED, 100, 200),
        )
        val rows = calculator.workload(visits, 0, 1000)
        assertEquals(1, rows.size)
        assertEquals("e1", rows.single().employeeId)
        assertEquals(2, rows.single().visitCount)
        assertEquals(300L, rows.single().plannedDurationMs)
    }

    @Test
    fun `employee output counts completed visits and work quantity`() {
        val visits = listOf(ServiceVisit("v1", "org", "r1", "b1", "e1", VisitStatus.COMPLETED, 0, 100, 10, 90))
        val works = listOf(
            VisitWorkEntry("w1", "v1", title = "A", quantity = 2.0),
            VisitWorkEntry("w2", "v1", title = "B", quantity = 1.5),
        )
        val row = calculator.employeeOutput(visits, works, 0, 1000).single()
        assertEquals("e1", row.employeeId)
        assertEquals(1, row.completedVisits)
        assertEquals(3.5, row.workQuantity, 0.001)
    }

    @Test
    fun `materials aggregate actual usage by code title and unit`() {
        val visits = listOf(ServiceVisit("v1", "org", "r1", "b1", "e1", VisitStatus.COMPLETED, 0, 100))
        val usage = listOf(
            VisitMaterialUsage("m1", "v1", "OIL", "Oil", 2.0, "l"),
            VisitMaterialUsage("m2", "v1", "OIL", "Oil", 1.5, "l"),
        )
        val row = calculator.materials(visits, usage, 0, 1000).single()
        assertEquals("OIL", row.materialCode)
        assertEquals(3.5, row.quantity, 0.001)
    }
}
