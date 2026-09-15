package com.lexora.service.core.data

import android.content.Context
import com.lexora.service.core.database.AuditEventEntity
import com.lexora.service.core.database.ContractDao
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.database.ServiceContractEntity
import com.lexora.service.core.database.ServiceDao
import com.lexora.service.core.model.ContractStatus
import com.lexora.service.core.model.ServiceContract
import com.lexora.service.core.model.SyncState
import java.util.UUID

class ContractRepository(
    private val contractDao: ContractDao,
    private val serviceDao: ServiceDao,
) {
    suspend fun contracts(organizationId: String): List<ServiceContract> =
        contractDao.contracts(organizationId).map(ServiceContractEntity::toModel)

    suspend fun archivedContracts(organizationId: String): List<ServiceContract> =
        contractDao.archivedContracts(organizationId).map(ServiceContractEntity::toModel)

    suspend fun create(
        organizationId: String,
        userId: String,
        clientId: String,
        branchId: String?,
        subject: String,
        startAtEpochMs: Long? = null,
        endAtEpochMs: Long? = null,
        note: String? = null,
    ): ServiceContract {
        require(subject.isNotBlank()) { "Предмет договора обязателен" }
        require(endAtEpochMs == null || startAtEpochMs == null || endAtEpochMs >= startAtEpochMs) {
            "Дата окончания договора не может быть раньше даты начала"
        }
        val now = System.currentTimeMillis()
        val number = nextNumber(organizationId)
        val entity = ServiceContractEntity(
            id = UUID.randomUUID().toString(),
            organizationId = organizationId,
            clientId = clientId,
            branchId = branchId,
            number = number,
            subject = subject.trim(),
            status = ContractStatus.DRAFT.name,
            startAtEpochMs = startAtEpochMs,
            endAtEpochMs = endAtEpochMs,
            signedAtEpochMs = null,
            externalFileRef = null,
            note = note?.trim()?.ifBlank { null },
            archived = false,
            syncState = SyncState.PENDING_CREATE.name,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
        )
        contractDao.upsertContract(entity)
        audit(organizationId, userId, "CONTRACT", entity.id, "CREATE", "Создан договор $number")
        return entity.toModel()
    }

    suspend fun changeStatus(
        organizationId: String,
        userId: String,
        contractId: String,
        target: ContractStatus,
    ) {
        val current = contractDao.contract(contractId) ?: error("Договор не найден")
        require(current.organizationId == organizationId) { "Договор относится к другой организации" }
        val currentStatus = ContractStatus.valueOf(current.status)
        require(isAllowed(currentStatus, target)) { "Недопустимый переход статуса: $currentStatus → $target" }
        val now = System.currentTimeMillis()
        val signedAt = when {
            target == ContractStatus.ACTIVE && current.signedAtEpochMs == null -> now
            else -> current.signedAtEpochMs
        }
        contractDao.updateStatus(contractId, target.name, signedAt, SyncState.PENDING_UPDATE.name, now)
        audit(organizationId, userId, "CONTRACT", contractId, "STATUS_CHANGE", "$currentStatus → $target")
    }

    suspend fun archive(organizationId: String, userId: String, contractId: String) {
        val current = contractDao.contract(contractId) ?: error("Договор не найден")
        require(current.organizationId == organizationId) { "Договор относится к другой организации" }
        contractDao.archive(contractId, SyncState.PENDING_UPDATE.name, System.currentTimeMillis())
        audit(organizationId, userId, "CONTRACT", contractId, "ARCHIVE", current.number)
    }

    suspend fun restore(organizationId: String, userId: String, contractId: String) {
        val current = contractDao.contract(contractId) ?: error("Договор не найден")
        require(current.organizationId == organizationId) { "Договор относится к другой организации" }
        contractDao.restore(contractId, SyncState.PENDING_UPDATE.name, System.currentTimeMillis())
        audit(organizationId, userId, "CONTRACT", contractId, "RESTORE", current.number)
    }

    private suspend fun nextNumber(organizationId: String): String {
        val max = contractDao.allContracts(organizationId)
            .mapNotNull { it.number.removePrefix("CTR-").toIntOrNull() }
            .maxOrNull() ?: 0
        return "CTR-%06d".format(max + 1)
    }

    private fun isAllowed(from: ContractStatus, to: ContractStatus): Boolean = when (from) {
        ContractStatus.DRAFT -> to == ContractStatus.ACTIVE || to == ContractStatus.TERMINATED
        ContractStatus.ACTIVE -> to == ContractStatus.SUSPENDED || to == ContractStatus.TERMINATED || to == ContractStatus.EXPIRED
        ContractStatus.SUSPENDED -> to == ContractStatus.ACTIVE || to == ContractStatus.TERMINATED || to == ContractStatus.EXPIRED
        ContractStatus.TERMINATED, ContractStatus.EXPIRED -> false
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

    companion object {
        fun create(context: Context): ContractRepository {
            val database = LexoraServiceDatabase.create(context)
            return ContractRepository(database.contractDao(), database.serviceDao())
        }
    }
}

private fun ServiceContractEntity.toModel() = ServiceContract(
    id = id,
    organizationId = organizationId,
    clientId = clientId,
    branchId = branchId,
    number = number,
    subject = subject,
    status = ContractStatus.valueOf(status),
    startAtEpochMs = startAtEpochMs,
    endAtEpochMs = endAtEpochMs,
    signedAtEpochMs = signedAtEpochMs,
    externalFileRef = externalFileRef,
    note = note,
    archived = archived,
    syncState = SyncState.valueOf(syncState),
)
