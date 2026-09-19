package com.lexora.service.feature.users

import com.lexora.service.core.domain.WorkspaceOperations
import com.lexora.service.core.model.ServiceUser
import com.lexora.service.core.model.UserRole
import com.lexora.service.core.presentation.LexoraViewModel

data class UsersUiState(val users: List<ServiceUser> = emptyList(), val error: String? = null)

class UsersViewModel(
    private val organizationId: String,
    private val actorUserId: String,
    private val operations: WorkspaceOperations,
) : LexoraViewModel<UsersUiState>(UsersUiState()) {
    init { reload() }
    fun reload() = launchSafely(::fail) {
        setState(UsersUiState(operations.refresh(organizationId, actorUserId).users))
    }
    fun create(displayName: String, role: UserRole) = launchSafely(::fail) {
        operations.createUser(actorUserId, organizationId, displayName, role); reload()
    }
    fun setRole(userId: String, role: UserRole, enabled: Boolean) = launchSafely(::fail) {
        operations.setRole(actorUserId, userId, organizationId, role, enabled); reload()
    }
    fun setActive(userId: String, active: Boolean) = launchSafely(::fail) {
        operations.setUserActive(actorUserId, userId, organizationId, active); reload()
    }
    private fun fail(error: Throwable) = updateState { it.copy(error = error.message ?: "users_failed") }
}
