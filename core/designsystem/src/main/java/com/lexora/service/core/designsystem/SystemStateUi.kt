package com.lexora.service.core.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.RequiredSystemState
import com.lexora.service.core.model.SystemUiState

@Composable
fun SystemStateHost(
    state: SystemUiState,
    modifier: Modifier = Modifier,
    onRetrySync: (() -> Unit)? = null,
    onRetryTechnicalError: (() -> Unit)? = null,
    emptyMessage: String = "Данных пока нет",
    content: @Composable () -> Unit,
) {
    val syncIssue = state.syncIssue
    Column(modifier.fillMaxSize()) {
        if (state.offline) {
            SystemBanner("Нет сети. Локальная работа продолжается; изменения будут синхронизированы позже.")
        } else if (syncIssue?.requiresUserAction == true) {
            SystemBanner(
                text = "Ошибка синхронизации: ${syncIssue.lastError ?: "есть несинхронизированные изменения"}",
                action = onRetrySync,
                actionLabel = "Повторить",
            )
        }
        when (state.primary) {
            RequiredSystemState.LOADING -> FullState("Загрузка…") { CircularProgressIndicator() }
            RequiredSystemState.EMPTY -> FullState(emptyMessage)
            RequiredSystemState.PERMISSION_DENIED -> FullState(
                state.permissionMessage ?: "Недостаточно прав для просмотра этого раздела"
            )
            RequiredSystemState.FATAL_TECHNICAL_ERROR -> FullState(
                text = "Произошла техническая ошибка. Данные не были удалены. Код: ${state.technicalReference ?: "неизвестен"}",
                action = onRetryTechnicalError,
                actionLabel = "Повторить",
            )
            RequiredSystemState.OFFLINE,
            RequiredSystemState.SYNC_ERROR,
            RequiredSystemState.CONTENT -> Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        }
    }
}

@Composable
fun PermissionGuard(
    allowed: Boolean,
    message: String = "Недостаточно прав для просмотра этого раздела",
    content: @Composable () -> Unit,
) {
    if (allowed) {
        content()
    } else {
        SystemStateHost(
            state = SystemUiState(
                primary = RequiredSystemState.PERMISSION_DENIED,
                permissionMessage = message,
            ),
            content = {},
        )
    }
}

@Composable
private fun SystemBanner(
    text: String,
    action: (() -> Unit)? = null,
    actionLabel: String = "",
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            if (action != null) TextButton(onClick = action) { Text(actionLabel) }
        }
    }
}

@Composable
private fun FullState(
    text: String,
    action: (() -> Unit)? = null,
    actionLabel: String = "",
    extra: (@Composable () -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        extra?.invoke()
        Text(text, style = MaterialTheme.typography.bodyLarge)
        if (action != null) {
            TextButton(onClick = action, modifier = Modifier.padding(top = 8.dp)) { Text(actionLabel) }
        }
    }
}