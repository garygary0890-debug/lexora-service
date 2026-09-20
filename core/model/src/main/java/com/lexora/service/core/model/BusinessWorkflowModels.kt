package com.lexora.service.core.model

enum class PurchaseReceiptStatus { PENDING_POSTING, POSTED, FAILED }

data class PurchaseReceiptLine(
    val itemId: String,
    val quantity: Double,
)

data class PurchaseReceipt(
    val id: String,
    val organizationId: String,
    val purchaseOrderId: String,
    val destinationLocationId: String,
    val lines: List<PurchaseReceiptLine>,
    val status: PurchaseReceiptStatus,
    val receivedAtEpochMs: Long,
    val receivedByUserId: String,
    val note: String? = null,
)

enum class ManagedPaymentStatus {
    PLANNED,
    PARTIALLY_PAID,
    PAID,
    PARTIALLY_REFUNDED,
    REFUNDED,
    CANCELLED,
}

enum class PaymentOperationType { PAYMENT, REFUND, ADJUSTMENT }

data class ManagedPayment(
    val id: String,
    val organizationId: String,
    val clientId: String? = null,
    val requestId: String? = null,
    val invoiceDocumentId: String? = null,
    val orderDocumentId: String? = null,
    val contractId: String? = null,
    val amountDueMinor: Long,
    val paidMinor: Long = 0,
    val refundedMinor: Long = 0,
    val currency: String = "RUB",
    val method: PaymentMethod = PaymentMethod.BANK_TRANSFER,
    val status: ManagedPaymentStatus = ManagedPaymentStatus.PLANNED,
    val externalReference: String? = null,
    val note: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
) {
    val netPaidMinor: Long get() = paidMinor - refundedMinor
    val outstandingMinor: Long get() = (amountDueMinor - netPaidMinor).coerceAtLeast(0)
}

data class PaymentOperation(
    val id: String,
    val organizationId: String,
    val paymentId: String,
    val type: PaymentOperationType,
    val amountMinor: Long,
    val reason: String? = null,
    val actorUserId: String,
    val occurredAtEpochMs: Long,
)

data class BusinessAuditEvent(
    val id: String,
    val organizationId: String,
    val actorUserId: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val beforeSummary: String? = null,
    val afterSummary: String? = null,
    val reason: String? = null,
    val occurredAtEpochMs: Long,
)

data class LinkedServiceTask(
    val id: String,
    val organizationId: String,
    val title: String,
    val description: String? = null,
    val requestId: String? = null,
    val clientId: String? = null,
    val documentId: String? = null,
    val assigneeUserId: String? = null,
    val assigneeEmployeeId: String? = null,
    val dueAtEpochMs: Long? = null,
    val reminderAtEpochMs: Long? = null,
    val priority: RequestPriority = RequestPriority.NORMAL,
    val status: TaskStatus = TaskStatus.OPEN,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
