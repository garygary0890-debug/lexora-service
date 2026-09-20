package com.lexora.service.feature.fieldwork

import com.lexora.service.core.data.InventoryRepository
import com.lexora.service.core.data.VisitExecutionReport
import com.lexora.service.core.data.VisitExecutionRepository
import com.lexora.service.core.domain.AutoDispatchPolicy
import com.lexora.service.core.domain.RoutePlanner
import com.lexora.service.core.domain.ServiceOperations
import com.lexora.service.core.model.*
import com.lexora.service.core.presentation.LexoraViewModel
import java.util.UUID

data class FieldWorkUiState(
    val loading: Boolean = true,
    val visits: List<ServiceVisit> = emptyList(),
    val requests: List<ServiceRequest> = emptyList(),
    val employees: List<Employee> = emptyList(),
    val objects: List<ServiceObject> = emptyList(),
    val checklist: List<VisitChecklistItem> = emptyList(),
    val selectedVisitId: String? = null,
    val execution: VisitExecutionReport? = null,
    val inventoryLocations: List<InventoryLocation> = emptyList(),
    val inventoryItems: List<InventoryItem> = emptyList(),
    val inventoryBalances: List<InventoryBalance> = emptyList(),
    val inventoryMovements: List<InventoryMovement> = emptyList(),
    val lowStock: List<InventoryBalance> = emptyList(),
    val dispatch: List<DispatchDecision> = emptyList(),
    val routes: List<OptimizedRoute> = emptyList(),
    val error: String? = null,
)

