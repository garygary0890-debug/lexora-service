package com.lexora.service.feature.documents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.ContractStatus
import com.lexora.service.core.model.ServiceContract

@Composable
fun ContractsSection(
    contracts: List<ServiceContract>,
    archivedContracts: List<ServiceContract>,
    onCreate: () -> Unit,
    onChangeStatus: (String, ContractStatus) -> Unit,
    onArchive: (String) -> Unit,
    onRestore: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Договоры", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onCreate) { Text("Создать договор") }
        }
        if (contracts.isEmpty()) {
            Text("Действующих и черновых договоров пока нет")
        }
        contracts.forEach { contract ->
            ContractCard(contract, onChangeStatus, onArchive)
        }
        if (archivedContracts.isNotEmpty()) {
            Text("Архив договоров", style = MaterialTheme.typography.titleSmall)
            archivedContracts.forEach { contract ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${contract.number} · ${contract.subject}")
                        Text("Статус: ${contract.status.name}")
                        OutlinedButton(onClick = { onRestore(contract.id) }) { Text("Восстановить") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContractCard(
    contract: ServiceContract,
    onChangeStatus: (String, ContractStatus) -> Unit,
    onArchive: (String) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(contract.number, style = MaterialTheme.typography.titleSmall)
            Text(contract.subject)
            Text("Статус: ${contract.status.name}")
            contract.startAtEpochMs?.let { Text("Начало: $it", style = MaterialTheme.typography.bodySmall) }
            contract.endAtEpochMs?.let { Text("Окончание: $it", style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (contract.status) {
                    ContractStatus.DRAFT -> Button(onClick = { onChangeStatus(contract.id, ContractStatus.ACTIVE) }) { Text("Активировать") }
                    ContractStatus.ACTIVE -> OutlinedButton(onClick = { onChangeStatus(contract.id, ContractStatus.SUSPENDED) }) { Text("Приостановить") }
                    ContractStatus.SUSPENDED -> Button(onClick = { onChangeStatus(contract.id, ContractStatus.ACTIVE) }) { Text("Возобновить") }
                    ContractStatus.TERMINATED, ContractStatus.EXPIRED -> Unit
                }
                if (contract.status != ContractStatus.TERMINATED && contract.status != ContractStatus.EXPIRED) {
                    OutlinedButton(onClick = { onChangeStatus(contract.id, ContractStatus.TERMINATED) }) { Text("Прекратить") }
                }
            }
            OutlinedButton(onClick = { onArchive(contract.id) }) { Text("В архив") }
        }
    }
}
