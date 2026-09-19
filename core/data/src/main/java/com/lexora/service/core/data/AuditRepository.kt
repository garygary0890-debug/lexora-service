package com.lexora.service.core.data

import com.lexora.service.core.domain.AuditOperations

import android.content.Context
import com.lexora.service.core.database.AuditEventEntity
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.database.ServiceDao
import com.lexora.service.core.model.AuditFilter
import com.lexora.service.core.model.AuditRecord

class AuditRepository(private val serviceDao: ServiceDao) : AuditOperations {
    override suspend fun records(
        organizationId: String,
        filter: AuditFilter,
        limit: Int,
    ): List<AuditRecord> {
        val query = filter.query.trim()
        return serviceDao.recentAuditEvents(organizationId, limit)
            .asSequence()
            .map(AuditEventEntity::toModel)
            .filter { filter.entityType == null || it.entityType == filter.entityType }
            .filter { filter.action == null || it.action == filter.action }
            .filter { filter.userId == null || it.userId == filter.userId }
            .filter {
                query.isBlank() || listOfNotNull(it.entityType, it.entityId, it.action, it.summary, it.userId)
                    .any { value -> value.contains(query, ignoreCase = true) }
            }
            .toList()
    }

    suspend fun entityTypes(organizationId: String): List<String> =
        serviceDao.recentAuditEvents(organizationId, 500).map { it.entityType }.distinct().sorted()

    suspend fun actions(organizationId: String): List<String> =
        serviceDao.recentAuditEvents(organizationId, 500).map { it.action }.distinct().sorted()

    suspend fun users(organizationId: String): List<String> =
        serviceDao.recentAuditEvents(organizationId, 500).map { it.userId }.distinct().sorted()

    companion object {
        fun create(context: Context): AuditRepository =
            AuditRepository(LexoraServiceDatabase.create(context.applicationContext).serviceDao())
    }
}

private fun AuditEventEntity.toModel() = AuditRecord(
    id = id,
    organizationId = organizationId,
    userId = userId,
    entityType = entityType,
    entityId = entityId,
    action = action,
    summary = summary,
    occurredAtEpochMs = occurredAtEpochMs,
)

