package com.lexora.service.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onOpenPlanning: () -> Unit,
) {
    val dashboard = state.dashboard

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Button(onClick = onOpenPlanning) { Text(stringResource(R.string.home_planning)) }
        }
        item { Text(stringResource(R.string.home_operational_summary), style = MaterialTheme.typography.titleLarge) }
        if (state.loading && dashboard == null) {
            item { CircularProgressIndicator() }
        } else if (state.error != null && dashboard == null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.home_load_error), color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onRefresh) { Text(stringResource(R.string.home_refresh)) }
                }
            }
        }
        dashboard?.let { value ->
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiCard(stringResource(R.string.home_open_requests), value.openRequests.toString(), Modifier.weight(1f))
                    KpiCard(stringResource(R.string.home_urgent_requests), value.urgentRequests.toString(), Modifier.weight(1f))
                    KpiCard(stringResource(R.string.home_overdue_requests), value.overdueRequests.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiCard(stringResource(R.string.home_today_visits), value.todayVisits.toString(), Modifier.weight(1f))
                    KpiCard(stringResource(R.string.home_active_employees), value.activeEmployees.toString(), Modifier.weight(1f))
                    KpiCard(stringResource(R.string.home_unsynced), value.unsyncedEntities.toString(), Modifier.weight(1f))
                }
            }
            item {
                KpiCard(
                    stringResource(R.string.home_planned_payments),
                    "%.2f ₽".format(value.plannedPaymentsMinor / 100.0),
                    Modifier.fillMaxWidth(),
                )
            }
            item { Text(stringResource(R.string.home_next_visits), style = MaterialTheme.typography.titleMedium) }
            if (value.nextVisits.isEmpty()) {
                item { Text(stringResource(R.string.home_no_next_visits)) }
            } else {
                items(value.nextVisits.size) { index ->
                    val event = value.nextVisits[index]
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${event.requestNumber} · ${event.title}", style = MaterialTheme.typography.titleSmall)
                            Text(formatEventTime(event.startAtEpochMs))
                            event.employeeName?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun KpiCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatEventTime(epochMs: Long): String =
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        .format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
