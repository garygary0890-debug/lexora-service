package com.lexora.service.core.model

data class ModuleRegistration(
    val moduleCode: String,
    val navigationRoutes: Set<String> = emptySet(),
    val permissionCodes: Set<String> = emptySet(),
    val referenceCatalogCodes: Set<String> = emptySet(),
    val backgroundJobCodes: Set<String> = emptySet(),
    val apiContractCodes: Set<String> = emptySet(),
    val migrationKeys: Set<String> = emptySet(),
)

data class ModuleDomainEvent(
    val id: String,
    val organizationId: String,
    val sourceModuleCode: String,
    val eventType: String,
    val entityType: String,
    val entityId: String,
    val payloadVersion: Int,
    val occurredAtEpochMs: Long,
)
