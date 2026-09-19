package com.lexora.service.feature.audit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.AuditRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AuditScreen(state: AuditUiState, onQueryChange: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Журнал аудита", style = MaterialTheme.typography.headlineMedium)
        Text("История действий доступна только для просмотра. Записи нельзя редактировать или удалять.")
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Поиск по действию, объекту, пользователю или описанию") },
            singleLine = true,
        )
        if (state.loading) CircularProgressIndicator()
        state.error?.let { Text("Не удалось загрузить журнал аудита", color = MaterialTheme.colorScheme.error) }
        Text("Показано записей: ${state.records.size}", style = MaterialTheme.typography.labelMedium)
        if (!state.loading && state.records.isEmpty()) Text("Записи аудита не найдены.")
        state.records.forEach { record -> AuditRecordCard(record) }
    }
}

@Composable
private fun AuditRecordCard(record: AuditRecord) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(record.action, style = MaterialTheme.typography.titleSmall)
                Text(formatAuditTime(record.occurredAtEpochMs), style = MaterialTheme.typography.labelSmall)
            }
            Text(record.summary)
            Text("Объект: ${record.entityType}${record.entityId?.let { " · $it" }.orEmpty()}")
            Text("Пользователь: ${record.userId}", style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun formatAuditTime(epochMs: Long): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(epochMs))
