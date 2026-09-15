package com.lexora.service.core.data

import com.lexora.service.core.domain.AccessPolicy
import com.lexora.service.core.model.Organization
import com.lexora.service.core.model.Permission
import com.lexora.service.core.model.ServiceUser
import com.lexora.service.core.model.UserRole

data class OrganizationSession(
    val organization: Organization,
    val user: ServiceUser,
    val organizations: List<Organization>,
)

class OrganizationSessionRepository(
    private val organizationRepository: PersistentOrganizationRepository,
    private val userRepository: PersistentUserRepository,
    private val accessPolicy: AccessPolicy,
) {
    suspend fun bootstrap(): OrganizationSession {
        val organization = organizationRepository.ensureBootstrapOrganization()
        val user = userRepository.ensureLocalAdmin(organization.id)
        return session(organization, user)
    }

    suspend fun switchOrganization(userId: String, organizationId: String): OrganizationSession {
        val user = requireNotNull(userRepository.user(userId))
        require(organizationId in user.organizationIds)
        val organization = organizationRepository.setActiveOrganization(userId, organizationId)
        return session(organization, requireNotNull(userRepository.user(userId)))
    }

    suspend fun createOrganization(actorUserId: String, name: String): OrganizationSession {
        val actor = requireNotNull(userRepository.user(actorUserId))
        require(accessPolicy.can(actor, Permission.MANAGE_ORGANIZATION))
        val created = organizationRepository.createOrganization(actorUserId, name)
        userRepository.setRole(actorUserId, actorUserId, created.id, UserRole.ADMIN, true)
        val organization = organizationRepository.setActiveOrganization(actorUserId, created.id)
        return session(organization, requireNotNull(userRepository.user(actorUserId)))
    }

    private suspend fun session(organization: Organization, user: ServiceUser) = OrganizationSession(
        organization = organization,
        user = user,
        organizations = organizationRepository.organizations(),
    )
}
