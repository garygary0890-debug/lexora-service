package com.lexora.service.feature.notifications

import com.lexora.service.core.domain.NotificationOperations
import com.lexora.service.core.model.ServiceNotification
import com.lexora.service.core.presentation.LexoraViewModel

data class NotificationsUiState(
    val archivedMode: Boolean = false,
    val loading: Boolean = true,
    val notifications: List<ServiceNotification> = emptyList(),
    val error: String? = null,
)

class NotificationsViewModel(
    private val organizationId: String,
    private val notifications: NotificationOperations,
) : LexoraViewModel<NotificationsUiState>(NotificationsUiState()) {
    init { reload() }

    fun setArchivedMode(value: Boolean) {
        updateState { it.copy(archivedMode = value) }
        reload()
    }

    fun reload() = launchSafely(::fail) {
        updateState { it.copy(loading = true, error = null) }
        notifications.refreshGenerated(organizationId)
        val list = if (state.value.archivedMode) notifications.archivedNotifications(organizationId) else notifications.notifications(organizationId)
        updateState { it.copy(loading = false, notifications = list) }
    }

    fun markAllRead() = launchSafely(::fail) { notifications.markAllRead(organizationId); refreshList() }
    fun markRead(id: String, read: Boolean) = launchSafely(::fail) { notifications.markRead(id, read); refreshList() }
    fun setArchived(id: String, archived: Boolean) = launchSafely(::fail) { notifications.setArchived(id, archived); refreshList() }

    private suspend fun refreshList() {
        val list = if (state.value.archivedMode) notifications.archivedNotifications(organizationId) else notifications.notifications(organizationId)
        updateState { it.copy(loading = false, notifications = list, error = null) }
    }

    private fun fail(error: Throwable) = updateState { it.copy(loading = false, error = error.message ?: "notifications_failed") }
}

