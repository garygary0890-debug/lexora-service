package com.lexora.service.core.model

enum class AttachmentKind { PHOTO, DOCUMENT, SCAN, ACT, SIGNATURE, OTHER }
enum class AttachmentSyncStatus { LOCAL_ONLY, QUEUED, UPLOADING, SYNCED, ERROR, DELETED }
data class AttachmentRef(
    val id: String,
    val organizationId: String,
    val entityType: String,
    val entityId: String,
    val kind: AttachmentKind,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long? = null,
    val localUri: String? = null,
    val remoteObjectKey: String? = null,
    val checksumSha256: String? = null,
    val createdByUserId: String,
    val createdAtEpochMs: Long,
    val syncStatus: AttachmentSyncStatus = AttachmentSyncStatus.LOCAL_ONLY,
    val archived: Boolean = false,
)

enum class InventoryMovementType { RECEIPT, ISSUE, TRANSFER, WRITE_OFF, RESERVATION, RELEASE_RESERVATION, ADJUSTMENT }
data class InventoryLocation(
    val id: String,
    val organizationId: String,
    val branchId: String? = null,
    val name: String,
    val active: Boolean = true,
)
data class InventoryItem(
    val id: String,
    val organizationId: String,
    val sku: String,
    val name: String,
    val unit: String,
    val minimumStock: Double? = null,
    val active: Boolean = true,
)
data class InventoryBalance(
    val organizationId: String,
    val locationId: String,
    val itemId: String,
    val quantity: Double,
    val reservedQuantity: Double = 0.0,
)
data class InventoryMovement(
    val id: String,
    val organizationId: String,
    val locationId: String,
    val targetLocationId: String? = null,
    val itemId: String,
    val requestId: String? = null,
    val workOrderDocumentId: String? = null,
    val type: InventoryMovementType,
    val quantity: Double,
    val occurredAtEpochMs: Long,
    val actorUserId: String,
    val note: String? = null,
)
data class Supplier(
    val id: String,
    val organizationId: String,
    val name: String,
    val taxId: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val active: Boolean = true,
)
enum class PurchaseOrderStatus { DRAFT, APPROVED, ORDERED, PARTIALLY_RECEIVED, RECEIVED, CANCELLED }
data class PurchaseOrderLine(val itemId: String, val quantity: Double, val unitPriceMinor: Long? = null)
data class PurchaseOrder(
    val id: String,
    val organizationId: String,
    val supplierId: String,
    val destinationLocationId: String,
    val status: PurchaseOrderStatus = PurchaseOrderStatus.DRAFT,
    val lines: List<PurchaseOrderLine>,
    val createdAtEpochMs: Long,
    val expectedAtEpochMs: Long? = null,
)

data class RouteStop(
    val requestId: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val windowStartEpochMs: Long? = null,
    val windowEndEpochMs: Long? = null,
    val priority: RequestPriority = RequestPriority.NORMAL,
    val serviceDurationMinutes: Int? = null,
)
data class OptimizedRoute(
    val employeeId: String,
    val branchId: String? = null,
    val stops: List<RouteStop>,
    val generatedAtEpochMs: Long,
    val manuallyAdjusted: Boolean = false,
)
data class DispatchCandidate(
    val employeeId: String,
    val branchId: String?,
    val skillCodes: Set<String>,
    val availableFromEpochMs: Long?,
    val availableToEpochMs: Long?,
    val activeWorkload: Int,
)
data class DispatchDecision(
    val requestId: String,
    val employeeId: String?,
    val score: Int,
    val reasons: List<String>,
    val automatic: Boolean,
)

enum class TelemetrySeverity { INFO, WARNING, CRITICAL }
data class TelemetryMetric(
    val equipmentId: String,
    val organizationId: String,
    val metricCode: String,
    val numericValue: Double,
    val unit: String? = null,
    val measuredAtEpochMs: Long,
)
data class TelemetryEvent(
    val id: String,
    val organizationId: String,
    val equipmentId: String,
    val eventCode: String,
    val severity: TelemetrySeverity,
    val message: String,
    val occurredAtEpochMs: Long,
    val generatedRequestId: String? = null,
)

