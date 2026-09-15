package com.lexora.service.core.model

enum class SyncState { SYNCED, PENDING_CREATE, PENDING_UPDATE, PENDING_DELETE, ERROR }
enum class ClientType { PERSON, COMPANY }
enum class RequestStatus { NEW, QUALIFICATION, PLANNED, IN_PROGRESS, WAITING, WORK_COMPLETED, CONFIRMATION, CLOSED, CANCELLED }
enum class RequestPriority { LOW, NORMAL, HIGH, URGENT }
enum class VisitStatus { PLANNED, EN_ROUTE, ON_SITE, COMPLETED, CANCELLED }
enum class ChecklistItemState { PENDING, DONE, NOT_APPLICABLE }
enum class ServiceDocumentType { WORK_ORDER, ACT, INVOICE }
enum class ServiceDocumentStatus { DRAFT, ISSUED, SIGNED, CANCELLED }
enum class PaymentStatus { PLANNED, PAID, CANCELLED }
enum class PaymentMethod { CASH, CARD, BANK_TRANSFER, OTHER }

data class Client(
    val id: String,
    val organizationId: String,
    val type: ClientType,
    val displayName: String,
    val phone: String? = null,
    val email: String? = null,
    val taxId: String? = null,
    val kpp: String? = null,
    val registrationAddress: String? = null,
    val actualAddress: String? = null,
    val note: String? = null,
    val consentPersonalData: Boolean = false,
    val archived: Boolean = false,
    val syncState: SyncState = SyncState.PENDING_CREATE,
)

data class Vehicle(val id: String, val organizationId: String, val clientId: String?, val registrationNumber: String, val vin: String? = null, val make: String? = null, val model: String? = null, val year: Int? = null, val bodyType: String? = null, val color: String? = null, val mileageKm: Int? = null, val archived: Boolean = false, val syncState: SyncState = SyncState.PENDING_CREATE)
data class Branch(val id: String, val organizationId: String, val name: String, val address: String? = null, val phone: String? = null, val email: String? = null, val workSchedule: String? = null, val timeZoneId: String, val active: Boolean = true, val syncState: SyncState = SyncState.PENDING_CREATE)
data class Employee(val id: String, val organizationId: String, val branchId: String?, val displayName: String, val position: String? = null, val phone: String? = null, val email: String? = null, val active: Boolean = true, val syncState: SyncState = SyncState.PENDING_CREATE)
data class ServiceObject(val id: String, val organizationId: String, val clientId: String?, val name: String, val address: String? = null, val accessMode: String? = null, val responsibleContact: String? = null, val archived: Boolean = false, val syncState: SyncState = SyncState.PENDING_CREATE)
data class Equipment(val id: String, val organizationId: String, val serviceObjectId: String?, val type: String, val make: String? = null, val model: String? = null, val serialNumber: String? = null, val inventoryNumber: String? = null, val barcode: String? = null, val commissionedNote: String? = null, val warrantyNote: String? = null, val archived: Boolean = false, val syncState: SyncState = SyncState.PENDING_CREATE)

data class ServiceRequest(
    val id: String,
    val organizationId: String,
    val number: String,
    val clientId: String?,
    val vehicleId: String? = null,
    val serviceObjectId: String? = null,
    val equipmentId: String? = null,
    val branchId: String? = null,
    val assigneeEmployeeId: String? = null,
    val title: String,
    val description: String? = null,
    val status: RequestStatus = RequestStatus.NEW,
    val priority: RequestPriority = RequestPriority.NORMAL,
    val plannedAtEpochMs: Long? = null,
    val dueAtEpochMs: Long? = null,
    val slaDeadlineEpochMs: Long? = null,
    val closedAtEpochMs: Long? = null,
    val archived: Boolean = false,
    val syncState: SyncState = SyncState.PENDING_CREATE,
)

data class RequestStatusHistory(val id: String, val requestId: String, val fromStatus: RequestStatus?, val toStatus: RequestStatus, val changedByUserId: String, val changedAtEpochMs: Long, val comment: String? = null)

data class ServiceVisit(
    val id: String,
    val organizationId: String,
    val requestId: String,
    val branchId: String? = null,
    val employeeId: String? = null,
    val status: VisitStatus = VisitStatus.PLANNED,
    val plannedStartEpochMs: Long? = null,
    val plannedEndEpochMs: Long? = null,
    val actualStartEpochMs: Long? = null,
    val actualEndEpochMs: Long? = null,
    val resultNote: String? = null,
    val customerName: String? = null,
    val customerSignatureRef: String? = null,
    val syncState: SyncState = SyncState.PENDING_CREATE,
)

data class VisitChecklistItem(val id: String, val visitId: String, val title: String, val state: ChecklistItemState = ChecklistItemState.PENDING, val comment: String? = null, val sortOrder: Int = 0, val syncState: SyncState = SyncState.PENDING_CREATE)
data class VisitWorkEntry(val id: String, val visitId: String, val serviceCode: String? = null, val title: String, val quantity: Double = 1.0, val unit: String? = null, val note: String? = null, val syncState: SyncState = SyncState.PENDING_CREATE)
data class VisitMaterialUsage(val id: String, val visitId: String, val materialCode: String? = null, val title: String, val quantity: Double, val unit: String? = null, val note: String? = null, val syncState: SyncState = SyncState.PENDING_CREATE)
data class VisitPhoto(val id: String, val visitId: String, val localUri: String, val caption: String? = null, val takenAtEpochMs: Long, val syncState: SyncState = SyncState.PENDING_CREATE)

data class ServiceDocument(
    val id: String,
    val organizationId: String,
    val requestId: String?,
    val visitId: String?,
    val clientId: String?,
    val type: ServiceDocumentType,
    val number: String,
    val status: ServiceDocumentStatus = ServiceDocumentStatus.DRAFT,
    val issuedAtEpochMs: Long? = null,
    val totalMinor: Long = 0,
    val currency: String = "RUB",
    val externalFileRef: String? = null,
    val note: String? = null,
    val archived: Boolean = false,
    val syncState: SyncState = SyncState.PENDING_CREATE,
)

data class Payment(
    val id: String,
    val organizationId: String,
    val requestId: String?,
    val documentId: String?,
    val clientId: String?,
    val amountMinor: Long,
    val currency: String = "RUB",
    val status: PaymentStatus = PaymentStatus.PLANNED,
    val method: PaymentMethod = PaymentMethod.BANK_TRANSFER,
    val paidAtEpochMs: Long? = null,
    val externalReference: String? = null,
    val note: String? = null,
    val archived: Boolean = false,
    val syncState: SyncState = SyncState.PENDING_CREATE,
)
