package com.lexora.service.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "service_users",
    indices = [
        Index(value = ["login"], unique = true),
        Index("active"),
    ],
)
data class ServiceUserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val login: String?,
    val active: Boolean,
    val syncState: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "user_organization_roles",
    primaryKeys = ["userId", "organizationId", "role"],
    indices = [
        Index("userId"),
        Index("organizationId"),
        Index(value = ["userId", "organizationId"]),
    ],
)
data class UserOrganizationRoleEntity(
    val userId: String,
    val organizationId: String,
    val role: String,
    val active: Boolean,
    val syncState: String,
    val updatedAtEpochMs: Long,
)
