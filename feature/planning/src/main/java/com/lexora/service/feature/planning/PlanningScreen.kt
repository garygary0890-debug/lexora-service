package com.lexora.service.feature.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.PlanningEvent
import com.lexora.service.core.model.PlanningMode
import com.lexora.service.core.model.ResourceLoad
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward

@Composable
fun PlanningScreen(
    state: PlanningUiState,
    onPeriodClick: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.planning_previous))
            }
            Button(
                onClick = onPeriodClick,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Сегодня, режим ${modeLabel(state.mode)}" },
            ) {
                Text(formatDateLabel(state), maxLines = 1)
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.planning_next))
            }
        }
        if (state.loading && state.snapshot == null) {
            CircularProgressIndicator()
            return@Column
        }
        if (state.error != null && state.snapshot == null) {
            Text(stringResource(R.string.planning_error), color = MaterialTheme.colorScheme.error)
            Button(onClick = onRefresh) { Text(stringResource(R.string.planning_refresh)) }
            return@Column
        }
        val snapshot = state.snapshot ?: return@Column
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(stringResource(R.string.planning_calendar), style = MaterialTheme.typography.titleLarge) }
            if (snapshot.events.isEmpty()) item { Text(stringResource(R.string.planning_no_events)) }
            items(snapshot.events, key = { it.id }) { event -> EventCard(event) }
            item { Text(stringResource(R.string.planning_employee_load), style = MaterialTheme.typography.titleLarge) }
            if (snapshot.employeeLoads.isEmpty()) item { Text(stringResource(R.string.planning_no_resources)) }
            items(snapshot.employeeLoads, key = { "employee:${it.resourceId}" }) { load -> LoadCard(load) }
            item { Text(stringResource(R.string.planning_team_load), style = MaterialTheme.typography.titleLarge) }
            items(snapshot.teamLoads, key = { "team:${it.resourceId}" }) { load -> LoadCard(load) }
            item { BusinessOperationsPanel(state.organizationId) }
        }
    }
}

@Composable
private fun EventCard(event: PlanningEvent) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${event.requestNumber} · ${event.title}", style = MaterialTheme.typography.titleMedium)
            Text("${formatTime(event.startAtEpochMs)} — ${formatTime(event.endAtEpochMs)}")
            Text(listOfNotNull(event.employeeName, event.branchName).joinToString(" · ").ifBlank { "Исполнитель не назначен" })
            Text("${event.priority.name} · ${event.status.name}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun LoadCard(load: ResourceLoad) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(load.displayName, style = MaterialTheme.typography.titleSmall)
                Text("${load.utilizationPercent}%")
            }
            LinearProgressIndicator(
                progress = { (load.utilizationPercent.coerceIn(0, 100) / 100f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("${load.scheduledMinutes} мин · ${load.eventCount} назначений", style = MaterialTheme.typography.bodySmall)
            if (load.overloaded) Text("Перегрузка", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun modeLabel(mode: PlanningMode): String = when (mode) {
        PlanningMode.DAY -> "День"
        PlanningMode.WEEK -> "Неделя"
        PlanningMode.MONTH -> "Месяц"
}

private fun formatDateLabel(state: PlanningUiState): String =
    state.anchorDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

private fun formatTime(epochMs: Long): String =
    DateTimeFormatter.ofPattern("dd.MM HH:mm").format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
