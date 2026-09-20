package com.lexora.service.core.data

import com.lexora.service.core.database.*
import com.lexora.service.core.model.*
import java.util.UUID

interface BusinessOperationsRepository {
    suspend fun suppliers(organizationId: String, includeInactive: Boolean = false): List<Supplier>
    suspend fun saveSupplier(supplier: Supplier, actorUserId: String): Supplier

    suspend fun purchaseOrders(organizationId: String, status: PurchaseOrderStatus? = null): List<PurchaseOrder>
    suspend fun savePurchaseOrder(order: PurchaseOrder, actorUserId: String): PurchaseOrder
    suspend fun changePurchaseOrderStatus(organizationId: String, purchaseOrderId: String, target: PurchaseOrderStatus, actorUserId: String): PurchaseOrder
    suspend fun receivePurchaseOrder(
        organizationId: String,
        purchaseOrderId: String,
        lines: List<PurchaseReceiptLine>,
        actorUserId: String,
        note: String? = null,
    ): PurchaseReceipt

    suspend fun payments(organizationId: String): List<ManagedPayment>
    suspend fun createPayment(payment: ManagedPayment, actorUserId: String): ManagedPayment
    suspend fun applyPayment(organizationId: String, paymentId: String, amountMinor: Long, actorUserId: String, externalReference: String? = null): ManagedPayment
    suspend fun refundPayment(organizationId: String, paymentId: String, amountMinor: Long, actorUserId: String, reason: String): ManagedPayment
    suspend fun adjustPayment(organizationId: String, paymentId: String, newAmountDueMinor: Long, actorUserId: String, reason: String): ManagedPayment
    suspend fun paymentOperations(paymentId: String): List<PaymentOperation>

    suspend fun tasks(organizationId: String, status: TaskStatus? = null): List<LinkedServiceTask>
    suspend fun saveTask(task: LinkedServiceTask, actorUserId: String): LinkedServiceTask
    suspend fun changeTaskStatus(organizationId: String, taskId: String, target: TaskStatus, actorUserId: String): LinkedServiceTask

    suspend fun audit(organizationId: String): List<BusinessAuditEvent>
}

