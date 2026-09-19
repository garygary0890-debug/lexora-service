package com.lexora.service.feature.notifications

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NotificationsScreen(
    state: NotificationsUiState,
    onArchivedMode: (Boolean) -> Unit,
    onMarkAllRead: () -> Unit,
    onMarkRead: (String, Boolean) -> Unit,
    onSetArchived: (String, Boolean) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Центр уведомлений", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !state.archivedMode, onClick = { onArchivedMode(false) }, label = { Text("Активные") })
            FilterChip(selected = state.archivedMode, onClick = { onArchivedMode(true) }, label = { Text("Архив") })
        }
        if (!state.archivedMode) Button(onClick = onMarkAllRead) { Text("Прочитать все") }
        if (state.loading) CircularProgressIndicator()
        state.error?.let { Text("Не удалось загрузить уведомления", color = MaterialTheme.colorScheme.error) }
        if (!state.loading && state.notifications.isEmpty()) {
            Text(if (state.archivedMode) "Архив уведомлений пуст." else "Новых уведомлений нет.")
        }
        state.notifications.forEach { item ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${item.priority.name} · ${item.title}", style = MaterialTheme.typography.titleSmall)
                    Text(item.message)
                    item.entityType?.let { Text("Источник: $it${item.entityId?.let { id -> " · $id" }.orEmpty()}") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!state.archivedMode) {
                            OutlinedButton(onClick = { onMarkRead(item.id, item.unread) }) { Text(if (item.unread) "Прочитано" else "Сделать непрочитанным") }
                            OutlinedButton(onClick = { onSetArchived(item.id, true) }) { Text("В архив") }
                        } else {
                            OutlinedButton(onClick = { onSetArchived(item.id, false) }) { Text("Восстановить") }
                        }
                    }
                }
            }
        }
    }
}