enum class IntegrationOperationStatus { PENDING, RUNNING, SUCCEEDED, RETRY_WAIT, FAILED }
data class IntegrationOperation(
    val id: String,
    val organizationId: String,
    val providerCode: String,
    val operationType: String,
    val idempotencyKey: String,
    val entityType: String? = null,
    val entityId: String? = null,
    val status: IntegrationOperationStatus = IntegrationOperationStatus.PENDING,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

data class SlaRule(
    val id: String,
    val organizationId: String,
    val contractId: String? = null,
    val clientId: String? = null,
    val categoryCode: String? = null,
    val priority: RequestPriority? = null,
    val reactionMinutes: Int,
    val resolutionMinutes: Int,
    val businessHoursOnly: Boolean = true,
    val active: Boolean = true,
)
data class SlaEvaluation(
    val requestId: String,
    val reactionDeadlineEpochMs: Long?,
    val resolutionDeadlineEpochMs: Long?,
    val atRisk: Boolean,
    val breached: Boolean,
)

data class ClientPortalSnapshot(
    val clientId: String,
    val organizationId: String,
    val vehicles: List<Vehicle>,
    val requests: List<ServiceRequest>,
    val documents: List<ServiceDocument>,
    val payments: List<Payment>,
    val loyaltyPoints: Long,
    val unreadNotifications: Int,
)

data class ModuleExtensionPoint(
    val moduleId: LexoraModuleId,
    val route: String,
    val requiredPermissions: Set<Permission> = emptySet(),
    val directoryCodes: Set<String> = emptySet(),
    val reportCodes: Set<String> = emptySet(),
    val quickActionCodes: Set<String> = emptySet(),
)

enum class UiContentState { LOADING, EMPTY, CONTENT, ERROR, OFFLINE, NO_PERMISSION }
data class UiMessage(val code: String, val text: String, val recoverable: Boolean = true)
data class ScreenState<T>(
    val state: UiContentState,
    val data: T? = null,
    val message: UiMessage? = null,
)

data class ArchiveMetadata(
    val archived: Boolean,
    val archivedByUserId: String? = null,
    val archivedAtEpochMs: Long? = null,
    val reason: String? = null,
)

enum class TaskStatus { OPEN, IN_PROGRESS, DONE, CANCELLED }
data class ServiceTask(
    val id: String,
    val organizationId: String,
    val entityType: String,
    val entityId: String,
    val title: String,
    val assigneeUserId: String? = null,
    val assigneeEmployeeId: String? = null,
    val dueAtEpochMs: Long? = null,
    val status: TaskStatus = TaskStatus.OPEN,
    val reminderAtEpochMs: Long? = null,
    val priority: RequestPriority = RequestPriority.NORMAL,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

data class PlanningSlot(
    val id: String,
    val organizationId: String,
    val employeeId: String? = null,
    val teamCode: String? = null,
    val requestId: String? = null,
    val branchId: String? = null,
    val startAtEpochMs: Long,
    val endAtEpochMs: Long,
)

data class DashboardKpi(
    val code: String,
    val label: String,
    val value: Double,
    val unit: String? = null,
)

data class SearchQuery(
    val text: String = "",
    val organizationId: String,
    val entityTypes: Set<String> = emptySet(),
    val includeArchived: Boolean = false,
    val branchIds: Set<String> = emptySet(),
    val statusCodes: Set<String> = emptySet(),
)

data class ReportSnapshot(
    val organizationId: String,
    val requestCount: Int,
    val overdueSlaCount: Int,
    val averageReactionMinutes: Double?,
    val averageResolutionMinutes: Double?,
    val activeEmployeeCount: Int,
    val revenueMinor: Long,
    val debtMinor: Long,
    val repeatedEquipmentIssueCount: Int,
)

enum class RuntimeEnvironment { DEVELOPMENT, STAGING, PRODUCTION }
data class BuildTrace(
    val versionName: String,
    val versionCode: Int,
    val commitSha: String?,
    val buildId: String?,
    val environment: RuntimeEnvironment,
)
data class HealthCheck(
    val component: String,
    val healthy: Boolean,
    val checkedAtEpochMs: Long,
    val details: String? = null,
)
data class BackupVerification(
    val backupId: String,
    val createdAtEpochMs: Long,
    val restoreTestedAtEpochMs: Long? = null,
    val restoreSucceeded: Boolean? = null,
)
