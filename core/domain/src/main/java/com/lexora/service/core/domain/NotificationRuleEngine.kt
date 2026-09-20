package com.lexora.service.core.domain

import com.lexora.service.core.model.ExtendedServiceContract
import com.lexora.service.core.model.LinkedServiceTask
import com.lexora.service.core.model.RequestPriority
import com.lexora.service.core.model.ServiceContractStatus
import com.lexora.service.core.model.ServiceNotificationPriority
import com.lexora.service.core.model.TaskStatus

data class GeneratedNotificationCandidate(
    val id: String,
    val priority: ServiceNotificationPriority,
    val title: String,
    val message: String,
    val entityType: String,
    val entityId: String,
    val scheduledAtEpochMs: Long? = null,
)

object NotificationRuleEngine {
    fun taskReminder(task: LinkedServiceTask, nowEpochMs: Long): GeneratedNotificationCandidate? {
        val reminderAt = task.reminderAtEpochMs ?: return null
        if (task.status !in setOf(TaskStatus.OPEN, TaskStatus.IN_PROGRESS) || reminderAt > nowEpochMs) return null
        val priority = if (task.priority == RequestPriority.URGENT) {
            ServiceNotificationPriority.CRITICAL
        } else {
            ServiceNotificationPriority.WARNING
        }
        return GeneratedNotificationCandidate(
            id = "task-reminder:${task.id}",
            priority = priority,
            title = "Напоминание по задаче",
            message = task.title,
            entityType = "SERVICE_TASK",
            entityId = task.id,
            scheduledAtEpochMs = reminderAt,
        )
    }

    fun contractExpiry(
        contract: ExtendedServiceContract,
        nowEpochMs: Long,
        warningLeadMs: Long,
    ): GeneratedNotificationCandidate? {
        if (contract.status !in setOf(ServiceContractStatus.ACTIVE, ServiceContractStatus.SUSPENDED)) return null
        val expiresAt = contract.validToEpochMs ?: return null
        if (nowEpochMs < expiresAt - warningLeadMs.coerceAtLeast(0L)) return null
        val expired = nowEpochMs > expiresAt
        return GeneratedNotificationCandidate(
            id = "contract-expiry:${contract.id}",
            priority = if (expired) ServiceNotificationPriority.CRITICAL else ServiceNotificationPriority.WARNING,
            title = if (expired) "Договор ${contract.number} истёк" else "Истекает договор ${contract.number}",
            message = if (expired) "Срок действия договора завершён" else "Приближается окончание срока договора",
            entityType = "SERVICE_CONTRACT",
            entityId = contract.id,
            scheduledAtEpochMs = expiresAt,
        )
    }

    fun tireStorage(
        storageId: String,
        storageCode: String,
        active: Boolean,
        expectedEndEpochMs: Long?,
        reminderLeadMs: Long,
        nowEpochMs: Long,
    ): GeneratedNotificationCandidate? {
        if (!active) return null
        val expiresAt = expectedEndEpochMs ?: return null
        if (nowEpochMs < expiresAt - reminderLeadMs.coerceAtLeast(0L)) return null
        val overdue = nowEpochMs > expiresAt
        return GeneratedNotificationCandidate(
            id = "tire-storage:$storageId",
            priority = if (overdue) ServiceNotificationPriority.CRITICAL else ServiceNotificationPriority.WARNING,
            title = if (overdue) "Просрочено хранение шин $storageCode" else "Заканчивается хранение шин $storageCode",
            message = if (overdue) "Требуется связаться с клиентом" else "Приближается дата окончания хранения",
            entityType = "TIRE_STORAGE",
            entityId = storageId,
            scheduledAtEpochMs = expiresAt,
        )
    }
}
