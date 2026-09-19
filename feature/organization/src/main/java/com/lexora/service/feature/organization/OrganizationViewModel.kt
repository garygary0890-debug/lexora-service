package com.lexora.service.feature.organization

import com.lexora.service.core.model.SaveBranchCommand
import com.lexora.service.core.model.SaveEmployeeCommand
import com.lexora.service.core.domain.ServiceOperations
import com.lexora.service.core.model.Branch
import com.lexora.service.core.model.Employee
import com.lexora.service.core.presentation.LexoraViewModel

data class OrganizationUiState(
    val loading: Boolean = true,
    val branches: List<Branch> = emptyList(),
    val inactiveBranches: List<Branch> = emptyList(),
    val employees: List<Employee> = emptyList(),
    val inactiveEmployees: List<Employee> = emptyList(),
    val error: String? = null,
)

class OrganizationViewModel(
    private val organizationId: String,
    private val userId: String,
    private val operations: ServiceOperations,
) : LexoraViewModel<OrganizationUiState>(OrganizationUiState()) {
    init { reload() }

    fun reload() = launchSafely(::fail) {
        updateState { it.copy(loading = true, error = null) }
        val data = operations.organization(organizationId)
        setState(OrganizationUiState(false, data.branches, data.inactiveBranches, data.employees, data.inactiveEmployees))
    }

    fun saveBranch(draft: BranchDraft, existingId: String?) = launchSafely(::fail) {
        operations.saveBranch(organizationId, userId, SaveBranchCommand(draft.name, draft.address, draft.phone, draft.email, draft.workSchedule, draft.timeZoneId), existingId)
        refreshData()
    }

    fun setBranchActive(id: String, active: Boolean) = launchSafely(::fail) {
        operations.setBranchActive(organizationId, userId, id, active); refreshData()
    }

    fun saveEmployee(draft: EmployeeDraft, existingId: String?) = launchSafely(::fail) {
        operations.saveEmployee(organizationId, userId, SaveEmployeeCommand(draft.displayName, draft.position, draft.phone, draft.email, draft.branchId), existingId)
        refreshData()
    }

    fun setEmployeeActive(id: String, active: Boolean) = launchSafely(::fail) {
        operations.setEmployeeActive(organizationId, userId, id, active); refreshData()
    }

    private suspend fun refreshData() {
        val data = operations.organization(organizationId)
        updateState { it.copy(loading = false, branches = data.branches, inactiveBranches = data.inactiveBranches, employees = data.employees, inactiveEmployees = data.inactiveEmployees, error = null) }
    }

    private fun fail(error: Throwable) = updateState { it.copy(loading = false, error = error.message ?: "organization_failed") }
}

