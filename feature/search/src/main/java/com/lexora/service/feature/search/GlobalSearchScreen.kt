package com.lexora.service.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.GlobalSearchEntityType
import com.lexora.service.core.model.GlobalSearchResult

@Composable
fun GlobalSearchScreen(
    state: GlobalSearchUiState,
    onQueryChange: (String) -> Unit,
    onToggleType: (GlobalSearchEntityType) -> Unit,
    onSearch: () -> Unit,
    onOpenResult: (GlobalSearchResult) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.search_title), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.search_hint)) },
            singleLine = true,
        )
        Text(stringResource(R.string.search_filters), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlobalSearchEntityType.entries.forEach { type ->
                FilterChip(
                    selected = type in state.selectedTypes,
                    onClick = { onToggleType(type) },
                    label = { Text(type.label()) },
                )
            }
        }
        Button(
            onClick = onSearch,
            enabled = state.query.trim().length >= 2 && !state.loading,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.search_action)) }
        if (state.loading) CircularProgressIndicator()
        state.error?.let { Text(stringResource(R.string.search_error), color = MaterialTheme.colorScheme.error) }
        if (!state.loading && state.query.trim().length >= 2 && state.results.isEmpty() && state.error == null) {
            Text(stringResource(R.string.search_empty))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.results, key = { "${it.entityType}:${it.entityId}" }) { result ->
                SearchResultCard(result, onOpenResult)
            }
        }
    }
}

@Composable
private fun SearchResultCard(result: GlobalSearchResult, onOpen: (GlobalSearchResult) -> Unit) {
    Card(onClick = { onOpen(result) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(result.title, style = MaterialTheme.typography.titleMedium)
            result.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text(result.entityType.label(), style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun GlobalSearchEntityType.label(): String = when (this) {
    GlobalSearchEntityType.CLIENT -> "Клиент"
    GlobalSearchEntityType.VEHICLE -> "Автомобиль"
    GlobalSearchEntityType.SERVICE_OBJECT -> "Объект"
    GlobalSearchEntityType.EQUIPMENT -> "Оборудование"
    GlobalSearchEntityType.REQUEST -> "Заявка"
    GlobalSearchEntityType.DOCUMENT -> "Документ"
    GlobalSearchEntityType.PAYMENT -> "Платёж"
    GlobalSearchEntityType.EMPLOYEE -> "Сотрудник"
}
