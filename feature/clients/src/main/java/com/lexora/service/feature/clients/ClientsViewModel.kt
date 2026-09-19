package com.lexora.service.feature.clients

import com.lexora.service.core.model.SaveClientCommand
import com.lexora.service.core.domain.ServiceOperations
import com.lexora.service.core.model.Client
import com.lexora.service.core.presentation.LexoraViewModel

data class ClientsUiState(
    val loading: Boolean = true,
    val clients: List<Client> = emptyList(),
    val archived: List<Client> = emptyList(),
    val error: String? = null,
)

class ClientsViewModel(
    private val organizationId: String,
    private val userId: String,
    private val operations: ServiceOperations,
) : LexoraViewModel<ClientsUiState>(ClientsUiState()) {
    init { reload() }

    fun reload() = launchSafely(::fail) {
        updateState { it.copy(loading = true, error = null) }
        operations.clients(organizationId).also { data ->
            setState(ClientsUiState(false, data.active, data.archived))
        }
    }

    fun save(draft: ClientDraft, existingId: String?) = launchSafely(::fail) {
        operations.saveClient(organizationId, userId, SaveClientCommand(draft.type, draft.displayName, draft.phone, draft.email, draft.taxId, draft.kpp, draft.registrationAddress, draft.actualAddress, draft.note, draft.consentPersonalData), existingId)
        reload()
    }

    fun archive(id: String, restore: Boolean = false) = launchSafely(::fail) {
        operations.archiveClient(organizationId, userId, id, restore)
        reload()
    }

    private fun fail(error: Throwable) = updateState { it.copy(loading = false, error = error.message ?: "clients_failed") }
}

