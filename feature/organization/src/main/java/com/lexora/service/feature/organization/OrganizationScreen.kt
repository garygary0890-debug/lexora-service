package com.lexora.service.feature.organization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.Branch
import com.lexora.service.core.model.Employee

data class BranchDraft(
    val name: String,
    val address: String,
    val phone: String,
    val email: String,
    val workSchedule: String,
    val timeZoneId: String,
)

data class EmployeeDraft(
    val displayName: String,
    val position: String,
    val phone: String,
    val email: String,
    val branchId: String?,
)

@Composable
fun OrganizationScreen(
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
    var branchName by remember { mutableStateOf("") }
    var branchAddress by remember { mutableStateOf("") }
    var branchPhone by remember { mutableStateOf("") }
    var branchEmail by remember { mutableStateOf("") }
    var branchSchedule by remember { mutableStateOf("") }
    var timeZone by remember { mutableStateOf("Europe/Moscow") }
    var employeeName by remember { mutableStateOf("") }
    var employeePosition by remember { mutableStateOf("") }
    var employeePhone by remember { mutableStateOf("") }
    var employeeEmail by remember { mutableStateOf("") }
    var employeeBranchId by remember { mutableStateOf<String?>(null) }
    var showInactive by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Филиалы и сотрудники") }
        item {
            OutlinedButton(onClick = { showInactive = !showInactive }) {
                Text(if (showInactive) "Показать активные" else "Показать неактивные")
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Новый филиал")
                    OutlinedTextField(branchName, { branchName = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(branchAddress, { branchAddress = it }, label = { Text("Адрес") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(branchPhone, { branchPhone = it }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(branchEmail, { branchEmail = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(branchSchedule, { branchSchedule = it }, label = { Text("График работы") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(timeZone, { timeZone = it }, label = { Text("Часовой пояс") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        enabled = branchName.isNotBlank() && timeZone.isNotBlank(),
                        onClick = {
                            onSaveBranch(BranchDraft(branchName.trim(), branchAddress.trim(), branchPhone.trim(), branchEmail.trim(), branchSchedule.trim(), timeZone.trim()), null)
                            branchName = ""; branchAddress = ""; branchPhone = ""; branchEmail = ""; branchSchedule = ""
                        },
                    ) { Text("Добавить филиал") }
                }
            }
        }
        items(if (showInactive) inactiveBranches else branches, key = { it.id }) { branch ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(branch.name)
                    branch.address?.let { Text(it) }
                    branch.phone?.let { Text(it) }
                    branch.workSchedule?.let { Text(it) }
                    Text(branch.timeZoneId)
                    Button(onClick = { if (showInactive) onActivateBranch(branch.id) else onDeactivateBranch(branch.id) }) {
                        Text(if (showInactive) "Восстановить" else "Деактивировать")
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Новый сотрудник")
                    OutlinedTextField(employeeName, { employeeName = it }, label = { Text("ФИО") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(employeePosition, { employeePosition = it }, label = { Text("Должность") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(employeePhone, { employeePhone = it }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(employeeEmail, { employeeEmail = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    if (branches.isNotEmpty()) {
                        Text("Филиал: " + (branches.firstOrNull { it.id == employeeBranchId }?.name ?: "не выбран"))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            branches.take(3).forEach { branch ->
                                OutlinedButton(onClick = { employeeBranchId = branch.id }) { Text(branch.name) }
                            }
                        }
                    }
                    Button(
                        enabled = employeeName.isNotBlank(),
                        onClick = {
                            onSaveEmployee(EmployeeDraft(employeeName.trim(), employeePosition.trim(), employeePhone.trim(), employeeEmail.trim(), employeeBranchId), null)
                            employeeName = ""; employeePosition = ""; employeePhone = ""; employeeEmail = ""; employeeBranchId = null
                        },
                    ) { Text("Добавить сотрудника") }
                }
            }
        }
        items(if (showInactive) inactiveEmployees else employees, key = { it.id }) { employee ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(employee.displayName)
                    employee.position?.let { Text(it) }
                    employee.phone?.let { Text(it) }
                    Text("Филиал: " + (branches.firstOrNull { it.id == employee.branchId }?.name ?: "не назначен"))
                    Button(onClick = { if (showInactive) onActivateEmployee(employee.id) else onDeactivateEmployee(employee.id) }) {
                        Text(if (showInactive) "Восстановить" else "Деактивировать")
                    }
                }
            }
        }
    }
}
