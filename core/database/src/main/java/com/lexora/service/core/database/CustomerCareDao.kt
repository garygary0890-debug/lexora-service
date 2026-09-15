package com.lexora.service.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CustomerCareDao {
    @Query("SELECT * FROM loyalty_accounts WHERE organizationId = :organizationId ORDER BY clientId")
    suspend fun loyaltyAccounts(organizationId: String): List<LoyaltyAccountEntity>

    @Query("SELECT * FROM loyalty_accounts WHERE organizationId = :organizationId AND clientId = :clientId LIMIT 1")
    suspend fun loyaltyAccount(organizationId: String, clientId: String): LoyaltyAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLoyaltyAccount(value: LoyaltyAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoyaltyTransaction(value: LoyaltyTransactionEntity)

    @Query("SELECT * FROM loyalty_transactions WHERE organizationId = :organizationId ORDER BY occurredAtEpochMs DESC LIMIT :limit")
    suspend fun loyaltyTransactions(organizationId: String, limit: Int = 100): List<LoyaltyTransactionEntity>

    @Query("SELECT * FROM quality_control WHERE organizationId = :organizationId ORDER BY COALESCE(controlledAtEpochMs, updatedAtEpochMs) DESC")
    suspend fun qualityRecords(organizationId: String): List<QualityControlRecordEntity>

    @Query("SELECT * FROM quality_control WHERE requestId = :requestId ORDER BY updatedAtEpochMs DESC LIMIT 1")
    suspend fun qualityRecordForRequest(requestId: String): QualityControlRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQualityRecord(value: QualityControlRecordEntity)
}
