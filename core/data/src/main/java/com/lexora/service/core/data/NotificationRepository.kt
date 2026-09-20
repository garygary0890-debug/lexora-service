package com.lexora.service.core.data

import com.lexora.service.core.domain.NotificationOperations

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
) : NotificationOperations {
    override suspend fun notifications(organizationId: String): List<ServiceNotification> =
        notificationDao.notifications(organizationId).map(ServiceNotificationEntity::toModel)

    override suspend fun archivedNotifications(organizationId: String): List<ServiceNotification> =
        notificationDao.archivedNotifications(organizationId).map(ServiceNotificationEntity::toModel)

    suspend fun unreadCount(organizationId: String): Int = notificationDao.unreadNotifications(organizationId).size

    override suspend fun markRead(id: String, read: Boolean) {
        val now = System.currentTimeMillis()
        notificationDao.markRead(id, if (read) now else null, SyncState.PENDING_UPDATE.name, now)
    }

    override suspend fun markAllRead(organizationId: String) {
        val now = System.currentTimeMillis()
        notificationDao.markAllRead(organizationId, now, SyncState.PENDING_UPDATE.name, now)
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        val now = System.currentTimeMillis()
        notificationDao.setArchived(id, archived, SyncState.PENDING_UPDATE.name, now)
    }

    override suspend fun refreshGenerated(organizationId: String, now: Long) {
        serviceDao.serviceRequests(organizationId).forEach { request ->
                val activeRequest = request.status != "CLOSED" && request.status != "CANCELLED"
                request.dueAtEpochMs?.let { due ->
                    if (due <= now + DAY_MS) {
                        createIfMissing(
                            id = "request-due:${request.id}",
                            organizationId = organizationId,
                            type = ServiceNotificationType.REQUEST_DUE,
                            priority = if (due < now) ServiceNotificationPriority.CRITICAL else ServiceNotificationPriority.WARNING,
                            title = if (due < now) "РџСЂРѕСЃСЂРѕС‡РµРЅР° Р·Р°СЏРІРєР° ${request.number}" else "РЎСЂРѕРє Р·Р°СЏРІРєРё ${request.number}",
                            message = request.title,
                            entityType = "SERVICE_REQUEST",
                            entityId = request.id,
                            scheduledAt = due,
                            now = now,
                        )
                    }
                }
                val warningWindowMs = request.slaWarningMinutes.coerceAtLeast(0).toLong() * 60_000L
                val reactionDeadline = request.slaReactionMinutes?.let { request.createdAtEpochMs + it.toLong() * 60_000L }
                syncSlaNotification(
                    id = "sla-reaction:${request.id}", organizationId = organizationId, requestNumber = request.number,
                    requestTitle = request.title, label = "реакции", deadline = reactionDeadline,
                    active = activeRequest && request.firstReactionAtEpochMs == null && reactionDeadline != null && reactionDeadline <= now + warningWindowMs,
                    now = now,
                )
                val resolutionDeadline = request.slaResolutionMinutes?.let { request.createdAtEpochMs + it.toLong() * 60_000L }
                syncSlaNotification(
                    id = "sla-resolution:${request.id}", organizationId = organizationId, requestNumber = request.number,
                    requestTitle = request.title, label = "выполнения", deadline = resolutionDeadline,
                    active = activeRequest && request.closedAtEpochMs == null && resolutionDeadline != null && resolutionDeadline <= now + warningWindowMs,
                    now = now,
                )
                notificationDao.notification("sla:${request.id}")?.let { legacy ->
                    if (!legacy.archived) notificationDao.setArchived(legacy.id, true, SyncState.PENDING_UPDATE.name, now)
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
                        title = "РўСЂРµР±СѓРµС‚СЃСЏ СЃРѕРіР»Р°СЃРѕРІР°РЅРёРµ РґРѕРїСЂР°Р±РѕС‚",
                        message = item.title,
                        entityType = "WORK_ORDER_ITEM",
                        entityId = item.id,
                        scheduledAt = null,
                        now = now,
                    )
                }
        }
    }

    private suspend fun syncSlaNotification(
        id: String,
        organizationId: String,
        requestNumber: String,
        requestTitle: String,
        label: String,
        deadline: Long?,
        active: Boolean,
        now: Long,
    ) {
        val existing = notificationDao.notification(id)
        if (!active || deadline == null) {
            if (existing != null && !existing.archived) notificationDao.setArchived(id, true, SyncState.PENDING_UPDATE.name, now)
            return
        }
        val breached = deadline < now
        val title = if (breached) "SLA $label нарушен: $requestNumber" else "Приближается SLA $label: $requestNumber"
        val priority = if (breached) ServiceNotificationPriority.CRITICAL else ServiceNotificationPriority.WARNING
        if (existing?.archived == true) return
        notificationDao.upsert(
            ServiceNotificationEntity(
                id = id, organizationId = organizationId, type = ServiceNotificationType.SLA_WARNING.name,
                priority = priority.name, title = title, message = requestTitle,
                entityType = "SERVICE_REQUEST", entityId = id.substringAfterLast(':'), scheduledAtEpochMs = deadline,
                occurredAtEpochMs = existing?.occurredAtEpochMs ?: now,
                readAtEpochMs = if (existing?.priority != null && existing.priority != priority.name) null else existing?.readAtEpochMs,
                archived = false, syncState = if (existing == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name, updatedAtEpochMs = now,
            ),
        )
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
