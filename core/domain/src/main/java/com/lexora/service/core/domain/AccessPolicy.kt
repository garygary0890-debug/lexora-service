package com.lexora.service.core.domain

import com.lexora.service.core.model.Permission
import com.lexora.service.core.model.ServiceUser
import com.lexora.service.core.model.UserRole

class AccessPolicy {
    fun permissionsFor(user: ServiceUser): Set<Permission> = buildSet {
        add(Permission.VIEW_HOME)
        user.roles.forEach { role ->
            when (role) {
                UserRole.ADMIN -> addAll(Permission.entries)
                UserRole.MANAGER -> addAll(
                    setOf(
                        Permission.VIEW_WASH,
                        Permission.VIEW_TIRES,
                        Permission.MANAGE_WASH,
                        Permission.MANAGE_TIRES,
                        Permission.VIEW_AUDIT,
                    )
                )
                UserRole.DISPATCHER -> addAll(
                    setOf(
                        Permission.VIEW_WASH,
                        Permission.VIEW_TIRES,
                        Permission.MANAGE_WASH,
                        Permission.MANAGE_TIRES,
                    )
                )
                UserRole.TECHNICIAN -> addAll(
                    setOf(Permission.VIEW_WASH, Permission.VIEW_TIRES)
                )
                UserRole.ACCOUNTANT -> addAll(
                    setOf(Permission.VIEW_WASH, Permission.VIEW_TIRES)
                )
                UserRole.AUDITOR -> add(Permission.VIEW_AUDIT)
            }
        }
    }

    fun can(user: ServiceUser, permission: Permission): Boolean =
        permission in permissionsFor(user)
}
