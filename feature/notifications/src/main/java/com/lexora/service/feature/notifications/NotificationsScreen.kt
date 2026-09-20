package com.lexora.service.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
    onDndEnabled: (Boolean) -> Unit = NotificationUiRuntimeActions::setDndEnabled,
    onLocalEnabled: (Boolean) -> Unit = NotificationUiRuntimeActions::setLocalEnabled,
    onPushEnabled: (Boolean) -> Unit = NotificationUiRuntimeActions::setPushEnabled,
    onAllowCritical: (Boolean) -> Unit = NotificationUiRuntimeActions::setAllowCritical,
    onDndStartChanged: (String) -> Unit = NotificationUiRuntimeActions::setDndStart,
    onDndEndChanged: (String) -> Unit = NotificationUiRuntimeActions::setDndEnd,
    onSaveDndWindow: () -> Unit = NotificationUiRuntimeActions::saveDndWindow,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Центр уведомлений", style = MaterialTheme.typography.headlineMedium)
        NotificationPreferencesCard(
            state = state,
            onDndEnabled = onDndEnabled,
            onLocalEnabled = onLocalEnabled,
            onPushEnabled = onPushEnabled,
            onAllowCritical = onAllowCritical,
            onDndStartChanged = onDndStartChanged,
            onDndEndChanged = onDndEndChanged,
            onSaveDndWindow = onSaveDndWindow,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !state.archivedMode, onClick = { onArchivedMode(false) }, label = { Text("Активные") })
            FilterChip(selected = state.archivedMode, onClick = { onArchivedMode(true) }, label = { Text("Архив") })
        }
        if (!state.archivedMode) Button(onClick = onMarkAllRead) { Text("Прочитать все") }
        if (state.loading) CircularProgressIndicator()
        state.error?.let { Text("Не удалось загрузить уведомления: $it", color = MaterialTheme.colorScheme.error) }
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
        if (state.deliveries.isNotEmpty()) {
            Text("Доставка", style = MaterialTheme.typography.titleMedium)
            state.deliveries.take(12).forEach { delivery ->
                Text("${delivery.channel.name} · ${delivery.status.name} · ${delivery.title}")
            }
        }
    }
}

@Composable
private fun NotificationPreferencesCard(
    state: NotificationsUiState,
    onDndEnabled: (Boolean) -> Unit,
    onLocalEnabled: (Boolean) -> Unit,
    onPushEnabled: (Boolean) -> Unit,
    onAllowCritical: (Boolean) -> Unit,
    onDndStartChanged: (String) -> Unit,
    onDndEndChanged: (String) -> Unit,
    onSaveDndWindow: () -> Unit,
) {
    val preferences = state.preferences ?: return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Настройки доставки", style = MaterialTheme.typography.titleMedium)
            ToggleRow("Системные уведомления Android", preferences.localEnabled, onLocalEnabled)
            ToggleRow("Push", preferences.pushEnabled, onPushEnabled)
            Text(state.pushProviderStatus, style = MaterialTheme.typography.bodySmall)
            ToggleRow("Не беспокоить", preferences.doNotDisturb.enabled, onDndEnabled)
            if (preferences.doNotDisturb.enabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.dndStartText,
                        onValueChange = onDndStartChanged,
                        label = { Text("Начало ЧЧ:ММ") },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.dndEndText,
                        onValueChange = onDndEndChanged,
                        label = { Text("Конец ЧЧ:ММ") },
                        modifier = Modifier.weight(1f),
                    )
                }
                ToggleRow("Пропускать критические события", preferences.doNotDisturb.allowCritical, onAllowCritical)
                Button(onClick = onSaveDndWindow) { Text("Сохранить интервал") }
                state.preferenceError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text("Часовой пояс: ${preferences.doNotDisturb.timeZoneId ?: "системный"}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
