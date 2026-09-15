package com.lexora.service.core.data

import android.content.Context
import com.lexora.service.core.database.AuditEventEntity
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.database.WorkOrderItemEntity
import com.lexora.service.core.model.AdditionalWorkApprovalStatus
import com.lexora.service.core.model.ServiceDocumentType
import com.lexora.service.core.model.SyncState
import com.lexora.service.core.model.WorkOrderItem
import java.util.UUID
import kotlin.math.roundToLong

class WorkOrderRepository private constructor(
    private val database: LexoraServiceDatabase,
) {
    private val serviceDao = database.serviceDao()
    private val catalogDao = database.catalogDao()
    private val workOrderDao = database.workOrderDao()

    suspend fun items(documentId: String): List<WorkOrderItem> =
        workOrderDao.items(documentId).map { it.toModel() }

    suspend fun addCatalogItem(
        organizationId: String,
        userId: String,
        documentId: String,
        additional: Boolean,
    ) {
        val document = serviceDao.serviceDocument(documentId) ?: return
        if (document.type != ServiceDocumentType.WORK_ORDER.name || document.status != "DRAFT") return

        val service = catalogDao.services(organizationId).firstOrNull { it.active } ?: return
        val now = System.currentTimeMillis()
        val priceList = catalogDao.priceLists(organizationId).firstOrNull {
            val effectiveTo = it.effectiveToEpochMs
            it.active && it.effectiveFromEpochMs <= now && (effectiveTo == null || effectiveTo >= now)
        }
        val price = priceList?.let { list ->
            catalogDao.priceListItems(list.id).firstOrNull { it.serviceCatalogItemId == service.id }?.priceMinor
        } ?: 0L

        val quantity = 1.0
        val item = WorkOrderItemEntity(
            id = UUID.randomUUID().toString(),
            documentId = documentId,
            serviceCatalogItemId = service.id,
            title = service.name,
            quantity = quantity,
            unit = service.unit,
            unitPriceMinor = price,
            totalMinor = (price.toDouble() * quantity).roundToLong(),
            additional = additional,
            approvalStatus = if (additional) AdditionalWorkApprovalStatus.PENDING.name else AdditionalWorkApprovalStatus.NOT_REQUIRED.name,
            approvalComment = null,
            approvedAtEpochMs = null,
            syncState = SyncState.PENDING_CREATE.name,
            updatedAtEpochMs = now,
        )
        workOrderDao.upsert(item)
        recalculateDocumentTotal(documentId)
        serviceDao.insertAuditEvent(
            AuditEventEntity(
                id = UUID.randomUUID().toString(),
                organizationId = organizationId,
                userId = userId,
                entityType = "WORK_ORDER_ITEM",
                entityId = item.id,
                action = if (additional) "ADD_ADDITIONAL_WORK" else "ADD_WORK",
                summary = service.name,
                occurredAtEpochMs = now,
            )
        )
    }

    suspend fun resolveAdditionalWork(
        organizationId: String,
        userId: String,
        itemId: String,
        approve: Boolean,
    ) {
        val item = workOrderDao.item(itemId) ?: return
        if (!item.additional || item.approvalStatus != AdditionalWorkApprovalStatus.PENDING.name) return
        val now = System.currentTimeMillis()
        val status = if (approve) AdditionalWorkApprovalStatus.APPROVED else AdditionalWorkApprovalStatus.REJECTED
        workOrderDao.updateApproval(
            id = itemId,
            status = status.name,
            comment = if (approve) "Согласовано клиентом" else "Отклонено клиентом",
            approvedAt = now,
            syncState = SyncState.PENDING_UPDATE.name,
            updatedAt = now,
        )
        recalculateDocumentTotal(item.documentId)
        serviceDao.insertAuditEvent(
            AuditEventEntity(
                id = UUID.randomUUID().toString(),
                organizationId = organizationId,
                userId = userId,
                entityType = "WORK_ORDER_ITEM",
                entityId = itemId,
                action = if (approve) "APPROVE_ADDITIONAL_WORK" else "REJECT_ADDITIONAL_WORK",
                summary = item.title,
                occurredAtEpochMs = now,
            )
        )
    }

    private suspend fun recalculateDocumentTotal(documentId: String) {
        val document = serviceDao.serviceDocument(documentId) ?: return
        val total = workOrderDao.approvedTotalMinor(documentId)
        serviceDao.upsertServiceDocument(
            document.copy(
                totalMinor = total,
                syncState = SyncState.PENDING_UPDATE.name,
                updatedAtEpochMs = System.currentTimeMillis(),
            )
        )
    }

    companion object {
        fun create(context: Context): WorkOrderRepository =
            WorkOrderRepository(LexoraServiceDatabase.create(context.applicationContext))
    }
}

private fun WorkOrderItemEntity.toModel() = WorkOrderItem(
    id = id,
    documentId = documentId,
    serviceCatalogItemId = serviceCatalogItemId,
    title = title,
    quantity = quantity,
    unit = unit,
    unitPriceMinor = unitPriceMinor,
    totalMinor = totalMinor,
    additional = additional,
    approvalStatus = AdditionalWorkApprovalStatus.valueOf(approvalStatus),
    approvalComment = approvalComment,
    approvedAtEpochMs = approvedAtEpochMs,
    syncState = SyncState.valueOf(syncState),
)
