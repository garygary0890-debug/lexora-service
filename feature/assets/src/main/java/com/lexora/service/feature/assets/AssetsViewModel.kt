package com.lexora.service.feature.assets

import com.lexora.service.core.model.SaveEquipmentCommand
import com.lexora.service.core.model.SaveServiceObjectCommand
import com.lexora.service.core.domain.ServiceOperations
import com.lexora.service.core.model.*
import com.lexora.service.core.presentation.LexoraViewModel

data class AssetsUiState(
    val loading: Boolean = true,
    val objects: List<ServiceObject> = emptyList(),
    val archivedObjects: List<ServiceObject> = emptyList(),
    val equipment: List<Equipment> = emptyList(),
    val archivedEquipment: List<Equipment> = emptyList(),
    val clients: List<Client> = emptyList(),
    val error: String? = null,
)

class AssetsViewModel(
    private val organizationId: String,
    private val userId: String,
    private val operations: ServiceOperations,
) : LexoraViewModel<AssetsUiState>(AssetsUiState()) {
    init { reload() }

    fun reload() = launchSafely(::fail) {
        updateState { it.copy(loading = true, error = null) }
        val assets = operations.assets(organizationId)
        val clients = operations.clients(organizationId)
        setState(AssetsUiState(false, assets.objects, assets.archivedObjects, assets.equipment, assets.archivedEquipment, clients.active + clients.archived))
    }

    fun saveObject(draft: ServiceObjectDraft, existingId: String?) = launchSafely(::fail) {
        operations.saveServiceObject(organizationId, userId, SaveServiceObjectCommand(draft.clientId, draft.name, draft.address, draft.accessMode, draft.responsibleContact), existingId)
        reload()
    }

    fun archiveObject(id: String, restore: Boolean = false) = launchSafely(::fail) {
        operations.archiveServiceObject(organizationId, userId, id, restore); reload()
    }

    fun saveEquipment(draft: EquipmentDraft, existingId: String?) = launchSafely(::fail) {
        operations.saveEquipment(organizationId, userId, SaveEquipmentCommand(draft.serviceObjectId, draft.type, draft.make, draft.model, draft.serialNumber, draft.inventoryNumber, draft.barcode, draft.commissionedNote, draft.warrantyNote), existingId)
        reload()
    }

    fun archiveEquipment(id: String, restore: Boolean = false) = launchSafely(::fail) {
        operations.archiveEquipment(organizationId, userId, id, restore); reload()
    }

    private fun fail(error: Throwable) = updateState { it.copy(loading = false, error = error.message ?: "assets_failed") }
}

