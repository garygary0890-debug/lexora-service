package com.lexora.service.core.model

enum class LexoraModuleId {
    CORE,
    WASH,
    TIRES,
    AUTO_SERVICE,
    DETAILING,
    BODY_REPAIR,
    TOW,
    STO,
    AC_SERVICE,
    CLEANING,
    APPLIANCE_REPAIR,
}

enum class ModuleLicenseStatus {
    NOT_REQUIRED,
    ACTIVE,
    EXPIRED,
    SUSPENDED,
    NOT_LICENSED,
}

data class ModuleDescriptor(
    val id: LexoraModuleId,
    val title: String,
    val enabled: Boolean,
    val licenseStatus: ModuleLicenseStatus,
) {
    val licensed: Boolean
        get() = licenseStatus == ModuleLicenseStatus.NOT_REQUIRED ||
            licenseStatus == ModuleLicenseStatus.ACTIVE
}
