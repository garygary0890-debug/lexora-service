package com.lexora.service.core.model

enum class ModuleAvailability { AVAILABLE, INSTALLED, LICENSE_REQUIRED, UNAVAILABLE }

data class ModuleCatalogItem(
    val moduleCode: String,
    val name: String,
    val description: String,
    val category: String,
    val availability: ModuleAvailability,
    val version: String? = null,
    val requiresLicense: Boolean = true,
    val dependencies: Set<String> = emptySet(),
)

data class OrganizationModuleSelection(
    val organizationId: String,
    val moduleCode: String,
    val enabled: Boolean,
    val changedByUserId: String,
    val changedAtEpochMs: Long,
)
