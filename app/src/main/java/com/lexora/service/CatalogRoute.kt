package com.lexora.service

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.lexora.service.core.data.CatalogRepository
import com.lexora.service.core.data.ServiceConstructorRepository
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.model.Organization
import com.lexora.service.core.model.PriceList
import com.lexora.service.core.model.PriceListItem
import com.lexora.service.core.model.ServiceCatalogItem
import com.lexora.service.core.model.ServiceRecipeSummary
import com.lexora.service.core.model.ServiceUser
import com.lexora.service.feature.catalog.CatalogActions
import com.lexora.service.feature.catalog.CatalogScreen
import com.lexora.service.feature.catalog.CatalogUiState
import kotlinx.coroutines.launch

@Composable
fun AppCatalogRoute(
    database: LexoraServiceDatabase,
    organization: Organization,
    user: ServiceUser,
) {
    val repository = remember(database) { CatalogRepository.create(database) }
    val constructorRepository = remember(database) {
        ServiceConstructorRepository(database.serviceConstructorDao(), database.serviceDao())
    }
    val scope = rememberCoroutineScope()

    var services by remember { mutableStateOf<List<ServiceCatalogItem>>(emptyList()) }
    var priceLists by remember { mutableStateOf<List<PriceList>>(emptyList()) }
    var selectedPriceListId by remember { mutableStateOf<String?>(null) }
    var priceItems by remember { mutableStateOf<List<PriceListItem>>(emptyList()) }
    var recipeSummaries by remember { mutableStateOf<List<ServiceRecipeSummary>>(emptyList()) }

    suspend fun reload() {
        services = repository.services(organization.id)
        services.filter { it.active }.forEach { service ->
            constructorRepository.ensureRecipe(
                organizationId = organization.id,
                userId = user.id,
                serviceCatalogItemId = service.id,
                serviceName = service.name,
                durationMinutes = service.durationMinutes,
            )
        }
        recipeSummaries = constructorRepository.recipes(organization.id)
        priceLists = repository.priceLists(organization.id)
        val selected = selectedPriceListId
            ?.takeIf { id -> priceLists.any { it.id == id } }
            ?: priceLists.firstOrNull()?.id
        selectedPriceListId = selected
        priceItems = selected?.let { repository.priceItems(it) }.orEmpty()
    }

    LaunchedEffect(organization.id, user.id) { reload() }

    CatalogScreen(
        state = CatalogUiState(
            services = services,
            priceLists = priceLists,
            selectedPriceListId = selectedPriceListId,
            priceItems = priceItems,
            recipeSummaries = recipeSummaries,
        ),
        actions = CatalogActions(
            onAddService = { scope.launch { repository.addService(organization.id, user.id); reload() } },
            onToggleService = { service -> scope.launch { repository.toggleService(service, user.id); reload() } },
            onAddComponent = { recipeId, type, name, quantity, unit, unitCostMinor ->
                scope.launch {
                    constructorRepository.addComponent(
                        organization.id,
                        user.id,
                        recipeId,
                        type,
                        name,
                        quantity,
                        unit,
                        unitCostMinor,
                    )
                    reload()
                }
            },
            onSetComponentActive = { componentId, active ->
                scope.launch {
                    constructorRepository.setComponentActive(organization.id, user.id, componentId, active)
                    reload()
                }
            },
            onAddPriceList = { scope.launch { repository.addPriceList(organization.id, user.id); reload() } },
            onSelectPriceList = { priceListId ->
                scope.launch {
                    selectedPriceListId = priceListId
                    priceItems = repository.priceItems(priceListId)
                }
            },
            onAddPriceItem = { priceList, service ->
                scope.launch {
                    repository.addPriceItem(priceList, service, user.id)
                    reload()
                }
            },
        ),
    )
}
