package com.lexora.service.core.data

import com.lexora.service.core.domain.WashOperations

import android.content.Context
import com.lexora.service.core.database.AuditEventEntity
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.database.WashChemicalUsageEntity
import com.lexora.service.core.database.WashPostEntity
import com.lexora.service.core.database.WashQueueItemEntity
import com.lexora.service.core.database.WashTechCardEntity
import com.lexora.service.core.model.SyncState
import com.lexora.service.core.model.WashChemicalUsage
import com.lexora.service.core.model.WashPost
import com.lexora.service.core.model.WashPostStatus
import com.lexora.service.core.model.WashQueueItem
import com.lexora.service.core.model.WashQueueStatus
import com.lexora.service.core.model.WashTechCard
import java.util.UUID

class WashRepository private constructor(
    private val database: LexoraServiceDatabase,
) : WashOperations {
    private val washDao = database.washDao()
    private val serviceDao = database.serviceDao()

    override suspend fun posts(organizationId: String): List<WashPost> = washDao.posts(organizationId).map { it.toModel() }
    override suspend fun queue(organizationId: String): List<WashQueueItem> = washDao.activeQueue(organizationId).map { it.toModel() }
    override suspend fun techCards(organizationId: String): List<WashTechCard> = washDao.techCards(organizationId).map { it.toModel() }
    override suspend fun chemicalUsage(organizationId: String): List<WashChemicalUsage> = washDao.recentChemicalUsage(organizationId).map { it.toModel() }

    override suspend fun addPost(organizationId: String, branchId: String?, name: String) {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        washDao.upsertPost(WashPostEntity(id, organizationId, branchId, name, WashPostStatus.AVAILABLE.name, true, SyncState.PENDING_CREATE.name, now))
        audit(organizationId, "WASH_POST", id, "CREATE", name, now)
    }

    override suspend fun togglePostStatus(post: WashPost) {
        val next = if (post.status == WashPostStatus.AVAILABLE) WashPostStatus.OCCUPIED else WashPostStatus.AVAILABLE
        val now = System.currentTimeMillis()
        washDao.updatePostStatus(post.id, next.name, SyncState.PENDING_UPDATE.name, now)
        audit(post.organizationId, "WASH_POST", post.id, "STATUS_CHANGE", "${post.status.name} → ${next.name}", now)
    }

    override suspend fun addQueueItem(organizationId: String) {
        val now = System.currentTimeMillis()
        val request = serviceDao.serviceRequests(organizationId).firstOrNull { it.status != "CLOSED" && it.status != "CANCELLED" }
        val position = (washDao.activeQueue(organizationId).maxOfOrNull { it.position } ?: 0) + 1
        val id = UUID.randomUUID().toString()
        washDao.upsertQueueItem(WashQueueItemEntity(id, organizationId, request?.id, request?.vehicleId, null, position, WashQueueStatus.WAITING.name, now, SyncState.PENDING_CREATE.name, now))
        audit(organizationId, "WASH_QUEUE", id, "CREATE", "Очередь №$position${request?.number?.let { " · $it" }.orEmpty()}", now)
    }

    override suspend fun advanceQueueItem(organizationId: String, item: WashQueueItem) {
        val posts = washDao.posts(organizationId).map { it.toModel() }
        val now = System.currentTimeMillis()
        val next = when (item.status) {
            WashQueueStatus.WAITING -> {
                val post = posts.firstOrNull { it.status == WashPostStatus.AVAILABLE }
                washDao.assignQueueItem(item.id, post?.id, WashQueueStatus.CALLED.name, SyncState.PENDING_UPDATE.name, now)
                WashQueueStatus.CALLED
            }
            WashQueueStatus.CALLED -> {
                val postId = item.postId
                if (postId != null) washDao.updatePostStatus(postId, WashPostStatus.OCCUPIED.name, SyncState.PENDING_UPDATE.name, now)
                washDao.assignQueueItem(item.id, postId, WashQueueStatus.IN_SERVICE.name, SyncState.PENDING_UPDATE.name, now)
                WashQueueStatus.IN_SERVICE
            }
            WashQueueStatus.IN_SERVICE -> {
                val postId = item.postId
                if (postId != null) washDao.updatePostStatus(postId, WashPostStatus.AVAILABLE.name, SyncState.PENDING_UPDATE.name, now)
                washDao.assignQueueItem(item.id, postId, WashQueueStatus.COMPLETED.name, SyncState.PENDING_UPDATE.name, now)
                WashQueueStatus.COMPLETED
            }
            WashQueueStatus.COMPLETED, WashQueueStatus.CANCELLED -> item.status
        }
        if (next != item.status) audit(organizationId, "WASH_QUEUE", item.id, "STATUS_CHANGE", "${item.status.name} → ${next.name}", now)
    }

    override suspend fun addTechCard(organizationId: String) {
        val now = System.currentTimeMillis()
        val nextNumber = washDao.techCards(organizationId).size + 1
        val id = UUID.randomUUID().toString()
        val name = "Техкарта $nextNumber"
        washDao.upsertTechCard(WashTechCardEntity(id, organizationId, name, 30, "Предварительная мойка → основная мойка → ополаскивание → сушка → контроль качества", true, SyncState.PENDING_CREATE.name, now))
        audit(organizationId, "WASH_TECH_CARD", id, "CREATE", name, now)
    }

    override suspend fun addChemicalUsage(organizationId: String) {
        val now = System.currentTimeMillis()
        val activeItem = washDao.activeQueue(organizationId).firstOrNull { it.status == WashQueueStatus.IN_SERVICE.name }
        val id = UUID.randomUUID().toString()
        washDao.insertChemicalUsage(WashChemicalUsageEntity(id, organizationId, activeItem?.requestId, activeItem?.postId, "Автошампунь", 100.0, now, SyncState.PENDING_CREATE.name, now))
        audit(organizationId, "WASH_CHEMICAL_USAGE", id, "CREATE", "Автошампунь · 100 мл", now)
    }

    private suspend fun audit(organizationId: String, entityType: String, entityId: String, action: String, summary: String, occurredAt: Long) {
        serviceDao.insertAuditEvent(AuditEventEntity(UUID.randomUUID().toString(), organizationId, "user-local-admin", entityType, entityId, action, summary, occurredAt))
    }

    companion object {
        fun create(context: Context): WashRepository = WashRepository(LexoraServiceDatabase.create(context.applicationContext))
    }
}

private fun WashPostEntity.toModel() = WashPost(id, organizationId, branchId, name, WashPostStatus.valueOf(status), active, SyncState.valueOf(syncState))
private fun WashQueueItemEntity.toModel() = WashQueueItem(id, organizationId, requestId, vehicleId, postId, position, WashQueueStatus.valueOf(status), createdAtEpochMs, SyncState.valueOf(syncState))
private fun WashTechCardEntity.toModel() = WashTechCard(id, organizationId, name, durationMinutes, stepsText, active, SyncState.valueOf(syncState))
private fun WashChemicalUsageEntity.toModel() = WashChemicalUsage(id, organizationId, requestId, postId, chemicalName, quantityMl, usedAtEpochMs, SyncState.valueOf(syncState))
