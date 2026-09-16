package com.lexora.service.core.domain

import com.lexora.service.core.model.LexoraModuleId
import com.lexora.service.core.model.ModuleDescriptor
import com.lexora.service.core.model.Permission
import com.lexora.service.core.model.ServiceUser

class ModuleAccessPolicy(
    private val accessPolicy: AccessPolicy = AccessPolicy(),
) {
    fun isAvailable(module: ModuleDescriptor, user: ServiceUser): Boolean {
        if (!module.enabled || !module.licensed) return false
        val permission = when (module.id) {
            LexoraModuleId.WASH -> Permission.VIEW_WASH
            LexoraModuleId.TIRES -> Permission.VIEW_TIRES
            else -> Permission.VIEW_HOME
        }
        return accessPolicy.can(user, permission)
    }
}
