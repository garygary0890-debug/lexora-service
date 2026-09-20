package com.lexora.service.feature.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexora.service.core.data.BusinessOperationsRuntimeDependencies
import com.lexora.service.core.model.LinkedServiceTask
import com.lexora.service.core.model.ManagedPayment
import com.lexora.service.core.model.ManagedPaymentStatus
import com.lexora.service.core.model.PaymentMethod
import com.lexora.service.core.model.PurchaseOrder
import com.lexora.service.core.model.PurchaseOrderLine
import com.lexora.service.core.model.PurchaseOrderStatus
import com.lexora.service.core.model.PurchaseReceiptLine
import com.lexora.service.core.model.RequestPriority
import com.lexora.service.core.model.Supplier
import com.lexora.service.core.model.TaskStatus
import kotlinx.coroutines.launch
import java.util.UUID

private enum class BusinessTab { TASKS, PURCHASES, PAYMENTS }

@Composable
internal fun BusinessOperationsPanel(organizationId: String) {
    if (organizationId.isBlank()) return
    val repository = remember { BusinessOperationsRuntimeDependencies.repository() }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(BusinessTab.TASKS) }
    var suppliers by remember { mutableStateOf(emptyList<Supplier>()) }
    var orders by remember { mutableStateOf(emptyList<PurchaseOrder>()) }
    var payments by remember { mutableStateOf(emptyList<ManagedPayment>()) }
    var tasks by remember { mutableStateOf(emptyList<LinkedServiceTask>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var operationMessage by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        suppliers = repository.suppliers(organizationId, includeInactive = true)
        orders = repository.purchaseOrders(organizationId)
        payments = repository.payments(organizationId)
        tasks = repository.tasks(organizationId)
    }

    fun runAction(block: suspend (String) -> Unit) {
        scope.launch {
            runCatching { block(BusinessOperationsRuntimeDependencies.actorUserId()); reload() }
                .onSuccess { error = null }
                .onFailure { error = it.message ?: "Операция не выполнена" }
        }
    }

    LaunchedEffect(organizationId) {
        runCatching { reload() }.onFailure { error = it.message }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Задачи, закупки и платежи", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(tab == BusinessTab.TASKS, { tab = BusinessTab.TASKS }, label = { Text("Задачи") })
            FilterChip(tab == BusinessTab.PURCHASES, { tab = BusinessTab.PURCHASES }, label = { Text("Закупки") })
            FilterChip(tab == BusinessTab.PAYMENTS, { tab = BusinessTab.PAYMENTS }, label = { Text("Платежи") })
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        operationMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

        when (tab) {
            BusinessTab.TASKS -> TasksPanel(organizationId, tasks) { action -> runAction(action) }
            BusinessTab.PURCHASES -> PurchasesPanel(organizationId, suppliers, orders) { action -> runAction(action) }
            BusinessTab.PAYMENTS -> PaymentsPanel(organizationId, payments, { message -> operationMessage = message }) { action -> runAction(action) }
        }
    }
}

