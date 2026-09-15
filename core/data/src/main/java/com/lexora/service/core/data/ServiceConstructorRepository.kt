package com.lexora.service.core.data

import com.lexora.service.core.database.AuditEventEntity
import com.lexora.service.core.database.ServiceConstructorDao
import com.lexora.service.core.database.ServiceDao
import com.lexora.service.core.database.ServiceRecipeComponentEntity
import com.lexora.service.core.database.ServiceRecipeEntity
import com.lexora.service.core.model.ServiceComponentType
import com.lexora.service.core.model.ServiceRecipe
import com.lexora.service.core.model.ServiceRecipeComponent
import com.lexora.service.core.model.ServiceRecipeSummary
import com.lexora.service.core.model.SyncState
import java.util.UUID

class ServiceConstructorRepository(
    private val dao: ServiceConstructorDao,
    private val serviceDao: ServiceDao,
) {
    suspend fun recipes(organizationId: String): List<ServiceRecipeSummary> =
        dao.recipes(organizationId).map { recipe ->
            ServiceRecipeSummary(
                recipe = recipe.toModel(),
                components = dao.components(recipe.id).map(ServiceRecipeComponentEntity::toModel),
            )
        }

    suspend fun ensureRecipe(
        organizationId: String,
        userId: String,
        serviceCatalogItemId: String,
        serviceName: String,
        durationMinutes: Int?,
    ): ServiceRecipeSummary {
        val existing = dao.recipeForService(organizationId, serviceCatalogItemId)
        if (existing != null) {
            return ServiceRecipeSummary(existing.toModel(), dao.components(existing.id).map(ServiceRecipeComponentEntity::toModel))
        }
        val now = System.currentTimeMillis()
        val entity = ServiceRecipeEntity(
            id = UUID.randomUUID().toString(),
            organizationId = organizationId,
            serviceCatalogItemId = serviceCatalogItemId,
            name = serviceName,
            description = null,
            durationMinutes = durationMinutes,
            active = true,
            syncState = SyncState.PENDING_CREATE.name,
            updatedAtEpochMs = now,
        )
        dao.upsertRecipe(entity)
        audit(organizationId, userId, "SERVICE_RECIPE", entity.id, "CREATE", serviceName, now)
        return ServiceRecipeSummary(entity.toModel(), emptyList())
    }

    suspend fun addComponent(
        organizationId: String,
        userId: String,
        recipeId: String,
        type: ServiceComponentType,
        name: String,
        quantity: Double,
        unit: String,
        unitCostMinor: Long,
        code: String? = null,
    ): ServiceRecipeComponent {
        require(name.isNotBlank())
        require(quantity > 0.0)
        require(unitCostMinor >= 0L)
        val recipe = requireNotNull(dao.recipe(recipeId))
        require(recipe.organizationId == organizationId)
        val existing = dao.components(recipeId)
        val now = System.currentTimeMillis()
        val entity = ServiceRecipeComponentEntity(
            id = UUID.randomUUID().toString(),
            organizationId = organizationId,
            recipeId = recipeId,
            type = type.name,
            code = code?.trim()?.ifBlank { null },
            name = name.trim(),
            quantity = quantity,
            unit = unit.trim().ifBlank { "шт." },
            unitCostMinor = unitCostMinor,
            sortOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1,
            active = true,
            syncState = SyncState.PENDING_CREATE.name,
            updatedAtEpochMs = now,
        )
        dao.upsertComponent(entity)
        audit(organizationId, userId, "SERVICE_RECIPE_COMPONENT", entity.id, "CREATE", "${type.name}: ${entity.name}", now)
        return entity.toModel()
    }

    suspend fun setRecipeActive(organizationId: String, userId: String, id: String, active: Boolean) {
        val current = requireNotNull(dao.recipe(id))
        require(current.organizationId == organizationId)
        val now = System.currentTimeMillis()
        dao.setRecipeActive(id, active, now)
        audit(organizationId, userId, "SERVICE_RECIPE", id, if (active) "ACTIVATE" else "DEACTIVATE", current.name, now)
    }

    suspend fun setComponentActive(organizationId: String, userId: String, id: String, active: Boolean) {
        val current = requireNotNull(dao.component(id))
        require(current.organizationId == organizationId)
        val now = System.currentTimeMillis()
        dao.setComponentActive(id, active, now)
        audit(organizationId, userId, "SERVICE_RECIPE_COMPONENT", id, if (active) "ACTIVATE" else "DEACTIVATE", current.name, now)
    }

    private suspend fun audit(
        organizationId: String,
        userId: String,
        entityType: String,
        entityId: String,
        action: String,
        summary: String,
        now: Long,
    ) {
        serviceDao.insertAuditEvent(
            AuditEventEntity(
                id = UUID.randomUUID().toString(),
                organizationId = organizationId,
                userId = userId,
                entityType = entityType,
                entityId = entityId,
                action = action,
                summary = summary,
                occurredAtEpochMs = now,
            ),
        )
    }
}

private fun ServiceRecipeEntity.toModel() = ServiceRecipe(
    id = id,
    organizationId = organizationId,
    serviceCatalogItemId = serviceCatalogItemId,
    name = name,
    description = description,
    durationMinutes = durationMinutes,
    active = active,
    syncState = SyncState.valueOf(syncState),
)

private fun ServiceRecipeComponentEntity.toModel() = ServiceRecipeComponent(
    id = id,
    organizationId = organizationId,
    recipeId = recipeId,
    type = ServiceComponentType.valueOf(type),
    code = code,
    name = name,
    quantity = quantity,
    unit = unit,
    unitCostMinor = unitCostMinor,
    sortOrder = sortOrder,
    active = active,
    syncState = SyncState.valueOf(syncState),
)
