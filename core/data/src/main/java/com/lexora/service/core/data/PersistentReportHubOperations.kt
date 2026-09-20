package com.lexora.service.core.data

import com.lexora.service.core.domain.OperationalReportingCalculator
import com.lexora.service.core.domain.ReportHubOperations
import com.lexora.service.core.domain.ReportingCalculator
import com.lexora.service.core.domain.ServiceOperations
import com.lexora.service.core.model.*

class PersistentReportHubOperations(
    private val operations: ServiceOperations,
    private val reporting: ReportingCalculator = ReportingCalculator(),
    private val operational: OperationalReportingCalculator = OperationalReportingCalculator(),
) : ReportHubOperations {
    override suspend fun snapshot(
        organizationId: String,
        branchIds: Set<String>,
        fromEpochMs: Long,
        toEpochMs: Long,
        nowEpochMs: Long,
    ): ReportHubSnapshot {
        require(fromEpochMs <= toEpochMs) { "Начало периода не может быть позже окончания" }
        val allRequests = operations.requests(organizationId)
        val branchRequests = if (branchIds.isEmpty()) allRequests else allRequests.filter { it.branchId in branchIds }
        val periodRequests = branchRequests.filter { it.createdAtEpochMs in fromEpochMs..toEpochMs }
        val relevantRequestIds = branchRequests.mapTo(mutableSetOf()) { it.id }
        val allVisits = operations.visits(organizationId, null).visits
        val visits = if (branchIds.isEmpty()) allVisits else allVisits.filter { it.requestId in relevantRequestIds }
        val finance = operations.finance(organizationId)
        val documents = if (branchIds.isEmpty()) finance.documents else finance.documents.filter { it.requestId == null || it.requestId in relevantRequestIds }
        val payments = if (branchIds.isEmpty()) finance.payments else finance.payments.filter { it.requestId == null || it.requestId in relevantRequestIds }

        val detailRequests = branchRequests.filter { request ->
            request.updatedAtEpochMs in fromEpochMs..toEpochMs || request.createdAtEpochMs in fromEpochMs..toEpochMs
        }
        val details = detailRequests.map { operations.requestOperationalDetails(organizationId, it.id) }
        val works = details.flatMap { it.works }.distinctBy { it.id }
        val materials = details.flatMap { it.materials }.distinctBy { it.id }

        val overdueDocumentIds = documents.filter { document ->
            val request = document.requestId?.let { id -> allRequests.firstOrNull { it.id == id } }
            request?.dueAtEpochMs?.let { it < nowEpochMs && request.closedAtEpochMs == null } == true
        }.mapTo(mutableSetOf()) { it.id }

        return ReportHubSnapshot(
            organizationId = organizationId,
            branchIds = branchIds,
            fromEpochMs = fromEpochMs,
            toEpochMs = toEpochMs,
            requestStatuses = RequestStatus.entries.map { status ->
                RequestStatusReportRow(status, periodRequests.count { it.status == status })
            }.filter { it.count > 0 },
            workload = operational.workload(visits, fromEpochMs, toEpochMs),
            employeeOutput = operational.employeeOutput(visits, works, fromEpochMs, toEpochMs),
            financial = reporting.financial(documents, payments, overdueDocumentIds),
            sla = reporting.sla(periodRequests, nowEpochMs),
            repeatIssues = reporting.repeatIssues(periodRequests, 30L * DAY_MS),
            materials = operational.materials(visits, materials, fromEpochMs, toEpochMs),
        )
    }

    override suspend fun exportCsv(snapshot: ReportHubSnapshot): String = buildString {
        appendLine("report;key;value")
        snapshot.requestStatuses.forEach { appendLine("requests;${it.status.name};${it.count}") }
        snapshot.workload.forEach { appendLine("workload;${escape(it.employeeId)};${it.visitCount}") }
        snapshot.employeeOutput.forEach { appendLine("employee_output;${escape(it.employeeId)};${it.workQuantity}") }
        appendLine("finance;net_revenue_minor;${snapshot.financial.netRevenueMinor}")
        appendLine("finance;debt_minor;${snapshot.financial.debtMinor}")
        appendLine("finance;overdue_debt_minor;${snapshot.financial.overdueDebtMinor}")
        appendLine("sla;reaction_breaches;${snapshot.sla.reactionBreachedCount}")
        appendLine("sla;resolution_breaches;${snapshot.sla.resolutionBreachedCount}")
        snapshot.repeatIssues.rows.forEach { appendLine("repeat_issue;${escape(it.equipmentId)};${it.requestIds.size}") }
        snapshot.materials.forEach { appendLine("materials;${escape(it.materialCode ?: it.title)};${it.quantity}") }
    }

    private fun escape(value: String): String = '"' + value.replace("\"", "\"\"") + '"'

    companion object { private const val DAY_MS = 24L * 60L * 60L * 1000L }
}
