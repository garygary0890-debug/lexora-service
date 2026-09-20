package com.lexora.service.core.domain

import com.lexora.service.core.model.FinancialReport
import com.lexora.service.core.model.Payment
import com.lexora.service.core.model.PaymentStatus
import com.lexora.service.core.model.RepeatIssueReport
import com.lexora.service.core.model.RepeatIssueRow
import com.lexora.service.core.model.ServiceDocument
import com.lexora.service.core.model.ServiceDocumentType
import com.lexora.service.core.model.ServiceRequest
import com.lexora.service.core.model.SlaReport

class ReportingCalculator {
    fun financial(
        documents: List<ServiceDocument>,
        payments: List<Payment>,
        overdueDocumentIds: Set<String> = emptySet(),
    ): FinancialReport {
        val paid = payments.filter { it.status == PaymentStatus.PAID }
        val grossPaid = paid.filter { it.amountMinor > 0L }.sumOf { it.amountMinor }
        val refunds = paid.filter { it.amountMinor < 0L }.sumOf { -it.amountMinor }
        val invoices = documents.filter { it.type == ServiceDocumentType.INVOICE && !it.archived }
        val positivePaidByDocument = paid.filter { it.amountMinor > 0L && it.documentId != null }
            .groupBy { requireNotNull(it.documentId) }
            .mapValues { (_, values) -> values.sumOf { it.amountMinor } }
        val outstandingByDocument = invoices.associate { document ->
            document.id to (document.totalMinor - (positivePaidByDocument[document.id] ?: 0L)).coerceAtLeast(0L)
        }
        return FinancialReport(
            grossPaidMinor = grossPaid,
            refundsMinor = refunds,
            netRevenueMinor = grossPaid - refunds,
            debtMinor = outstandingByDocument.values.sum(),
            overdueDebtMinor = outstandingByDocument.filterKeys(overdueDocumentIds::contains).values.sum(),
        )
    }

    fun sla(requests: List<ServiceRequest>, nowEpochMs: Long): SlaReport {
        val measuredReaction = requests.mapNotNull { request ->
            request.firstReactionAtEpochMs?.let { (it - request.createdAtEpochMs).coerceAtLeast(0L) }
        }
        val measuredResolution = requests.mapNotNull { request ->
            request.closedAtEpochMs?.let { (it - request.createdAtEpochMs).coerceAtLeast(0L) }
        }
        val reactionBreaches = requests.count { request ->
            val limit = request.slaReactionMinutes ?: return@count false
            val deadline = request.createdAtEpochMs + limit * MINUTE_MS
            val actual = request.firstReactionAtEpochMs
            if (actual != null) actual > deadline else nowEpochMs > deadline
        }
        val resolutionBreaches = requests.count { request ->
            val limit = request.slaResolutionMinutes ?: return@count false
            val deadline = request.createdAtEpochMs + limit * MINUTE_MS
            val actual = request.closedAtEpochMs
            if (actual != null) actual > deadline else nowEpochMs > deadline
        }
        return SlaReport(
            measuredReactionCount = measuredReaction.size,
            measuredResolutionCount = measuredResolution.size,
            reactionBreachedCount = reactionBreaches,
            resolutionBreachedCount = resolutionBreaches,
            averageReactionMinutes = measuredReaction.averageMinutesOrNull(),
            averageResolutionMinutes = measuredResolution.averageMinutesOrNull(),
        )
    }

    fun repeatIssues(requests: List<ServiceRequest>, windowMs: Long): RepeatIssueReport {
        require(windowMs >= 0L) { "Repeat window must be non-negative" }
        val rows = requests
            .filter { !it.archived && !it.equipmentId.isNullOrBlank() }
            .groupBy { requireNotNull(it.equipmentId) to normalizeIssue(it.title) }
            .mapNotNull { (key, values) ->
                val sorted = values.sortedBy { it.createdAtEpochMs }
                val qualifying = largestWindow(sorted, windowMs)
                if (qualifying.size < 2) null else RepeatIssueRow(
                    equipmentId = key.first,
                    issueKey = key.second,
                    requestIds = qualifying.map { it.id },
                    firstAtEpochMs = qualifying.first().createdAtEpochMs,
                    lastAtEpochMs = qualifying.last().createdAtEpochMs,
                )
            }
            .sortedByDescending { it.lastAtEpochMs }
        return RepeatIssueReport(repeatedIssueCount = rows.size, rows = rows)
    }

    private fun largestWindow(sorted: List<ServiceRequest>, windowMs: Long): List<ServiceRequest> {
        var best: List<ServiceRequest> = emptyList()
        var start = 0
        sorted.indices.forEach { end ->
            while (sorted[end].createdAtEpochMs - sorted[start].createdAtEpochMs > windowMs) start++
            val candidate = sorted.subList(start, end + 1)
            if (candidate.size > best.size) best = candidate.toList()
        }
        return best
    }

    private fun normalizeIssue(title: String): String = title.trim().lowercase()

    companion object { private const val MINUTE_MS = 60_000L }
}

private fun List<Long>.averageMinutesOrNull(): Double? =
    if (isEmpty()) null else average() / 60_000.0
