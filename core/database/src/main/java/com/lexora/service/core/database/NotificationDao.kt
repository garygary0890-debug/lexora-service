package com.lexora.service.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationDao {
    @Query("SELECT * FROM service_notifications WHERE organizationId = :organizationId AND archived = 0 ORDER BY COALESCE(scheduledAtEpochMs, occurredAtEpochMs) DESC, occurredAtEpochMs DESC")
    suspend fun notifications(organizationId: String): List<ServiceNotificationEntity>

    @Query("SELECT * FROM service_notifications WHERE organizationId = :organizationId AND archived = 0 AND readAtEpochMs IS NULL ORDER BY priority DESC, COALESCE(scheduledAtEpochMs, occurredAtEpochMs) DESC")
    suspend fun unreadNotifications(organizationId: String): List<ServiceNotificationEntity>

    @Query("SELECT * FROM service_notifications WHERE organizationId = :organizationId AND archived = 1 ORDER BY occurredAtEpochMs DESC")
    suspend fun archivedNotifications(organizationId: String): List<ServiceNotificationEntity>

    @Query("SELECT * FROM service_notifications WHERE id = :id AND organizationId = (SELECT id FROM organizations WHERE isActive = 1 LIMIT 1) LIMIT 1")
    suspend fun notification(id: String): ServiceNotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(value: ServiceNotificationEntity)

    @Query("UPDATE service_notifications SET readAtEpochMs = :readAtEpochMs, syncState = :syncState, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id AND organizationId = (SELECT id FROM organizations WHERE isActive = 1 LIMIT 1)")
    suspend fun markRead(id: String, readAtEpochMs: Long?, syncState: String, updatedAtEpochMs: Long)

    @Query("UPDATE service_notifications SET readAtEpochMs = :readAtEpochMs, syncState = :syncState, updatedAtEpochMs = :updatedAtEpochMs WHERE organizationId = :organizationId AND archived = 0 AND readAtEpochMs IS NULL")
    suspend fun markAllRead(organizationId: String, readAtEpochMs: Long, syncState: String, updatedAtEpochMs: Long)

    @Query("UPDATE service_notifications SET archived = :archived, syncState = :syncState, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id AND organizationId = (SELECT id FROM organizations WHERE isActive = 1 LIMIT 1)")
    suspend fun setArchived(id: String, archived: Boolean, syncState: String, updatedAtEpochMs: Long)
}
