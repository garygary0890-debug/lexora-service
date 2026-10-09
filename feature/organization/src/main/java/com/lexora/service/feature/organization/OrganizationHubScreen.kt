package com.lexora.service.feature.organization

import androidx.compose.runtime.Composable
import com.lexora.service.core.model.Branch
import com.lexora.service.core.model.Employee

@Composable
fun OrganizationHubScreen(
    branches: List<Branch>,
    inactiveBranches: List<Branch>,
    employees: List<Employee>,
    inactiveEmployees: List<Employee>,
    onSaveBranch: (BranchDraft, String?) -> Unit,
    onDeactivateBranch: (String) -> Unit,
    onActivateBranch: (String) -> Unit,
    onSaveEmployee: (EmployeeDraft, String?) -> Unit,
    onDeactivateEmployee: (String) -> Unit,
    onActivateEmployee: (String) -> Unit,
) {
    OrganizationScreen(
        branches = branches,
        inactiveBranches = inactiveBranches,
        employees = employees,
        inactiveEmployees = inactiveEmployees,
        onSaveBranch = onSaveBranch,
        onDeactivateBranch = onDeactivateBranch,
        onActivateBranch = onActivateBranch,
        onSaveEmployee = onSaveEmployee,
        onDeactivateEmployee = onDeactivateEmployee,
        onActivateEmployee = onActivateEmployee,
    )
}
