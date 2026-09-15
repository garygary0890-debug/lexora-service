package com.lexora.service.core.data

import android.content.Context
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
) {
    private val washDao = database.washDao()
    private val serviceDao = database.serviceDao()

    suspend fun posts(organizationId: String): List<WashPost> =
        washDao.posts(organizationId).map { it.toModel() }

    suspend fun queue(organizationId: String): List<WashQueueItem> =
        washDao.activeQueue(organizationId).map { it.toModel() }

    suspend fun techCards(organizationId: String): List<WashTechCard> =
        washDao.techCards(organizationId).map { it.toModel() }

    suspend fun chemicalUsage(organizationId: String): List<WashChemicalUsage> =
        washDao.recentChemicalUsage(organizationId).map { it.toModel() }

    suspend fun addPost(organizationId: String, branchId: String?, name: String) {
        val now = System.currentTimeMillis()
        washDao.upsertPost(
            WashPostEntity(
                id = UUID.randomUUID().toString(),
                organizationId = organizationId,
                branchId = branchId,
                name = name,
                status = WashPostStatus.AVAILABLE.name,
                active = true,
                syncState = SyncState.PENDING_CREATE.name,
                updatedAtEpochMs = now,
            )
        )
    }

    suspend fun togglePostStatus(post: WashPost) {
        val next = if (post.status == WashPostStatus.AVAILABLE) WashPostStatus.OCCUPIED else WashPostStatus.AVAILABLE
        washDao.updatePostStatus(post.id, next.name, SyncState.PENDING_UPDATE.name, System.currentTimeMillis())
    }

    suspend fun addQueueItem(organizationId: String) {
        val now = System.currentTimeMillis()
        val requests = serviceDao.serviceRequests(organizationId)
            .filter { it.status != "CLOSED" && it.status != "CANCELLED" }
        val request = requests.firstOrNull()
        val position = (washDao.activeQueue(organizationId).maxOfOrNull { it.position } ?: 0) + 1
        washDao.upsertQueueItem(
            WashQueueItemEntity(
                id = UUID.randomUUID().toString(),
                organizationId = organizationId,
                requestId = request?.id,
                vehicleId = request?.vehicleId,
                postId = null,
                position = position,
                status = WashQueueStatus.WAITING.name,
                createdAtEpochMs = now,
                syncState = SyncState.PENDING_CREATE.name,
                updatedAtEpochMs = now,
            )
        )
    }

    suspend fun advanceQueueItem(organizationId: String, item: WashQueueItem) {
        val posts = washDao.posts(organizationId).map { it.toModel() }
        val now = System.currentTimeMillis()
        when (item.status) {
            WashQueueStatus.WAITING -> {
                val post = posts.firstOrNull { it.status == WashPostStatus.AVAILABLE }
                washDao.assignQueueItem(item.id, post?.id, WashQueueStatus.CALLED.name, SyncState.PENDING_UPDATE.name, now)
            }
            WashQueueStatus.CALLED -> {
                if (item.postId != null) washDao.updatePostStatus(item.postId, WashPostStatus.OCCUPIED.name, SyncState.PENDING_UPDATE.name, now)
                washDao.assignQueueItem(item.id, item.postId, WashQueueStatus.IN_SERVICE.name, SyncState.PENDING_UPDATE.name, now)
            }
            WashQueueStatus.IN_SERVICE -> {
                if (item.postId != null) washDao.updatePostStatus(item.postId, WashPostStatus.AVAILABLE.name, SyncState.PENDING_UPDATE.name, now)
                washDao.assignQueueItem(item.id, item.postId, WashQueueStatus.COMPLETED.name, SyncState.PENDING_UPDATE.name, now)
            }
            WashQueueStatus.COMPLETED, WashQueueStatus.CANCELLED -> Unit
        }
    }

    suspend fun addTechCard(organizationId: String) {
        val now = System.currentTimeMillis()
        val nextNumber = washDao.techCards(organizationId).size + 1
        washDao.upsertTechCard(
            WashTechCardEntity(
                id = UUID.randomUUID().toString(),
                organizationId = organizationId,
                name = "Техкарта $nextNumber",
                durationMinutes = 30,
                stepsText = "Предварительная мойка → основная мойка → ополаскивание → сушка → контроль качества",
                active = true,
                syncState = SyncState.PENDING_CREATE.name,
                updatedAtEpochMs = now,
            )
        )
    }

    suspend fun addChemicalUsage(organizationId: String) {
        val now = System.currentTimeMillis()
        val activeItem = washDao.activeQueue(organizationId).firstOrNull { it.status == WashQueueStatus.IN_SERVICE.name }
        washDao.insertChemicalUsage(
            WashChemicalUsageEntity(
                id = UUID.randomUUID().toString(),
                organizationId = organizationId,
                requestId = activeItem?.requestId,
                postId = activeItem?.postId,
                chemicalName = "Автошампунь",
                quantityMl = 100.0,
                usedAtEpochMs = now,
                syncState = SyncState.PENDING_CREATE.name,
                updatedAtEpochMs = now,
            )
        )
    }

    companion object {
        fun create(context: Context): WashRepository =
            WashRepository(LexoraServiceDatabase.create(context.applicationContext))
    }
}

private fun WashPostEntity.toModel() = WashPost(id, organizationId, branchId, name, WashPostStatus.valueOf(status), active, SyncState.valueOf(syncState))
private fun WashQueueItemEntity.toModel() = WashQueueItem(id, organizationId, requestId, vehicleId, postId, position, WashQueueStatus.valueOf(status), createdAtEpochMs, SyncState.valueOf(syncState))
private fun WashTechCardEntity.toModel() = WashTechCard(id, organizationId, name, durationMinutes, stepsText, active, SyncState.valueOf(syncState))
private fun WashChemicalUsageEntity.toModel() = WashChemicalUsage(id, organizationId, requestId, postId, chemicalName, quantityMl, usedAtEpochMs, SyncState.valueOf(syncState))
