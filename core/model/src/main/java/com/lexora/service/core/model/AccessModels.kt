package com.lexora.service.core.model

enum class UserRole {
    ADMIN,
    MANAGER,
    DISPATCHER,
    TECHNICIAN,
    ACCOUNTANT,
    AUDITOR,
}

enum class Permission {
    VIEW_HOME,
    MANAGE_ORGANIZATION,
    MANAGE_USERS,
    MANAGE_MODULES,
    VIEW_WASH,
    VIEW_TIRES,
    MANAGE_WASH,
    MANAGE_TIRES,
    VIEW_AUDIT,
}

data class ServiceUser(
    val id: String,
    val displayName: String,
    val roles: Set<UserRole>,
    val organizationIds: Set<String>,
    val active: Boolean = true,
)
