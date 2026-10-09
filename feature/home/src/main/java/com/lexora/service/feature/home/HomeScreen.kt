package com.lexora.service.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val QuickAccessButtonColor = Color(0xFF61656D)

data class HomeQuickAccessItem(
    val route: String,
    val title: String,
)

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onOpenPlanning: () -> Unit,
    quickAccess: List<HomeQuickAccessItem>,
    availableQuickAccess: List<HomeQuickAccessItem>,
    onQuickAccessChange: (List<String>) -> Unit,
    onOpenQuickAccess: (String) -> Unit,
) {
    var quickAccessDialogOpen by remember { mutableStateOf(false) }
    var draftQuickAccessRoutes by remember(quickAccess) {
        mutableStateOf(quickAccess.map { it.route })
    }
    val dashboard = state.dashboard

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                quickAccess.forEach { destination ->
                    Button(
                        onClick = { onOpenQuickAccess(destination.route) },
                        colors = ButtonDefaults.buttonColors(containerColor = QuickAccessButtonColor),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Text(destination.title, maxLines = 1, softWrap = false)
                    }
                }
                Button(
                    onClick = {
                        draftQuickAccessRoutes = quickAccess.map { it.route }
                        quickAccessDialogOpen = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = QuickAccessButtonColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp),
                ) {
                    Text("+", style = MaterialTheme.typography.titleMedium)
                }
            }
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
                    wide = true,
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

    if (quickAccessDialogOpen) {
        AlertDialog(
            onDismissRequest = { quickAccessDialogOpen = false },
            title = { Text(stringResource(R.string.home_quick_access_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    availableQuickAccess.forEach { destination ->
                        val selectedIndex = draftQuickAccessRoutes.indexOf(destination.route)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedIndex >= 0,
                                enabled = selectedIndex >= 0 || draftQuickAccessRoutes.size < 6,
                                onCheckedChange = { checked ->
                                    draftQuickAccessRoutes = if (checked) {
                                        (draftQuickAccessRoutes + destination.route).distinct().take(6)
                                    } else {
                                        draftQuickAccessRoutes.filterNot { it == destination.route }
                                    }
                                },
                            )
                            Text(destination.title, modifier = Modifier.weight(1f))
                            TextButton(
                                enabled = selectedIndex > 0,
                                onClick = {
                                    draftQuickAccessRoutes = draftQuickAccessRoutes.toMutableList().also {
                                        val previous = it[selectedIndex - 1]
                                        it[selectedIndex - 1] = it[selectedIndex]
                                        it[selectedIndex] = previous
                                    }
                                },
                            ) { Text("↑") }
                            TextButton(
                                enabled = selectedIndex >= 0 && selectedIndex < draftQuickAccessRoutes.lastIndex,
                                onClick = {
                                    draftQuickAccessRoutes = draftQuickAccessRoutes.toMutableList().also {
                                        val next = it[selectedIndex + 1]
                                        it[selectedIndex + 1] = it[selectedIndex]
                                        it[selectedIndex] = next
                                    }
                                },
                            ) { Text("↓") }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onQuickAccessChange(draftQuickAccessRoutes)
                        quickAccessDialogOpen = false
                    },
                ) { Text(stringResource(R.string.home_quick_access_save)) }
            },
            dismissButton = {
                TextButton(onClick = { quickAccessDialogOpen = false }) {
                    Text(stringResource(R.string.home_quick_access_cancel))
                }
            },
        )
    }
}

@Composable
private fun KpiCard(label: String, value: String, modifier: Modifier = Modifier, wide: Boolean = false) {
    Card(modifier.height(if (wide) 64.dp else 84.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = if (wide) 24.sp else 26.sp,
                ),
            )
            Text(
                label,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            )
        }
    }
}

private fun formatEventTime(epochMs: Long): String =
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        .format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
