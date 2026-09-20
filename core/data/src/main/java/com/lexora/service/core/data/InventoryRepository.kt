package com.lexora.service.core.data

import com.lexora.service.core.model.InventoryBalance
import com.lexora.service.core.model.InventoryItem
import com.lexora.service.core.model.InventoryLocation
import com.lexora.service.core.model.InventoryMovement

/**
 * Persistent warehouse contract.
 *
 * All quantity-changing operations go through [recordMovement], which validates stock
 * invariants and applies the movement atomically inside the warehouse database.
 */
interface InventoryRepository {
    suspend fun locations(organizationId: String, branchId: String? = null): List<InventoryLocation>
    suspend fun saveLocation(location: InventoryLocation): InventoryLocation

    suspend fun items(organizationId: String, includeInactive: Boolean = false): List<InventoryItem>
    suspend fun saveItem(item: InventoryItem): InventoryItem

    suspend fun balances(organizationId: String, locationId: String? = null): List<InventoryBalance>

    /** On-hand minus reserved quantity. Never returned as a negative value. */
    suspend fun availableQuantity(organizationId: String, locationId: String, itemId: String): Double

    suspend fun movements(organizationId: String, itemId: String? = null, locationId: String? = null): List<InventoryMovement>

    /**
     * Supports receipt, issue, write-off, transfer, reservation, reservation release and adjustment.
     * Implementations reject zero/negative quantities, prevent negative on-hand stock and prevent
     * reserved stock from exceeding on-hand stock.
     */
    suspend fun recordMovement(movement: InventoryMovement): InventoryMovement

    suspend fun lowStockItems(organizationId: String, locationId: String? = null): List<InventoryBalance>
}
