package com.lexora.service.core.domain

import com.lexora.service.core.model.*

data class ClientCollection(val active: List<Client>, val archived: List<Client>)
data class VehicleCollection(val active: List<Vehicle>, val archived: List<Vehicle>)
data class AssetCollection(
    val objects: List<ServiceObject>,
    val archivedObjects: List<ServiceObject>,
    val equipment: List<Equipment>,
    val archivedEquipment: List<Equipment>,
)
data class OrganizationCollection(
    val branches: List<Branch>,
    val inactiveBranches: List<Branch>,
    val employees: List<Employee>,
    val inactiveEmployees: List<Employee>,
)
data class VisitCollection(
    val visits: List<ServiceVisit>,
    val selectedVisitId: String?,
    val checklist: List<VisitChecklistItem>,
)
data class FinanceCollection(val documents: List<ServiceDocument>, val payments: List<Payment>)
data class RequestOperationalDetails(
    val visits: List<ServiceVisit>,
    val works: List<VisitWorkEntry>,
    val materials: List<VisitMaterialUsage>,
    val documents: List<ServiceDocument>,
    val payments: List<Payment>,
    val history: List<RequestChangeHistory>,
    val sla: SlaEvaluation,
)

interface ServiceOperations {
    suspend fun ensureDemoClient(organizationId: String)
    suspend fun clients(organizationId: String): ClientCollection
    suspend fun saveClient(organizationId: String, userId: String, draft: SaveClientCommand, existingId: String?)
    suspend fun archiveClient(organizationId: String, userId: String, id: String, restore: Boolean)
    suspend fun vehicles(organizationId: String): VehicleCollection
    suspend fun saveVehicle(organizationId: String, userId: String, draft: SaveVehicleCommand, existingId: String?)
    suspend fun archiveVehicle(organizationId: String, userId: String, id: String, restore: Boolean)
    suspend fun assets(organizationId: String): AssetCollection
    suspend fun saveServiceObject(organizationId: String, userId: String, draft: SaveServiceObjectCommand, existingId: String?)
    suspend fun archiveServiceObject(organizationId: String, userId: String, id: String, restore: Boolean)
    suspend fun saveEquipment(organizationId: String, userId: String, draft: SaveEquipmentCommand, existingId: String?)
    suspend fun archiveEquipment(organizationId: String, userId: String, id: String, restore: Boolean)
    suspend fun organization(organizationId: String): OrganizationCollection
    suspend fun saveBranch(organizationId: String, userId: String, draft: SaveBranchCommand, existingId: String?)
    suspend fun setBranchActive(organizationId: String, userId: String, id: String, active: Boolean)
    suspend fun saveEmployee(organizationId: String, userId: String, draft: SaveEmployeeCommand, existingId: String?)
    suspend fun setEmployeeActive(organizationId: String, userId: String, id: String, active: Boolean)
    suspend fun requests(organizationId: String): List<ServiceRequest>
    suspend fun saveRequest(organizationId: String, userId: String, draft: SaveRequestCommand, existingId: String?)
    suspend fun changeRequestStatus(organizationId: String, userId: String, id: String, target: RequestStatus)
    suspend fun assignRequest(organizationId: String, userId: String, id: String, employeeId: String?, teamName: String?)
    suspend fun rescheduleRequest(organizationId: String, userId: String, id: String, plannedStartEpochMs: Long, plannedEndEpochMs: Long, reason: String)
    suspend fun requestOperationalDetails(organizationId: String, requestId: String): RequestOperationalDetails
    suspend fun visits(organizationId: String, selectedVisitId: String?): VisitCollection
    suspend fun checklist(visitId: String): List<VisitChecklistItem>
    suspend fun createVisit(organizationId: String, userId: String, requestId: String, employeeId: String?)
    suspend fun changeVisitStatus(organizationId: String, userId: String, id: String, target: VisitStatus)
    suspend fun addChecklistItem(organizationId: String, userId: String, visitId: String)
    suspend fun toggleChecklistItem(organizationId: String, userId: String, current: VisitChecklistItem)
    suspend fun finance(organizationId: String): FinanceCollection
    suspend fun createDocument(organizationId: String, userId: String, type: ServiceDocumentType, requestId: String?)
    suspend fun changeDocumentStatus(organizationId: String, userId: String, id: String, target: ServiceDocumentStatus)
    suspend fun createPayment(organizationId: String, userId: String, requestId: String?, currentDocuments: List<ServiceDocument>)
    suspend fun markPaymentPaid(organizationId: String, userId: String, id: String)
}