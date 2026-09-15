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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lexora.service.core.data.CatalogRepository
import com.lexora.service.core.data.InMemoryOrganizationRepository
import com.lexora.service.core.data.InMemoryUserRepository
import com.lexora.service.core.data.ServiceConstructorRepository
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.model.PriceList
import com.lexora.service.core.model.PriceListItem
import com.lexora.service.core.model.ServiceCatalogItem
import com.lexora.service.core.model.ServiceRecipeSummary
import kotlinx.coroutines.launch

@Composable
fun CatalogScreen() {
    val context = LocalContext.current
    val database = remember { LexoraServiceDatabase.create(context.applicationContext) }
    val repository = remember { CatalogRepository.create(context) }
    val constructorRepository = remember {
        ServiceConstructorRepository(database.serviceConstructorDao(), database.serviceDao())
    }
    val organization = remember { InMemoryOrganizationRepository().activeOrganization() }
    val user = remember { InMemoryUserRepository().currentUser() }
    val scope = rememberCoroutineScope()

    var services by remember { mutableStateOf<List<ServiceCatalogItem>>(emptyList()) }
    var priceLists by remember { mutableStateOf<List<PriceList>>(emptyList()) }
    var selectedPriceListId by remember { mutableStateOf<String?>(null) }
    var priceItems by remember { mutableStateOf<List<PriceListItem>>(emptyList()) }
    var recipeSummaries by remember { mutableStateOf<List<ServiceRecipeSummary>>(emptyList()) }

    suspend fun reload() {
        val orgId = organization?.id ?: return
        services = repository.services(orgId)
        services.filter { it.active }.forEach { service ->
            constructorRepository.ensureRecipe(
                organizationId = orgId,
                userId = user.id,
                serviceCatalogItemId = service.id,
                serviceName = service.name,
                durationMinutes = service.durationMinutes,
            )
        }
        recipeSummaries = constructorRepository.recipes(orgId)
        priceLists = repository.priceLists(orgId)
        val selected = selectedPriceListId?.takeIf { id -> priceLists.any { it.id == id } } ?: priceLists.firstOrNull()?.id
        selectedPriceListId = selected
        priceItems = selected?.let { repository.priceItems(it) }.orEmpty()
    }

    LaunchedEffect(organization?.id) { reload() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Услуги и прайс-листы", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = { scope.launch { organization?.id?.let { repository.addService(it, user.id) }; reload() } }) { Text("Добавить услугу") }
        services.forEach { service ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${service.code} · ${service.name}", style = MaterialTheme.typography.titleSmall)
                    Text("${service.category.orEmpty()} · ${service.unit} · ${service.durationMinutes ?: 0} мин")
                    OutlinedButton(onClick = { scope.launch { repository.toggleService(service, user.id); reload() } }) {
                        Text(if (service.active) "Отключить" else "Включить")
                    }
                }
            }
        }

        ServiceConstructorSection(
            summaries = recipeSummaries.filter { it.recipe.active },
            onAddComponent = { recipeId, type, name, quantity, unit, unitCostMinor ->
                val orgId = organization?.id ?: return@ServiceConstructorSection
                scope.launch {
                    constructorRepository.addComponent(orgId, user.id, recipeId, type, name, quantity, unit, unitCostMinor)
                    reload()
                }
            },
            onSetComponentActive = { componentId, active ->
                val orgId = organization?.id ?: return@ServiceConstructorSection
                scope.launch {
                    constructorRepository.setComponentActive(orgId, user.id, componentId, active)
                    reload()
                }
            },
        )

        Text("Прайс-листы", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { scope.launch { organization?.id?.let { repository.addPriceList(it, user.id) }; reload() } }) { Text("Добавить прайс-лист") }
        priceLists.forEach { list ->
            OutlinedButton(onClick = { scope.launch { selectedPriceListId = list.id; priceItems = repository.priceItems(list.id) } }) {
                Text("${list.name} · ${list.currency}${if (list.id == selectedPriceListId) " · выбран" else ""}")
            }
        }

        val selected = priceLists.firstOrNull { it.id == selectedPriceListId }
        if (selected != null) {
            Text("Позиции: ${selected.name}", style = MaterialTheme.typography.titleMedium)
            services.filter { it.active }.forEach { service ->
                val item = priceItems.firstOrNull { it.serviceCatalogItemId == service.id }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${service.code} · ${service.name}")
                        Text(if (item == null) "Цена не задана" else "${item.priceMinor / 100.0} ${selected.currency}")
                        Button(onClick = { scope.launch { repository.addPriceItem(selected, service, user.id); reload() } }) {
                            Text(if (item == null) "Добавить цену" else "Обновить цену")
                        }
                    }
                }
            }
        }
    }
}
