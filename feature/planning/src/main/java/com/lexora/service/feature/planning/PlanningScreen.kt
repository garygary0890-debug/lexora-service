package com.lexora.service.feature.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.PlanningEvent
import com.lexora.service.core.model.PlanningMode
import com.lexora.service.core.model.ResourceLoad
import com.lexora.service.core.model.PlanningSnapshot
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val EmptyTabTopPadding = 48.dp
private val PopulatedTabTopPadding = 8.dp

@Composable
fun PlanningScreen(
    state: PlanningUiState,
    onPeriodClick: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRefresh: () -> Unit,
    onCreateEntry: (PlanningEntryDraft) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) { Text("‹", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { contentDescription = "Предыдущий период" }) }
            Button(onClick = onPeriodClick, modifier = Modifier.weight(1f)) { Text(formatDateLabel(state), maxLines = 1) }
            IconButton(onClick = onNext) { Text("›", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { contentDescription = "Следующий период" }) }
        }
        if (state.loading && state.snapshot == null) { CircularProgressIndicator(); return@Column }
        if (state.error != null && state.snapshot == null) {
            Text("Не удалось загрузить планирование", color = MaterialTheme.colorScheme.error)
            Button(onClick = onRefresh) { Text("Повторить") }
            return@Column
        }
        val snapshot = state.snapshot ?: return@Column
        val tabs = listOf("Календарь\nвыездов", "Загруженность", "Задачи и\nплатежи")
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title -> Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title, maxLines = 2) }) }
        }
        LazyColumn(
            contentPadding = PaddingValues(top = if (selectedTab == 0 && snapshot.events.isEmpty()) EmptyTabTopPadding else PopulatedTabTopPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (selectedTab) {
                0 -> {
                    item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Button(onClick = { showCreateDialog = true }) { Text("Добавить") } } }
                    if (snapshot.events.isEmpty()) item { Text("В выбранном периоде выездов нет", Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                    items(snapshot.events, key = { it.id }) { event -> EventCard(event) }
                }
                1 -> {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Загрузка сотрудников", style = MaterialTheme.typography.titleLarge)
                            Button(onClick = { showCreateDialog = true }) { Text("Добавить") }
                        }
                    }
                    if (snapshot.employeeLoads.isEmpty()) item { Text("Нет активных исполнителей", Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                    items(snapshot.employeeLoads, key = { "employee:${it.resourceId}" }) { load -> LoadCard(load) }
                    item { Text("Загрузка бригад", style = MaterialTheme.typography.titleLarge) }
                    if (snapshot.teamLoads.isEmpty()) item { Text("Нет активных бригад", Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                    items(snapshot.teamLoads, key = { "team:${it.resourceId}" }) { load -> LoadCard(load) }
                }
                2 -> item { BusinessOperationsPanel(state.organizationId) }
            }
        }
    }
    if (showCreateDialog && state.snapshot != null) {
        PlanningCreateDialog(state.anchorDate, state.snapshot!!, { showCreateDialog = false }, { onCreateEntry(it); showCreateDialog = false })
    }
}

@Composable
private fun PlanningCreateDialog(anchorDate: LocalDate, snapshot: PlanningSnapshot, onDismiss: () -> Unit, onCreate: (PlanningEntryDraft) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(anchorDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))) }
    var start by remember { mutableStateOf("09:00") }
    var end by remember { mutableStateOf("10:00") }
    var employeeId by remember { mutableStateOf<String?>(null) }
    var employeeName by remember { mutableStateOf<String?>(null) }
    var branchId by remember { mutableStateOf<String?>(null) }
    var teamName by remember { mutableStateOf<String?>(null) }
    var employeeMenu by remember { mutableStateOf(false) }
    var teamMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая запись") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Название") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Описание") }, singleLine = true)
                OutlinedTextField(date, { date = it }, label = { Text("Дата, ДД.ММ.ГГГГ") }, singleLine = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(start, { start = it }, label = { Text("Начало") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(end, { end = it }, label = { Text("Конец") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Box {
                    OutlinedTextField(employeeName ?: "Исполнитель не выбран", {}, readOnly = true, label = { Text("Исполнитель") }, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = { employeeMenu = true }) { Text("Выбрать") }
                    DropdownMenu(expanded = employeeMenu, onDismissRequest = { employeeMenu = false }) {
                        DropdownMenuItem(text = { Text("Не назначать") }, onClick = { employeeId = null; employeeName = null; employeeMenu = false })
                        snapshot.employeeLoads.forEach { load -> DropdownMenuItem(text = { Text(load.displayName) }, onClick = { employeeId = load.resourceId; employeeName = load.displayName; employeeMenu = false }) }
                    }
                }
                Box {
                    OutlinedTextField(teamName ?: "Бригада не выбрана", {}, readOnly = true, label = { Text("Бригада") }, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = { teamMenu = true }) { Text("Выбрать") }
                    DropdownMenu(expanded = teamMenu, onDismissRequest = { teamMenu = false }) {
                        DropdownMenuItem(text = { Text("Не назначать") }, onClick = { branchId = null; teamName = null; teamMenu = false })
                        snapshot.teamLoads.forEach { load -> DropdownMenuItem(text = { Text(load.displayName) }, onClick = { branchId = null; teamName = load.displayName; teamMenu = false }) }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val draft = runCatching {
                    val day = LocalDate.parse(date.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    val zone = ZoneId.systemDefault()
                    val startAt = day.atTime(LocalTime.parse(start.trim())).atZone(zone).toInstant().toEpochMilli()
                    val endAt = day.atTime(LocalTime.parse(end.trim())).atZone(zone).toInstant().toEpochMilli()
                    PlanningEntryDraft(title, description, startAt, endAt, employeeId, branchId, teamName)
                }.getOrElse { error = "Проверьте название, дату и время"; null }
                if (draft != null) onCreate(draft)
            }) { Text("Создать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
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
            LinearProgressIndicator(progress = { (load.utilizationPercent.coerceIn(0, 100) / 100f) }, modifier = Modifier.fillMaxWidth())
            Text("${load.scheduledMinutes} мин · ${load.eventCount} назначений", style = MaterialTheme.typography.bodySmall)
            if (load.overloaded) Text("Перегрузка", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun formatDateLabel(state: PlanningUiState): String {
    val locale = Locale("ru")
    val prefix = when (state.mode) { PlanningMode.DAY -> "День"; PlanningMode.WEEK -> "Неделя"; PlanningMode.MONTH -> "Месяц" }
    val value = when (state.mode) {
        PlanningMode.DAY -> state.anchorDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy, EEE", locale))
        PlanningMode.WEEK -> {
            val start = state.anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val end = start.plusDays(6)
            if (start.month == end.month && start.year == end.year) "${start.dayOfMonth}-${end.dayOfMonth}.${end.format(DateTimeFormatter.ofPattern("MM.yyyy", locale))}"
            else "${start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", locale))}-${end.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", locale))}"
        }
        PlanningMode.MONTH -> state.anchorDate.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.titlecase(locale) }
    }
    return "$prefix · $value"
}

private fun formatTime(epochMs: Long): String =
    DateTimeFormatter.ofPattern("dd.MM HH:mm").format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
