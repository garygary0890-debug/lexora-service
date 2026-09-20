package com.lexora.service.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationDeliveryDao {
    @Query("SELECT * FROM notification_preferences WHERE organizationId = :organizationId AND userId = :userId LIMIT 1")
    suspend fun preferences(organizationId: String, userId: String): NotificationPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreferences(value: NotificationPreferenceEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(value: NotificationDeliveryEntity): Long

    @Query("SELECT * FROM notification_deliveries WHERE organizationId = :organizationId AND userId = :userId ORDER BY createdAtEpochMs DESC LIMIT :limit")
    suspend fun deliveries(organizationId: String, userId: String, limit: Int = 200): List<NotificationDeliveryEntity>

    @Query("SELECT * FROM notification_deliveries WHERE status IN ('PENDING','DEFERRED','FAILED') AND (nextAttemptAtEpochMs IS NULL OR nextAttemptAtEpochMs <= :now) ORDER BY COALESCE(nextAttemptAtEpochMs, scheduledAtEpochMs, createdAtEpochMs), createdAtEpochMs LIMIT :limit")
    suspend fun due(now: Long, limit: Int = 100): List<NotificationDeliveryEntity>

    @Query("SELECT * FROM notification_deliveries WHERE id = :id LIMIT 1")
    suspend fun delivery(id: String): NotificationDeliveryEntity?

    @Query("UPDATE notification_deliveries SET status = :status, nextAttemptAtEpochMs = :nextAttemptAt, deliveredAtEpochMs = :deliveredAt, attemptCount = :attemptCount, lastError = :lastError, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun updateState(id: String, status: String, nextAttemptAt: Long?, deliveredAt: Long?, attemptCount: Int, lastError: String?, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPushToken(value: PushDeviceTokenEntity)

    @Query("SELECT * FROM push_device_tokens WHERE organizationId = :organizationId AND userId = :userId ORDER BY updatedAtEpochMs DESC")
    suspend fun pushTokens(organizationId: String, userId: String): List<PushDeviceTokenEntity>

    @Query("UPDATE push_device_tokens SET active = 0, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun deactivatePushToken(id: String, updatedAt: Long)
}
