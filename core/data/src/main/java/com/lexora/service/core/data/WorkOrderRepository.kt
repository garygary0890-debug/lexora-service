package com.lexora.service.core.data

import android.content.Context
import com.lexora.service.core.database.AdditionalWorkApprovalEventEntity
import com.lexora.service.core.database.AuditEventEntity
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.database.WorkOrderCommercialDao
import com.lexora.service.core.database.WorkOrderCommercialDatabase
import com.lexora.service.core.database.WorkOrderItemEntity
import com.lexora.service.core.model.AdditionalWorkApprovalDecision
import com.lexora.service.core.model.AdditionalWorkApprovalEvent
import com.lexora.service.core.model.AdditionalWorkApprovalStatus
import com.lexora.service.core.model.ServiceCatalogItem
import com.lexora.service.core.model.ServiceDocumentStatus
import com.lexora.service.core.model.ServiceDocumentType
import com.lexora.service.core.model.SyncState
import com.lexora.service.core.model.SyncOperationType
import com.lexora.service.core.model.WorkOrderItem
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.roundToLong
import org.json.JSONObject

class WorkOrderRepository private constructor(
    private val database: LexoraServiceDatabase,
    private val commercialDao: WorkOrderCommercialDao,
) {
    private val serviceDao = database.serviceDao()
    private val catalogDao = database.catalogDao()
    private val workOrderDao = database.workOrderDao()
    private val syncQueue = SyncQueueRepository(serviceDao)

    suspend fun items(documentId: String): List<WorkOrderItem> =
        workOrderDao.items(documentId).map { it.toModel() }

    suspend fun approvalEvents(itemId: String): List<AdditionalWorkApprovalEvent> =
        commercialDao.approvalEvents(itemId).map { it.toModel() }

    suspend fun availableServices(organizationId: String): List<ServiceCatalogItem> =
        catalogDao.services(organizationId)
            .asSequence()
            .filter { it.active }
            .map {
                ServiceCatalogItem(
                    id = it.id,
                    organizationId = it.organizationId,
                    code = it.code,
                    name = it.name,
                    category = it.category,
                    unit = it.unit,
                    durationMinutes = it.durationMinutes,
                    active = it.active,
                    syncState = SyncState.valueOf(it.syncState),
                )
            }
            .toList()

    suspend fun addCatalogItem(
        organizationId: String,
        userId: String,
        documentId: String,
        additional: Boolean,
    ) {
        val serviceId = catalogDao.services(organizationId).firstOrNull { it.active }?.id ?: return
        addCatalogItem(
            organizationId = organizationId,
            userId = userId,
            documentId = documentId,
            serviceCatalogItemId = serviceId,
            additional = additional,
        )
    }

    suspend fun addCatalogItem(
        organizationId: String,
        userId: String,
        documentId: String,
        serviceCatalogItemId: String,
        additional: Boolean,
    ) {
        val document = requireEditableWorkOrder(organizationId, documentId)
        val service = catalogDao.services(organizationId).firstOrNull {
            it.id == serviceCatalogItemId && it.active
        } ?: return
        val now = System.currentTimeMillis()
        val priceList = catalogDao.priceLists(organizationId).firstOrNull {
            val effectiveTo = it.effectiveToEpochMs
            it.active && it.effectiveFromEpochMs <= now && (effectiveTo == null || effectiveTo >= now)
        }
        val price = priceList?.let { list ->
            catalogDao.priceListItems(list.id).firstOrNull { it.serviceCatalogItemId == service.id }?.priceMinor
        } ?: 0L

        addItem(
            organizationId = organizationId,
            userId = userId,
            documentId = document.id,
            serviceCatalogItemId = service.id,
            title = service.name,
            quantity = 1.0,
            unit = service.unit,
            unitPriceMinor = price,
            additional = additional,
        )
    }

    suspend fun addCustomItem(
        organizationId: String,
        userId: String,
        documentId: String,
        title: String,
        quantity: Double,
        unit: String,
        unitPriceMinor: Long,
        additional: Boolean = false,
    ): WorkOrderItem {
        require(title.isNotBlank()) { "Наименование работы обязательно" }
        require(quantity > 0) { "Количество должно быть больше нуля" }
        require(unit.isNotBlank()) { "Единица измерения обязательна" }
        require(unitPriceMinor >= 0) { "Цена не может быть отрицательной" }
        requireEditableWorkOrder(organizationId, documentId)
        return addItem(
            organizationId = organizationId,
            userId = userId,
            documentId = documentId,
            serviceCatalogItemId = null,
            title = title.trim(),
            quantity = quantity,
            unit = unit.trim(),
            unitPriceMinor = unitPriceMinor,
            additional = additional,
        )
    }

    suspend fun updateItem(
        organizationId: String,
        userId: String,
        itemId: String,
        quantity: Double,
        unitPriceMinor: Long,
    ) {
        require(quantity > 0) { "Количество должно быть больше нуля" }
        require(unitPriceMinor >= 0) { "Цена не может быть отрицательной" }
        val item = workOrderDao.item(itemId) ?: return
        requireEditableWorkOrder(organizationId, item.documentId)
        val now = System.currentTimeMillis()
        val updated = item.copy(
            quantity = quantity,
            unitPriceMinor = unitPriceMinor,
            totalMinor = (quantity * unitPriceMinor.toDouble()).roundToLong(),
            approvalStatus = if (item.additional) AdditionalWorkApprovalStatus.PENDING.name else item.approvalStatus,
            approvalComment = if (item.additional) null else item.approvalComment,
            approvedAtEpochMs = if (item.additional) null else item.approvedAtEpochMs,
            syncState = SyncState.PENDING_UPDATE.name,
            updatedAtEpochMs = now,
        )
        workOrderDao.upsert(updated)
        recalculateDocumentTotal(item.documentId)
        audit(organizationId, userId, "WORK_ORDER_ITEM", itemId, "UPDATE", updated.title)
    }

    suspend fun removeItem(
        organizationId: String,
        userId: String,
        itemId: String,
    ) {
        val item = workOrderDao.item(itemId) ?: return
        requireEditableWorkOrder(organizationId, item.documentId)
        workOrderDao.delete(itemId)
        recalculateDocumentTotal(item.documentId)
        audit(organizationId, userId, "WORK_ORDER_ITEM", itemId, "DELETE", item.title)
    }

    suspend fun resolveAdditionalWork(
        organizationId: String,
        userId: String,
        itemId: String,
        approve: Boolean,
        comment: String? = null,
        channel: String = "IN_APP",
    ) {
        val item = workOrderDao.item(itemId) ?: return
        requireEditableWorkOrder(organizationId, item.documentId)
        if (!item.additional || item.approvalStatus != AdditionalWorkApprovalStatus.PENDING.name) return
        val now = System.currentTimeMillis()
        val status = if (approve) AdditionalWorkApprovalStatus.APPROVED else AdditionalWorkApprovalStatus.REJECTED
        val decision = if (approve) AdditionalWorkApprovalDecision.APPROVED else AdditionalWorkApprovalDecision.REJECTED
        val approvalComment = comment?.trim()?.takeIf { it.isNotBlank() }
            ?: if (approve) "Согласовано клиентом" else "Отклонено клиентом"
        workOrderDao.updateApproval(
            id = itemId,
            status = status.name,
            comment = approvalComment,
            approvedAt = now,
            syncState = SyncState.PENDING_UPDATE.name,
            updatedAt = now,
        )
        val payloadHash = sha256(
            listOf(item.id, item.documentId, item.title, item.quantity, item.unit, item.unitPriceMinor, approvalComment)
                .joinToString("|")
        )
        commercialDao.insertApprovalEvent(
            AdditionalWorkApprovalEventEntity(
                id = UUID.randomUUID().toString(),
                organizationId = organizationId,
                workOrderItemId = itemId,
                actorUserId = userId,
                channel = channel,
                payloadHash = payloadHash,
                decision = decision.name,
                sentAtEpochMs = item.updatedAtEpochMs,
                decidedAtEpochMs = now,
            ),
        )
        recalculateDocumentTotal(item.documentId)
        audit(
            organizationId,
            userId,
            "WORK_ORDER_ITEM",
            itemId,
            if (approve) "APPROVE_ADDITIONAL_WORK" else "REJECT_ADDITIONAL_WORK",
            "${item.title} · $approvalComment · $payloadHash",
        )
    }

    private suspend fun addItem(
        organizationId: String,
        userId: String,
        documentId: String,
        serviceCatalogItemId: String?,
        title: String,
        quantity: Double,
        unit: String,
        unitPriceMinor: Long,
        additional: Boolean,
    ): WorkOrderItem {
        val now = System.currentTimeMillis()
        val item = WorkOrderItemEntity(
            id = UUID.randomUUID().toString(),
            documentId = documentId,
            serviceCatalogItemId = serviceCatalogItemId,
            title = title,
            quantity = quantity,
            unit = unit,
            unitPriceMinor = unitPriceMinor,
            totalMinor = (unitPriceMinor.toDouble() * quantity).roundToLong(),
            additional = additional,
            approvalStatus = if (additional) AdditionalWorkApprovalStatus.PENDING.name else AdditionalWorkApprovalStatus.NOT_REQUIRED.name,
            approvalComment = null,
            approvedAtEpochMs = null,
            syncState = SyncState.PENDING_CREATE.name,
            updatedAtEpochMs = now,
        )
        workOrderDao.upsert(item)
        recalculateDocumentTotal(documentId)
        audit(
            organizationId,
            userId,
            "WORK_ORDER_ITEM",
            item.id,
            if (additional) "ADD_ADDITIONAL_WORK" else "ADD_WORK",
            title,
        )
        return item.toModel()
    }

    private suspend fun requireEditableWorkOrder(organizationId: String, documentId: String) =
        requireNotNull(serviceDao.serviceDocument(documentId)) { "Заказ-наряд не найден" }.also { document ->
            require(document.organizationId == organizationId) { "Заказ-наряд относится к другой организации" }
            require(document.type == ServiceDocumentType.WORK_ORDER.name) { "Документ не является заказ-нарядом" }
            require(document.status == ServiceDocumentStatus.DRAFT.name) { "Изменять можно только черновик заказ-наряда" }
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
        document.clientId?.let { clientId ->
            val assetId = document.requestId?.let { serviceDao.serviceRequest(it)?.vehicleId }
            syncQueue.enqueue(document.organizationId, "ServiceWorkOrder", document.id, SyncOperationType.UPDATE, JSONObject().put("clientId", clientId).put("assetId", assetId ?: JSONObject.NULL).put("orderNumber", document.number).put("status", document.status).put("totalMinor", total).toString())
        }
    }

    private suspend fun audit(
        organizationId: String,
        userId: String,
        entityType: String,
        entityId: String,
        action: String,
        summary: String,
    ) {
        serviceDao.insertAuditEvent(
            AuditEventEntity(
                id = UUID.randomUUID().toString(),
                organizationId = organizationId,
                userId = userId,
                entityType = entityType,
                entityId = entityId,
                action = action,
                summary = summary,
                occurredAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        fun create(context: Context): WorkOrderRepository =
            WorkOrderRepository(
                LexoraServiceDatabase.create(context.applicationContext),
                WorkOrderCommercialDatabase.create(context.applicationContext).dao(),
            )
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

private fun AdditionalWorkApprovalEventEntity.toModel() = AdditionalWorkApprovalEvent(
    id = id,
    organizationId = organizationId,
    workOrderItemId = workOrderItemId,
    actorUserId = actorUserId,
    channel = channel,
    payloadHash = payloadHash,
    decision = AdditionalWorkApprovalDecision.valueOf(decision),
    sentAtEpochMs = sentAtEpochMs,
    decidedAtEpochMs = decidedAtEpochMs,
)
