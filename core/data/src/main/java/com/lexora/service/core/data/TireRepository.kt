package com.lexora.service.core.data

import android.content.Context
import com.lexora.service.core.database.*
import com.lexora.service.core.model.*
import java.util.UUID

class TireRepository private constructor(private val database: LexoraServiceDatabase) {
    private val tireDao = database.tireDao()
    private val serviceDao = database.serviceDao()

    suspend fun queue(organizationId: String) = tireDao.activeQueue(organizationId).map { it.toModel() }
    suspend fun diagnostics(organizationId: String) = tireDao.diagnostics(organizationId).map { it.toModel() }
    suspend fun workEntries(organizationId: String) = tireDao.workEntries(organizationId).map { it.toModel() }
    suspend fun storage(organizationId: String) = tireDao.storage(organizationId).map { it.toModel() }

    suspend fun addQueueItem(organizationId: String) {
        val now = System.currentTimeMillis()
        val request = serviceDao.serviceRequests(organizationId).firstOrNull { it.status != "CLOSED" && it.status != "CANCELLED" }
        val position = (tireDao.activeQueue(organizationId).maxOfOrNull { it.position } ?: 0) + 1
        tireDao.upsertQueueItem(TireQueueItemEntity(UUID.randomUUID().toString(), organizationId, request?.id, request?.vehicleId, position, TireQueueStatus.WAITING.name, now, SyncState.PENDING_CREATE.name, now))
    }

    suspend fun advanceQueueItem(item: TireQueueItem) {
        val next = when (item.status) {
            TireQueueStatus.WAITING -> TireQueueStatus.CALLED
            TireQueueStatus.CALLED -> TireQueueStatus.IN_SERVICE
            TireQueueStatus.IN_SERVICE -> TireQueueStatus.COMPLETED
            TireQueueStatus.COMPLETED, TireQueueStatus.CANCELLED -> return
        }
        tireDao.updateQueueStatus(item.id, next.name, SyncState.PENDING_UPDATE.name, System.currentTimeMillis())
    }

    suspend fun addDiagnostic(organizationId: String) {
        val now = System.currentTimeMillis()
        val active = tireDao.activeQueue(organizationId).firstOrNull { it.status == TireQueueStatus.IN_SERVICE.name }
            ?: tireDao.activeQueue(organizationId).firstOrNull()
        tireDao.insertDiagnostic(TireDiagnosticEntity(UUID.randomUUID().toString(), organizationId, active?.requestId, active?.vehicleId, "Давление проверено", 5.0, null, "Проверить давление и состояние протектора перед выдачей", now, SyncState.PENDING_CREATE.name, now))
    }

    suspend fun addWorkEntry(organizationId: String) {
        val now = System.currentTimeMillis()
        val active = tireDao.activeQueue(organizationId).firstOrNull { it.status == TireQueueStatus.IN_SERVICE.name }
            ?: tireDao.activeQueue(organizationId).firstOrNull()
        tireDao.insertWorkEntry(TireWorkEntryEntity(UUID.randomUUID().toString(), organizationId, active?.requestId, active?.vehicleId, "Шиномонтаж колеса", 1.0, null, now, SyncState.PENDING_CREATE.name, now))
    }

    suspend fun addStorageItem(organizationId: String) {
        val now = System.currentTimeMillis()
        val request = serviceDao.serviceRequests(organizationId).firstOrNull { it.vehicleId != null }
        val vehicle = request?.vehicleId?.let { serviceDao.vehicle(it) }
        val next = tireDao.storage(organizationId).size + 1
        val code = "TS-%06d".format(next)
        tireDao.upsertStorageItem(TireStorageItemEntity(UUID.randomUUID().toString(), organizationId, vehicle?.clientId, vehicle?.id, code, "Комплект шин", 4, "Склад", TireStorageStatus.STORED.name, now, null, SyncState.PENDING_CREATE.name, now))
    }

    suspend fun issueStorageItem(item: TireStorageItem) {
        if (item.status != TireStorageStatus.STORED) return
        val now = System.currentTimeMillis()
        tireDao.issueStorageItem(item.id, now, SyncState.PENDING_UPDATE.name, now)
    }

    companion object {
        fun create(context: Context) = TireRepository(LexoraServiceDatabase.create(context.applicationContext))
    }
}

private fun TireQueueItemEntity.toModel() = TireQueueItem(id, organizationId, requestId, vehicleId, position, TireQueueStatus.valueOf(status), createdAtEpochMs, SyncState.valueOf(syncState))
private fun TireDiagnosticEntity.toModel() = TireDiagnostic(id, organizationId, requestId, vehicleId, pressureNote, treadDepthMm, damageNote, recommendation, diagnosedAtEpochMs, SyncState.valueOf(syncState))
private fun TireWorkEntryEntity.toModel() = TireWorkEntry(id, organizationId, requestId, vehicleId, title, quantity, note, performedAtEpochMs, SyncState.valueOf(syncState))
private fun TireStorageItemEntity.toModel() = TireStorageItem(id, organizationId, clientId, vehicleId, storageCode, tireDescription, quantity, location, TireStorageStatus.valueOf(status), storedAtEpochMs, issuedAtEpochMs, SyncState.valueOf(syncState))
