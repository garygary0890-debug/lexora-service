package com.lexora.service.core.data

import android.content.Context
import com.lexora.service.core.database.AuditEventEntity
import com.lexora.service.core.database.CustomerCareDao
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.database.LoyaltyAccountEntity
import com.lexora.service.core.database.LoyaltyTransactionEntity
import com.lexora.service.core.database.QualityControlRecordEntity
import com.lexora.service.core.database.ServiceDao
import com.lexora.service.core.model.LoyaltyAccount
import com.lexora.service.core.model.LoyaltyTransaction
import com.lexora.service.core.model.LoyaltyTransactionType
import com.lexora.service.core.model.QualityControlRecord
import com.lexora.service.core.model.QualityControlStatus
import com.lexora.service.core.model.SyncState
import java.util.UUID

class CustomerCareRepository(
    private val dao: CustomerCareDao,
    private val serviceDao: ServiceDao,
) {
    suspend fun accounts(organizationId: String): List<LoyaltyAccount> =
        dao.loyaltyAccounts(organizationId).map { it.toModel() }

    suspend fun transactions(organizationId: String): List<LoyaltyTransaction> =
        dao.loyaltyTransactions(organizationId).map { it.toModel() }

    suspend fun qualityRecords(organizationId: String): List<QualityControlRecord> =
        dao.qualityRecords(organizationId).map { it.toModel() }

    suspend fun ensureAccount(organizationId: String, clientId: String): LoyaltyAccount {
        dao.loyaltyAccount(organizationId, clientId)?.let { return it.toModel() }
        val now = System.currentTimeMillis()
        val entity = LoyaltyAccountEntity(
            id = UUID.randomUUID().toString(),
            organizationId = organizationId,
            clientId = clientId,
            pointsBalance = 0,
            active = true,
            syncState = SyncState.PENDING_CREATE.name,
            updatedAtEpochMs = now,
        )
        dao.upsertLoyaltyAccount(entity)
        audit(organizationId, "LOYALTY_ACCOUNT", entity.id, "CREATE", "Создан бонусный счёт клиента")
        return entity.toModel()
    }

    suspend fun accrue(
        organizationId: String,
        clientId: String,
        points: Long,
        requestId: String? = null,
        paymentId: String? = null,
        comment: String? = null,
    ): Boolean {
        require(points > 0) { "Начисление должно быть больше нуля" }
        return applyPoints(organizationId, clientId, points, LoyaltyTransactionType.ACCRUAL, requestId, paymentId, comment)
    }

    suspend fun redeem(
        organizationId: String,
        clientId: String,
        points: Long,
        requestId: String? = null,
        paymentId: String? = null,
        comment: String? = null,
    ): Boolean {
        require(points > 0) { "Списание должно быть больше нуля" }
        return applyPoints(organizationId, clientId, -points, LoyaltyTransactionType.REDEMPTION, requestId, paymentId, comment)
    }

    private suspend fun applyPoints(
        organizationId: String,
        clientId: String,
        delta: Long,
        type: LoyaltyTransactionType,
        requestId: String?,
        paymentId: String?,
        comment: String?,
    ): Boolean {
        val account = ensureAccount(organizationId, clientId)
        val now = System.currentTimeMillis()
        val transaction = LoyaltyTransactionEntity(
            id = UUID.randomUUID().toString(),
            organizationId = organizationId,
            clientId = clientId,
            accountId = account.id,
            requestId = requestId,
            paymentId = paymentId,
            type = type.name,
            pointsDelta = delta,
            comment = comment,
            occurredAtEpochMs = now,
            syncState = SyncState.PENDING_CREATE.name,
            updatedAtEpochMs = now,
        )
        val applied = dao.applyLoyaltyTransaction(account.id, delta, transaction)
        if (applied) {
            audit(
                organizationId,
                "LOYALTY_TRANSACTION",
                transaction.id,
                type.name,
                if (delta > 0) "Начислено $delta бонусов" else "Списано ${-delta} бонусов",
            )
        }
        return applied
    }

    suspend fun createQualityCheck(organizationId: String, requestId: String): QualityControlRecord {
        dao.qualityRecordForRequest(requestId)?.let { return it.toModel() }
        val request = serviceDao.serviceRequest(requestId) ?: error("Заявка не найдена")
        require(request.organizationId == organizationId) { "Заявка относится к другой организации" }
        val now = System.currentTimeMillis()
        val entity = QualityControlRecordEntity(
            id = UUID.randomUUID().toString(),
            organizationId = organizationId,
            requestId = request.id,
            clientId = request.clientId,
            vehicleId = request.vehicleId,
            status = QualityControlStatus.PENDING.name,
            rating = null,
            checklistResult = null,
            issueDescription = null,
            resolutionNote = null,
            controlledAtEpochMs = null,
            syncState = SyncState.PENDING_CREATE.name,
            updatedAtEpochMs = now,
        )
        dao.upsertQualityRecord(entity)
        audit(organizationId, "QUALITY_CONTROL", entity.id, "CREATE", "Создан контроль качества по заявке ${request.number}")
        return entity.toModel()
    }

    suspend fun completeQualityCheck(
        record: QualityControlRecord,
        rating: Int,
        checklistResult: String,
        issueDescription: String? = null,
    ) {
        require(rating in 1..5) { "Оценка должна быть от 1 до 5" }
        val now = System.currentTimeMillis()
        val hasIssue = !issueDescription.isNullOrBlank()
        dao.upsertQualityRecord(
            QualityControlRecordEntity(
                id = record.id,
                organizationId = record.organizationId,
                requestId = record.requestId,
                clientId = record.clientId,
                vehicleId = record.vehicleId,
                status = if (hasIssue) QualityControlStatus.ISSUE_FOUND.name else QualityControlStatus.PASSED.name,
                rating = rating,
                checklistResult = checklistResult,
                issueDescription = issueDescription,
                resolutionNote = record.resolutionNote,
                controlledAtEpochMs = now,
                syncState = SyncState.PENDING_UPDATE.name,
                updatedAtEpochMs = now,
            ),
        )
        audit(record.organizationId, "QUALITY_CONTROL", record.id, "COMPLETE", if (hasIssue) "Выявлено замечание по качеству" else "Контроль качества пройден")
    }

    suspend fun resolveQualityIssue(record: QualityControlRecord, resolutionNote: String) {
        require(record.status == QualityControlStatus.ISSUE_FOUND) { "Нет открытого замечания" }
        require(resolutionNote.isNotBlank()) { "Необходимо указать результат устранения" }
        val now = System.currentTimeMillis()
        dao.upsertQualityRecord(
            QualityControlRecordEntity(
                id = record.id,
                organizationId = record.organizationId,
                requestId = record.requestId,
                clientId = record.clientId,
                vehicleId = record.vehicleId,
                status = QualityControlStatus.RESOLVED.name,
                rating = record.rating,
                checklistResult = record.checklistResult,
                issueDescription = record.issueDescription,
                resolutionNote = resolutionNote,
                controlledAtEpochMs = record.controlledAtEpochMs ?: now,
                syncState = SyncState.PENDING_UPDATE.name,
                updatedAtEpochMs = now,
            ),
        )
        audit(record.organizationId, "QUALITY_CONTROL", record.id, "RESOLVE", "Замечание по качеству устранено")
    }

    private suspend fun audit(organizationId: String, entityType: String, entityId: String, action: String, summary: String) {
        serviceDao.insertAuditEvent(
            AuditEventEntity(
                id = UUID.randomUUID().toString(),
                organizationId = organizationId,
                userId = "local-user",
                entityType = entityType,
                entityId = entityId,
                action = action,
                summary = summary,
                occurredAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    companion object {
        fun create(context: Context): CustomerCareRepository {
            val db = LexoraServiceDatabase.create(context)
            return CustomerCareRepository(db.customerCareDao(), db.serviceDao())
        }
    }
}

private fun LoyaltyAccountEntity.toModel() = LoyaltyAccount(
    id = id,
    organizationId = organizationId,
    clientId = clientId,
    pointsBalance = pointsBalance,
    active = active,
    syncState = SyncState.valueOf(syncState),
)

private fun LoyaltyTransactionEntity.toModel() = LoyaltyTransaction(
    id = id,
    organizationId = organizationId,
    clientId = clientId,
    accountId = accountId,
    requestId = requestId,
    paymentId = paymentId,
    type = LoyaltyTransactionType.valueOf(type),
    pointsDelta = pointsDelta,
    comment = comment,
    occurredAtEpochMs = occurredAtEpochMs,
    syncState = SyncState.valueOf(syncState),
)

private fun QualityControlRecordEntity.toModel() = QualityControlRecord(
    id = id,
    organizationId = organizationId,
    requestId = requestId,
    clientId = clientId,
    vehicleId = vehicleId,
    status = QualityControlStatus.valueOf(status),
    rating = rating,
    checklistResult = checklistResult,
    issueDescription = issueDescription,
    resolutionNote = resolutionNote,
    controlledAtEpochMs = controlledAtEpochMs,
    syncState = SyncState.valueOf(syncState),
)
