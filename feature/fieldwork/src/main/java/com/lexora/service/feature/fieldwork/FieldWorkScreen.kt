package com.lexora.service.feature.fieldwork

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.Employee
import com.lexora.service.core.model.ServiceRequest
import com.lexora.service.core.model.ServiceVisit
import com.lexora.service.core.model.VisitChecklistItem
import com.lexora.service.core.model.VisitStatus

@Composable
fun FieldWorkScreen(
    visits: List<ServiceVisit>,
    requests: List<ServiceRequest>,
    employees: List<Employee>,
    checklist: List<VisitChecklistItem>,
    onCreateVisit: (requestId: String, employeeId: String?) -> Unit,
    onSelectVisit: (String) -> Unit,
    onChangeVisitStatus: (String, VisitStatus) -> Unit,
    onAddChecklistItem: (String) -> Unit,
    onToggleChecklistItem: (String) -> Unit,
) {
    var selectedVisitId by remember(visits) { mutableStateOf(visits.firstOrNull()?.id) }
    val selectedVisit = visits.firstOrNull { it.id == selectedVisitId }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.fieldwork_title), style = MaterialTheme.typography.headlineMedium)

        if (requests.isNotEmpty()) {
            Button(onClick = {
                val request = requests.first()
                val employee = employees.firstOrNull { it.active }
                onCreateVisit(request.id, employee?.id)
            }) {
                Text(stringResource(R.string.fieldwork_create_visit))
            }
        }

        if (visits.isEmpty()) {
            Text(stringResource(R.string.fieldwork_empty))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visits, key = { it.id }) { visit ->
                    val requestNumber = requests.firstOrNull { it.id == visit.requestId }?.number ?: visit.requestId
                    val employeeName = employees.firstOrNull { it.id == visit.employeeId }?.displayName
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${stringResource(R.string.fieldwork_request)}: $requestNumber", style = MaterialTheme.typography.titleMedium)
                            employeeName?.let { Text("${stringResource(R.string.fieldwork_employee)}: $it") }
                            Text(statusLabel(visit.status))
                            OutlinedButton(onClick = {
                                selectedVisitId = visit.id
                                onSelectVisit(visit.id)
                            }) { Text(stringResource(R.string.fieldwork_checklist)) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                when (visit.status) {
                                    VisitStatus.PLANNED -> Button(onClick = { onChangeVisitStatus(visit.id, VisitStatus.EN_ROUTE) }) { Text(stringResource(R.string.fieldwork_start)) }
                                    VisitStatus.EN_ROUTE -> Button(onClick = { onChangeVisitStatus(visit.id, VisitStatus.ON_SITE) }) { Text(stringResource(R.string.fieldwork_on_site)) }
                                    VisitStatus.ON_SITE -> Button(onClick = { onChangeVisitStatus(visit.id, VisitStatus.COMPLETED) }) { Text(stringResource(R.string.fieldwork_complete)) }
                                    VisitStatus.COMPLETED, VisitStatus.CANCELLED -> Unit
                                }
                                if (visit.status != VisitStatus.COMPLETED && visit.status != VisitStatus.CANCELLED) {
                                    OutlinedButton(onClick = { onChangeVisitStatus(visit.id, VisitStatus.CANCELLED) }) { Text(stringResource(R.string.fieldwork_cancel)) }
                                }
                            }
                        }
                    }
                }
            }
        }

        selectedVisit?.let { visit ->
            Text(stringResource(R.string.fieldwork_checklist), style = MaterialTheme.typography.titleMedium)
            checklist.filter { it.visitId == visit.id }.forEach { item ->
                OutlinedButton(onClick = { onToggleChecklistItem(item.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("${if (item.state.name == "DONE") "✓" else "○"} ${item.title}")
                }
            }
            Button(onClick = { onAddChecklistItem(visit.id) }) { Text(stringResource(R.string.fieldwork_add_check)) }
            Text(stringResource(R.string.fieldwork_photo_foundation))
            Text(stringResource(R.string.fieldwork_signature_foundation))
        }
    }
}

@Composable
private fun statusLabel(status: VisitStatus): String = when (status) {
    VisitStatus.PLANNED -> stringResource(R.string.fieldwork_status_planned)
    VisitStatus.EN_ROUTE -> stringResource(R.string.fieldwork_status_en_route)
    VisitStatus.ON_SITE -> stringResource(R.string.fieldwork_status_on_site)
    VisitStatus.COMPLETED -> stringResource(R.string.fieldwork_status_completed)
    VisitStatus.CANCELLED -> stringResource(R.string.fieldwork_status_cancelled)
}
