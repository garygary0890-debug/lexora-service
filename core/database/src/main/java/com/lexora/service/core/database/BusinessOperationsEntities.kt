package com.lexora.service.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "business_suppliers", indices = [Index("organizationId"), Index(value = ["organizationId", "taxId"])])
data class BusinessSupplierEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val name: String,
    val taxId: String?,
    val phone: String?,
    val email: String?,
    val active: Boolean,
    val updatedAtEpochMs: Long,
)

@Entity(tableName = "business_purchase_orders", indices = [Index("organizationId"), Index("supplierId"), Index("destinationLocationId"), Index("status")])
data class BusinessPurchaseOrderEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val supplierId: String,
    val destinationLocationId: String,
    val status: String,
    val createdAtEpochMs: Long,
    val expectedAtEpochMs: Long?,
    val updatedAtEpochMs: Long,
)

@Entity(tableName = "business_purchase_order_lines", primaryKeys = ["purchaseOrderId", "itemId"], indices = [Index("purchaseOrderId"), Index("itemId")])
data class BusinessPurchaseOrderLineEntity(
    val purchaseOrderId: String,
    val itemId: String,
    val quantity: Double,
    val unitPriceMinor: Long?,
)

@Entity(tableName = "business_purchase_receipts", indices = [Index("organizationId"), Index("purchaseOrderId"), Index("status")])
data class BusinessPurchaseReceiptEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val purchaseOrderId: String,
    val destinationLocationId: String,
    val status: String,
    val receivedAtEpochMs: Long,
    val receivedByUserId: String,
    val note: String?,
)

@Entity(tableName = "business_purchase_receipt_lines", primaryKeys = ["receiptId", "itemId"], indices = [Index("receiptId"), Index("itemId")])
data class BusinessPurchaseReceiptLineEntity(
    val receiptId: String,
    val itemId: String,
    val quantity: Double,
)

@Entity(tableName = "business_payments", indices = [Index("organizationId"), Index("clientId"), Index("requestId"), Index("invoiceDocumentId"), Index("orderDocumentId"), Index("contractId"), Index("status")])
data class ManagedPaymentEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val clientId: String?,
    val requestId: String?,
    val invoiceDocumentId: String?,
    val orderDocumentId: String?,
    val contractId: String?,
    val amountDueMinor: Long,
    val paidMinor: Long,
    val refundedMinor: Long,
    val currency: String,
    val method: String,
    val status: String,
    val externalReference: String?,
    val note: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

@Entity(tableName = "business_payment_operations", indices = [Index("organizationId"), Index("paymentId"), Index("type"), Index("occurredAtEpochMs")])
data class PaymentOperationEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val paymentId: String,
    val type: String,
    val amountMinor: Long,
    val reason: String?,
    val actorUserId: String,
    val occurredAtEpochMs: Long,
)

@Entity(tableName = "business_tasks", indices = [Index("organizationId"), Index("requestId"), Index("clientId"), Index("documentId"), Index("assigneeUserId"), Index("assigneeEmployeeId"), Index("status"), Index("dueAtEpochMs")])
data class LinkedServiceTaskEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val title: String,
    val description: String?,
    val requestId: String?,
    val clientId: String?,
    val documentId: String?,
    val assigneeUserId: String?,
    val assigneeEmployeeId: String?,
    val dueAtEpochMs: Long?,
    val reminderAtEpochMs: Long?,
    val priority: String,
    val status: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

@Entity(tableName = "business_audit", indices = [Index("organizationId"), Index("entityType"), Index("entityId"), Index("occurredAtEpochMs")])
data class BusinessAuditEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val actorUserId: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val beforeSummary: String?,
    val afterSummary: String?,
    val reason: String?,
    val occurredAtEpochMs: Long,
)
