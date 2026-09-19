package com.lexora.service.core.data

import com.lexora.service.core.domain.WorkspaceOperations
import com.lexora.service.core.domain.WorkspaceSnapshot
import com.lexora.service.core.model.UserRole

class PersistentWorkspaceOperations(
    private val sessions: OrganizationSessionRepository,
    private val users: PersistentUserRepository,
    private val modules: ModuleLicenseRepository,
) : WorkspaceOperations {
    override suspend fun bootstrap(): WorkspaceSnapshot = sessions.bootstrap().toSnapshot()

    override suspend fun refresh(organizationId: String, userId: String): WorkspaceSnapshot {
        val session = sessions.switchOrganization(userId, organizationId)
        return session.toSnapshot()
    }

    override suspend fun switchOrganization(userId: String, organizationId: String): WorkspaceSnapshot =
        sessions.switchOrganization(userId, organizationId).toSnapshot()

    override suspend fun createOrganization(actorUserId: String, name: String): WorkspaceSnapshot =
        sessions.createOrganization(actorUserId, name).toSnapshot()

    override suspend fun createUser(actorUserId: String, organizationId: String, displayName: String, role: UserRole) {
        users.createLocalUser(actorUserId, organizationId, displayName, role)
    }

    override suspend fun setRole(actorUserId: String, userId: String, organizationId: String, role: UserRole, enabled: Boolean) {
        users.setRole(actorUserId, userId, organizationId, role, enabled)
    }

    override suspend fun setUserActive(actorUserId: String, userId: String, organizationId: String, active: Boolean) {
        users.setUserActive(actorUserId, userId, organizationId, active)
    }

    private suspend fun OrganizationSession.toSnapshot(): WorkspaceSnapshot = WorkspaceSnapshot(
        organization = organization,
        user = user,
        organizations = organizations,
        users = users.usersForOrganization(organization.id),
        modules = modules.descriptors(organization.id),
    )
}