class PersistentBusinessOperationsRepository(
    private val dao: BusinessOperationsDao,
    private val inventory: InventoryRepository,
) : BusinessOperationsRepository {
    override suspend fun suppliers(organizationId: String, includeInactive: Boolean): List<Supplier> =
        dao.suppliers(organizationId, includeInactive).map { it.toModel() }

    override suspend fun saveSupplier(supplier: Supplier, actorUserId: String): Supplier {
        require(supplier.id.isNotBlank() && supplier.organizationId.isNotBlank() && supplier.name.isNotBlank()) { "Заполните карточку поставщика" }
        val before = dao.supplier(supplier.organizationId, supplier.id)
        dao.upsertSupplier(supplier.toEntity(System.currentTimeMillis()))
        audit(supplier.organizationId, actorUserId, "SUPPLIER", supplier.id, if (before == null) "CREATE" else "UPDATE", before?.name, supplier.name, null)
        return supplier
    }

    override suspend fun purchaseOrders(organizationId: String, status: PurchaseOrderStatus?): List<PurchaseOrder> =
        dao.purchaseOrders(organizationId, status?.name).map { entity ->
            entity.toModel(dao.purchaseOrderLines(entity.id))
        }

    override suspend fun savePurchaseOrder(order: PurchaseOrder, actorUserId: String): PurchaseOrder {
        require(order.id.isNotBlank() && order.organizationId.isNotBlank()) { "Некорректный заказ поставщику" }
        require(order.lines.isNotEmpty()) { "В заказе должна быть хотя бы одна позиция" }
        require(order.lines.all { it.itemId.isNotBlank() && it.quantity > 0.0 && (it.unitPriceMinor == null || it.unitPriceMinor >= 0) }) { "Проверьте позиции заказа" }
        val supplier = dao.supplier(order.organizationId, order.supplierId) ?: error("Поставщик не найден")
        require(supplier.active) { "Поставщик неактивен" }
        require(inventory.locations(order.organizationId).any { it.id == order.destinationLocationId && it.active }) { "Склад назначения не найден или неактивен" }
        val itemIds = inventory.items(order.organizationId, includeInactive = false).map { it.id }.toSet()
        require(order.lines.all { it.itemId in itemIds }) { "В заказе есть несуществующий или неактивный материал" }

        val before = dao.purchaseOrder(order.organizationId, order.id)
        require(before == null || PurchaseOrderStatus.valueOf(before.status) == PurchaseOrderStatus.DRAFT) { "Редактировать можно только черновик заказа" }
        val now = System.currentTimeMillis()
        val normalized = order.copy(status = before?.let { PurchaseOrderStatus.valueOf(it.status) } ?: order.status)
        dao.replacePurchaseOrder(normalized.toEntity(now), normalized.lines.map { it.toEntity(normalized.id) })
        audit(order.organizationId, actorUserId, "PURCHASE_ORDER", order.id, if (before == null) "CREATE" else "UPDATE", before?.status, normalized.status.name, null)
        return normalized
    }

    override suspend fun changePurchaseOrderStatus(organizationId: String, purchaseOrderId: String, target: PurchaseOrderStatus, actorUserId: String): PurchaseOrder {
        val current = dao.purchaseOrder(organizationId, purchaseOrderId) ?: error("Заказ поставщику не найден")
        val from = PurchaseOrderStatus.valueOf(current.status)
        require(canTransitionPurchaseOrder(from, target)) { "Недопустимый переход заказа: $from → $target" }
        val now = System.currentTimeMillis()
        dao.updatePurchaseOrderStatus(organizationId, purchaseOrderId, target.name, now)
        audit(organizationId, actorUserId, "PURCHASE_ORDER", purchaseOrderId, "STATUS_CHANGE", from.name, target.name, null)
        return dao.purchaseOrder(organizationId, purchaseOrderId)!!.toModel(dao.purchaseOrderLines(purchaseOrderId))
    }

    override suspend fun receivePurchaseOrder(
        organizationId: String,
        purchaseOrderId: String,
        lines: List<PurchaseReceiptLine>,
        actorUserId: String,
        note: String?,
    ): PurchaseReceipt {
        require(lines.isNotEmpty()) { "Укажите принятые материалы" }
        require(lines.all { it.itemId.isNotBlank() && it.quantity > 0.0 }) { "Количество при приемке должно быть больше нуля" }
        val orderEntity = dao.purchaseOrder(organizationId, purchaseOrderId) ?: error("Заказ поставщику не найден")
        val orderStatus = PurchaseOrderStatus.valueOf(orderEntity.status)
        require(orderStatus == PurchaseOrderStatus.ORDERED || orderStatus == PurchaseOrderStatus.PARTIALLY_RECEIVED) { "Приемка доступна только для отправленного поставщику заказа" }
        val orderLines = dao.purchaseOrderLines(purchaseOrderId)
        val orderedByItem = orderLines.associate { it.itemId to it.quantity }
        val alreadyReceived = mutableMapOf<String, Double>()
        dao.receiptsForOrder(purchaseOrderId).filter { it.status == PurchaseReceiptStatus.POSTED.name }.forEach { receipt ->
            dao.receiptLines(receipt.id).forEach { line -> alreadyReceived[line.itemId] = (alreadyReceived[line.itemId] ?: 0.0) + line.quantity }
        }
        lines.forEach { line ->
            val ordered = orderedByItem[line.itemId] ?: error("Материал ${line.itemId} отсутствует в заказе")
            val remaining = ordered - (alreadyReceived[line.itemId] ?: 0.0)
            require(line.quantity <= remaining + EPS) { "Количество приемки превышает остаток заказа" }
        }

        val receiptId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val pending = BusinessPurchaseReceiptEntity(receiptId, organizationId, purchaseOrderId, orderEntity.destinationLocationId, PurchaseReceiptStatus.PENDING_POSTING.name, now, actorUserId, note?.trim()?.ifBlank { null })
        dao.insertReceiptWithLines(pending, lines.map { BusinessPurchaseReceiptLineEntity(receiptId, it.itemId, it.quantity) })

        try {
            lines.forEach { line ->
                inventory.recordMovement(
                    InventoryMovement(
                        id = "purchase:$receiptId:${line.itemId}",
                        organizationId = organizationId,
                        locationId = orderEntity.destinationLocationId,
                        itemId = line.itemId,
                        type = InventoryMovementType.RECEIPT,
                        quantity = line.quantity,
                        occurredAtEpochMs = now,
                        actorUserId = actorUserId,
                        note = "Приход по заказу $purchaseOrderId",
                    ),
                )
            }
            dao.updateReceiptStatus(receiptId, PurchaseReceiptStatus.POSTED.name)
        } catch (error: Throwable) {
            dao.updateReceiptStatus(receiptId, PurchaseReceiptStatus.FAILED.name)
            audit(organizationId, actorUserId, "PURCHASE_RECEIPT", receiptId, "POST_FAILED", null, error.message, note)
            throw error
        }

        lines.forEach { line -> alreadyReceived[line.itemId] = (alreadyReceived[line.itemId] ?: 0.0) + line.quantity }
        val fullyReceived = orderLines.all { (alreadyReceived[it.itemId] ?: 0.0) + EPS >= it.quantity }
        val targetStatus = if (fullyReceived) PurchaseOrderStatus.RECEIVED else PurchaseOrderStatus.PARTIALLY_RECEIVED
        dao.updatePurchaseOrderStatus(organizationId, purchaseOrderId, targetStatus.name, now)
        audit(organizationId, actorUserId, "PURCHASE_RECEIPT", receiptId, "POST", null, targetStatus.name, note)
        return PurchaseReceipt(receiptId, organizationId, purchaseOrderId, orderEntity.destinationLocationId, lines, PurchaseReceiptStatus.POSTED, now, actorUserId, note)
    }

    override suspend fun payments(organizationId: String): List<ManagedPayment> = dao.payments(organizationId).map { it.toModel() }

    override suspend fun createPayment(payment: ManagedPayment, actorUserId: String): ManagedPayment {
        require(payment.id.isNotBlank() && payment.organizationId.isNotBlank()) { "Некорректный платеж" }
        require(payment.amountDueMinor > 0) { "Сумма платежа должна быть больше нуля" }
        require(payment.paidMinor == 0L && payment.refundedMinor == 0L) { "Новый платеж должен создаваться без проведенных сумм" }
        require(payment.invoiceDocumentId != null || payment.orderDocumentId != null || payment.contractId != null) { "Платеж нужно связать со счетом, заказом или договором" }
        require(dao.payment(payment.organizationId, payment.id) == null) { "Платеж уже существует" }
        val now = System.currentTimeMillis()
        val created = payment.copy(status = ManagedPaymentStatus.PLANNED, createdAtEpochMs = payment.createdAtEpochMs.takeIf { it > 0 } ?: now, updatedAtEpochMs = now)
        dao.upsertPayment(created.toEntity())
        audit(payment.organizationId, actorUserId, "PAYMENT", payment.id, "CREATE", null, paymentSummary(created), null)
        return created
    }

    override suspend fun applyPayment(organizationId: String, paymentId: String, amountMinor: Long, actorUserId: String, externalReference: String?): ManagedPayment {
        require(amountMinor > 0) { "Сумма оплаты должна быть больше нуля" }
        val current = dao.payment(organizationId, paymentId)?.toModel() ?: error("Платеж не найден")
        require(current.status != ManagedPaymentStatus.CANCELLED && current.status != ManagedPaymentStatus.REFUNDED) { "Платеж закрыт" }
        require(amountMinor <= current.outstandingMinor) { "Сумма оплаты превышает остаток к оплате" }
        val now = System.currentTimeMillis()
        val paid = current.paidMinor + amountMinor
        val status = if (paid - current.refundedMinor >= current.amountDueMinor) ManagedPaymentStatus.PAID else ManagedPaymentStatus.PARTIALLY_PAID
        val updated = current.copy(paidMinor = paid, status = status, externalReference = externalReference?.trim()?.ifBlank { current.externalReference } ?: current.externalReference, updatedAtEpochMs = now)
        dao.upsertPayment(updated.toEntity())
        dao.insertPaymentOperation(PaymentOperationEntity(UUID.randomUUID().toString(), organizationId, paymentId, PaymentOperationType.PAYMENT.name, amountMinor, null, actorUserId, now))
        audit(organizationId, actorUserId, "PAYMENT", paymentId, "PAY", paymentSummary(current), paymentSummary(updated), null)
        return updated
    }

    override suspend fun refundPayment(organizationId: String, paymentId: String, amountMinor: Long, actorUserId: String, reason: String): ManagedPayment {
        require(amountMinor > 0) { "Сумма возврата должна быть больше нуля" }
        require(reason.isNotBlank()) { "Причина возврата обязательна" }
        val current = dao.payment(organizationId, paymentId)?.toModel() ?: error("Платеж не найден")
        val refundable = current.paidMinor - current.refundedMinor
        require(refundable > 0 && amountMinor <= refundable) { "Сумма возврата превышает доступную к возврату" }
        val now = System.currentTimeMillis()
        val refunded = current.refundedMinor + amountMinor
        val status = if (refunded >= current.paidMinor) ManagedPaymentStatus.REFUNDED else ManagedPaymentStatus.PARTIALLY_REFUNDED
        val updated = current.copy(refundedMinor = refunded, status = status, updatedAtEpochMs = now)
        dao.upsertPayment(updated.toEntity())
        dao.insertPaymentOperation(PaymentOperationEntity(UUID.randomUUID().toString(), organizationId, paymentId, PaymentOperationType.REFUND.name, amountMinor, reason.trim(), actorUserId, now))
        audit(organizationId, actorUserId, "PAYMENT", paymentId, "REFUND", paymentSummary(current), paymentSummary(updated), reason.trim())
        return updated
    }

    override suspend fun adjustPayment(organizationId: String, paymentId: String, newAmountDueMinor: Long, actorUserId: String, reason: String): ManagedPayment {
        require(newAmountDueMinor > 0) { "Новая сумма платежа должна быть больше нуля" }
        require(reason.isNotBlank()) { "Причина корректировки обязательна" }
        val current = dao.payment(organizationId, paymentId)?.toModel() ?: error("Платеж не найден")
        require(current.status != ManagedPaymentStatus.CANCELLED) { "Отмененный платеж нельзя корректировать" }
        require(newAmountDueMinor >= current.netPaidMinor) { "Новая сумма не может быть меньше фактически оплаченной после возвратов" }
        val now = System.currentTimeMillis()
        val newStatus = when {
            current.netPaidMinor == 0L -> ManagedPaymentStatus.PLANNED
            current.netPaidMinor >= newAmountDueMinor -> ManagedPaymentStatus.PAID
            else -> ManagedPaymentStatus.PARTIALLY_PAID
        }
        val updated = current.copy(amountDueMinor = newAmountDueMinor, status = newStatus, updatedAtEpochMs = now)
        dao.upsertPayment(updated.toEntity())
        val delta = newAmountDueMinor - current.amountDueMinor
        dao.insertPaymentOperation(PaymentOperationEntity(UUID.randomUUID().toString(), organizationId, paymentId, PaymentOperationType.ADJUSTMENT.name, delta, reason.trim(), actorUserId, now))
        audit(organizationId, actorUserId, "PAYMENT", paymentId, "ADJUST", paymentSummary(current), paymentSummary(updated), reason.trim())
        return updated
    }

    override suspend fun paymentOperations(paymentId: String): List<PaymentOperation> = dao.paymentOperations(paymentId).map { it.toModel() }

    override suspend fun tasks(organizationId: String, status: TaskStatus?): List<LinkedServiceTask> = dao.tasks(organizationId, status?.name).map { it.toModel() }

    override suspend fun saveTask(task: LinkedServiceTask, actorUserId: String): LinkedServiceTask {
        require(task.id.isNotBlank() && task.organizationId.isNotBlank() && task.title.isNotBlank()) { "Заполните задачу" }
        require(task.requestId != null || task.clientId != null || task.documentId != null) { "Задача должна быть связана с заявкой, клиентом или документом" }
        val before = dao.task(task.organizationId, task.id)?.toModel()
        val now = System.currentTimeMillis()
        val normalized = task.copy(createdAtEpochMs = before?.createdAtEpochMs ?: task.createdAtEpochMs.takeIf { it > 0 } ?: now, updatedAtEpochMs = now)
        dao.upsertTask(normalized.toEntity())
        audit(task.organizationId, actorUserId, "TASK", task.id, if (before == null) "CREATE" else "UPDATE", before?.let(::taskSummary), taskSummary(normalized), null)
        return normalized
    }

    override suspend fun changeTaskStatus(organizationId: String, taskId: String, target: TaskStatus, actorUserId: String): LinkedServiceTask {
        val current = dao.task(organizationId, taskId)?.toModel() ?: error("Задача не найдена")
        require(canTransitionTask(current.status, target)) { "Недопустимый переход задачи: ${current.status} → $target" }
        val updated = current.copy(status = target, updatedAtEpochMs = System.currentTimeMillis())
        dao.upsertTask(updated.toEntity())
        audit(organizationId, actorUserId, "TASK", taskId, "STATUS_CHANGE", current.status.name, target.name, null)
        return updated
    }

    override suspend fun audit(organizationId: String): List<BusinessAuditEvent> = dao.audit(organizationId).map { it.toModel() }

    private suspend fun audit(organizationId: String, actorUserId: String, entityType: String, entityId: String, action: String, before: String?, after: String?, reason: String?) {
        dao.insertAudit(BusinessAuditEntity(UUID.randomUUID().toString(), organizationId, actorUserId, entityType, entityId, action, before, after, reason, System.currentTimeMillis()))
    }

    companion object {
        private const val EPS = 0.000001
    }
}

