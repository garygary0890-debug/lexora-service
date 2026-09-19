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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.TireDiagnostic
import com.lexora.service.core.model.TireQueueItem
import com.lexora.service.core.model.TireQueueStatus
import com.lexora.service.core.model.TireStorageItem
import com.lexora.service.core.model.TireStorageStatus
import com.lexora.service.core.model.TireWorkEntry

@Composable
fun TiresScreen(
    state: TiresUiState,
    onAddQueueItem: () -> Unit,
    onAdvanceQueue: (String) -> Unit,
    onAddDiagnostic: () -> Unit,
    onAddWorkEntry: () -> Unit,
    onAddStorageItem: () -> Unit,
    onIssueStorage: (String) -> Unit,
) {    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Шиномонтаж", style = MaterialTheme.typography.headlineMedium)
        Text("Очередь", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onAddQueueItem) { Text("Добавить в очередь") }
        state.queue.sortedBy { it.position }.forEach { item ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("№${item.position}")
                        item.requestId?.let { Text("Заявка: $it", style = MaterialTheme.typography.bodySmall) }
                        item.vehicleId?.let { Text("Автомобиль: $it", style = MaterialTheme.typography.bodySmall) }
                        Text("Статус: ${item.status.name}")
                    }
                    if (item.status != TireQueueStatus.COMPLETED && item.status != TireQueueStatus.CANCELLED) {
                        OutlinedButton(onClick = { onAdvanceQueue(item.id) }) { Text("Далее") }
                    }
                }
            }
        }

        Text("Диагностика", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onAddDiagnostic) { Text("Добавить диагностику") }
        state.diagnostics.take(20).forEach { diagnostic ->
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
        Button(onClick = onAddWorkEntry) { Text("Добавить работу") }
        state.workEntries.take(20).forEach { work -> Text("${work.title} × ${work.quantity}") }

        Text("Хранение шин", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onAddStorageItem) { Text("Принять на хранение") }
        state.storage.forEach { item ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(item.storageCode, style = MaterialTheme.typography.titleSmall)
                        Text("${item.tireDescription}, ${item.quantity} шт.")
                        item.location?.let { Text("Место: $it") }
                        Text("Статус: ${item.status.name}")
                    }
                    if (item.status == TireStorageStatus.STORED) {
                        OutlinedButton(onClick = { onIssueStorage(item.id) }) { Text("Выдать") }
                    }
                }
            }
        }
    }
}
