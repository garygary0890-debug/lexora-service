package com.lexora.service.core.data

import com.lexora.service.core.database.ServiceUserEntity
import com.lexora.service.core.database.UserDao
import com.lexora.service.core.database.UserOrganizationRoleEntity
import com.lexora.service.core.model.ServiceUser
import com.lexora.service.core.model.SyncState
import com.lexora.service.core.model.UserRole

class PersistentUserRepository(
    private val userDao: UserDao,
) {
    suspend fun ensureLocalAdmin(organizationId: String): ServiceUser {
        val now = System.currentTimeMillis()
        val userId = LOCAL_ADMIN_ID
        val existing = userDao.user(userId)
        if (existing == null) {
            userDao.upsertUser(
                ServiceUserEntity(
                    id = userId,
                    displayName = "Локальный администратор",
                    login = null,
                    active = true,
                    syncState = SyncState.PENDING_CREATE.name,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now,
                ),
            )
        }
        val roles = userDao.activeRolesForUserInOrganization(userId, organizationId)
        if (roles.none { it.role == UserRole.ADMIN.name }) {
            userDao.upsertOrganizationRole(
                UserOrganizationRoleEntity(
                    userId = userId,
                    organizationId = organizationId,
                    role = UserRole.ADMIN.name,
                    active = true,
                    syncState = SyncState.PENDING_CREATE.name,
                    updatedAtEpochMs = now,
                ),
            )
        }
        return requireNotNull(user(userId))
    }

    suspend fun user(userId: String): ServiceUser? {
        val entity = userDao.user(userId) ?: return null
        if (!entity.active) return null
        val memberships = userDao.activeRolesForUser(userId)
        return ServiceUser(
            id = entity.id,
            displayName = entity.displayName,
            roles = memberships.mapNotNull { runCatching { UserRole.valueOf(it.role) }.getOrNull() }.toSet(),
            organizationIds = memberships.map { it.organizationId }.toSet(),
        )
    }

    suspend fun usersForOrganization(organizationId: String): List<ServiceUser> {
        val rolesByUser = userDao.activeRolesForOrganization(organizationId).groupBy { it.userId }
        return userDao.activeUsers().mapNotNull { entity ->
            val memberships = rolesByUser[entity.id].orEmpty()
            if (memberships.isEmpty()) return@mapNotNull null
            ServiceUser(
                id = entity.id,
                displayName = entity.displayName,
                roles = memberships.mapNotNull { runCatching { UserRole.valueOf(it.role) }.getOrNull() }.toSet(),
                organizationIds = setOf(organizationId),
            )
        }
    }

    suspend fun setRole(
        userId: String,
        organizationId: String,
        role: UserRole,
        active: Boolean,
    ) {
        val now = System.currentTimeMillis()
        val existing = userDao.activeRolesForUserInOrganization(userId, organizationId)
            .firstOrNull { it.role == role.name }
        if (existing == null && active) {
            userDao.upsertOrganizationRole(
                UserOrganizationRoleEntity(
                    userId = userId,
                    organizationId = organizationId,
                    role = role.name,
                    active = true,
                    syncState = SyncState.PENDING_CREATE.name,
                    updatedAtEpochMs = now,
                ),
            )
        } else if (existing != null) {
            userDao.setRoleActive(
                userId = userId,
                organizationId = organizationId,
                role = role.name,
                active = active,
                syncState = SyncState.PENDING_UPDATE.name,
                updatedAtEpochMs = now,
            )
        }
    }

    companion object {
        const val LOCAL_ADMIN_ID = "user-local-admin"
    }
}
