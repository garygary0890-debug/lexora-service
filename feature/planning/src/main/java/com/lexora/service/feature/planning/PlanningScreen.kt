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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun PlanningScreen(
    state: PlanningUiState,
    onPeriodClick: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRefresh: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val previousDescription = stringResource(R.string.planning_previous)
    val nextDescription = stringResource(R.string.planning_next)
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                Text("‹", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { contentDescription = previousDescription })
            }
            Button(
                onClick = onPeriodClick,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "РЎРµРіРѕРґРЅСЏ, СЂРµР¶РёРј ${modeLabel(state.mode)}" },
            ) {
                Text(formatDateLabel(state), maxLines = 1)
            }
            IconButton(onClick = onNext) {
                Text("›", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { contentDescription = nextDescription })
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
        val tabs = listOf(
            "РљР°Р»РµРЅРґР°СЂСЊ\nРІС‹РµР·РґРѕРІ",
            "Р—Р°РіСЂСѓР¶РµРЅРЅРѕСЃС‚СЊ",
            "Р—Р°РґР°С‡Рё Рё\nРїР»Р°С‚РµР¶Рё",
        )
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, maxLines = 2) },
                )
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (selectedTab) {
                0 -> {
                    item { Text(stringResource(R.string.planning_calendar), style = MaterialTheme.typography.titleLarge) }
                    if (snapshot.events.isEmpty()) item { Text(stringResource(R.string.planning_no_events)) }
                    items(snapshot.events, key = { it.id }) { event -> EventCard(event) }
                }
                1 -> {
                    item { Text(stringResource(R.string.planning_employee_load), style = MaterialTheme.typography.titleLarge) }
                    if (snapshot.employeeLoads.isEmpty()) item { Text(stringResource(R.string.planning_no_resources)) }
                    items(snapshot.employeeLoads, key = { "employee:${it.resourceId}" }) { load -> LoadCard(load) }
                    item { Text(stringResource(R.string.planning_team_load), style = MaterialTheme.typography.titleLarge) }
                    items(snapshot.teamLoads, key = { "team:${it.resourceId}" }) { load -> LoadCard(load) }
                }
                2 -> item { BusinessOperationsPanel(state.organizationId) }
            }
        }
    }
}

@Composable
private fun EventCard(event: PlanningEvent) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${event.requestNumber} В· ${event.title}", style = MaterialTheme.typography.titleMedium)
            Text("${formatTime(event.startAtEpochMs)} вЂ” ${formatTime(event.endAtEpochMs)}")
            Text(listOfNotNull(event.employeeName, event.branchName).joinToString(" В· ").ifBlank { "РСЃРїРѕР»РЅРёС‚РµР»СЊ РЅРµ РЅР°Р·РЅР°С‡РµРЅ" })
            Text("${event.priority.name} В· ${event.status.name}", style = MaterialTheme.typography.labelSmall)
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
            Text("${load.scheduledMinutes} РјРёРЅ В· ${load.eventCount} РЅР°Р·РЅР°С‡РµРЅРёР№", style = MaterialTheme.typography.bodySmall)
            if (load.overloaded) Text("РџРµСЂРµРіСЂСѓР·РєР°", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun modeLabel(mode: PlanningMode): String = when (mode) {
        PlanningMode.DAY -> "Р”РµРЅСЊ"
        PlanningMode.WEEK -> "РќРµРґРµР»СЏ"
        PlanningMode.MONTH -> "РњРµСЃСЏС†"
}

private fun formatDateLabel(state: PlanningUiState): String {
    val locale = Locale("ru")
    return when (state.mode) {
        PlanningMode.DAY -> state.anchorDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy, EEE", locale))
        PlanningMode.WEEK -> {
            val start = state.anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val end = start.plusDays(6)
            if (start.month == end.month && start.year == end.year) {
                "${start.dayOfMonth}-${end.dayOfMonth}.${end.format(DateTimeFormatter.ofPattern("MM.yyyy", locale))}"
            } else {
                "${start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", locale))}-${end.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", locale))}"
            }
        }
        PlanningMode.MONTH -> state.anchorDate
            .format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
            .replaceFirstChar { it.titlecase(locale) }
    }
}

private fun formatTime(epochMs: Long): String =
    DateTimeFormatter.ofPattern("dd.MM HH:mm").format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
