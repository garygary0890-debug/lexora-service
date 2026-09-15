package com.lexora.service.feature.organization

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.Branch
import com.lexora.service.core.model.Employee
import com.lexora.service.core.model.Organization

@Composable
fun OrganizationHubScreen(
    activeOrganization: Organization,
    organizations: List<Organization>,
    allowedOrganizationIds: Set<String>,
    canManageOrganization: Boolean,
    onCreateOrganization: (String) -> Unit,
    onSwitchOrganization: (String) -> Unit,
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
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.heightIn(max = 360.dp)) {
            OrganizationsScreen(
                activeOrganization = activeOrganization,
                organizations = organizations,
                allowedOrganizationIds = allowedOrganizationIds,
                canManageOrganization = canManageOrganization,
                onCreateOrganization = onCreateOrganization,
                onSwitchOrganization = onSwitchOrganization,
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
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
}
