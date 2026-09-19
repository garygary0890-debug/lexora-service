package com.lexora.service.feature.settings

import com.lexora.service.core.domain.SettingsOperations
import com.lexora.service.core.domain.SettingsSnapshot
import com.lexora.service.core.model.*
import com.lexora.service.core.presentation.LexoraViewModel

data class SettingsUiState(
    val loading: Boolean = true,
    val data: SettingsSnapshot? = null,
    val message: String? = null,
)

class SettingsViewModel(
    private val organizationId: String,
    private val userId: String,
    private val operations: SettingsOperations,
) : LexoraViewModel<SettingsUiState>(SettingsUiState()) {
    init { reload() }

    fun reload(preferredDirectoryId: String? = currentState.data?.selectedDirectoryId) = launchSafely(::fail) {
        updateState { it.copy(loading = true) }
        setState(SettingsUiState(false, operations.snapshot(organizationId, preferredDirectoryId)))
    }

    fun selectDirectory(id: String) = reload(id)
    fun createDirectory(code: String, name: String) = launchSafely(::fail) {
        val id = operations.createDirectory(organizationId, userId, code, name)
        updateState { it.copy(message = "Справочник создан.") }
        reload(id)
    }

    fun createItem(directoryId: String, code: String, name: String) = launchSafely(::fail) {
        operations.createDirectoryItem(organizationId, userId, directoryId, code, name)
        updateState { it.copy(message = "Значение добавлено.") }
        reload(directoryId)
    }

    fun setDirectoryActive(id: String, active: Boolean) = launchSafely(::fail) {
        operations.setDirectoryActive(organizationId, userId, id, active); reload(id)
    }

    fun setItemActive(directoryId: String, id: String, active: Boolean) = launchSafely(::fail) {
        operations.setDirectoryItemActive(organizationId, userId, id, active); reload(directoryId)
    }

    fun setModuleEnabled(moduleId: LexoraModuleId, enabled: Boolean, onChanged: () -> Unit) = launchSafely(::fail) {
        operations.setModuleEnabled(organizationId, moduleId, enabled); reload(); onChanged()
    }

    fun setLicense(moduleId: LexoraModuleId, status: ModuleLicenseStatus, onChanged: () -> Unit = {}) = launchSafely(::fail) {
        operations.setModuleLicenseStatus(organizationId, moduleId, status); reload(); onChanged()
    }

    private fun fail(error: Throwable) = updateState { it.copy(loading = false, message = error.message ?: "settings_failed") }
}
