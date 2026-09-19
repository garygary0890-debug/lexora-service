package com.lexora.service.presentation

import com.lexora.service.core.domain.WorkspaceOperations
import com.lexora.service.core.domain.WorkspaceSnapshot
import com.lexora.service.core.model.UserRole
import com.lexora.service.core.presentation.LexoraViewModel

data class WorkspaceUiState(
    val loading: Boolean = true,
    val snapshot: WorkspaceSnapshot? = null,
    val error: String? = null,
)

class WorkspaceViewModel(
    private val workspace: WorkspaceOperations,
) : LexoraViewModel<WorkspaceUiState>(WorkspaceUiState()) {
    init { bootstrap() }

    fun bootstrap() = launchSafely(::fail) {
        updateState { it.copy(loading = true, error = null) }
        setState(WorkspaceUiState(false, workspace.bootstrap()))
    }

    fun switchOrganization(organizationId: String) = launchSafely(::fail) {
        val userId = state.value.snapshot?.user?.id ?: return@launchSafely
        setState(WorkspaceUiState(false, workspace.switchOrganization(userId, organizationId)))
    }

    fun createOrganization(name: String) = launchSafely(::fail) {
        val userId = state.value.snapshot?.user?.id ?: return@launchSafely
        setState(WorkspaceUiState(false, workspace.createOrganization(userId, name)))
    }

    fun createUser(displayName: String, role: UserRole) = launchSafely(::fail) {
        val current = state.value.snapshot ?: return@launchSafely
        workspace.createUser(current.user.id, current.organization.id, displayName, role)
        refreshCurrent()
    }

    fun setRole(userId: String, role: UserRole, enabled: Boolean) = launchSafely(::fail) {
        val current = state.value.snapshot ?: return@launchSafely
        workspace.setRole(current.user.id, userId, current.organization.id, role, enabled)
        refreshCurrent()
    }

    fun setUserActive(userId: String, active: Boolean) = launchSafely(::fail) {
        val current = state.value.snapshot ?: return@launchSafely
        workspace.setUserActive(current.user.id, userId, current.organization.id, active)
        refreshCurrent()
    }

    fun refreshCurrent() = launchSafely(::fail) {
        val current = state.value.snapshot ?: return@launchSafely
        setState(WorkspaceUiState(false, workspace.refresh(current.organization.id, current.user.id)))
    }

    private fun fail(error: Throwable) = updateState { it.copy(loading = false, error = error.message ?: "workspace_failed") }
}
