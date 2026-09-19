package com.lexora.service.feature.requests

import com.lexora.service.core.domain.DocumentFeatureOperations
import com.lexora.service.core.domain.RequestOperationalDetails
import com.lexora.service.core.domain.ServiceOperations
import com.lexora.service.core.model.*
import com.lexora.service.core.presentation.LexoraViewModel

data class RequestsUiState(
    val loading: Boolean = true,
    val requests: List<ServiceRequest> = emptyList(),
    val clients: List<Client> = emptyList(),
    val objects: List<ServiceObject> = emptyList(),
    val equipment: List<Equipment> = emptyList(),
    val branches: List<Branch> = emptyList(),
    val employees: List<Employee> = emptyList(),
    val contracts: List<ServiceContract> = emptyList(),
    val selectedRequestId: String? = null,
    val details: RequestOperationalDetails? = null,
    val error: String? = null,
)

class RequestsViewModel(
    private val organizationId: String,
    private val userId: String,
    private val operations: ServiceOperations,
    private val documents: DocumentFeatureOperations,
) : LexoraViewModel<RequestsUiState>(RequestsUiState()) {
    init { reload() }

    fun reload() = launchSafely(::fail) { refresh(state.value.selectedRequestId) }
    fun open(id: String) = launchSafely(::fail) { refresh(id) }
    fun close() = updateState { it.copy(selectedRequestId = null, details = null) }

    fun save(draft: RequestDraft, existingId: String?) = launchSafely(::fail) {
        operations.saveRequest(organizationId, userId, SaveRequestCommand(
            title = draft.title, description = draft.description, priority = draft.priority,
            clientId = draft.clientId, serviceObjectId = draft.serviceObjectId, equipmentId = draft.equipmentId,
            contractId = draft.contractId, branchId = draft.branchId, assigneeEmployeeId = draft.assigneeEmployeeId,
            assigneeTeamName = draft.assigneeTeamName, plannedAtEpochMs = draft.plannedAtEpochMs,
            dueAtEpochMs = draft.dueAtEpochMs, slaDeadlineEpochMs = draft.slaDeadlineEpochMs,
        ), existingId)
        refresh(existingId)
    }

    fun assign(id: String, employeeId: String?, teamName: String?) = launchSafely(::fail) {
        operations.assignRequest(organizationId, userId, id, employeeId, teamName)
        refresh(id)
    }

    fun changeStatus(id: String, target: RequestStatus) = launchSafely(::fail) {
        operations.changeRequestStatus(organizationId, userId, id, target)
        refresh(id)
    }

    private suspend fun refresh(selectedId: String?) {
        updateState { it.copy(loading = true, error = null) }
        val clients = operations.clients(organizationId)
        val assets = operations.assets(organizationId)
        val org = operations.organization(organizationId)
        val contracts = documents.snapshot(organizationId, null).contracts.filter { !it.archived }
        setState(RequestsUiState(
            loading = false, requests = operations.requests(organizationId),
            clients = clients.active, objects = assets.objects, equipment = assets.equipment,
            branches = org.branches, employees = org.employees, contracts = contracts,
            selectedRequestId = selectedId,
            details = selectedId?.let { operations.requestOperationalDetails(organizationId, it) },
        ))
    }

    private fun fail(error: Throwable) = updateState { it.copy(loading = false, error = error.message ?: "requests_failed") }
}
