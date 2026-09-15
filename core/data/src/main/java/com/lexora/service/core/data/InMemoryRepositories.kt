package com.lexora.service.core.data

import com.lexora.service.core.model.Organization
import com.lexora.service.core.model.ServiceUser
import com.lexora.service.core.model.UserRole

class InMemoryOrganizationRepository : OrganizationRepository {
    private val source = mutableListOf(
        Organization(id = "org-demo", name = "Демонстрационная организация", isActive = true),
    )

    override fun organizations(): List<Organization> = source.toList()

    override fun activeOrganization(): Organization? = source.firstOrNull { it.isActive }

    override fun setActiveOrganization(id: String) {
        for (index in source.indices) {
            source[index] = source[index].copy(isActive = source[index].id == id)
        }
    }
}

class InMemoryUserRepository : UserRepository {
    private val user = ServiceUser(
        id = "user-local-admin",
        displayName = "Локальный администратор",
        roles = setOf(UserRole.ADMIN),
        organizationIds = setOf("org-demo"),
    )

    override fun currentUser(): ServiceUser = user
}
