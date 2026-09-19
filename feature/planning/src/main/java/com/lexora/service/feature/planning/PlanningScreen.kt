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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.PlanningEvent
import com.lexora.service.core.model.PlanningMode
import com.lexora.service.core.model.ResourceLoad
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PlanningScreen(
    state: PlanningUiState,
    onModeChange: (PlanningMode) -> Unit,
    onPrevious: () -> Unit,
    onToday: () -> Unit,
    onNext: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.planning_title), style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip(PlanningMode.DAY, state.mode, stringResource(R.string.planning_day), onModeChange)
            ModeChip(PlanningMode.WEEK, state.mode, stringResource(R.string.planning_week), onModeChange)
            ModeChip(PlanningMode.MONTH, state.mode, stringResource(R.string.planning_month), onModeChange)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPrevious) { Text(stringResource(R.string.planning_previous)) }
            Button(onClick = onToday) { Text(stringResource(R.string.planning_today)) }
            OutlinedButton(onClick = onNext) { Text(stringResource(R.string.planning_next)) }
        }
        Text(formatAnchor(state), style = MaterialTheme.typography.titleMedium)
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
        }
    }
}

@Composable
private fun ModeChip(mode: PlanningMode, selected: PlanningMode, label: String, onModeChange: (PlanningMode) -> Unit) {
    FilterChip(selected = mode == selected, onClick = { onModeChange(mode) }, label = { Text(label) })
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

private fun formatAnchor(state: PlanningUiState): String = when (state.mode) {
    PlanningMode.DAY -> state.anchorDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    PlanningMode.WEEK -> "Неделя ${state.anchorDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}"
    PlanningMode.MONTH -> state.anchorDate.format(DateTimeFormatter.ofPattern("LLLL yyyy"))
}

private fun formatTime(epochMs: Long): String =
    DateTimeFormatter.ofPattern("dd.MM HH:mm").format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