private fun canTransitionPurchaseOrder(from: PurchaseOrderStatus, to: PurchaseOrderStatus): Boolean = when (from) {
    PurchaseOrderStatus.DRAFT -> to == PurchaseOrderStatus.APPROVED || to == PurchaseOrderStatus.CANCELLED
    PurchaseOrderStatus.APPROVED -> to == PurchaseOrderStatus.ORDERED || to == PurchaseOrderStatus.CANCELLED
    PurchaseOrderStatus.ORDERED -> to == PurchaseOrderStatus.PARTIALLY_RECEIVED || to == PurchaseOrderStatus.RECEIVED || to == PurchaseOrderStatus.CANCELLED
    PurchaseOrderStatus.PARTIALLY_RECEIVED -> to == PurchaseOrderStatus.RECEIVED || to == PurchaseOrderStatus.CANCELLED
    PurchaseOrderStatus.RECEIVED, PurchaseOrderStatus.CANCELLED -> false
}

private fun canTransitionTask(from: TaskStatus, to: TaskStatus): Boolean = from != to && when (from) {
    TaskStatus.OPEN -> to == TaskStatus.IN_PROGRESS || to == TaskStatus.DONE || to == TaskStatus.CANCELLED
    TaskStatus.IN_PROGRESS -> to == TaskStatus.OPEN || to == TaskStatus.DONE || to == TaskStatus.CANCELLED
    TaskStatus.DONE -> to == TaskStatus.OPEN
    TaskStatus.CANCELLED -> to == TaskStatus.OPEN
}

