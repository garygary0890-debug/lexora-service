package com.lexora.service.core.data

import com.lexora.service.core.database.AuditEventEntity
import com.lexora.service.core.database.OrganizationEntity
import com.lexora.service.core.database.ServiceDao
import com.lexora.service.core.model.Organization
import java.util.UUID

class PersistentOrganizationRepository(
    private val serviceDao: ServiceDao,
) {
    suspend fun ensureBootstrapOrganization(): Organization {
        val existing = serviceDao.organizations()
        if (existing.isNotEmpty()) {
            val active = serviceDao.activeOrganization() ?: existing.first().also {
                serviceDao.setActiveOrganization(it.id)
            }
            return active.toModel(isActive = true)
        }

        val now = System.currentTimeMillis()
        val entity = OrganizationEntity(
            id = BOOTSTRAP_ORGANIZATION_ID,
            name = "Демонстрационная организация",
            isActive = true,
            updatedAtEpochMs = now,
        )
        serviceDao.upsertOrganization(entity)
        return entity.toModel()
    }

    suspend fun organizations(): List<Organization> =
        serviceDao.organizations().map(OrganizationEntity::toModel)

    suspend fun activeOrganization(): Organization? =
        serviceDao.activeOrganization()?.toModel()

    suspend fun createOrganization(
        actorUserId: String,
        name: String,
    ): Organization {
        require(actorUserId.isNotBlank())
        val normalizedName = name.trim()
        require(normalizedName.isNotBlank()) { "Название организации обязательно." }
        require(serviceDao.organizations().none { it.name.equals(normalizedName, ignoreCase = true) }) {
            "Организация с таким названием уже существует."
        }

        val now = System.currentTimeMillis()
        val entity = OrganizationEntity(
            id = UUID.randomUUID().toString(),
            name = normalizedName,
            isActive = false,
            updatedAtEpochMs = now,
        )
        serviceDao.upsertOrganization(entity)
        serviceDao.insertAuditEvent(
            AuditEventEntity(
                id = UUID.randomUUID().toString(),
                organizationId = entity.id,
                userId = actorUserId,
                entityType = "ORGANIZATION",
                entityId = entity.id,
                action = "CREATE",
                summary = normalizedName,
                occurredAtEpochMs = now,
            ),
        )
        return entity.toModel()
    }

    suspend fun setActiveOrganization(
        actorUserId: String,
        organizationId: String,
    ): Organization {
        require(actorUserId.isNotBlank())
        val target = serviceDao.organizations().firstOrNull { it.id == organizationId }
            ?: error("Организация не найдена.")
        val previous = serviceDao.activeOrganization()
        if (previous?.id == organizationId) return target.toModel(isActive = true)

        serviceDao.setActiveOrganization(organizationId)
        val now = System.currentTimeMillis()
        serviceDao.upsertOrganization(target.copy(isActive = true, updatedAtEpochMs = now))
        serviceDao.insertAuditEvent(
            AuditEventEntity(
                id = UUID.randomUUID().toString(),
                organizationId = organizationId,
                userId = actorUserId,
                entityType = "ORGANIZATION",
                entityId = organizationId,
                action = "SET_ACTIVE",
                summary = "${previous?.name.orEmpty()} → ${target.name}",
                occurredAtEpochMs = now,
            ),
        )
        return target.toModel(isActive = true)
    }

    private fun OrganizationEntity.toModel(isActive: Boolean = this.isActive) = Organization(
        id = id,
        name = name,
        isActive = isActive,
    )

    companion object {
        const val BOOTSTRAP_ORGANIZATION_ID = "org-demo"
    }
}
