package com.lexora.service.core.data

import com.lexora.service.core.database.InventoryBalanceEntity
import com.lexora.service.core.database.InventoryDao
import com.lexora.service.core.database.InventoryItemEntity
import com.lexora.service.core.database.InventoryLocationEntity
import com.lexora.service.core.database.InventoryMovementEntity
import com.lexora.service.core.model.InventoryBalance
import com.lexora.service.core.model.InventoryItem
import com.lexora.service.core.model.InventoryLocation
import com.lexora.service.core.model.InventoryMovement
import com.lexora.service.core.model.InventoryMovementType

class PersistentInventoryRepository(private val dao: InventoryDao) : InventoryRepository {
    override suspend fun locations(organizationId: String, branchId: String?): List<InventoryLocation> =
        dao.locations(organizationId, branchId).map { it.toModel() }

    override suspend fun saveLocation(location: InventoryLocation): InventoryLocation {
        require(location.id.isNotBlank() && location.organizationId.isNotBlank() && location.name.isNotBlank()) { "Заполните склад" }
        dao.upsertLocation(location.toEntity())
        return location
    }

    override suspend fun items(organizationId: String, includeInactive: Boolean): List<InventoryItem> =
        dao.items(organizationId, includeInactive).map { it.toModel() }

    override suspend fun saveItem(item: InventoryItem): InventoryItem {
        require(item.id.isNotBlank() && item.organizationId.isNotBlank() && item.sku.isNotBlank() && item.name.isNotBlank() && item.unit.isNotBlank()) { "Заполните карточку материала" }
        val minimumStock = item.minimumStock
        require(minimumStock == null || minimumStock >= 0.0) { "Минимальный остаток не может быть отрицательным" }
        dao.upsertItem(item.toEntity())
        return item
    }

    override suspend fun balances(organizationId: String, locationId: String?): List<InventoryBalance> =
        dao.balances(organizationId, locationId).map { it.toModel() }

    override suspend fun availableQuantity(organizationId: String, locationId: String, itemId: String): Double {
        val balance = dao.balance(organizationId, locationId, itemId) ?: return 0.0
        return (balance.quantity - balance.reservedQuantity).coerceAtLeast(0.0)
    }

    override suspend fun movements(organizationId: String, itemId: String?, locationId: String?): List<InventoryMovement> =
        dao.movements(organizationId, itemId, locationId).map { it.toModel() }

    override suspend fun recordMovement(movement: InventoryMovement): InventoryMovement {
        require(movement.id.isNotBlank() && movement.organizationId.isNotBlank() && movement.locationId.isNotBlank() && movement.itemId.isNotBlank()) { "Некорректное складское движение" }
        require(movement.quantity > 0.0) { "Количество должно быть больше нуля" }
        dao.applyMovement(movement.toEntity())
        return movement
    }

    override suspend fun lowStockItems(organizationId: String, locationId: String?): List<InventoryBalance> {
        val items = dao.items(organizationId, includeInactive = false).associateBy { it.id }
        return dao.balances(organizationId, locationId)
            .filter { balance -> items[balance.itemId]?.minimumStock?.let { balance.quantity - balance.reservedQuantity <= it } == true }
            .map { it.toModel() }
    }
}

private fun InventoryLocationEntity.toModel() = InventoryLocation(id, organizationId, branchId, name, active)
private fun InventoryLocation.toEntity() = InventoryLocationEntity(id, organizationId, branchId, name.trim(), active)
private fun InventoryItemEntity.toModel() = InventoryItem(id, organizationId, sku, name, unit, minimumStock, active)
private fun InventoryItem.toEntity() = InventoryItemEntity(id, organizationId, sku.trim(), name.trim(), unit.trim(), minimumStock, active)
private fun InventoryBalanceEntity.toModel() = InventoryBalance(organizationId, locationId, itemId, quantity, reservedQuantity)
private fun InventoryMovementEntity.toModel() = InventoryMovement(id, organizationId, locationId, targetLocationId, itemId, requestId, workOrderDocumentId, InventoryMovementType.valueOf(type), quantity, occurredAtEpochMs, actorUserId, note)
private fun InventoryMovement.toEntity() = InventoryMovementEntity(id, organizationId, locationId, targetLocationId, itemId, requestId, workOrderDocumentId, type.name, quantity, occurredAtEpochMs, actorUserId, note?.trim()?.ifBlank { null })
