package com.lexora.service.core.data

import com.lexora.service.core.model.LexoraModuleId
import com.lexora.service.core.model.ModuleDescriptor
import com.lexora.service.core.model.ModuleLicenseStatus
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

class InMemoryModuleRegistry : ModuleRegistry {
    private val modules = mutableMapOf(
        "org-demo" to mutableListOf(
            ModuleDescriptor(
                id = LexoraModuleId.CORE,
                title = "Базовое ядро",
                enabled = true,
                licenseStatus = ModuleLicenseStatus.NOT_REQUIRED,
            ),
            ModuleDescriptor(
                id = LexoraModuleId.WASH,
                title = "Автомойка",
                enabled = true,
                licenseStatus = ModuleLicenseStatus.ACTIVE,
            ),
            ModuleDescriptor(
                id = LexoraModuleId.TIRES,
                title = "Шиномонтаж",
                enabled = true,
                licenseStatus = ModuleLicenseStatus.ACTIVE,
            ),
        )
    )

    override fun modules(organizationId: String): List<ModuleDescriptor> =
        modules[organizationId]?.toList().orEmpty()

    override fun updateEnabled(
        organizationId: String,
        moduleId: LexoraModuleId,
        enabled: Boolean,
    ) {
        if (moduleId == LexoraModuleId.CORE) return
        val items = modules[organizationId] ?: return
        val index = items.indexOfFirst { it.id == moduleId }
        if (index >= 0) items[index] = items[index].copy(enabled = enabled)
    }
}