@Composable
private fun TasksPanel(
    organizationId: String,
    tasks: List<LinkedServiceTask>,
    runAction: ((suspend (String) -> Unit)) -> Unit,
) {
    val repository = remember { BusinessOperationsRuntimeDependencies.repository() }
    var title by remember { mutableStateOf("") }
    var requestId by remember { mutableStateOf("") }
    var clientId by remember { mutableStateOf("") }
    var documentId by remember { mutableStateOf("") }

    Text("Новая задача", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(title, { title = it }, label = { Text("Что нужно сделать") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(requestId, { requestId = it }, label = { Text("ID заявки — необязательно") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(clientId, { clientId = it }, label = { Text("ID клиента — необязательно") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(documentId, { documentId = it }, label = { Text("ID документа — необязательно") }, modifier = Modifier.fillMaxWidth())
    val hasLink = requestId.isNotBlank() || clientId.isNotBlank() || documentId.isNotBlank()
    Button(
        onClick = {
            val now = System.currentTimeMillis()
            val task = LinkedServiceTask(
                id = UUID.randomUUID().toString(), organizationId = organizationId, title = title.trim(),
                requestId = requestId.trim().ifBlank { null }, clientId = clientId.trim().ifBlank { null },
                documentId = documentId.trim().ifBlank { null }, priority = RequestPriority.NORMAL,
                createdAtEpochMs = now, updatedAtEpochMs = now,
            )
            runAction { actor -> repository.saveTask(task, actor) }
            title = ""; requestId = ""; clientId = ""; documentId = ""
        },
        enabled = title.isNotBlank() && hasLink,
    ) { Text("Создать задачу") }

    Text("Все задачи", style = MaterialTheme.typography.titleMedium)
    if (tasks.isEmpty()) Text("Задач пока нет")
    tasks.forEach { task ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(task.title, style = MaterialTheme.typography.titleSmall)
                Text("Статус: ${task.status.name} · Приоритет: ${task.priority.name}")
                Text(listOfNotNull(task.requestId?.let { "заявка $it" }, task.clientId?.let { "клиент $it" }, task.documentId?.let { "документ $it" }).joinToString(" · "))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (task.status != TaskStatus.IN_PROGRESS) OutlinedButton(onClick = { runAction { actor -> repository.changeTaskStatus(organizationId, task.id, TaskStatus.IN_PROGRESS, actor) } }) { Text("В работу") }
                    if (task.status != TaskStatus.DONE) OutlinedButton(onClick = { runAction { actor -> repository.changeTaskStatus(organizationId, task.id, TaskStatus.DONE, actor) } }) { Text("Готово") }
                    if (task.status != TaskStatus.CANCELLED) OutlinedButton(onClick = { runAction { actor -> repository.changeTaskStatus(organizationId, task.id, TaskStatus.CANCELLED, actor) } }) { Text("Отменить") }
                }
            }
        }
    }
}

@Composable
private fun PurchasesPanel(
    organizationId: String,
    suppliers: List<Supplier>,
    orders: List<PurchaseOrder>,
    runAction: ((suspend (String) -> Unit)) -> Unit,
) {
    val repository = remember { BusinessOperationsRuntimeDependencies.repository() }
    var supplierName by remember { mutableStateOf("") }
    var supplierTaxId by remember { mutableStateOf("") }
    Text("Поставщики", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(supplierName, { supplierName = it }, label = { Text("Наименование поставщика") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(supplierTaxId, { supplierTaxId = it }, label = { Text("ИНН — необязательно") }, modifier = Modifier.fillMaxWidth())
    Button(onClick = {
        val supplier = Supplier(UUID.randomUUID().toString(), organizationId, supplierName.trim(), supplierTaxId.trim().ifBlank { null })
        runAction { actor -> repository.saveSupplier(supplier, actor) }
        supplierName = ""; supplierTaxId = ""
    }, enabled = supplierName.isNotBlank()) { Text("Добавить поставщика") }
    suppliers.forEach { Text("• ${it.name}${it.taxId?.let { tax -> " · ИНН $tax" }.orEmpty()}") }

    var supplierId by remember { mutableStateOf("") }
    var destinationLocationId by remember { mutableStateOf("") }
    var itemId by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unitPrice by remember { mutableStateOf("") }
    Text("Новый заказ поставщику", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(supplierId, { supplierId = it }, label = { Text("ID поставщика") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(destinationLocationId, { destinationLocationId = it }, label = { Text("ID склада назначения") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(itemId, { itemId = it }, label = { Text("ID материала") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(quantity, { quantity = it }, label = { Text("Количество") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(unitPrice, { unitPrice = it }, label = { Text("Цена за единицу, коп.") }, modifier = Modifier.fillMaxWidth())
    Button(onClick = {
        val now = System.currentTimeMillis()
        val order = PurchaseOrder(
            id = UUID.randomUUID().toString(), organizationId = organizationId,
            supplierId = supplierId.trim(), destinationLocationId = destinationLocationId.trim(),
            lines = listOf(PurchaseOrderLine(itemId.trim(), quantity.toDoubleOrNull() ?: 0.0, unitPrice.toLongOrNull())),
            createdAtEpochMs = now,
        )
        runAction { actor -> repository.savePurchaseOrder(order, actor) }
        itemId = ""; quantity = "1"; unitPrice = ""
    }, enabled = supplierId.isNotBlank() && destinationLocationId.isNotBlank() && itemId.isNotBlank() && (quantity.toDoubleOrNull() ?: 0.0) > 0) { Text("Создать заказ") }

    Text("Заказы поставщикам", style = MaterialTheme.typography.titleMedium)
    if (orders.isEmpty()) Text("Заказов пока нет")
    orders.forEach { order ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${order.id.take(8)} · ${order.status.name}", style = MaterialTheme.typography.titleSmall)
                Text("Поставщик: ${order.supplierId} · Склад: ${order.destinationLocationId}")
                order.lines.forEach { Text("• ${it.itemId}: ${it.quantity} × ${it.unitPriceMinor ?: 0} коп.") }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (order.status == PurchaseOrderStatus.DRAFT) OutlinedButton(onClick = { runAction { actor -> repository.changePurchaseOrderStatus(organizationId, order.id, PurchaseOrderStatus.APPROVED, actor) } }) { Text("Согласовать") }
                    if (order.status == PurchaseOrderStatus.APPROVED) OutlinedButton(onClick = { runAction { actor -> repository.changePurchaseOrderStatus(organizationId, order.id, PurchaseOrderStatus.ORDERED, actor) } }) { Text("Отправить") }
                }
                if (order.status == PurchaseOrderStatus.ORDERED || order.status == PurchaseOrderStatus.PARTIALLY_RECEIVED) {
                    var receiveQty by remember(order.id) { mutableStateOf("1") }
                    OutlinedTextField(receiveQty, { receiveQty = it }, label = { Text("Принять количество первой позиции") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        val first = order.lines.firstOrNull() ?: return@Button
                        runAction { actor -> repository.receivePurchaseOrder(organizationId, order.id, listOf(PurchaseReceiptLine(first.itemId, receiveQty.toDoubleOrNull() ?: 0.0)), actor, "Приемка через раздел закупок") }
                    }, enabled = (receiveQty.toDoubleOrNull() ?: 0.0) > 0 && order.lines.isNotEmpty()) { Text("Оприходовать") }
                }
            }
        }
    }
}

@Composable
private fun PaymentsPanel(
    organizationId: String,
    payments: List<ManagedPayment>,
    onMessage: (String?) -> Unit,
    runAction: ((suspend (String) -> Unit)) -> Unit,
) {
    val repository = remember { BusinessOperationsRuntimeDependencies.repository() }
    var amount by remember { mutableStateOf("") }
    var invoiceId by remember { mutableStateOf("") }
    var orderId by remember { mutableStateOf("") }
    var contractId by remember { mutableStateOf("") }
    Text("Новый платеж", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(amount, { amount = it }, label = { Text("Сумма к оплате, коп.") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(invoiceId, { invoiceId = it }, label = { Text("ID счета — необязательно") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(orderId, { orderId = it }, label = { Text("ID заказ-наряда — необязательно") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(contractId, { contractId = it }, label = { Text("ID договора — необязательно") }, modifier = Modifier.fillMaxWidth())
    val linked = invoiceId.isNotBlank() || orderId.isNotBlank() || contractId.isNotBlank()
    Button(onClick = {
        val now = System.currentTimeMillis()
        val payment = ManagedPayment(
            id = UUID.randomUUID().toString(), organizationId = organizationId,
            invoiceDocumentId = invoiceId.trim().ifBlank { null }, orderDocumentId = orderId.trim().ifBlank { null }, contractId = contractId.trim().ifBlank { null },
            amountDueMinor = amount.toLongOrNull() ?: 0L, method = PaymentMethod.BANK_TRANSFER,
            createdAtEpochMs = now, updatedAtEpochMs = now,
        )
        runAction { actor -> repository.createPayment(payment, actor) }
        amount = ""; invoiceId = ""; orderId = ""; contractId = ""
    }, enabled = linked && (amount.toLongOrNull() ?: 0L) > 0) { Text("Создать платеж") }

    Text("Платежи", style = MaterialTheme.typography.titleMedium)
    if (payments.isEmpty()) Text("Платежей пока нет")
    payments.forEach { payment ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${payment.id.take(8)} · ${payment.status.name}", style = MaterialTheme.typography.titleSmall)
                Text("К оплате: ${payment.amountDueMinor} · оплачено: ${payment.paidMinor} · возвращено: ${payment.refundedMinor} · остаток: ${payment.outstandingMinor} коп.")
                Text(listOfNotNull(payment.invoiceDocumentId?.let { "счет $it" }, payment.orderDocumentId?.let { "заказ $it" }, payment.contractId?.let { "договор $it" }).joinToString(" · "))
                var actionAmount by remember(payment.id) { mutableStateOf("") }
                var reason by remember(payment.id) { mutableStateOf("") }
                OutlinedTextField(actionAmount, { actionAmount = it }, label = { Text("Сумма операции, коп.") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(reason, { reason = it }, label = { Text("Причина возврата/корректировки") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (payment.status !in setOf(ManagedPaymentStatus.CANCELLED, ManagedPaymentStatus.REFUNDED) && payment.outstandingMinor > 0) {
                        OutlinedButton(onClick = {
                            val value = actionAmount.toLongOrNull() ?: return@OutlinedButton
                            runAction { actor -> repository.applyPayment(organizationId, payment.id, value, actor) }
                        }) { Text("Оплатить") }
                    }
                    if (payment.netPaidMinor > 0) {
                        OutlinedButton(onClick = {
                            val value = actionAmount.toLongOrNull() ?: return@OutlinedButton
                            if (reason.isBlank()) { onMessage("Для возврата укажите причину"); return@OutlinedButton }
                            runAction { actor -> repository.refundPayment(organizationId, payment.id, value, actor, reason) }
                        }) { Text("Возврат") }
                    }
                    OutlinedButton(onClick = {
                        val value = actionAmount.toLongOrNull() ?: return@OutlinedButton
                        if (reason.isBlank()) { onMessage("Для корректировки укажите причину"); return@OutlinedButton }
                        runAction { actor -> repository.adjustPayment(organizationId, payment.id, value, actor, reason) }
                    }) { Text("Скорректировать") }
                }
            }
        }
    }
}
