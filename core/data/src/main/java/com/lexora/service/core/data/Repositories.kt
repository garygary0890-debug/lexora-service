package com.lexora.service.core.data

import com.lexora.service.core.model.LexoraModuleId
import com.lexora.service.core.model.ModuleDescriptor
import com.lexora.service.core.model.Organization
import com.lexora.service.core.model.ServiceUser

interface OrganizationRepository {
    fun organizations(): List<Organization>
    fun activeOrganization(): Organization?
    fun setActiveOrganization(id: String)
}

interface UserRepository {
    fun currentUser(): ServiceUser
}

interface ModuleRegistry {
    fun modules(organizationId: String): List<ModuleDescriptor>
    fun updateEnabled(organizationId: String, moduleId: LexoraModuleId, enabled: Boolean)
}
