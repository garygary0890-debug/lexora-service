package com.lexora.service.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface BusinessOperationsDao {
    @Query("SELECT * FROM business_suppliers WHERE organizationId=:organizationId AND (:includeInactive=1 OR active=1) ORDER BY name")
    suspend fun suppliers(organizationId: String, includeInactive: Boolean = false): List<BusinessSupplierEntity>

    @Query("SELECT * FROM business_suppliers WHERE id=:id AND organizationId=:organizationId LIMIT 1")
    suspend fun supplier(organizationId: String, id: String): BusinessSupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSupplier(value: BusinessSupplierEntity)

    @Query("SELECT * FROM business_purchase_orders WHERE organizationId=:organizationId AND (:status IS NULL OR status=:status) ORDER BY createdAtEpochMs DESC")
    suspend fun purchaseOrders(organizationId: String, status: String? = null): List<BusinessPurchaseOrderEntity>

    @Query("SELECT * FROM business_purchase_orders WHERE id=:id AND organizationId=:organizationId LIMIT 1")
    suspend fun purchaseOrder(organizationId: String, id: String): BusinessPurchaseOrderEntity?

    @Query("SELECT * FROM business_purchase_order_lines WHERE purchaseOrderId=:purchaseOrderId ORDER BY itemId")
    suspend fun purchaseOrderLines(purchaseOrderId: String): List<BusinessPurchaseOrderLineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPurchaseOrder(value: BusinessPurchaseOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPurchaseOrderLines(values: List<BusinessPurchaseOrderLineEntity>)

    @Query("DELETE FROM business_purchase_order_lines WHERE purchaseOrderId=:purchaseOrderId")
    suspend fun deletePurchaseOrderLines(purchaseOrderId: String)

    @Transaction
    suspend fun replacePurchaseOrder(value: BusinessPurchaseOrderEntity, lines: List<BusinessPurchaseOrderLineEntity>) {
        upsertPurchaseOrder(value)
        deletePurchaseOrderLines(value.id)
        upsertPurchaseOrderLines(lines)
    }

    @Query("UPDATE business_purchase_orders SET status=:status, updatedAtEpochMs=:updatedAtEpochMs WHERE id=:id AND organizationId=:organizationId")
    suspend fun updatePurchaseOrderStatus(organizationId: String, id: String, status: String, updatedAtEpochMs: Long)

    @Query("SELECT * FROM business_purchase_receipts WHERE purchaseOrderId=:purchaseOrderId ORDER BY receivedAtEpochMs")
    suspend fun receiptsForOrder(purchaseOrderId: String): List<BusinessPurchaseReceiptEntity>

    @Query("SELECT * FROM business_purchase_receipt_lines WHERE receiptId=:receiptId ORDER BY itemId")
    suspend fun receiptLines(receiptId: String): List<BusinessPurchaseReceiptLineEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReceipt(value: BusinessPurchaseReceiptEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReceiptLines(values: List<BusinessPurchaseReceiptLineEntity>)

    @Query("UPDATE business_purchase_receipts SET status=:status WHERE id=:id")
    suspend fun updateReceiptStatus(id: String, status: String)

    @Transaction
    suspend fun insertReceiptWithLines(value: BusinessPurchaseReceiptEntity, lines: List<BusinessPurchaseReceiptLineEntity>) {
        insertReceipt(value)
        insertReceiptLines(lines)
    }

    @Query("SELECT * FROM business_payments WHERE organizationId=:organizationId ORDER BY createdAtEpochMs DESC")
    suspend fun payments(organizationId: String): List<ManagedPaymentEntity>

    @Query("SELECT * FROM business_payments WHERE id=:id AND organizationId=:organizationId LIMIT 1")
    suspend fun payment(organizationId: String, id: String): ManagedPaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPayment(value: ManagedPaymentEntity)

    @Query("SELECT * FROM business_payment_operations WHERE paymentId=:paymentId ORDER BY occurredAtEpochMs")
    suspend fun paymentOperations(paymentId: String): List<PaymentOperationEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPaymentOperation(value: PaymentOperationEntity)

    @Query("SELECT * FROM business_tasks WHERE organizationId=:organizationId AND (:status IS NULL OR status=:status) ORDER BY CASE WHEN dueAtEpochMs IS NULL THEN 1 ELSE 0 END, dueAtEpochMs, createdAtEpochMs DESC")
    suspend fun tasks(organizationId: String, status: String? = null): List<LinkedServiceTaskEntity>

    @Query("SELECT * FROM business_tasks WHERE id=:id AND organizationId=:organizationId LIMIT 1")
    suspend fun task(organizationId: String, id: String): LinkedServiceTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(value: LinkedServiceTaskEntity)

    @Query("SELECT * FROM business_audit WHERE organizationId=:organizationId ORDER BY occurredAtEpochMs DESC")
    suspend fun audit(organizationId: String): List<BusinessAuditEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAudit(value: BusinessAuditEntity)
}
