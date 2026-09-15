package com.lexora.service.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkOrderDao {
    @Query("SELECT * FROM work_order_items WHERE documentId = :documentId ORDER BY updatedAtEpochMs, title")
    suspend fun items(documentId: String): List<WorkOrderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(value: WorkOrderItemEntity)

    @Query("UPDATE work_order_items SET approvalStatus = :status, approvalComment = :comment, approvedAtEpochMs = :approvedAt, syncState = :syncState, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun updateApproval(id: String, status: String, comment: String?, approvedAt: Long?, syncState: String, updatedAt: Long)

    @Query("DELETE FROM work_order_items WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COALESCE(SUM(totalMinor), 0) FROM work_order_items WHERE documentId = :documentId AND (additional = 0 OR approvalStatus = 'APPROVED')")
    suspend fun approvedTotalMinor(documentId: String): Long
}
