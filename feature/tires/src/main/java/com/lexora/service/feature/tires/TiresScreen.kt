package com.lexora.service.feature.tires

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.lexora.service.core.data.TireRepository
import com.lexora.service.core.model.Organization
import com.lexora.service.core.model.TireDiagnostic
import com.lexora.service.core.model.TireQueueItem
import com.lexora.service.core.model.TireQueueStatus
import com.lexora.service.core.model.TireStorageItem
import com.lexora.service.core.model.TireStorageStatus
import com.lexora.service.core.model.TireWorkEntry
import kotlinx.coroutines.launch

@Composable
fun TiresScreen(organization: Organization) {
    val context = LocalContext.current
    val repository = remember { TireRepository.create(context) }
    val scope = rememberCoroutineScope()

    var queue by remember { mutableStateOf<List<TireQueueItem>>(emptyList()) }
    var diagnostics by remember { mutableStateOf<List<TireDiagnostic>>(emptyList()) }
    var workEntries by remember { mutableStateOf<List<TireWorkEntry>>(emptyList()) }
    var storage by remember { mutableStateOf<List<TireStorageItem>>(emptyList()) }

    suspend fun reload() {
        queue = repository.queue(organization.id)
        diagnostics = repository.diagnostics(organization.id)
        workEntries = repository.workEntries(organization.id)
        storage = repository.storage(organization.id)
    }

    LaunchedEffect(organization.id) { reload() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Шиномонтаж", style = MaterialTheme.typography.headlineMedium)
        Text("Очередь", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { scope.launch { repository.addQueueItem(organization.id); reload() } }) { Text("Добавить в очередь") }
        queue.sortedBy { it.position }.forEach { item ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("№${item.position}")
                        item.requestId?.let { Text("Заявка: $it", style = MaterialTheme.typography.bodySmall) }
                        item.vehicleId?.let { Text("Автомобиль: $it", style = MaterialTheme.typography.bodySmall) }
                        Text("Статус: ${item.status.name}")
                    }
                    if (item.status != TireQueueStatus.COMPLETED && item.status != TireQueueStatus.CANCELLED) {
                        OutlinedButton(onClick = { scope.launch { repository.advanceQueueItem(item); reload() } }) { Text("Далее") }
                    }
                }
            }
        }

        Text("Диагностика", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { scope.launch { repository.addDiagnostic(organization.id); reload() } }) { Text("Добавить диагностику") }
        diagnostics.take(20).forEach { diagnostic ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Протектор: ${diagnostic.treadDepthMm ?: "—"} мм")
                    diagnostic.pressureNote?.let { Text(it) }
                    diagnostic.damageNote?.let { Text("Повреждения: $it") }
                    diagnostic.recommendation?.let { Text("Рекомендация: $it") }
                }
            }
        }

        Text("Работы", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { scope.launch { repository.addWorkEntry(organization.id); reload() } }) { Text("Добавить работу") }
        workEntries.take(20).forEach { work -> Text("${work.title} × ${work.quantity}") }

        Text("Хранение шин", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { scope.launch { repository.addStorageItem(organization.id); reload() } }) { Text("Принять на хранение") }
        storage.forEach { item ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(item.storageCode, style = MaterialTheme.typography.titleSmall)
                        Text("${item.tireDescription}, ${item.quantity} шт.")
                        item.location?.let { Text("Место: $it") }
                        Text("Статус: ${item.status.name}")
                    }
                    if (item.status == TireStorageStatus.STORED) {
                        OutlinedButton(onClick = { scope.launch { repository.issueStorageItem(item); reload() } }) { Text("Выдать") }
                    }
                }
            }
        }
    }
}
