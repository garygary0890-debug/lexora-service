package com.lexora.service.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WashDao {
    @Query("SELECT * FROM wash_posts WHERE organizationId = :organizationId AND active = 1 ORDER BY name")
    suspend fun posts(organizationId: String): List<WashPostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPost(value: WashPostEntity)

    @Query("UPDATE wash_posts SET status = :status, syncState = :syncState, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun updatePostStatus(id: String, status: String, syncState: String, updatedAt: Long)

    @Query("SELECT * FROM wash_queue WHERE organizationId = :organizationId AND status NOT IN ('COMPLETED','CANCELLED') ORDER BY position, createdAtEpochMs")
    suspend fun activeQueue(organizationId: String): List<WashQueueItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQueueItem(value: WashQueueItemEntity)

    @Query("UPDATE wash_queue SET postId = :postId, status = :status, syncState = :syncState, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun assignQueueItem(id: String, postId: String?, status: String, syncState: String, updatedAt: Long)

    @Query("SELECT * FROM wash_tech_cards WHERE organizationId = :organizationId AND active = 1 ORDER BY name")
    suspend fun techCards(organizationId: String): List<WashTechCardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTechCard(value: WashTechCardEntity)

    @Query("SELECT * FROM wash_chemical_usage WHERE organizationId = :organizationId ORDER BY usedAtEpochMs DESC LIMIT :limit")
    suspend fun recentChemicalUsage(organizationId: String, limit: Int = 100): List<WashChemicalUsageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChemicalUsage(value: WashChemicalUsageEntity)
}
