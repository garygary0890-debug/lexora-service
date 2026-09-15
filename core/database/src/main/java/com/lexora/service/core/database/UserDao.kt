package com.lexora.service.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(value: ServiceUserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrganizationRole(value: UserOrganizationRoleEntity)

    @Query("SELECT * FROM service_users WHERE id = :id LIMIT 1")
    suspend fun user(id: String): ServiceUserEntity?

    @Query("SELECT * FROM service_users WHERE active = 1 ORDER BY displayName")
    suspend fun activeUsers(): List<ServiceUserEntity>

    @Query("SELECT * FROM service_users WHERE active = 0 ORDER BY displayName")
    suspend fun inactiveUsers(): List<ServiceUserEntity>

    @Query("SELECT * FROM user_organization_roles WHERE userId = :userId AND active = 1 ORDER BY organizationId, role")
    suspend fun activeRolesForUser(userId: String): List<UserOrganizationRoleEntity>

    @Query("SELECT * FROM user_organization_roles WHERE userId = :userId AND organizationId = :organizationId AND active = 1 ORDER BY role")
    suspend fun activeRolesForUserInOrganization(userId: String, organizationId: String): List<UserOrganizationRoleEntity>

    @Query("SELECT * FROM user_organization_roles WHERE organizationId = :organizationId AND active = 1 ORDER BY userId, role")
    suspend fun activeRolesForOrganization(organizationId: String): List<UserOrganizationRoleEntity>

    @Query("UPDATE service_users SET active = :active, syncState = :syncState, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun setUserActive(id: String, active: Boolean, syncState: String, updatedAtEpochMs: Long)

    @Query("UPDATE user_organization_roles SET active = :active, syncState = :syncState, updatedAtEpochMs = :updatedAtEpochMs WHERE userId = :userId AND organizationId = :organizationId AND role = :role")
    suspend fun setRoleActive(
        userId: String,
        organizationId: String,
        role: String,
        active: Boolean,
        syncState: String,
        updatedAtEpochMs: Long,
    )
}
