package com.lexora.service.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(
    tableName = "reference_directories",
    indices = [
        Index("organizationId"),
        Index(value = ["organizationId", "code"], unique = true),
        Index("active"),
    ],
)
data class ReferenceDirectoryEntity(
    @androidx.room.PrimaryKey val id: String,
    val organizationId: String,
    val code: String,
    val name: String,
    val description: String?,
    val system: Boolean,
    val active: Boolean,
    val syncState: String,
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "reference_directory_items",
    indices = [
        Index("organizationId"),
        Index("directoryId"),
        Index(value = ["directoryId", "code"], unique = true),
        Index("active"),
        Index("sortOrder"),
    ],
)
data class ReferenceDirectoryItemEntity(
    @androidx.room.PrimaryKey val id: String,
    val organizationId: String,
    val directoryId: String,
    val code: String,
    val name: String,
    val sortOrder: Int,
    val active: Boolean,
    val syncState: String,
    val updatedAtEpochMs: Long,
)

@Dao
interface ReferenceDataDao {
    @Query("SELECT * FROM reference_directories WHERE organizationId = :organizationId ORDER BY name")
    suspend fun directories(organizationId: String): List<ReferenceDirectoryEntity>

    @Query("SELECT * FROM reference_directories WHERE id = :id LIMIT 1")
    suspend fun directory(id: String): ReferenceDirectoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDirectory(value: ReferenceDirectoryEntity)

    @Query("SELECT * FROM reference_directory_items WHERE directoryId = :directoryId ORDER BY sortOrder, name")
    suspend fun items(directoryId: String): List<ReferenceDirectoryItemEntity>

    @Query("SELECT * FROM reference_directory_items WHERE id = :id LIMIT 1")
    suspend fun item(id: String): ReferenceDirectoryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(value: ReferenceDirectoryItemEntity)

    @Query("UPDATE reference_directories SET active = :active, syncState = 'PENDING_UPDATE', updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun setDirectoryActive(id: String, active: Boolean, updatedAt: Long)

    @Query("UPDATE reference_directory_items SET active = :active, syncState = 'PENDING_UPDATE', updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun setItemActive(id: String, active: Boolean, updatedAt: Long)
}
