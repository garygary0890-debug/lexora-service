package com.lexora.service.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.QualityControlStatus
import com.lexora.service.core.model.RequestStatus
import com.lexora.service.core.model.ServiceRequest

@Composable
fun CustomerCareScreen(requests: List<ServiceRequest>, viewModel: CustomerCareViewModel) {
    val state by viewModel.state.collectAsState()
    val data = state.data
    var selectedClientId by remember(data?.clientNames) { mutableStateOf(data?.clientNames?.keys?.firstOrNull()) }
    var pointsText by remember { mutableStateOf("") }
    var selectedRequestId by remember { mutableStateOf<String?>(null) }
    var ratingText by remember { mutableStateOf("5") }
    var checklistText by remember { mutableStateOf("") }
    var issueText by remember { mutableStateOf("") }

    val eligibleRequests = requests.filter {
        it.status == RequestStatus.WORK_COMPLETED || it.status == RequestStatus.CONFIRMATION || it.status == RequestStatus.CLOSED
    }
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Лояльность и контроль качества", style = MaterialTheme.typography.headlineSmall)
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (data == null) { Text("Загрузка данных…"); return@Column }

        Text("Программа лояльности", style = MaterialTheme.typography.titleMedium)
        data.clientNames.forEach { (id, name) ->
            FilterChip(selected = selectedClientId == id, onClick = { selectedClientId = id }, label = { Text(name) })
        }
        OutlinedTextField(pointsText, { pointsText = it.filter(Char::isDigit) }, label = { Text("Бонусы") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val client = selectedClientId ?: return@Button
                val points = pointsText.toLongOrNull() ?: return@Button
                viewModel.accrue(client, points); pointsText = ""
            }) { Text("Начислить") }
            OutlinedButton(onClick = {
                val client = selectedClientId ?: return@OutlinedButton
                val points = pointsText.toLongOrNull() ?: return@OutlinedButton
                viewModel.redeem(client, points); pointsText = ""
            }) { Text("Списать") }
        }
        data.accounts.forEach { account ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(data.clientNames[account.clientId] ?: account.clientId, style = MaterialTheme.typography.titleSmall)
                    Text("Баланс: ${account.pointsBalance} бонусов")
                }
            }
        }
        if (data.transactions.isNotEmpty()) {
            Text("Последние операции", style = MaterialTheme.typography.titleSmall)
            data.transactions.take(10).forEach { tx ->
                val sign = if (tx.pointsDelta > 0) "+" else ""
                Text("${data.clientNames[tx.clientId] ?: tx.clientId}: $sign${tx.pointsDelta} · ${tx.type.name}")
            }
        }

        Text("Контроль качества", style = MaterialTheme.typography.titleMedium)
        eligibleRequests.forEach { request ->
            FilterChip(
                selected = selectedRequestId == request.id,
                onClick = { selectedRequestId = request.id },
                label = { Text("${request.number} · ${request.title}") },
            )
        }
        Button(
            enabled = selectedRequestId != null,
            onClick = { selectedRequestId?.let(viewModel::createQualityCheck) },
        ) { Text("Создать проверку") }

        data.qualityRecords.forEach { record ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val request = requests.firstOrNull { it.id == record.requestId }
                    Text(request?.let { "${it.number} · ${it.title}" } ?: record.requestId, style = MaterialTheme.typography.titleSmall)
                    Text("Статус: ${record.status.name}")
                    record.rating?.let { Text("Оценка: $it/5") }
                    record.issueDescription?.let { Text("Замечание: $it") }
                    if (record.status == QualityControlStatus.PENDING) {
                        OutlinedTextField(ratingText, { ratingText = it.filter(Char::isDigit).take(1) }, label = { Text("Оценка 1–5") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(checklistText, { checklistText = it }, label = { Text("Результат проверки") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(issueText, { issueText = it }, label = { Text("Замечание, если есть") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            val rating = ratingText.toIntOrNull() ?: return@Button
                            viewModel.completeQualityCheck(record, rating, checklistText, issueText.ifBlank { null })
                            checklistText = ""; issueText = ""
                        }) { Text("Завершить проверку") }
                    }
                    if (record.status == QualityControlStatus.ISSUE_FOUND) {
                        OutlinedTextField(checklistText, { checklistText = it }, label = { Text("Как устранено замечание") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            if (checklistText.isNotBlank()) {
                                viewModel.resolveQualityIssue(record, checklistText); checklistText = ""
                            }
                        }) { Text("Отметить устранённым") }
                    }
                }
            }
        }
    }
}
