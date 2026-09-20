package com.lexora.service.core.domain

import com.lexora.service.core.model.*

class OperationalReportingCalculator {
    fun workload(visits: List<ServiceVisit>, fromEpochMs: Long, toEpochMs: Long): List<WorkloadReportRow> =
        visits.asSequence()
            .filter { it.status != VisitStatus.CANCELLED }
            .filter { overlaps(it.plannedStartEpochMs, it.plannedEndEpochMs, fromEpochMs, toEpochMs) }
            .filter { !it.employeeId.isNullOrBlank() }
            .groupBy { requireNotNull(it.employeeId) to it.branchId }
            .map { (key, items) ->
                WorkloadReportRow(
                    employeeId = key.first,
                    branchId = key.second,
                    visitCount = items.size,
                    plannedDurationMs = items.sumOf {
                        ((it.plannedEndEpochMs ?: it.plannedStartEpochMs ?: 0L) - (it.plannedStartEpochMs ?: 0L)).coerceAtLeast(0L)
                    },
                )
            }.sortedWith(compareBy(WorkloadReportRow::branchId, WorkloadReportRow::employeeId))

    fun employeeOutput(
        visits: List<ServiceVisit>,
        works: List<VisitWorkEntry>,
        fromEpochMs: Long,
        toEpochMs: Long,
    ): List<EmployeeOutputReportRow> {
        val completed = visits.filter {
            it.status == VisitStatus.COMPLETED &&
                !it.employeeId.isNullOrBlank() &&
                overlaps(it.actualStartEpochMs ?: it.plannedStartEpochMs, it.actualEndEpochMs ?: it.plannedEndEpochMs, fromEpochMs, toEpochMs)
        }
        val worksByVisit = works.groupBy { it.visitId }
        return completed.groupBy { requireNotNull(it.employeeId) }.map { (employeeId, employeeVisits) ->
            EmployeeOutputReportRow(
                employeeId = employeeId,
                completedVisits = employeeVisits.size,
                workQuantity = employeeVisits.sumOf { visit -> worksByVisit[visit.id].orEmpty().sumOf { it.quantity } },
            )
        }.sortedBy { it.employeeId }
    }

    fun materials(
        visits: List<ServiceVisit>,
        usage: List<VisitMaterialUsage>,
        fromEpochMs: Long,
        toEpochMs: Long,
    ): List<MaterialUsageReportRow> {
        val includedVisitIds = visits.filter {
            it.status != VisitStatus.CANCELLED && overlaps(it.actualStartEpochMs ?: it.plannedStartEpochMs, it.actualEndEpochMs ?: it.plannedEndEpochMs, fromEpochMs, toEpochMs)
        }.mapTo(mutableSetOf()) { it.id }
        return usage.asSequence().filter { it.visitId in includedVisitIds }
            .groupBy { Triple(it.materialCode, it.title, it.unit) }
            .map { (key, values) -> MaterialUsageReportRow(key.first, key.second, key.third, values.sumOf { it.quantity }) }
            .sortedWith(compareBy(MaterialUsageReportRow::materialCode, MaterialUsageReportRow::title))
    }

    private fun overlaps(start: Long?, end: Long?, from: Long, to: Long): Boolean {
        val s = start ?: return false
        val e = end ?: s
        return s <= to && e >= from
    }
}
