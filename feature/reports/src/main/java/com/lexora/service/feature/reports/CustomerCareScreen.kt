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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lexora.service.core.data.CustomerCareRepository
import com.lexora.service.core.data.InMemoryOrganizationRepository
import com.lexora.service.core.model.LoyaltyAccount
import com.lexora.service.core.model.LoyaltyTransaction
import com.lexora.service.core.model.QualityControlRecord
import com.lexora.service.core.model.QualityControlStatus
import com.lexora.service.core.model.RequestStatus
import com.lexora.service.core.model.ServiceRequest
import kotlinx.coroutines.launch

@Composable
fun CustomerCareScreen(requests: List<ServiceRequest>) {
    val context = LocalContext.current
    val repository = remember { CustomerCareRepository.create(context) }
    val organization = remember { InMemoryOrganizationRepository().activeOrganization() }
    val scope = rememberCoroutineScope()

    var accounts by remember { mutableStateOf<List<LoyaltyAccount>>(emptyList()) }
    var transactions by remember { mutableStateOf<List<LoyaltyTransaction>>(emptyList()) }
    var qualityRecords by remember { mutableStateOf<List<QualityControlRecord>>(emptyList()) }
    var clientNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedClientId by remember { mutableStateOf<String?>(null) }
    var pointsText by remember { mutableStateOf("") }
    var selectedRequestId by remember { mutableStateOf<String?>(null) }
    var ratingText by remember { mutableStateOf("5") }
    var checklistText by remember { mutableStateOf("") }
    var issueText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        val organizationId = organization?.id ?: return
        accounts = repository.accounts(organizationId)
        transactions = repository.transactions(organizationId)
        qualityRecords = repository.qualityRecords(organizationId)
        clientNames = repository.clientNames(organizationId)
        if (selectedClientId !in clientNames.keys) selectedClientId = clientNames.keys.firstOrNull()
    }

    LaunchedEffect(organization?.id) { reload() }

    val eligibleRequests = requests.filter {
        it.status == RequestStatus.WORK_COMPLETED || it.status == RequestStatus.CONFIRMATION || it.status == RequestStatus.CLOSED
    }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Лояльность и контроль качества", style = MaterialTheme.typography.headlineSmall)

        Text("Программа лояльности", style = MaterialTheme.typography.titleMedium)
        if (clientNames.isEmpty()) {
            Text("Нет активных клиентов")
        } else {
            Text("Клиент", style = MaterialTheme.typography.labelMedium)
            clientNames.forEach { (id, name) ->
                FilterChip(
                    selected = selectedClientId == id,
                    onClick = { selectedClientId = id },
                    label = { Text(name) },
                )
            }
            OutlinedTextField(
                value = pointsText,
                onValueChange = { pointsText = it.filter(Char::isDigit) },
                label = { Text("Бонусы") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val clientId = selectedClientId ?: return@Button
                    val points = pointsText.toLongOrNull() ?: return@Button
                    scope.launch {
                        runCatching { repository.accrue(organization!!.id, clientId, points, comment = "Ручное начисление") }
                            .onSuccess { message = "Бонусы начислены"; pointsText = ""; reload() }
                            .onFailure { message = it.message }
                    }
                }) { Text("Начислить") }
                OutlinedButton(onClick = {
                    val clientId = selectedClientId ?: return@OutlinedButton
                    val points = pointsText.toLongOrNull() ?: return@OutlinedButton
                    scope.launch {
                        runCatching { repository.redeem(organization!!.id, clientId, points, comment = "Ручное списание") }
                            .onSuccess { applied ->
                                message = if (applied) "Бонусы списаны" else "Недостаточно бонусов"
                                if (applied) pointsText = ""
                                reload()
                            }
                            .onFailure { message = it.message }
                    }
                }) { Text("Списать") }
            }
        }

        accounts.forEach { account ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(clientNames[account.clientId] ?: account.clientId, style = MaterialTheme.typography.titleSmall)
                    Text("Баланс: ${account.pointsBalance} бонусов")
                }
            }
        }
        if (transactions.isNotEmpty()) {
            Text("Последние операции", style = MaterialTheme.typography.titleSmall)
            transactions.take(10).forEach { tx ->
                val sign = if (tx.pointsDelta > 0) "+" else ""
                Text("${clientNames[tx.clientId] ?: tx.clientId}: $sign${tx.pointsDelta} · ${tx.type.name}")
            }
        }

        Text("Контроль качества", style = MaterialTheme.typography.titleMedium)
        if (eligibleRequests.isEmpty()) {
            Text("Нет заявок с завершёнными работами для проверки")
        } else {
            eligibleRequests.forEach { request ->
                FilterChip(
                    selected = selectedRequestId == request.id,
                    onClick = { selectedRequestId = request.id },
                    label = { Text("${request.number} · ${request.title}") },
                )
            }
            Button(onClick = {
                val requestId = selectedRequestId ?: return@Button
                scope.launch {
                    runCatching { repository.createQualityCheck(organization!!.id, requestId) }
                        .onSuccess { message = "Проверка качества создана"; reload() }
                        .onFailure { message = it.message }
                }
            }) { Text("Создать проверку") }
        }

        qualityRecords.forEach { record ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val request = requests.firstOrNull { it.id == record.requestId }
                    Text(request?.let { "${it.number} · ${it.title}" } ?: record.requestId, style = MaterialTheme.typography.titleSmall)
                    Text("Статус: ${record.status.name}")
                    record.rating?.let { Text("Оценка: $it/5") }
                    record.checklistResult?.let { Text("Проверка: $it") }
                    record.issueDescription?.let { Text("Замечание: $it") }
                    record.resolutionNote?.let { Text("Устранение: $it") }

                    if (record.status == QualityControlStatus.PENDING) {
                        OutlinedTextField(
                            value = ratingText,
                            onValueChange = { ratingText = it.filter(Char::isDigit).take(1) },
                            label = { Text("Оценка 1–5") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = checklistText,
                            onValueChange = { checklistText = it },
                            label = { Text("Результат проверки") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = issueText,
                            onValueChange = { issueText = it },
                            label = { Text("Замечание, если есть") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(onClick = {
                            val rating = ratingText.toIntOrNull() ?: return@Button
                            scope.launch {
                                runCatching { repository.completeQualityCheck(record, rating, checklistText, issueText.ifBlank { null }) }
                                    .onSuccess { message = "Контроль качества завершён"; checklistText = ""; issueText = ""; reload() }
                                    .onFailure { message = it.message }
                            }
                        }) { Text("Завершить проверку") }
                    }
                    if (record.status == QualityControlStatus.ISSUE_FOUND) {
                        OutlinedTextField(
                            value = checklistText,
                            onValueChange = { checklistText = it },
                            label = { Text("Как устранено замечание") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(onClick = {
                            scope.launch {
                                runCatching { repository.resolveQualityIssue(record, checklistText) }
                                    .onSuccess { message = "Замечание закрыто"; checklistText = ""; reload() }
                                    .onFailure { message = it.message }
                            }
                        }) { Text("Отметить устранённым") }
                    }
                }
            }
        }

        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}
