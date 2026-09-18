package com.lexora.service.core.data

import com.lexora.service.core.model.PurchaseOrder
import com.lexora.service.core.model.PurchaseOrderStatus
import com.lexora.service.core.model.Supplier

/** SRV-000086 — purchasing and suppliers foundation. */
interface ProcurementRepository {
    suspend fun suppliers(organizationId: String, includeInactive: Boolean = false): List<Supplier>
    suspend fun saveSupplier(supplier: Supplier): Supplier
    suspend fun purchaseOrders(organizationId: String, status: PurchaseOrderStatus? = null): List<PurchaseOrder>
    suspend fun savePurchaseOrder(order: PurchaseOrder): PurchaseOrder
    suspend fun changeStatus(organizationId: String, purchaseOrderId: String, status: PurchaseOrderStatus): PurchaseOrder
}
