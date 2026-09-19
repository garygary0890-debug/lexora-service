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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.RequestStatus
import com.lexora.service.core.model.VisitStatus

private enum class FieldWorkTab { VISITS, DISPATCH, ROUTES }

@Composable
fun FieldWorkScreen(
    state: FieldWorkUiState,
    onCreateVisit: (String, String?) -> Unit,    onAssignRequest: (String, String?) -> Unit,
    onSelectVisit: (String) -> Unit,
    onChangeVisitStatus: (String, VisitStatus) -> Unit,
    onAddChecklistItem: (String) -> Unit,
    onToggleChecklistItem: (String) -> Unit,
) {
    var tab by remember { mutableStateOf(FieldWorkTab.DISPATCH) }
    var selectedVisitId by remember(state.visits) { mutableStateOf(state.selectedVisitId ?: state.visits.firstOrNull()?.id) }
    val selectedVisit = state.visits.firstOrNull { it.id == selectedVisitId }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Выездные работы", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(tab == FieldWorkTab.DISPATCH, { tab = FieldWorkTab.DISPATCH }, label = { Text("Диспетчеризация") })
            FilterChip(tab == FieldWorkTab.ROUTES, { tab = FieldWorkTab.ROUTES }, label = { Text("Маршруты") })
            FilterChip(tab == FieldWorkTab.VISITS, { tab = FieldWorkTab.VISITS }, label = { Text("Выезды") })
        }

        when (tab) {
            FieldWorkTab.DISPATCH -> DispatchBoard(state, onAssignRequest, onCreateVisit)
            FieldWorkTab.ROUTES -> RoutesBoard(state, onCreateVisit)
            FieldWorkTab.VISITS -> VisitsBoard(state, selectedVisitId, { id -> selectedVisitId = id; onSelectVisit(id) }, onChangeVisitStatus)
        }

        if (tab == FieldWorkTab.VISITS) selectedVisit?.let { visit ->
            Text("Чек-лист", style = MaterialTheme.typography.titleMedium)
            state.checklist.filter { it.visitId == visit.id }.forEach { item ->
                OutlinedButton(onClick = { onToggleChecklistItem(item.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("${if (item.state.name == "DONE") "✓" else "○"} ${item.title}")
                }
            }
            Button(onClick = { onAddChecklistItem(visit.id) }) { Text("Добавить пункт") }
        }
    }
}
@Composable
private fun DispatchBoard(
    state: FieldWorkUiState,
    onAssignRequest: (String, String?) -> Unit,
    onCreateVisit: (String, String?) -> Unit,
) {
    val unassigned = state.requests.filter { it.assigneeEmployeeId == null && it.status !in setOf(RequestStatus.CLOSED, RequestStatus.CANCELLED) }
    if (unassigned.isEmpty()) {
        Text("Все открытые заявки распределены.")
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(unassigned, key = { it.id }) { request ->
            val decision = state.dispatch.firstOrNull { it.requestId == request.id }
            val employee = state.employees.firstOrNull { it.id == decision?.employeeId }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${request.number} · ${request.title}", style = MaterialTheme.typography.titleMedium)
                    Text("Приоритет: ${request.priority.name}")
                    Text("Рекомендация: ${employee?.displayName ?: "нет подходящего исполнителя"}")
                    decision?.reasons?.forEach { Text("• $it") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (employee != null) {
                            Button(onClick = { onAssignRequest(request.id, employee.id) }) { Text("Назначить") }
                            OutlinedButton(onClick = { onCreateVisit(request.id, employee.id) }) { Text("Создать выезд") }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun RoutesBoard(state: FieldWorkUiState, onCreateVisit: (String, String?) -> Unit) {
    if (state.routes.isEmpty()) {
        Text("Нет маршрутов: сначала назначьте исполнителей на открытые заявки.")
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.routes, key = { it.employeeId }) { route ->
            val employee = state.employees.firstOrNull { it.id == route.employeeId }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(employee?.displayName ?: route.employeeId, style = MaterialTheme.typography.titleMedium)
                    route.stops.forEachIndexed { index, stop ->
                        val request = state.requests.firstOrNull { it.id == stop.requestId }
                        Text("${index + 1}. ${request?.number ?: stop.requestId} · ${stop.address}")
                        Text("   ${request?.title.orEmpty()} · ${request?.priority?.name.orEmpty()}")
                        val hasVisit = state.visits.any { it.requestId == stop.requestId && it.status != VisitStatus.CANCELLED }
                        if (!hasVisit) {
                            OutlinedButton(onClick = { onCreateVisit(stop.requestId, route.employeeId) }) { Text("Создать выезд") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VisitsBoard(
    state: FieldWorkUiState,
    selectedVisitId: String?,
    onSelect: (String) -> Unit,
    onChangeStatus: (String, VisitStatus) -> Unit,
) {
    if (state.visits.isEmpty()) { Text("Выезды ещё не созданы."); return }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.visits, key = { it.id }) { visit ->
            val request = state.requests.firstOrNull { it.id == visit.requestId }
            val employee = state.employees.firstOrNull { it.id == visit.employeeId }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${request?.number ?: visit.requestId} · ${request?.title.orEmpty()}", style = MaterialTheme.typography.titleMedium)
                    Text("Исполнитель: ${employee?.displayName ?: "не назначен"}")
                    Text("Статус: ${visit.status.name}${if (visit.id == selectedVisitId) " · выбран" else ""}")
                    OutlinedButton(onClick = { onSelect(visit.id) }) { Text("Открыть") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (visit.status) {
                            VisitStatus.PLANNED -> Button(onClick = { onChangeStatus(visit.id, VisitStatus.EN_ROUTE) }) { Text("Выехал") }
                            VisitStatus.EN_ROUTE -> Button(onClick = { onChangeStatus(visit.id, VisitStatus.ON_SITE) }) { Text("На месте") }
                            VisitStatus.ON_SITE -> Button(onClick = { onChangeStatus(visit.id, VisitStatus.COMPLETED) }) { Text("Завершить") }
                            VisitStatus.COMPLETED, VisitStatus.CANCELLED -> Unit
                        }
                        if (visit.status != VisitStatus.COMPLETED && visit.status != VisitStatus.CANCELLED) {
                            OutlinedButton(onClick = { onChangeStatus(visit.id, VisitStatus.CANCELLED) }) { Text("Отменить") }
                        }
                    }
                }
            }
        }
    }
}
