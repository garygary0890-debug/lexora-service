package com.lexora.service.core.model

enum class ServiceNotificationType {
    REQUEST_DUE,
    SLA_WARNING,
    PAYMENT,
    ADDITIONAL_WORK_APPROVAL,
    QUALITY_CONTROL,
    CONTRACT,
    SYSTEM,
    MANUAL,
}

enum class ServiceNotificationPriority { INFO, WARNING, CRITICAL }

data class ServiceNotification(
    val id: String,
    val organizationId: String,
    val type: ServiceNotificationType,
    val priority: ServiceNotificationPriority = ServiceNotificationPriority.INFO,
    val title: String,
    val message: String,
    val entityType: String? = null,
    val entityId: String? = null,
    val scheduledAtEpochMs: Long? = null,
    val occurredAtEpochMs: Long,
    val readAtEpochMs: Long? = null,
    val archived: Boolean = false,
    val syncState: SyncState = SyncState.PENDING_CREATE,
) {
    val unread: Boolean get() = readAtEpochMs == null
}
