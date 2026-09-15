package com.lexora.service.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PublicPortalDao {
    @Query("SELECT * FROM public_bookings WHERE organizationId = :organizationId ORDER BY createdAtEpochMs DESC")
    suspend fun bookings(organizationId: String): List<PublicBookingEntity>

    @Query("SELECT * FROM public_bookings WHERE id = :id LIMIT 1")
    suspend fun booking(id: String): PublicBookingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBooking(value: PublicBookingEntity)

    @Query("UPDATE public_bookings SET status = :status, convertedRequestId = :convertedRequestId, syncState = :syncState, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun updateBookingStatus(
        id: String,
        status: String,
        convertedRequestId: String?,
        syncState: String,
        updatedAtEpochMs: Long,
    )

    @Query("SELECT number FROM service_requests WHERE organizationId = :organizationId")
    suspend fun requestNumbers(organizationId: String): List<String>

    @Query("SELECT * FROM portal_access_grants WHERE organizationId = :organizationId AND clientId = :clientId ORDER BY createdAtEpochMs DESC")
    suspend fun accessGrants(organizationId: String, clientId: String): List<PortalAccessGrantEntity>

    @Query("SELECT * FROM portal_access_grants WHERE organizationId = :organizationId AND tokenHash = :tokenHash LIMIT 1")
    suspend fun accessGrantByTokenHash(organizationId: String, tokenHash: String): PortalAccessGrantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccessGrant(value: PortalAccessGrantEntity)

    @Query("UPDATE portal_access_grants SET status = :status, revokedAtEpochMs = :revokedAtEpochMs, syncState = :syncState, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun updateAccessGrantStatus(
        id: String,
        status: String,
        revokedAtEpochMs: Long?,
        syncState: String,
        updatedAtEpochMs: Long,
    )
}
