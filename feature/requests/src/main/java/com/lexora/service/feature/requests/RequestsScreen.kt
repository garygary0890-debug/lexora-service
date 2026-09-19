package com.lexora.service.feature.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lexora.service.core.domain.RequestWorkflow
import com.lexora.service.core.model.RequestPriority
import com.lexora.service.core.model.RequestStatus
import com.lexora.service.core.model.ServiceRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RequestsScreen(
    state: RequestsUiState,
    onOpen: (String) -> Unit,
    onClose: () -> Unit,
    onSave: (RequestDraft, String?) -> Unit,
    onAssign: (String, String?, String?) -> Unit,
    onChangeStatus: (String, RequestStatus) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<RequestStatus?>(null) }
    var editing by remember { mutableStateOf<ServiceRequest?>(null) }
    var creating by remember { mutableStateOf(false) }
    val selected = state.requests.firstOrNull { it.id == state.selectedRequestId }
    val filtered = remember(state.requests, query, statusFilter) {
        state.requests.filter { request ->
            (statusFilter == null || request.status == statusFilter) &&
                (query.isBlank() || request.number.contains(query, true) || request.title.contains(query, true))
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.requests_title), style = MaterialTheme.typography.headlineMedium)
            Button(onClick = { creating = true }) { Text(stringResource(R.string.request_create)) }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.request_search)) },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(statusFilter == null, { statusFilter = null }, label = { Text("Все") })
            FilterChip(statusFilter == RequestStatus.NEW, { statusFilter = RequestStatus.NEW }, label = { Text("Новые") })
            FilterChip(statusFilter == RequestStatus.IN_PROGRESS, { statusFilter = RequestStatus.IN_PROGRESS }, label = { Text("В работе") })
        }
        if (filtered.isEmpty()) {
            Text(stringResource(R.string.requests_empty))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { request ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${request.number} · ${request.title}", style = MaterialTheme.typography.titleMedium)
                            Text("${statusLabel(request.status)} · ${priorityLabel(request.priority)}")
                            val client = state.clients.firstOrNull { it.id == request.clientId }?.displayName ?: "Клиент не выбран"
                            val assignee = state.employees.firstOrNull { it.id == request.assigneeEmployeeId }?.displayName
                            Text("Клиент: $client")
                            Text("Исполнитель: ${assignee ?: request.assigneeTeamName ?: "не назначен"}")
                            Text("SLA: ${formatDateTime(request.slaDeadlineEpochMs)}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onOpen(request.id) }) { Text(stringResource(R.string.request_open)) }
                                RequestWorkflow.nextStatuses(request.status).take(2).forEach { next ->
                                    TextButton(onClick = { onChangeStatus(request.id, next) }) { Text(statusLabel(next)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { request ->
        RequestDetailsDialog(request, state, onClose, { editing = request }, onAssign)
    }
    if (creating) RequestEditor(null, state, { creating = false }) { onSave(it, null); creating = false }
    editing?.let { request ->
        RequestEditor(request, state, { editing = null }) { onSave(it, request.id); editing = null }
    }
}
data class RequestDraft(
    val title: String,
    val description: String,
    val priority: RequestPriority,
    val clientId: String?,
    val serviceObjectId: String?,
    val equipmentId: String?,
    val contractId: String?,
    val branchId: String?,
    val assigneeEmployeeId: String?,
    val assigneeTeamName: String?,
    val plannedAtEpochMs: Long?,
    val dueAtEpochMs: Long?,
    val slaDeadlineEpochMs: Long?,
)

@Composable
private fun RequestDetailsDialog(
    request: ServiceRequest,
    state: RequestsUiState,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onAssign: (String, String?, String?) -> Unit,
) {
    val details = state.details
    val client = state.clients.firstOrNull { it.id == request.clientId }?.displayName ?: "—"
    val obj = state.objects.firstOrNull { it.id == request.serviceObjectId }
    val equipment = state.equipment.firstOrNull { it.id == request.equipmentId }
    val contract = state.contracts.firstOrNull { it.id == request.contractId }
    val employee = state.employees.firstOrNull { it.id == request.assigneeEmployeeId }
    val branch = state.branches.firstOrNull { it.id == request.branchId }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${request.number} · ${request.title}") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item { Text("Клиент: $client") }
                item { Text("Объект: ${obj?.name ?: "—"}") }
                item { Text("Оборудование: ${equipment?.let { "${it.type} ${it.make.orEmpty()} ${it.model.orEmpty()}".trim() } ?: "—"}") }
                item { Text("Договор: ${contract?.let { "${it.number} · ${it.subject}" } ?: "—"}") }
                item { Text("Проблема: ${request.description?.takeIf { it.isNotBlank() } ?: request.title}") }
                item { Text("Филиал: ${branch?.name ?: "—"}") }
                item { Text("Исполнитель: ${employee?.displayName ?: "—"}") }
                item { Text("Бригада: ${request.assigneeTeamName ?: "—"}") }
                item { Text("План: ${formatDateTime(request.plannedAtEpochMs)}") }
                item { Text("Срок: ${formatDateTime(request.dueAtEpochMs)}") }
                item { Text("SLA: ${formatDateTime(request.slaDeadlineEpochMs)}") }
                item { Text("Работы", style = MaterialTheme.typography.titleSmall) }
                if (details?.works.isNullOrEmpty()) item { Text("Нет связанных работ") }
                details?.works?.forEach { work -> item { Text("• ${work.title}: ${work.quantity} ${work.unit.orEmpty()}") } }
                item { Text("Материалы", style = MaterialTheme.typography.titleSmall) }
                if (details?.materials.isNullOrEmpty()) item { Text("Нет связанных материалов") }
                details?.materials?.forEach { material -> item { Text("• ${material.title}: ${material.quantity} ${material.unit.orEmpty()}") } }
                item { Text("Документы", style = MaterialTheme.typography.titleSmall) }
                if (details?.documents.isNullOrEmpty()) item { Text("Нет связанных документов") }
                details?.documents?.forEach { document ->
                    item { Text("• ${document.number} · ${document.type.name} · ${document.status.name}") }
                }
                item { Text("Финансы", style = MaterialTheme.typography.titleSmall) }
                if (details?.payments.isNullOrEmpty()) item { Text("Нет связанных платежей") }
                details?.payments?.forEach { payment ->
                    item { Text("• ${payment.amountMinor / 100.0} ${payment.currency} · ${payment.status.name}") }
                }
                item { Text("Выезды: ${details?.visits?.size ?: 0}") }
            }
        },
        confirmButton = { Button(onClick = onEdit) { Text("Редактировать") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

@Composable
private fun RequestEditor(
    request: ServiceRequest?,
    state: RequestsUiState,
    onDismiss: () -> Unit,
    onSave: (RequestDraft) -> Unit,
) {
    var title by remember(request?.id) { mutableStateOf(request?.title.orEmpty()) }
    var description by remember(request?.id) { mutableStateOf(request?.description.orEmpty()) }
    var priority by remember(request?.id) { mutableStateOf(request?.priority ?: RequestPriority.NORMAL) }
    var clientId by remember(request?.id) { mutableStateOf(request?.clientId) }
    var objectId by remember(request?.id) { mutableStateOf(request?.serviceObjectId) }
    var equipmentId by remember(request?.id) { mutableStateOf(request?.equipmentId) }
    var contractId by remember(request?.id) { mutableStateOf(request?.contractId) }
    var branchId by remember(request?.id) { mutableStateOf(request?.branchId) }
    var employeeId by remember(request?.id) { mutableStateOf(request?.assigneeEmployeeId) }
    var teamName by remember(request?.id) { mutableStateOf(request?.assigneeTeamName.orEmpty()) }
    var plannedText by remember(request?.id) { mutableStateOf(formatDateTimeInput(request?.plannedAtEpochMs)) }
    var dueText by remember(request?.id) { mutableStateOf(formatDateTimeInput(request?.dueAtEpochMs)) }
    var slaText by remember(request?.id) { mutableStateOf(formatDateTimeInput(request?.slaDeadlineEpochMs)) }
    var showRequired by remember(request?.id) { mutableStateOf(false) }

    val objects = state.objects.filter { it.clientId == clientId }
    if (objectId != null && objects.none { it.id == objectId }) objectId = null
    val equipment = state.equipment.filter { it.serviceObjectId == objectId }
    if (equipmentId != null && equipment.none { it.id == equipmentId }) equipmentId = null
    val contracts = state.contracts.filter { it.clientId == clientId }
    if (contractId != null && contracts.none { it.id == contractId }) contractId = null
    val employees = state.employees.filter { branchId == null || it.branchId == null || it.branchId == branchId }
    if (employeeId != null && employees.none { it.id == employeeId }) employeeId = null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (request == null) "Создать заявку" else request.number) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { OutlinedTextField(title, { title = it; showRequired = false }, label = { Text("Проблема / тема заявки *") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(description, { description = it }, label = { Text("Описание проблемы") }, modifier = Modifier.fillMaxWidth()) }
                item { SelectField("Клиент *", clientId, state.clients.map { it.id to it.displayName }) { clientId = it; objectId = null; equipmentId = null; contractId = null } }
                item { SelectField("Объект *", objectId, objects.map { it.id to it.name }) { objectId = it; equipmentId = null } }
                item { SelectField("Оборудование *", equipmentId, equipment.map { it.id to equipmentLabel(it) }) { equipmentId = it } }
                item { SelectField("Договор", contractId, contracts.map { it.id to "${it.number} · ${it.subject}" }) { contractId = it } }
                item { SelectField("Филиал", branchId, state.branches.map { it.id to it.name }) { branchId = it; employeeId = null } }
                item { SelectField("Исполнитель", employeeId, employees.map { it.id to it.displayName }) { employeeId = it } }
                item { OutlinedTextField(teamName, { teamName = it }, label = { Text("Бригада / группа") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Text("Приоритет")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RequestPriority.entries.forEach { value ->
                            FilterChip(priority == value, { priority = value }, label = { Text(priorityLabel(value)) })
                        }
                    }
                }
                item { OutlinedTextField(plannedText, { plannedText = it }, label = { Text("План, дд.мм.гггг чч:мм") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(dueText, { dueText = it }, label = { Text("Срок выполнения, дд.мм.гггг чч:мм") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(slaText, { slaText = it }, label = { Text("SLA, дд.мм.гггг чч:мм") }, modifier = Modifier.fillMaxWidth()) }
                if (showRequired) item {
                    Text("Заполните тему, клиента, объект и оборудование. Даты должны быть в формате дд.мм.гггг чч:мм.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val planned = parseDateTime(plannedText)
                val due = parseDateTime(dueText)
                val sla = parseDateTime(slaText)
                val datesValid = (plannedText.isBlank() || planned != null) && (dueText.isBlank() || due != null) && (slaText.isBlank() || sla != null)
                if (title.isBlank() || clientId == null || objectId == null || equipmentId == null || !datesValid) {
                    showRequired = true
                } else {
                    onSave(RequestDraft(title.trim(), description.trim(), priority, clientId, objectId, equipmentId, contractId, branchId, employeeId, teamName.trim().ifBlank { null }, planned, due, sla))
                }
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
@Composable
private fun SelectField(
    label: String,
    selectedId: String?,
    options: List<Pair<String, String>>,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second ?: "Не выбрано"
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: $selectedLabel")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Не выбрано") }, onClick = { onSelect(null); expanded = false })
            options.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); expanded = false })
            }
        }
    }
}

private fun equipmentLabel(value: com.lexora.service.core.model.Equipment): String =
    listOfNotNull(value.type, value.make, value.model, value.serialNumber?.let { "№ $it" }).joinToString(" · ")

private val requestDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).apply { isLenient = false }
private fun formatDateTime(value: Long?): String = value?.let { requestDateFormat.format(Date(it)) } ?: "—"
private fun formatDateTimeInput(value: Long?): String = value?.let { requestDateFormat.format(Date(it)) }.orEmpty()
private fun parseDateTime(value: String): Long? = if (value.isBlank()) null else runCatching { requestDateFormat.parse(value.trim())?.time }.getOrNull()

@Composable
private fun statusLabel(status: RequestStatus): String = status.name

@Composable
private fun priorityLabel(priority: RequestPriority): String = priority.name
