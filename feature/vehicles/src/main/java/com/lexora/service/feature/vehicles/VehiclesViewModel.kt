package com.lexora.service.feature.vehicles

import com.lexora.service.core.model.SaveVehicleCommand
import com.lexora.service.core.domain.ServiceHistoryOperations
import com.lexora.service.core.domain.ServiceOperations
import com.lexora.service.core.model.Client
import com.lexora.service.core.model.ServiceHistoryRecord
import com.lexora.service.core.model.Vehicle
import com.lexora.service.core.presentation.LexoraViewModel

data class VehiclesUiState(
    val loading: Boolean = true,
    val vehicles: List<Vehicle> = emptyList(),
    val archived: List<Vehicle> = emptyList(),
    val clients: List<Client> = emptyList(),
    val historyVehicleId: String? = null,
    val historyRecords: List<ServiceHistoryRecord> = emptyList(),
    val historyLoading: Boolean = false,
    val error: String? = null,
)

class VehiclesViewModel(
    private val organizationId: String,
    private val userId: String,
    private val operations: ServiceOperations,
    private val history: ServiceHistoryOperations,
) : LexoraViewModel<VehiclesUiState>(VehiclesUiState()) {
    init { reload() }

    fun reload() = launchSafely(::fail) {
        updateState { it.copy(loading = true, error = null) }
        val vehicles = operations.vehicles(organizationId)
        val clients = operations.clients(organizationId)
        updateState { it.copy(loading = false, vehicles = vehicles.active, archived = vehicles.archived, clients = clients.active + clients.archived) }
    }

    fun save(draft: VehicleDraft, existingId: String?) = launchSafely(::fail) {
        operations.saveVehicle(organizationId, userId, SaveVehicleCommand(draft.clientId, draft.registrationNumber, draft.vin, draft.make, draft.model, draft.year, draft.bodyType, draft.color, draft.mileageKm), existingId)
        refreshVehicles()
    }

    fun archive(id: String, restore: Boolean = false) = launchSafely(::fail) {
        operations.archiveVehicle(organizationId, userId, id, restore)
        refreshVehicles()
    }

    fun openHistory(vehicleId: String) = launchSafely(::fail) {
        updateState { it.copy(historyVehicleId = vehicleId, historyRecords = emptyList(), historyLoading = true) }
        val records = history.history(vehicleId)
        updateState { it.copy(historyRecords = records, historyLoading = false) }
    }

    fun closeHistory() = updateState { it.copy(historyVehicleId = null, historyRecords = emptyList(), historyLoading = false) }

    private suspend fun refreshVehicles() {
        val data = operations.vehicles(organizationId)
        updateState { it.copy(vehicles = data.active, archived = data.archived, loading = false) }
    }

    private fun fail(error: Throwable) = updateState { it.copy(loading = false, historyLoading = false, error = error.message ?: "vehicles_failed") }
}

