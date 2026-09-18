package com.lexora.service.core.data

import com.lexora.service.core.model.InventoryBalance
import com.lexora.service.core.model.InventoryItem
import com.lexora.service.core.model.InventoryLocation
import com.lexora.service.core.model.InventoryMovement

/** SRV-000085 — advanced inventory foundation from Lexora Service post-MVP scope. */
interface InventoryRepository {
    suspend fun locations(organizationId: String, branchId: String? = null): List<InventoryLocation>
    suspend fun items(organizationId: String, includeInactive: Boolean = false): List<InventoryItem>
    suspend fun balances(organizationId: String, locationId: String? = null): List<InventoryBalance>
    suspend fun movements(organizationId: String, itemId: String? = null, locationId: String? = null): List<InventoryMovement>
    suspend fun recordMovement(movement: InventoryMovement): InventoryMovement
    suspend fun lowStockItems(organizationId: String, locationId: String? = null): List<InventoryBalance>
}
