package com.lexora.service.core.domain

import com.lexora.service.core.model.Permission
import com.lexora.service.core.model.ServiceUser
import com.lexora.service.core.model.UserRole
import com.lexora.service.core.model.UserWorkspaceMembership

/** Enforces owner-only access by default and explicit role-based team access. */
class UserWorkspaceAccessPolicy(
    private val accessPolicy: AccessPolicy = AccessPolicy(),
) {
    fun canRead(
        userId: String,
        ownerUserId: String,
        memberships: Collection<UserWorkspaceMembership>,
    ): Boolean = userId == ownerUserId || activeMembership(userId, ownerUserId, memberships) != null

    fun can(
        userId: String,
        ownerUserId: String,
        memberships: Collection<UserWorkspaceMembership>,
        permission: Permission,
    ): Boolean {
        if (userId == ownerUserId) return true
        val membership = activeMembership(userId, ownerUserId, memberships) ?: return false
        return accessPolicy.can(
            ServiceUser(
                id = userId,
                displayName = userId,
                roles = membership.roles,
                organizationIds = emptySet(),
            ),
            permission,
        )
    }

    private fun activeMembership(
        userId: String,
        ownerUserId: String,
        memberships: Collection<UserWorkspaceMembership>,
    ): UserWorkspaceMembership? = memberships.firstOrNull {
        it.active && it.ownerUserId == ownerUserId && it.memberUserId == userId
    }
}
