package com.lexora.service.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface CustomerCareDao {
    @Query("SELECT * FROM loyalty_accounts WHERE organizationId = :organizationId ORDER BY clientId")
    suspend fun loyaltyAccounts(organizationId: String): List<LoyaltyAccountEntity>

    @Query("SELECT * FROM loyalty_accounts WHERE organizationId = :organizationId AND clientId = :clientId LIMIT 1")
    suspend fun loyaltyAccount(organizationId: String, clientId: String): LoyaltyAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLoyaltyAccount(value: LoyaltyAccountEntity)

    @Query("UPDATE loyalty_accounts SET pointsBalance = pointsBalance + :delta, syncState = 'PENDING_UPDATE', updatedAtEpochMs = :updatedAt WHERE id = :accountId AND organizationId = (SELECT id FROM organizations WHERE isActive = 1 LIMIT 1) AND active = 1 AND pointsBalance + :delta >= 0")
    suspend fun adjustLoyaltyBalance(accountId: String, delta: Long, updatedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoyaltyTransaction(value: LoyaltyTransactionEntity)

    @Transaction
    suspend fun applyLoyaltyTransaction(accountId: String, delta: Long, transaction: LoyaltyTransactionEntity): Boolean {
        val changed = adjustLoyaltyBalance(accountId, delta, transaction.updatedAtEpochMs)
        if (changed != 1) return false
        insertLoyaltyTransaction(transaction)
        return true
    }

    @Query("SELECT * FROM loyalty_transactions WHERE organizationId = :organizationId ORDER BY occurredAtEpochMs DESC LIMIT :limit")
    suspend fun loyaltyTransactions(organizationId: String, limit: Int = 100): List<LoyaltyTransactionEntity>

    @Query("SELECT * FROM quality_control WHERE organizationId = :organizationId ORDER BY COALESCE(controlledAtEpochMs, updatedAtEpochMs) DESC")
    suspend fun qualityRecords(organizationId: String): List<QualityControlRecordEntity>

    @Query("SELECT * FROM quality_control WHERE requestId = :requestId AND organizationId = (SELECT id FROM organizations WHERE isActive = 1 LIMIT 1) ORDER BY updatedAtEpochMs DESC LIMIT 1")
    suspend fun qualityRecordForRequest(requestId: String): QualityControlRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQualityRecord(value: QualityControlRecordEntity)
}
