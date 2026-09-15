package com.lexora.service.core.model

data class ModuleStoreItem(
    val moduleId: LexoraModuleId,
    val title: String,
    val description: String,
    val includedInCore: Boolean = false,
    val availableForLicensing: Boolean = true,
)

data class OrganizationModuleLicense(
    val organizationId: String,
    val moduleId: LexoraModuleId,
    val enabled: Boolean,
    val licenseStatus: ModuleLicenseStatus,
)
