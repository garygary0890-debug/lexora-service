package com.lexora.service.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContractDao {
    @Query("SELECT * FROM service_contracts WHERE organizationId = :organizationId AND archived = 0 ORDER BY number DESC")
    suspend fun contracts(organizationId: String): List<ServiceContractEntity>

    @Query("SELECT * FROM service_contracts WHERE organizationId = :organizationId AND archived = 1 ORDER BY number DESC")
    suspend fun archivedContracts(organizationId: String): List<ServiceContractEntity>

    @Query("SELECT * FROM service_contracts WHERE organizationId = :organizationId ORDER BY number DESC")
    suspend fun allContracts(organizationId: String): List<ServiceContractEntity>

    @Query("SELECT * FROM service_contracts WHERE id = :id LIMIT 1")
    suspend fun contract(id: String): ServiceContractEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContract(value: ServiceContractEntity)

    @Query("UPDATE service_contracts SET status = :status, signedAtEpochMs = :signedAtEpochMs, syncState = :syncState, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, signedAtEpochMs: Long?, syncState: String, updatedAtEpochMs: Long)

    @Query("UPDATE service_contracts SET archived = 1, syncState = :syncState, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun archive(id: String, syncState: String, updatedAtEpochMs: Long)

    @Query("UPDATE service_contracts SET archived = 0, syncState = :syncState, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun restore(id: String, syncState: String, updatedAtEpochMs: Long)
}
