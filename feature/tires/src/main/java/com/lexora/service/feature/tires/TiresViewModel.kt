package com.lexora.service.feature.tires

import com.lexora.service.core.domain.TireOperations
import com.lexora.service.core.model.*
import com.lexora.service.core.presentation.LexoraViewModel

data class TiresUiState(
    val loading: Boolean = true,
    val queue: List<TireQueueItem> = emptyList(),
    val diagnostics: List<TireDiagnostic> = emptyList(),
    val workEntries: List<TireWorkEntry> = emptyList(),
    val storage: List<TireStorageItem> = emptyList(),
    val error: String? = null,
)

class TiresViewModel(
    private val organizationId: String,
    private val tires: TireOperations,
) : LexoraViewModel<TiresUiState>(TiresUiState()) {
    init { reload() }
    fun reload() = launchSafely(::fail) { refresh() }
    fun addQueueItem() = launchSafely(::fail) { tires.addQueueItem(organizationId); refresh() }
    fun advanceQueue(id: String) = launchSafely(::fail) { state.value.queue.firstOrNull { it.id == id }?.let { tires.advanceQueueItem(it) }; refresh() }
    fun addDiagnostic() = launchSafely(::fail) { tires.addDiagnostic(organizationId); refresh() }
    fun addWorkEntry() = launchSafely(::fail) { tires.addWorkEntry(organizationId); refresh() }
    fun addStorageItem() = launchSafely(::fail) { tires.addStorageItem(organizationId); refresh() }
    fun issueStorage(id: String) = launchSafely(::fail) { state.value.storage.firstOrNull { it.id == id }?.let { tires.issueStorageItem(it) }; refresh() }

    private suspend fun refresh() {
        updateState { it.copy(loading = true, error = null) }
        setState(TiresUiState(false, tires.queue(organizationId), tires.diagnostics(organizationId), tires.workEntries(organizationId), tires.storage(organizationId)))
    }
    private fun fail(error: Throwable) = updateState { it.copy(loading = false, error = error.message ?: "tires_failed") }
}

