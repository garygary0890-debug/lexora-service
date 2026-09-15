package com.lexora.service.feature.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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

@Composable
fun RequestsScreen(
    requests: List<ServiceRequest>,
    onSave: (RequestDraft, String?) -> Unit,
    onChangeStatus: (String, RequestStatus) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<RequestStatus?>(null) }
    var editing by remember { mutableStateOf<ServiceRequest?>(null) }
    var creating by remember { mutableStateOf(false) }

    val filtered = remember(requests, query, statusFilter) {
        requests.filter { request ->
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
            FilterChip(selected = statusFilter == null, onClick = { statusFilter = null }, label = { Text(stringResource(R.string.request_filter_all)) })
            FilterChip(selected = statusFilter == RequestStatus.NEW, onClick = { statusFilter = RequestStatus.NEW }, label = { Text(stringResource(R.string.request_status_new)) })
            FilterChip(selected = statusFilter == RequestStatus.IN_PROGRESS, onClick = { statusFilter = RequestStatus.IN_PROGRESS }, label = { Text(stringResource(R.string.request_status_in_progress)) })
            FilterChip(selected = statusFilter == RequestStatus.CLOSED, onClick = { statusFilter = RequestStatus.CLOSED }, label = { Text(stringResource(R.string.request_status_closed)) })
        }

        if (filtered.isEmpty()) {
            Text(stringResource(R.string.requests_empty))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { request ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${request.number} · ${request.title}", style = MaterialTheme.typography.titleMedium)
                            Text(statusLabel(request.status))
                            Text(priorityLabel(request.priority))
                            request.slaDeadlineEpochMs?.let { Text(stringResource(R.string.request_sla_present)) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { editing = request }) { Text(stringResource(R.string.request_open)) }
                                RequestWorkflow.nextStatuses(request.status).forEach { next ->
                                    TextButton(onClick = { onChangeStatus(request.id, next) }) { Text(statusLabel(next)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        RequestEditor(
            request = null,
            onDismiss = { creating = false },
            onSave = { draft -> onSave(draft, null); creating = false },
        )
    }
    editing?.let { request ->
        RequestEditor(
            request = request,
            onDismiss = { editing = null },
            onSave = { draft -> onSave(draft, request.id); editing = null },
        )
    }
}

data class RequestDraft(
    val title: String,
    val description: String,
    val priority: RequestPriority,
)

@Composable
private fun RequestEditor(
    request: ServiceRequest?,
    onDismiss: () -> Unit,
    onSave: (RequestDraft) -> Unit,
) {
    var title by remember(request?.id) { mutableStateOf(request?.title.orEmpty()) }
    var description by remember(request?.id) { mutableStateOf(request?.description.orEmpty()) }
    var priority by remember(request?.id) { mutableStateOf(request?.priority ?: RequestPriority.NORMAL) }
    var showRequired by remember(request?.id) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (request == null) stringResource(R.string.request_create) else request.number) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it; showRequired = false }, label = { Text(stringResource(R.string.request_title)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text(stringResource(R.string.request_description)) }, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.request_priority))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RequestPriority.entries.forEach { value ->
                        FilterChip(selected = priority == value, onClick = { priority = value }, label = { Text(priorityLabel(value)) })
                    }
                }
                if (showRequired) Text(stringResource(R.string.request_required), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isBlank()) showRequired = true
                else onSave(RequestDraft(title.trim(), description.trim(), priority))
            }) { Text(stringResource(R.string.request_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.request_cancel)) } },
    )
}

@Composable
private fun statusLabel(status: RequestStatus): String = when (status) {
    RequestStatus.NEW -> stringResource(R.string.request_status_new)
    RequestStatus.QUALIFICATION -> stringResource(R.string.request_status_qualification)
    RequestStatus.PLANNED -> stringResource(R.string.request_status_planned)
    RequestStatus.IN_PROGRESS -> stringResource(R.string.request_status_in_progress)
    RequestStatus.WAITING -> stringResource(R.string.request_status_waiting)
    RequestStatus.WORK_COMPLETED -> stringResource(R.string.request_status_work_completed)
    RequestStatus.CONFIRMATION -> stringResource(R.string.request_status_confirmation)
    RequestStatus.CLOSED -> stringResource(R.string.request_status_closed)
    RequestStatus.CANCELLED -> stringResource(R.string.request_status_cancelled)
}

@Composable
private fun priorityLabel(priority: RequestPriority): String = when (priority) {
    RequestPriority.LOW -> stringResource(R.string.request_priority_low)
    RequestPriority.NORMAL -> stringResource(R.string.request_priority_normal)
    RequestPriority.HIGH -> stringResource(R.string.request_priority_high)
    RequestPriority.URGENT -> stringResource(R.string.request_priority_urgent)
}
