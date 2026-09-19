package com.lexora.service.core.model

enum class GlobalSearchEntityType {
    CLIENT, VEHICLE, SERVICE_OBJECT, EQUIPMENT, REQUEST, DOCUMENT, PAYMENT, EMPLOYEE
}

data class GlobalSearchResult(
    val entityType: GlobalSearchEntityType,
    val entityId: String,
    val title: String,
    val subtitle: String? = null,
    val route: String,
    val score: Int = 0,
)

data class OperationalDashboard(
    val openRequests: Int,
    val urgentRequests: Int,
    val overdueRequests: Int,
    val todayVisits: Int,
    val activeEmployees: Int,
    val unsyncedEntities: Int,
    val plannedPaymentsMinor: Long,
    val nextVisits: List<PlanningEvent>,
)

enum class PlanningMode { DAY, WEEK, MONTH }
enum class PlanningResourceKind { EMPLOYEE, TEAM }

data class PlanningEvent(
    val id: String,
    val requestId: String,
    val requestNumber: String,
    val title: String,
    val employeeId: String?,
    val employeeName: String?,
    val branchId: String?,
    val branchName: String?,
    val startAtEpochMs: Long,
    val endAtEpochMs: Long,
    val status: VisitStatus,
    val priority: RequestPriority,
)

data class ResourceLoad(
    val resourceId: String,
    val resourceKind: PlanningResourceKind,
    val displayName: String,
    val scheduledMinutes: Int,
    val capacityMinutes: Int,
    val utilizationPercent: Int,
    val eventCount: Int,
    val overloaded: Boolean,
)

data class PlanningSnapshot(
    val rangeStartEpochMs: Long,
    val rangeEndEpochMs: Long,
    val events: List<PlanningEvent>,
    val employeeLoads: List<ResourceLoad>,
    val teamLoads: List<ResourceLoad>,
)
