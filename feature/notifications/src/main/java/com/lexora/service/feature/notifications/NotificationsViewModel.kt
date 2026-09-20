package com.lexora.service.feature.notifications

import com.lexora.service.core.domain.NotificationDeliveryOperations
import com.lexora.service.core.domain.NotificationOperations
import com.lexora.service.core.model.DoNotDisturbPolicy
import com.lexora.service.core.model.NotificationChannel
import com.lexora.service.core.model.NotificationDelivery
import com.lexora.service.core.model.NotificationPreferences
import com.lexora.service.core.model.ServiceNotification
import com.lexora.service.core.presentation.LexoraViewModel
import java.time.ZoneId

data class NotificationsUiState(
    val archivedMode: Boolean = false,
    val loading: Boolean = true,
    val notifications: List<ServiceNotification> = emptyList(),
    val preferences: NotificationPreferences? = null,
    val deliveries: List<NotificationDelivery> = emptyList(),
    val dndStartText: String = "22:00",
    val dndEndText: String = "07:00",
    val preferenceError: String? = null,
    val pushProviderStatus: String = "Push: провайдер не настроен",
    val error: String? = null,
)

class NotificationsViewModel(
    private val organizationId: String,
    private val userId: String,
    private val notifications: NotificationOperations,
    private val delivery: NotificationDeliveryOperations,
    private val onDeliveryQueued: (NotificationDelivery) -> Unit = {},
) : LexoraViewModel<NotificationsUiState>(NotificationsUiState()) {
    init { reload() }

    fun setArchivedMode(value: Boolean) {
        updateState { it.copy(archivedMode = value) }
        reload()
    }

    fun reload() = launchSafely(::fail) {
        updateState { it.copy(loading = true, error = null) }
        notifications.refreshGenerated(organizationId)
        val preferences = delivery.preferences(organizationId, userId)
        val list = if (state.value.archivedMode) notifications.archivedNotifications(organizationId) else notifications.notifications(organizationId)
        if (!state.value.archivedMode) enqueueDeliveries(list, preferences)
        val deliveries = delivery.deliveries(organizationId, userId)
        updateState {
            it.copy(
                loading = false,
                notifications = list,
                preferences = preferences,
                deliveries = deliveries,
                dndStartText = NotificationTimeInput.formatMinutes(preferences.doNotDisturb.startMinuteOfDay).ifBlank { "22:00" },
                dndEndText = NotificationTimeInput.formatMinutes(preferences.doNotDisturb.endMinuteOfDay).ifBlank { "07:00" },
                preferenceError = null,
            )
        }
    }

    fun markAllRead() = launchSafely(::fail) { notifications.markAllRead(organizationId); refreshList() }
    fun markRead(id: String, read: Boolean) = launchSafely(::fail) { notifications.markRead(id, read); refreshList() }
    fun setArchived(id: String, archived: Boolean) = launchSafely(::fail) { notifications.setArchived(id, archived); refreshList() }

    fun setDndEnabled(enabled: Boolean) = savePreferences { current ->
        current.copy(doNotDisturb = current.doNotDisturb.copy(enabled = enabled))
    }

    fun setLocalEnabled(enabled: Boolean) = savePreferences { it.copy(localEnabled = enabled) }
    fun setPushEnabled(enabled: Boolean) = savePreferences { it.copy(pushEnabled = enabled) }
    fun setAllowCritical(enabled: Boolean) = savePreferences { current ->
        current.copy(doNotDisturb = current.doNotDisturb.copy(allowCritical = enabled))
    }

    fun setDndStartText(value: String) = updateState { it.copy(dndStartText = value, preferenceError = null) }
    fun setDndEndText(value: String) = updateState { it.copy(dndEndText = value, preferenceError = null) }

    fun saveDndWindow() {
        val start = NotificationTimeInput.parseMinutes(state.value.dndStartText)
        val end = NotificationTimeInput.parseMinutes(state.value.dndEndText)
        if (start == null || end == null) {
            updateState { it.copy(preferenceError = "Укажите время в формате ЧЧ:ММ") }
            return
        }
        savePreferences { current ->
            current.copy(
                doNotDisturb = current.doNotDisturb.copy(
                    startMinuteOfDay = start,
                    endMinuteOfDay = end,
                    timeZoneId = current.doNotDisturb.timeZoneId ?: ZoneId.systemDefault().id,
                ),
            )
        }
    }

    private fun savePreferences(transform: (NotificationPreferences) -> NotificationPreferences) = launchSafely(::fail) {
        val current = state.value.preferences ?: delivery.preferences(organizationId, userId)
        val updated = transform(current)
        delivery.savePreferences(updated)
        updateState { it.copy(preferences = updated, preferenceError = null) }
        if (!state.value.archivedMode) enqueueDeliveries(state.value.notifications, updated)
        updateState { it.copy(deliveries = delivery.deliveries(organizationId, userId)) }
    }

    private suspend fun enqueueDeliveries(
        items: List<ServiceNotification>,
        preferences: NotificationPreferences,
    ) {
        items.filterNot { it.archived }.forEach { item ->
            if (preferences.localEnabled) {
                val queued = delivery.enqueue(
                    organizationId = organizationId,
                    userId = userId,
                    channel = NotificationChannel.LOCAL,
                    idempotencyKey = "inbox:${item.id}",
                    eventCode = item.type.name,
                    title = item.title,
                    body = item.message,
                    priority = item.priority,
                    entityType = item.entityType,
                    entityId = item.entityId,
                    scheduledAtEpochMs = item.scheduledAtEpochMs,
                )
                onDeliveryQueued(queued)
            }
            if (preferences.pushEnabled) {
                delivery.enqueue(
                    organizationId = organizationId,
                    userId = userId,
                    channel = NotificationChannel.PUSH,
                    idempotencyKey = "inbox:${item.id}",
                    eventCode = item.type.name,
                    title = item.title,
                    body = item.message,
                    priority = item.priority,
                    entityType = item.entityType,
                    entityId = item.entityId,
                    scheduledAtEpochMs = item.scheduledAtEpochMs,
                )
            }
        }
    }

    private suspend fun refreshList() {
        val list = if (state.value.archivedMode) notifications.archivedNotifications(organizationId) else notifications.notifications(organizationId)
        updateState {
            it.copy(
                loading = false,
                notifications = list,
                deliveries = delivery.deliveries(organizationId, userId),
                error = null,
            )
        }
    }

    private fun fail(error: Throwable) = updateState { it.copy(loading = false, error = error.message ?: "notifications_failed") }
}
