package com.lexora.service.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TireDao {
    @Query("SELECT * FROM tire_queue WHERE organizationId = :organizationId AND status NOT IN ('COMPLETED','CANCELLED') ORDER BY position, createdAtEpochMs")
    suspend fun activeQueue(organizationId: String): List<TireQueueItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQueueItem(value: TireQueueItemEntity)

    @Query("UPDATE tire_queue SET status = :status, syncState = :syncState, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun updateQueueStatus(id: String, status: String, syncState: String, updatedAt: Long)

    @Query("SELECT * FROM tire_diagnostics WHERE organizationId = :organizationId ORDER BY diagnosedAtEpochMs DESC LIMIT :limit")
    suspend fun diagnostics(organizationId: String, limit: Int = 100): List<TireDiagnosticEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiagnostic(value: TireDiagnosticEntity)

    @Query("SELECT * FROM tire_work_entries WHERE organizationId = :organizationId ORDER BY performedAtEpochMs DESC LIMIT :limit")
    suspend fun workEntries(organizationId: String, limit: Int = 100): List<TireWorkEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkEntry(value: TireWorkEntryEntity)

    @Query("SELECT * FROM tire_storage WHERE organizationId = :organizationId ORDER BY storedAtEpochMs DESC")
    suspend fun storage(organizationId: String): List<TireStorageItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStorageItem(value: TireStorageItemEntity)

    @Query("UPDATE tire_storage SET status = 'ISSUED', issuedAtEpochMs = :issuedAt, syncState = :syncState, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun issueStorageItem(id: String, issuedAt: Long, syncState: String, updatedAt: Long)
}
