package com.lexora.service.core.model

enum class AccessScopeType { ORGANIZATION, BRANCH, ENTITY_TYPE, RECORD }

data class AccessScope(
    val userId: String,
    val organizationId: String,
    val scopeType: AccessScopeType,
    val scopeId: String,
    val permissionCodes: Set<String>,
)

data class RecordOwnershipRule(
    val entityType: String,
    val ownerField: String,
    val allowOwnerRead: Boolean = true,
    val allowOwnerWrite: Boolean = false,
)
