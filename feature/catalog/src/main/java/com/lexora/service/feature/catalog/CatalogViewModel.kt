package com.lexora.service.feature.catalog

import com.lexora.service.core.domain.CatalogOperations
import com.lexora.service.core.model.*
import com.lexora.service.core.presentation.LexoraViewModel

data class CatalogViewState(
    val loading: Boolean = true,
    val data: CatalogUiState = CatalogUiState(),
    val error: String? = null,
)

class CatalogViewModel(
    private val organizationId: String,
    private val userId: String,
    private val operations: CatalogOperations,
) : LexoraViewModel<CatalogViewState>(CatalogViewState()) {
    init { reload() }

    fun reload(selectedPriceListId: String? = currentState.data.selectedPriceListId) = launchSafely(::fail) {
        val snapshot = operations.snapshot(organizationId, userId, selectedPriceListId)
        setState(CatalogViewState(false, CatalogUiState(snapshot.services, snapshot.priceLists, snapshot.selectedPriceListId, snapshot.priceItems, snapshot.recipeSummaries)))
    }

    fun addService() = launchSafely(::fail) { operations.addService(organizationId, userId); reload() }
    fun toggleService(item: ServiceCatalogItem) = launchSafely(::fail) { operations.toggleService(item, userId); reload() }
    fun addComponent(recipeId: String, type: ServiceComponentType, name: String, quantity: Double, unit: String, unitCostMinor: Long) = launchSafely(::fail) {
        operations.addComponent(organizationId, userId, recipeId, type, name, quantity, unit, unitCostMinor); reload()
    }
    fun setComponentActive(componentId: String, active: Boolean) = launchSafely(::fail) {
        operations.setComponentActive(organizationId, userId, componentId, active); reload()
    }
    fun addPriceList() = launchSafely(::fail) { operations.addPriceList(organizationId, userId); reload() }
    fun selectPriceList(id: String) = reload(id)
    fun addPriceItem(priceList: PriceList, service: ServiceCatalogItem) = launchSafely(::fail) {
        operations.addPriceItem(priceList, service, userId); reload(priceList.id)
    }
    private fun fail(error: Throwable) = updateState { it.copy(loading = false, error = error.message ?: "catalog_failed") }
}
