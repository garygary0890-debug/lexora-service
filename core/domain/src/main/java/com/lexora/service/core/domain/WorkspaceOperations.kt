package com.lexora.service.core.domain

import com.lexora.service.core.model.ModuleDescriptor
import com.lexora.service.core.model.Organization
import com.lexora.service.core.model.ServiceUser
import com.lexora.service.core.model.UserRole

data class WorkspaceSnapshot(
    val organization: Organization,
    val user: ServiceUser,
    val organizations: List<Organization>,
    val users: List<ServiceUser>,
    val modules: List<ModuleDescriptor>,
)

interface WorkspaceOperations {
    suspend fun bootstrap(): WorkspaceSnapshot
    suspend fun refresh(organizationId: String, userId: String): WorkspaceSnapshot
    suspend fun switchOrganization(userId: String, organizationId: String): WorkspaceSnapshot
    suspend fun createOrganization(actorUserId: String, name: String): WorkspaceSnapshot
    suspend fun createUser(actorUserId: String, organizationId: String, displayName: String, role: UserRole)
    suspend fun setRole(actorUserId: String, userId: String, organizationId: String, role: UserRole, enabled: Boolean)
    suspend fun setUserActive(actorUserId: String, userId: String, organizationId: String, active: Boolean)
}

class BootstrapWorkspaceUseCase(private val workspace: WorkspaceOperations) {
    suspend operator fun invoke(): WorkspaceSnapshot = workspace.bootstrap()
}

class SwitchWorkspaceUseCase(private val workspace: WorkspaceOperations) {
    suspend operator fun invoke(userId: String, organizationId: String): WorkspaceSnapshot =
        workspace.switchOrganization(userId, organizationId)
}
