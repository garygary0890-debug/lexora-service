package com.lexora.service.core.model

data class FinancialReport(
    val grossPaidMinor: Long,
    val refundsMinor: Long,
    val netRevenueMinor: Long,
    val debtMinor: Long,
    val overdueDebtMinor: Long,
)

data class SlaReport(
    val measuredReactionCount: Int,
    val measuredResolutionCount: Int,
    val reactionBreachedCount: Int,
    val resolutionBreachedCount: Int,
    val averageReactionMinutes: Double?,
    val averageResolutionMinutes: Double?,
)

data class RepeatIssueRow(
    val equipmentId: String,
    val issueKey: String,
    val requestIds: List<String>,
    val firstAtEpochMs: Long,
    val lastAtEpochMs: Long,
)

data class RepeatIssueReport(
    val repeatedIssueCount: Int,
    val rows: List<RepeatIssueRow>,
)

data class WorkloadReportRow(
    val employeeId: String,
    val branchId: String?,
    val visitCount: Int,
    val plannedDurationMs: Long,
)

data class EmployeeOutputReportRow(
    val employeeId: String,
    val completedVisits: Int,
    val workQuantity: Double,
)

data class MaterialUsageReportRow(
    val materialCode: String?,
    val title: String,
    val unit: String?,
    val quantity: Double,
)
