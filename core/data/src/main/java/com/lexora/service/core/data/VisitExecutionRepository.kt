package com.lexora.service.core.data

import com.lexora.service.core.database.AuditEventEntity
import com.lexora.service.core.database.ServiceDao
import com.lexora.service.core.database.VisitMaterialUsageEntity
import com.lexora.service.core.database.VisitWorkEntryEntity
import com.lexora.service.core.model.ServiceVisit
import com.lexora.service.core.model.SyncState
import com.lexora.service.core.model.VisitMaterialUsage
import com.lexora.service.core.model.VisitStatus
import com.lexora.service.core.model.VisitWorkEntry
import java.util.UUID

data class VisitExecutionReport(
    val visit: ServiceVisit,
    val works: List<VisitWorkEntry>,
    val materials: List<VisitMaterialUsage>,
) {
    val hasTechnicalConclusion: Boolean get() = !visit.resultNote.isNullOrBlank()
    val hasClientSignature: Boolean get() = !visit.customerName.isNullOrBlank() && !visit.customerSignatureRef.isNullOrBlank()
    val canComplete: Boolean get() = works.isNotEmpty() && hasTechnicalConclusion && hasClientSignature

    fun completionProblems(): List<String> = buildList {
        if (works.isEmpty()) add("Добавьте хотя бы одну выполненную работу")
        if (!hasTechnicalConclusion) add("Заполните техническое заключение исполнителя")
        if (!hasClientSignature) add("Получите и подтвердите подпись клиента")
    }
}

class VisitExecutionRepository(private val dao: ServiceDao) {
    suspend fun report(visitId: String): VisitExecutionReport {
        val visit = dao.serviceVisit(visitId) ?: error("Выезд не найден")
        return VisitExecutionReport(
            visit = visit.toModel(),
            works = dao.visitWorkEntries(visitId).map { it.toModel() },
            materials = dao.visitMaterialUsage(visitId).map { it.toModel() },
        )
    }

    suspend fun saveTechnicalConclusion(organizationId: String, userId: String, visitId: String, conclusion: String) {
        val normalized = conclusion.trim()
        require(normalized.isNotBlank()) { "Техническое заключение не может быть пустым" }
        val current = dao.serviceVisit(visitId) ?: error("Выезд не найден")
        require(current.organizationId == organizationId) { "Выезд относится к другой организации" }
        val now = System.currentTimeMillis()
        dao.upsertServiceVisit(current.copy(resultNote = normalized, syncState = SyncState.PENDING_UPDATE.name, updatedAtEpochMs = now))
        audit(organizationId, userId, "SERVICE_VISIT", visitId, "TECHNICAL_CONCLUSION", normalized.take(160))
    }

    suspend fun saveClientSignature(organizationId: String, userId: String, visitId: String, customerName: String, signatureRef: String) {
        val signer = customerName.trim()
        val signature = signatureRef.trim()
        require(signer.isNotBlank()) { "Укажите ФИО клиента или представителя" }
        require(signature.startsWith("points:v1:")) { "Подпись клиента не сформирована" }
        require(signature.length > "points:v1:".length + 8) { "Подпись клиента слишком короткая" }
        val current = dao.serviceVisit(visitId) ?: error("Выезд не найден")
        require(current.organizationId == organizationId) { "Выезд относится к другой организации" }
        val now = System.currentTimeMillis()
        dao.upsertServiceVisit(current.copy(customerName = signer, customerSignatureRef = signature, syncState = SyncState.PENDING_UPDATE.name, updatedAtEpochMs = now))
        audit(organizationId, userId, "SERVICE_VISIT", visitId, "CLIENT_SIGNATURE", "Подпись подтверждена: $signer")
    }

    suspend fun saveWork(organizationId: String, userId: String, visitId: String, title: String, quantity: Double, unit: String?, note: String?, existingId: String? = null) {
        requireVisit(organizationId, visitId)
        require(title.isNotBlank()) { "Укажите выполненную работу" }
        require(quantity > 0.0) { "Количество должно быть больше нуля" }
        val now = System.currentTimeMillis()
        val id = existingId ?: UUID.randomUUID().toString()
        dao.upsertVisitWorkEntry(
            VisitWorkEntryEntity(id, visitId, null, title.trim(), quantity, unit?.trim()?.ifBlank { null }, note?.trim()?.ifBlank { null }, if (existingId == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name, now),
        )
        audit(organizationId, userId, "VISIT_WORK", id, if (existingId == null) "CREATE" else "UPDATE", title.trim())
    }

    suspend fun saveMaterial(organizationId: String, userId: String, visitId: String, materialCode: String?, title: String, quantity: Double, unit: String?, note: String?, existingId: String? = null) {
        requireVisit(organizationId, visitId)
        require(title.isNotBlank()) { "Укажите использованный материал" }
        require(quantity > 0.0) { "Количество должно быть больше нуля" }
        val now = System.currentTimeMillis()
        val id = existingId ?: UUID.randomUUID().toString()
        dao.upsertVisitMaterialUsage(
            VisitMaterialUsageEntity(id, visitId, materialCode?.trim()?.ifBlank { null }, title.trim(), quantity, unit?.trim()?.ifBlank { null }, note?.trim()?.ifBlank { null }, if (existingId == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name, now),
        )
        audit(organizationId, userId, "VISIT_MATERIAL", id, if (existingId == null) "CREATE" else "UPDATE", "$title × $quantity")
    }

    suspend fun assertCanComplete(organizationId: String, visitId: String) {
        val report = report(visitId)
        require(report.visit.organizationId == organizationId) { "Выезд относится к другой организации" }
        val problems = report.completionProblems()
        require(problems.isEmpty()) { problems.joinToString(". ") }
    }

    private suspend fun requireVisit(organizationId: String, visitId: String) {
        val visit = dao.serviceVisit(visitId) ?: error("Выезд не найден")
        require(visit.organizationId == organizationId) { "Выезд относится к другой организации" }
        require(visit.status != VisitStatus.COMPLETED.name && visit.status != VisitStatus.CANCELLED.name) { "Завершенный или отмененный выезд нельзя редактировать" }
    }

    private suspend fun audit(organizationId: String, userId: String, entityType: String, entityId: String, action: String, summary: String) {
        dao.insertAuditEvent(AuditEventEntity(UUID.randomUUID().toString(), organizationId, userId, entityType, entityId, action, summary, System.currentTimeMillis()))
    }
}

private fun com.lexora.service.core.database.ServiceVisitEntity.toModel() = ServiceVisit(id, organizationId, requestId, branchId, employeeId, VisitStatus.valueOf(status), plannedStartEpochMs, plannedEndEpochMs, actualStartEpochMs, actualEndEpochMs, resultNote, customerName, customerSignatureRef, SyncState.valueOf(syncState))
private fun VisitWorkEntryEntity.toModel() = VisitWorkEntry(id, visitId, serviceCode, title, quantity, unit, note, SyncState.valueOf(syncState))
private fun VisitMaterialUsageEntity.toModel() = VisitMaterialUsage(id, visitId, materialCode, title, quantity, unit, note, SyncState.valueOf(syncState))
