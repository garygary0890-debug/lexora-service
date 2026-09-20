package com.lexora.service.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkOrderCommercialDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertApprovalEvent(value: AdditionalWorkApprovalEventEntity)

    @Query("SELECT * FROM additional_work_approval_events WHERE workOrderItemId = :itemId ORDER BY decidedAtEpochMs, id")
    suspend fun approvalEvents(itemId: String): List<AdditionalWorkApprovalEventEntity>
}
