package com.lexora.service.core.data

import com.lexora.service.core.database.ModuleSettingEntity
import com.lexora.service.core.database.ServiceDao
import com.lexora.service.core.model.LexoraModuleId
import com.lexora.service.core.model.ModuleDescriptor
import com.lexora.service.core.model.ModuleLicenseStatus
import com.lexora.service.core.model.ModuleStoreItem
import com.lexora.service.core.model.OrganizationModuleLicense

class ModuleLicenseRepository(
    private val serviceDao: ServiceDao,
) {
    fun storeCatalog(): List<ModuleStoreItem> = listOf(
        ModuleStoreItem(
            moduleId = LexoraModuleId.CORE,
            title = "Базовое ядро",
            description = "Организации, пользователи, клиенты, автомобили и объекты, сотрудники, документы, платежи, уведомления и аудит.",
            includedInCore = true,
            availableForLicensing = false,
        ),
        ModuleStoreItem(
            moduleId = LexoraModuleId.WASH,
            title = "Автомойка",
            description = "Посты, очередь, технологические карты и учёт расхода химии.",
        ),
        ModuleStoreItem(
            moduleId = LexoraModuleId.TIRES,
            title = "Шиномонтаж",
            description = "Очередь, диагностика, шиномонтажные работы и сезонное хранение шин.",
        ),
    )

    suspend fun ensureDefaults(organizationId: String) {
        val existing = serviceDao.moduleSettings(organizationId).associateBy { it.moduleId }
        val now = System.currentTimeMillis()
        if (existing[LexoraModuleId.CORE.name] == null) {
            serviceDao.upsertModuleSetting(
                ModuleSettingEntity(
                    organizationId = organizationId,
                    moduleId = LexoraModuleId.CORE.name,
                    enabled = true,
                    licenseStatus = ModuleLicenseStatus.NOT_REQUIRED.name,
                    updatedAtEpochMs = now,
                ),
            )
        }
        for (moduleId in listOf(LexoraModuleId.WASH, LexoraModuleId.TIRES)) {
            if (existing[moduleId.name] == null) {
                serviceDao.upsertModuleSetting(
                    ModuleSettingEntity(
                        organizationId = organizationId,
                        moduleId = moduleId.name,
                        enabled = false,
                        licenseStatus = ModuleLicenseStatus.NOT_LICENSED.name,
                        updatedAtEpochMs = now,
                    ),
                )
            }
        }
    }

    suspend fun licenses(organizationId: String): List<OrganizationModuleLicense> {
        ensureDefaults(organizationId)
        return serviceDao.moduleSettings(organizationId).mapNotNull { entity ->
            val moduleId = runCatching { LexoraModuleId.valueOf(entity.moduleId) }.getOrNull() ?: return@mapNotNull null
            val status = runCatching { ModuleLicenseStatus.valueOf(entity.licenseStatus) }.getOrDefault(ModuleLicenseStatus.NOT_LICENSED)
            OrganizationModuleLicense(
                organizationId = organizationId,
                moduleId = moduleId,
                enabled = entity.enabled,
                licenseStatus = status,
            )
        }
    }

    suspend fun descriptors(organizationId: String): List<ModuleDescriptor> {
        val licenses = licenses(organizationId).associateBy { it.moduleId }
        return storeCatalog().map { storeItem ->
            val license = licenses[storeItem.moduleId]
            ModuleDescriptor(
                id = storeItem.moduleId,
                title = storeItem.title,
                enabled = if (storeItem.moduleId == LexoraModuleId.CORE) true else license?.enabled == true,
                licenseStatus = if (storeItem.moduleId == LexoraModuleId.CORE) {
                    ModuleLicenseStatus.NOT_REQUIRED
                } else {
                    license?.licenseStatus ?: ModuleLicenseStatus.NOT_LICENSED
                },
            )
        }
    }

    suspend fun setEnabled(organizationId: String, moduleId: LexoraModuleId, enabled: Boolean) {
        require(moduleId != LexoraModuleId.CORE) { "Базовое ядро нельзя отключить" }
        val current = licenses(organizationId).firstOrNull { it.moduleId == moduleId }
            ?: error("Модуль не найден")
        require(current.licenseStatus == ModuleLicenseStatus.ACTIVE) { "Модуль нельзя включить без активной лицензии" }
        serviceDao.upsertModuleSetting(
            ModuleSettingEntity(
                organizationId = organizationId,
                moduleId = moduleId.name,
                enabled = enabled,
                licenseStatus = current.licenseStatus.name,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun applyLicenseStatus(
        organizationId: String,
        moduleId: LexoraModuleId,
        status: ModuleLicenseStatus,
    ) {
        require(moduleId != LexoraModuleId.CORE) { "Для базового ядра лицензия не требуется" }
        require(status != ModuleLicenseStatus.NOT_REQUIRED) { "NOT_REQUIRED допустим только для базового ядра" }
        val current = licenses(organizationId).firstOrNull { it.moduleId == moduleId }
            ?: error("Модуль не найден")
        val enabled = current.enabled && status == ModuleLicenseStatus.ACTIVE
        serviceDao.upsertModuleSetting(
            ModuleSettingEntity(
                organizationId = organizationId,
                moduleId = moduleId.name,
                enabled = enabled,
                licenseStatus = status.name,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }
}
