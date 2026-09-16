package com.lexora.service.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.PriceList
import com.lexora.service.core.model.PriceListItem
import com.lexora.service.core.model.ServiceCatalogItem
import com.lexora.service.core.model.ServiceComponentType
import com.lexora.service.core.model.ServiceRecipeSummary

data class CatalogUiState(
    val services: List<ServiceCatalogItem> = emptyList(),
    val priceLists: List<PriceList> = emptyList(),
    val selectedPriceListId: String? = null,
    val priceItems: List<PriceListItem> = emptyList(),
    val recipeSummaries: List<ServiceRecipeSummary> = emptyList(),
)

data class CatalogActions(
    val onAddService: () -> Unit,
    val onToggleService: (ServiceCatalogItem) -> Unit,
    val onAddComponent: (String, ServiceComponentType, String, Double, String, Long) -> Unit,
    val onSetComponentActive: (String, Boolean) -> Unit,
    val onAddPriceList: () -> Unit,
    val onSelectPriceList: (String) -> Unit,
    val onAddPriceItem: (PriceList, ServiceCatalogItem) -> Unit,
)

@Composable
fun CatalogScreen(
    state: CatalogUiState,
    actions: CatalogActions,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Услуги и прайс-листы", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = actions.onAddService) { Text("Добавить услугу") }

        state.services.forEach { service ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${service.code} · ${service.name}", style = MaterialTheme.typography.titleSmall)
                    Text("${service.category.orEmpty()} · ${service.unit} · ${service.durationMinutes ?: 0} мин")
                    OutlinedButton(onClick = { actions.onToggleService(service) }) {
                        Text(if (service.active) "Отключить" else "Включить")
                    }
                }
            }
        }

        ServiceConstructorSection(
            summaries = state.recipeSummaries.filter { it.recipe.active },
            onAddComponent = actions.onAddComponent,
            onSetComponentActive = actions.onSetComponentActive,
        )

        Text("Прайс-листы", style = MaterialTheme.typography.titleMedium)
        Button(onClick = actions.onAddPriceList) { Text("Добавить прайс-лист") }

        state.priceLists.forEach { list ->
            OutlinedButton(onClick = { actions.onSelectPriceList(list.id) }) {
                Text("${list.name} · ${list.currency}${if (list.id == state.selectedPriceListId) " · выбран" else ""}")
            }
        }

        val selected = state.priceLists.firstOrNull { it.id == state.selectedPriceListId }
        if (selected != null) {
            Text("Позиции: ${selected.name}", style = MaterialTheme.typography.titleMedium)
            state.services.filter { it.active }.forEach { service ->
                val item = state.priceItems.firstOrNull { it.serviceCatalogItemId == service.id }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${service.code} · ${service.name}")
                        Text(if (item == null) "Цена не задана" else "${item.priceMinor / 100.0} ${selected.currency}")
                        Button(onClick = { actions.onAddPriceItem(selected, service) }) {
                            Text(if (item == null) "Добавить цену" else "Обновить цену")
                        }
                    }
                }
            }
        }
    }
}