private fun BusinessSupplierEntity.toModel() = Supplier(id, organizationId, name, taxId, phone, email, active)
private fun Supplier.toEntity(now: Long) = BusinessSupplierEntity(id, organizationId, name.trim(), taxId?.trim()?.ifBlank { null }, phone?.trim()?.ifBlank { null }, email?.trim()?.ifBlank { null }, active, now)
private fun BusinessPurchaseOrderEntity.toModel(lines: List<BusinessPurchaseOrderLineEntity>) = PurchaseOrder(id, organizationId, supplierId, destinationLocationId, PurchaseOrderStatus.valueOf(status), lines.map { PurchaseOrderLine(it.itemId, it.quantity, it.unitPriceMinor) }, createdAtEpochMs, expectedAtEpochMs)
private fun PurchaseOrder.toEntity(now: Long) = BusinessPurchaseOrderEntity(id, organizationId, supplierId, destinationLocationId, status.name, createdAtEpochMs.takeIf { it > 0 } ?: now, expectedAtEpochMs, now)
private fun PurchaseOrderLine.toEntity(orderId: String) = BusinessPurchaseOrderLineEntity(orderId, itemId, quantity, unitPriceMinor)
private fun ManagedPaymentEntity.toModel() = ManagedPayment(id, organizationId, clientId, requestId, invoiceDocumentId, orderDocumentId, contractId, amountDueMinor, paidMinor, refundedMinor, currency, PaymentMethod.valueOf(method), ManagedPaymentStatus.valueOf(status), externalReference, note, createdAtEpochMs, updatedAtEpochMs)
private fun ManagedPayment.toEntity() = ManagedPaymentEntity(id, organizationId, clientId, requestId, invoiceDocumentId, orderDocumentId, contractId, amountDueMinor, paidMinor, refundedMinor, currency, method.name, status.name, externalReference, note, createdAtEpochMs, updatedAtEpochMs)
private fun PaymentOperationEntity.toModel() = PaymentOperation(id, organizationId, paymentId, PaymentOperationType.valueOf(type), amountMinor, reason, actorUserId, occurredAtEpochMs)
private fun LinkedServiceTaskEntity.toModel() = LinkedServiceTask(id, organizationId, title, description, requestId, clientId, documentId, assigneeUserId, assigneeEmployeeId, dueAtEpochMs, reminderAtEpochMs, RequestPriority.valueOf(priority), TaskStatus.valueOf(status), createdAtEpochMs, updatedAtEpochMs)
private fun LinkedServiceTask.toEntity() = LinkedServiceTaskEntity(id, organizationId, title.trim(), description?.trim()?.ifBlank { null }, requestId, clientId, documentId, assigneeUserId, assigneeEmployeeId, dueAtEpochMs, reminderAtEpochMs, priority.name, status.name, createdAtEpochMs, updatedAtEpochMs)
private fun BusinessAuditEntity.toModel() = BusinessAuditEvent(id, organizationId, actorUserId, entityType, entityId, action, beforeSummary, afterSummary, reason, occurredAtEpochMs)
private fun paymentSummary(value: ManagedPayment) = "due=${value.amountDueMinor};paid=${value.paidMinor};refunded=${value.refundedMinor};status=${value.status.name};invoice=${value.invoiceDocumentId.orEmpty()};order=${value.orderDocumentId.orEmpty()};contract=${value.contractId.orEmpty()}"
private fun taskSummary(value: LinkedServiceTask) = "${value.title};status=${value.status.name};request=${value.requestId.orEmpty()};client=${value.clientId.orEmpty()};document=${value.documentId.orEmpty()}"
