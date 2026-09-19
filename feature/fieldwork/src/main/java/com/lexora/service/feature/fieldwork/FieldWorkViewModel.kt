package com.lexora.service.feature.fieldwork

import com.lexora.service.core.domain.AutoDispatchPolicy
import com.lexora.service.core.domain.RoutePlanner
import com.lexora.service.core.domain.ServiceOperations
import com.lexora.service.core.model.*
import com.lexora.service.core.presentation.LexoraViewModel

data class FieldWorkUiState(
    val loading: Boolean = true,
    val visits: List<ServiceVisit> = emptyList(),
    val requests: List<ServiceRequest> = emptyList(),
    val employees: List<Employee> = emptyList(),
    val objects: List<ServiceObject> = emptyList(),
    val checklist: List<VisitChecklistItem> = emptyList(),
    val selectedVisitId: String? = null,
    val dispatch: List<DispatchDecision> = emptyList(),
    val routes: List<OptimizedRoute> = emptyList(),
    val error: String? = null,
)

class FieldWorkViewModel(
    private val organizationId: String,
    private val userId: String,
    private val operations: ServiceOperations,
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
    fun changeStatus(id: String, target: VisitStatus) = launchSafely(::fail) { operations.changeVisitStatus(organizationId, userId, id, target); refresh(id) }
    fun addChecklistItem(visitId: String) = launchSafely(::fail) { operations.addChecklistItem(organizationId, userId, visitId); refresh(visitId) }
    fun toggleChecklistItem(itemId: String) = launchSafely(::fail) {
        val current = state.value.checklist.firstOrNull { it.id == itemId } ?: return@launchSafely
        operations.toggleChecklistItem(organizationId, userId, current); refresh(current.visitId)
    }

    private suspend fun refresh(selectedId: String?) {
        updateState { it.copy(loading = true, error = null) }
        val visitData = operations.visits(organizationId, selectedId ?: state.value.selectedVisitId)
        val requests = operations.requests(organizationId)
        val organization = operations.organization(organizationId)
        val assets = operations.assets(organizationId)
        val employees = organization.employees.filter { it.active }
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
        setState(FieldWorkUiState(false, visitData.visits, requests, employees, assets.objects, visitData.checklist, visitData.selectedVisitId, dispatch, routes))
    }
    private fun fail(error: Throwable) = updateState { it.copy(loading = false, error = error.message ?: "field_work_failed") }
}