class FieldWorkViewModel(
    private val organizationId: String,
    private val userId: String,
    private val operations: ServiceOperations,
    private val executionRepository: VisitExecutionRepository,
    private val inventoryRepository: InventoryRepository,
    private val dispatchPolicy: AutoDispatchPolicy = AutoDispatchPolicy(),
    private val routePlanner: RoutePlanner = RoutePlanner(),
) : LexoraViewModel<FieldWorkUiState>(FieldWorkUiState()) {
    init { reload() }
    fun reload() = launchSafely(::fail) { refresh(null) }
    fun selectVisit(id: String) = launchSafely(::fail) { refresh(id) }

    fun createVisit(requestId: String, employeeId: String?) = launchSafely(::fail) {
        operations.createVisit(organizationId, userId, requestId, employeeId); refresh(state.value.selectedVisitId)
    }
    fun assignRequest(requestId: String, employeeId: String?) = launchSafely(::fail) {
        operations.assignRequest(organizationId, userId, requestId, employeeId, null); refresh(state.value.selectedVisitId)
    }
    fun changeStatus(id: String, target: VisitStatus) = launchSafely(::fail) {
        if (target == VisitStatus.COMPLETED) executionRepository.assertCanComplete(organizationId, id)
        operations.changeVisitStatus(organizationId, userId, id, target)
        refresh(id)
    }
    fun addChecklistItem(visitId: String) = launchSafely(::fail) { operations.addChecklistItem(organizationId, userId, visitId); refresh(visitId) }
    fun toggleChecklistItem(itemId: String) = launchSafely(::fail) {
        val current = state.value.checklist.firstOrNull { it.id == itemId } ?: return@launchSafely
        operations.toggleChecklistItem(organizationId, userId, current); refresh(current.visitId)
    }

    fun saveTechnicalConclusion(text: String) = selectedVisitAction { visitId ->
        executionRepository.saveTechnicalConclusion(organizationId, userId, visitId, text)
    }

    fun saveClientSignature(customerName: String, signatureRef: String) = selectedVisitAction { visitId ->
        executionRepository.saveClientSignature(organizationId, userId, visitId, customerName, signatureRef)
    }

    fun addWork(title: String, quantity: Double, unit: String?, note: String?) = selectedVisitAction { visitId ->
        executionRepository.saveWork(organizationId, userId, visitId, title, quantity, unit, note)
    }

    /** Issues stock and records it in the field-visit material list. */
    fun useMaterial(locationId: String, itemId: String, quantity: Double, note: String?) = selectedVisitAction { visitId ->
        val item = state.value.inventoryItems.firstOrNull { it.id == itemId } ?: error("Материал не найден")
        val requestId = state.value.execution?.visit?.requestId
        inventoryRepository.recordMovement(
            InventoryMovement(
                id = UUID.randomUUID().toString(), organizationId = organizationId, locationId = locationId,
                itemId = itemId, requestId = requestId, type = InventoryMovementType.ISSUE, quantity = quantity,
                occurredAtEpochMs = System.currentTimeMillis(), actorUserId = userId, note = note,
            ),
        )
        executionRepository.saveMaterial(organizationId, userId, visitId, item.sku, item.name, quantity, item.unit, note)
    }

    fun createWarehouse(name: String, branchId: String?) = launchSafely(::fail) {
        inventoryRepository.saveLocation(InventoryLocation(UUID.randomUUID().toString(), organizationId, branchId, name.trim()))
        refresh(state.value.selectedVisitId)
    }

    fun createInventoryItem(sku: String, name: String, unit: String, minimumStock: Double?) = launchSafely(::fail) {
        inventoryRepository.saveItem(InventoryItem(UUID.randomUUID().toString(), organizationId, sku.trim(), name.trim(), unit.trim(), minimumStock))
        refresh(state.value.selectedVisitId)
    }

    fun receipt(locationId: String, itemId: String, quantity: Double, note: String?) = warehouseMovement(
        InventoryMovementType.RECEIPT, locationId, itemId, quantity, null, null, null, note,
    )

    fun writeOff(locationId: String, itemId: String, quantity: Double, note: String?) = warehouseMovement(
        InventoryMovementType.WRITE_OFF, locationId, itemId, quantity, null, null, null, note,
    )

    fun transfer(sourceLocationId: String, targetLocationId: String, itemId: String, quantity: Double, note: String?) = warehouseMovement(
        InventoryMovementType.TRANSFER, sourceLocationId, itemId, quantity, targetLocationId, null, null, note,
    )

    fun reserve(locationId: String, itemId: String, quantity: Double, requestId: String?, workOrderDocumentId: String?, note: String?) = warehouseMovement(
        InventoryMovementType.RESERVATION, locationId, itemId, quantity, null, requestId, workOrderDocumentId, note,
    )

    fun releaseReservation(locationId: String, itemId: String, quantity: Double, requestId: String?, workOrderDocumentId: String?, note: String?) = warehouseMovement(
        InventoryMovementType.RELEASE_RESERVATION, locationId, itemId, quantity, null, requestId, workOrderDocumentId, note,
    )

    private fun warehouseMovement(
        type: InventoryMovementType,
        locationId: String,
        itemId: String,
        quantity: Double,
        targetLocationId: String?,
        requestId: String?,
        workOrderDocumentId: String?,
        note: String?,
    ) = launchSafely(::fail) {
        inventoryRepository.recordMovement(
            InventoryMovement(
                id = UUID.randomUUID().toString(), organizationId = organizationId, locationId = locationId,
                targetLocationId = targetLocationId, itemId = itemId, requestId = requestId,
                workOrderDocumentId = workOrderDocumentId, type = type, quantity = quantity,
                occurredAtEpochMs = System.currentTimeMillis(), actorUserId = userId, note = note,
            ),
        )
        refresh(state.value.selectedVisitId)
    }

    private fun selectedVisitAction(block: suspend (String) -> Unit) = launchSafely(::fail) {
        val visitId = state.value.selectedVisitId ?: error("Сначала выберите выезд")
        block(visitId)
        refresh(visitId)
    }

    private suspend fun refresh(selectedId: String?) {
        updateState { it.copy(loading = true, error = null) }
        val visitData = operations.visits(organizationId, selectedId ?: state.value.selectedVisitId)
        val requests = operations.requests(organizationId)
        val organization = operations.organization(organizationId)
        val assets = operations.assets(organizationId)
        val employees = organization.employees.filter { it.active }
        val selected = visitData.selectedVisitId
        val execution = selected?.let { executionRepository.report(it) }
        val locations = inventoryRepository.locations(organizationId)
        val items = inventoryRepository.items(organizationId)
        val balances = inventoryRepository.balances(organizationId)
        val movements = inventoryRepository.movements(organizationId).take(100)
        val lowStock = inventoryRepository.lowStockItems(organizationId)
        val now = System.currentTimeMillis()
        val candidates = employees.map { employee ->
            DispatchCandidate(employee.id, employee.branchId, emptySet(), null, null, requests.count { it.assigneeEmployeeId == employee.id && it.status !in setOf(RequestStatus.CLOSED, RequestStatus.CANCELLED) })
        }
        val dispatch = requests.filter { it.assigneeEmployeeId == null && it.status !in setOf(RequestStatus.CLOSED, RequestStatus.CANCELLED) }
            .map { dispatchPolicy.choose(it, emptySet(), candidates, now) }
        val routes = employees.mapNotNull { employee ->
            val assigned = requests.filter { it.assigneeEmployeeId == employee.id && it.status !in setOf(RequestStatus.CLOSED, RequestStatus.CANCELLED) }
            if (assigned.isEmpty()) return@mapNotNull null
            val stops = assigned.map { request ->
                val obj = assets.objects.firstOrNull { it.id == request.serviceObjectId }
                RouteStop(request.id, obj?.address ?: obj?.name ?: request.title, windowStartEpochMs = request.plannedAtEpochMs, windowEndEpochMs = request.dueAtEpochMs, priority = request.priority)
            }
            routePlanner.optimize(employee.id, employee.branchId, stops, generatedAtEpochMs = now)
        }
        setState(
            FieldWorkUiState(
                loading = false, visits = visitData.visits, requests = requests, employees = employees, objects = assets.objects,
                checklist = visitData.checklist, selectedVisitId = selected, execution = execution,
                inventoryLocations = locations, inventoryItems = items, inventoryBalances = balances, inventoryMovements = movements,
                lowStock = lowStock, dispatch = dispatch, routes = routes,
            ),
        )
    }
    private fun fail(error: Throwable) = updateState { it.copy(loading = false, error = error.message ?: "field_work_failed") }
}
