package com.lexora.service.core.data

import android.content.Context
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.database.NotificationDao
import com.lexora.service.core.database.ServiceDao
import com.lexora.service.core.database.ServiceNotificationEntity
import com.lexora.service.core.database.WorkOrderDao
import com.lexora.service.core.model.ServiceNotification
import com.lexora.service.core.model.ServiceNotificationPriority
import com.lexora.service.core.model.ServiceNotificationType
import com.lexora.service.core.model.SyncState

class NotificationRepository(
    private val notificationDao: NotificationDao,
    private val serviceDao: ServiceDao,
    private val workOrderDao: WorkOrderDao,
) {
    suspend fun notifications(organizationId: String): List<ServiceNotification> =
        notificationDao.notifications(organizationId).map(ServiceNotificationEntity::toModel)

    suspend fun archivedNotifications(organizationId: String): List<ServiceNotification> =
        notificationDao.archivedNotifications(organizationId).map(ServiceNotificationEntity::toModel)

    suspend fun unreadCount(organizationId: String): Int = notificationDao.unreadNotifications(organizationId).size

    suspend fun markRead(id: String, read: Boolean) {
        val now = System.currentTimeMillis()
        notificationDao.markRead(id, if (read) now else null, SyncState.PENDING_UPDATE.name, now)
    }

    suspend fun markAllRead(organizationId: String) {
        val now = System.currentTimeMillis()
        notificationDao.markAllRead(organizationId, now, SyncState.PENDING_UPDATE.name, now)
    }

    suspend fun setArchived(id: String, archived: Boolean) {
        val now = System.currentTimeMillis()
        notificationDao.setArchived(id, archived, SyncState.PENDING_UPDATE.name, now)
    }

    suspend fun refreshGenerated(organizationId: String, now: Long = System.currentTimeMillis()) {
        serviceDao.serviceRequests(organizationId)
            .filter { it.status != "CLOSED" }
            .forEach { request ->
                request.dueAtEpochMs?.let { due ->
                    if (due <= now + DAY_MS) {
                        createIfMissing(
                            id = "request-due:${request.id}",
                            organizationId = organizationId,
                            type = ServiceNotificationType.REQUEST_DUE,
                            priority = if (due < now) ServiceNotificationPriority.CRITICAL else ServiceNotificationPriority.WARNING,
                            title = if (due < now) "Просрочена заявка ${request.number}" else "Срок заявки ${request.number}",
                            message = request.title,
                            entityType = "SERVICE_REQUEST",
                            entityId = request.id,
                            scheduledAt = due,
                            now = now,
                        )
                    }
                }
                request.slaDeadlineEpochMs?.let { sla ->
                    if (sla <= now + DAY_MS) {
                        createIfMissing(
                            id = "sla:${request.id}",
                            organizationId = organizationId,
                            type = ServiceNotificationType.SLA_WARNING,
                            priority = if (sla < now) ServiceNotificationPriority.CRITICAL else ServiceNotificationPriority.WARNING,
                            title = if (sla < now) "SLA нарушен: ${request.number}" else "Приближается SLA: ${request.number}",
                            message = request.title,
                            entityType = "SERVICE_REQUEST",
                            entityId = request.id,
                            scheduledAt = sla,
                            now = now,
                        )
                    }
                }
            }

        serviceDao.serviceDocuments(organizationId).forEach { document ->
            workOrderDao.items(document.id)
                .filter { it.additional && it.approvalStatus == "PENDING" }
                .forEach { item ->
                    createIfMissing(
                        id = "approval:${item.id}",
                        organizationId = organizationId,
                        type = ServiceNotificationType.ADDITIONAL_WORK_APPROVAL,
                        priority = ServiceNotificationPriority.WARNING,
                        title = "Требуется согласование допработ",
                        message = item.title,
                        entityType = "WORK_ORDER_ITEM",
                        entityId = item.id,
                        scheduledAt = null,
                        now = now,
                    )
                }
        }
    }

    private suspend fun createIfMissing(
        id: String,
        organizationId: String,
        type: ServiceNotificationType,
        priority: ServiceNotificationPriority,
        title: String,
        message: String,
        entityType: String,
        entityId: String,
        scheduledAt: Long?,
        now: Long,
    ) {
        if (notificationDao.notification(id) != null) return
        notificationDao.upsert(
            ServiceNotificationEntity(
                id = id,
                organizationId = organizationId,
                type = type.name,
                priority = priority.name,
                title = title,
                message = message,
                entityType = entityType,
                entityId = entityId,
                scheduledAtEpochMs = scheduledAt,
                occurredAtEpochMs = now,
                readAtEpochMs = null,
                archived = false,
                syncState = SyncState.PENDING_CREATE.name,
                updatedAtEpochMs = now,
            ),
        )
    }

    companion object {
        private const val DAY_MS = 24L * 60L * 60L * 1000L

        fun create(context: Context): NotificationRepository {
            val db = LexoraServiceDatabase.create(context.applicationContext)
            return NotificationRepository(db.notificationDao(), db.serviceDao(), db.workOrderDao())
        }
    }
}

private fun ServiceNotificationEntity.toModel() = ServiceNotification(
    id = id,
    organizationId = organizationId,
    type = ServiceNotificationType.valueOf(type),
    priority = ServiceNotificationPriority.valueOf(priority),
    title = title,
    message = message,
    entityType = entityType,
    entityId = entityId,
    scheduledAtEpochMs = scheduledAtEpochMs,
    occurredAtEpochMs = occurredAtEpochMs,
    readAtEpochMs = readAtEpochMs,
    archived = archived,
    syncState = SyncState.valueOf(syncState),
)
