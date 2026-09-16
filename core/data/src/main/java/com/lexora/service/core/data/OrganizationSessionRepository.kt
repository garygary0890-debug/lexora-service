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

    suspend fun switch(user: ServiceUser, organizationId: String): OrganizationSession =
        switchOrganization(user.id, organizationId)

    suspend fun switchOrganization(userId: String, organizationId: String): OrganizationSession {
        val globalIdentity = requireNotNull(userRepository.user(userId))
        require(globalIdentity.active) { "Пользователь деактивирован." }
        require(organizationId in globalIdentity.organizationIds) { "Нет доступа к выбранной организации." }
        val organization = organizationRepository.setActiveOrganization(userId, organizationId)
        val scopedUser = requireNotNull(userRepository.userInOrganization(userId, organizationId)) {
            "Нет активной роли в выбранной организации."
        }
        return session(organization, scopedUser)
    }

    suspend fun createAndSwitch(actor: ServiceUser, name: String): OrganizationSession =
        createOrganization(actor.id, name)

    suspend fun createOrganization(actorUserId: String, name: String): OrganizationSession {
        val actorIdentity = requireNotNull(userRepository.user(actorUserId))
        require(actorIdentity.active) { "Пользователь деактивирован." }
        val activeOrganizationId = organizationRepository.activeOrganization()?.id
            ?: actorIdentity.organizationIds.firstOrNull()
            ?: error("Нет активной организации.")
        val scopedActor = requireNotNull(userRepository.userInOrganization(actorUserId, activeOrganizationId))
        require(accessPolicy.can(scopedActor, Permission.MANAGE_ORGANIZATION)) {
            "Недостаточно прав для создания организации."
        }
        val created = organizationRepository.createOrganization(actorUserId, name)
        userRepository.setRole(actorUserId, actorUserId, created.id, UserRole.ADMIN, true)
        val organization = organizationRepository.setActiveOrganization(actorUserId, created.id)
        return session(
            organization,
            requireNotNull(userRepository.userInOrganization(actorUserId, created.id)),
        )
    }

    private suspend fun session(organization: Organization, user: ServiceUser): OrganizationSession {
        val memberships = userRepository.user(user.id)?.organizationIds.orEmpty()
        return OrganizationSession(
            organization = organization,
            user = user,
            organizations = organizationRepository.organizations().filter { it.id in memberships },
        )
    }
}
