package com.lexora.service.core.data

import com.lexora.service.core.database.AuditEventEntity
import com.lexora.service.core.database.ServiceDao
import com.lexora.service.core.database.ServiceUserEntity
import com.lexora.service.core.database.UserDao
import com.lexora.service.core.database.UserOrganizationRoleEntity
import com.lexora.service.core.model.ServiceUser
import com.lexora.service.core.model.SyncState
import com.lexora.service.core.model.UserRole
import java.util.UUID

class PersistentUserRepository(
    private val userDao: UserDao,
    private val serviceDao: ServiceDao,
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
        } else if (!existing.active) {
            userDao.setUserActive(userId, true, SyncState.PENDING_UPDATE.name, now)
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
        return requireNotNull(userInOrganization(userId, organizationId))
    }

    /**
     * Returns the cross-organization identity. Do not use this object for authorization.
     * Authorization must use [userInOrganization] so roles from another organization
     * never leak into the active tenant context.
     */
    suspend fun user(userId: String): ServiceUser? {
        val entity = userDao.user(userId) ?: return null
        val memberships = userDao.activeRolesForUser(userId)
        return ServiceUser(
            id = entity.id,
            displayName = entity.displayName,
            roles = memberships.mapNotNull { runCatching { UserRole.valueOf(it.role) }.getOrNull() }.toSet(),
            organizationIds = memberships.map { it.organizationId }.toSet(),
            active = entity.active,
        )
    }

    /** Authorization-safe representation scoped to exactly one organization. */
    suspend fun userInOrganization(userId: String, organizationId: String): ServiceUser? {
        val entity = userDao.user(userId) ?: return null
        val memberships = userDao.activeRolesForUserInOrganization(userId, organizationId)
        if (memberships.isEmpty()) return null
        return ServiceUser(
            id = entity.id,
            displayName = entity.displayName,
            roles = memberships.mapNotNull { runCatching { UserRole.valueOf(it.role) }.getOrNull() }.toSet(),
            organizationIds = setOf(organizationId),
            active = entity.active,
        )
    }

    suspend fun usersForOrganization(organizationId: String): List<ServiceUser> {
        val rolesByUser = userDao.activeRolesForOrganization(organizationId).groupBy { it.userId }
        val users = (userDao.activeUsers() + userDao.inactiveUsers()).distinctBy { it.id }
        return users.mapNotNull { entity ->
            val memberships = rolesByUser[entity.id].orEmpty()
            if (memberships.isEmpty()) return@mapNotNull null
            ServiceUser(
                id = entity.id,
                displayName = entity.displayName,
                roles = memberships.mapNotNull { runCatching { UserRole.valueOf(it.role) }.getOrNull() }.toSet(),
                organizationIds = setOf(organizationId),
                active = entity.active,
            )
        }.sortedWith(compareByDescending<ServiceUser> { it.active }.thenBy { it.displayName.lowercase() })
    }

    suspend fun createLocalUser(
        actorUserId: String,
        organizationId: String,
        displayName: String,
        initialRole: UserRole,
    ): ServiceUser {
        require(actorUserId.isNotBlank())
        require(displayName.isNotBlank())
        val now = System.currentTimeMillis()
        val userId = UUID.randomUUID().toString()
        userDao.upsertUser(
            ServiceUserEntity(
                id = userId,
                displayName = displayName.trim(),
                login = null,
                active = true,
                syncState = SyncState.PENDING_CREATE.name,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )
        userDao.upsertOrganizationRole(
            UserOrganizationRoleEntity(
                userId = userId,
                organizationId = organizationId,
                role = initialRole.name,
                active = true,
                syncState = SyncState.PENDING_CREATE.name,
                updatedAtEpochMs = now,
            ),
        )
        serviceDao.insertAuditEvent(
            AuditEventEntity(
                id = UUID.randomUUID().toString(),
                organizationId = organizationId,
                userId = actorUserId,
                entityType = "USER",
                entityId = userId,
                action = "CREATE",
                summary = "${displayName.trim()} · ${initialRole.name}",
                occurredAtEpochMs = now,
            ),
        )
        return requireNotNull(userInOrganization(userId, organizationId))
    }

    suspend fun setRole(
        actorUserId: String,
        userId: String,
        organizationId: String,
        role: UserRole,
        active: Boolean,
    ) {
        require(actorUserId.isNotBlank())
        require(!(actorUserId == userId && role == UserRole.ADMIN && !active)) {
            "Нельзя отозвать собственную роль ADMIN."
        }
        val organizationRoles = userDao.activeRolesForUserInOrganization(userId, organizationId)
        require(active || organizationRoles.size > 1) {
            "У пользователя должна остаться хотя бы одна активная роль в организации."
        }
        val now = System.currentTimeMillis()
        val existing = organizationRoles.firstOrNull { it.role == role.name }

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
        } else {
            return
        }

        serviceDao.insertAuditEvent(
            AuditEventEntity(
                id = UUID.randomUUID().toString(),
                organizationId = organizationId,
                userId = actorUserId,
                entityType = "USER_ROLE",
                entityId = userId,
                action = if (active) "ROLE_GRANTED" else "ROLE_REVOKED",
                summary = "${role.name} · user=$userId",
                occurredAtEpochMs = now,
            ),
        )
    }

    suspend fun setUserActive(
        actorUserId: String,
        userId: String,
        organizationId: String,
        active: Boolean,
    ) {
        require(actorUserId.isNotBlank())
        require(!(actorUserId == userId && !active)) { "Нельзя деактивировать текущего пользователя." }
        val existing = userDao.user(userId) ?: return
        if (existing.active == active) return
        val now = System.currentTimeMillis()
        userDao.setUserActive(userId, active, SyncState.PENDING_UPDATE.name, now)
        serviceDao.insertAuditEvent(
            AuditEventEntity(
                id = UUID.randomUUID().toString(),
                organizationId = organizationId,
                userId = actorUserId,
                entityType = "USER",
                entityId = userId,
                action = if (active) "USER_ACTIVATED" else "USER_DEACTIVATED",
                summary = existing.displayName,
                occurredAtEpochMs = now,
            ),
        )
    }

    companion object {
        const val LOCAL_ADMIN_ID = "user-local-admin"
    }
}
