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
import androidx.compose.material3.FilterChip
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
import com.lexora.service.core.data.NotificationRepository
import com.lexora.service.core.model.Organization
import com.lexora.service.core.model.ServiceNotification
import kotlinx.coroutines.launch

@Composable
fun NotificationsScreen(organization: Organization) {
    val context = LocalContext.current
    val repository = remember { NotificationRepository.create(context) }
    val scope = rememberCoroutineScope()
    var archivedMode by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf<List<ServiceNotification>>(emptyList()) }

    suspend fun reload() {
        repository.refreshGenerated(organization.id)
        notifications = if (archivedMode) repository.archivedNotifications(organization.id) else repository.notifications(organization.id)
    }

    LaunchedEffect(organization.id, archivedMode) { reload() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Центр уведомлений", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !archivedMode, onClick = { archivedMode = false }, label = { Text("Активные") })
            FilterChip(selected = archivedMode, onClick = { archivedMode = true }, label = { Text("Архив") })
        }
        if (!archivedMode) {
            Button(onClick = { scope.launch { repository.markAllRead(organization.id); reload() } }) { Text("Прочитать все") }
        }
        if (notifications.isEmpty()) {
            Text(if (archivedMode) "Архив уведомлений пуст." else "Новых уведомлений нет.")
        }
        notifications.forEach { item ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${item.priority.name} · ${item.title}", style = MaterialTheme.typography.titleSmall)
                    Text(item.message)
                    item.entityType?.let { Text("Источник: $it${item.entityId?.let { id -> " · $id" }.orEmpty()}") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!archivedMode) {
                            OutlinedButton(onClick = { scope.launch { repository.markRead(item.id, item.unread); reload() } }) {
                                Text(if (item.unread) "Прочитано" else "Сделать непрочитанным")
                            }
                            OutlinedButton(onClick = { scope.launch { repository.setArchived(item.id, true); reload() } }) { Text("В архив") }
                        } else {
                            OutlinedButton(onClick = { scope.launch { repository.setArchived(item.id, false); reload() } }) { Text("Восстановить") }
                        }
                    }
                }
            }
        }
    }
}
