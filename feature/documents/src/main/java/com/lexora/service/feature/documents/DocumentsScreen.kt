package com.lexora.service.feature.documents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lexora.service.core.data.InMemoryOrganizationRepository
import com.lexora.service.core.data.InMemoryUserRepository
import com.lexora.service.core.data.WorkOrderRepository
import com.lexora.service.core.model.AdditionalWorkApprovalStatus
import com.lexora.service.core.model.Payment
import com.lexora.service.core.model.PaymentStatus
import com.lexora.service.core.model.ServiceDocument
import com.lexora.service.core.model.ServiceDocumentStatus
import com.lexora.service.core.model.ServiceDocumentType
import com.lexora.service.core.model.ServiceRequest
import com.lexora.service.core.model.WorkOrderItem
import kotlinx.coroutines.launch

private enum class Tab { DOCUMENTS, PAYMENTS }

@Composable
fun DocumentsScreen(
    documents: List<ServiceDocument>,
    payments: List<Payment>,
    requests: List<ServiceRequest>,
    onCreateDocument: (ServiceDocumentType, String?) -> Unit,
    onChangeDocumentStatus: (String, ServiceDocumentStatus) -> Unit,
    onCreatePayment: (String?) -> Unit,
    onMarkPaymentPaid: (String) -> Unit,
) {
    val context = LocalContext.current
    val workOrderRepository = remember { WorkOrderRepository.create(context) }
    val organization = remember { InMemoryOrganizationRepository().activeOrganization() }
    val user = remember { InMemoryUserRepository().currentUser() }
    val scope = rememberCoroutineScope()

    var tab by remember { mutableStateOf(Tab.DOCUMENTS) }
    var selectedWorkOrderId by remember { mutableStateOf<String?>(null) }
    var workOrderItems by remember { mutableStateOf<List<WorkOrderItem>>(emptyList()) }

    suspend fun reloadWorkOrderItems(documentId: String?) {
        selectedWorkOrderId = documentId
        workOrderItems = documentId?.let { workOrderRepository.items(it) }.orEmpty()
    }

    LaunchedEffect(documents) {
        val available = documents.firstOrNull { it.type == ServiceDocumentType.WORK_ORDER }?.id
        val selected = selectedWorkOrderId?.takeIf { id -> documents.any { it.id == id } } ?: available
        reloadWorkOrderItems(selected)
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.documents_title), style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == Tab.DOCUMENTS, onClick = { tab = Tab.DOCUMENTS }, label = { Text(stringResource(R.string.documents_tab)) })
            FilterChip(selected = tab == Tab.PAYMENTS, onClick = { tab = Tab.PAYMENTS }, label = { Text(stringResource(R.string.payments_tab)) })
        }
        val firstRequestId = requests.firstOrNull()?.id
        when (tab) {
            Tab.DOCUMENTS -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onCreateDocument(ServiceDocumentType.WORK_ORDER, firstRequestId) }) { Text(stringResource(R.string.create_work_order)) }
                    OutlinedButton(onClick = { onCreateDocument(ServiceDocumentType.ACT, firstRequestId) }) { Text(stringResource(R.string.create_act)) }
                }
                if (documents.isEmpty()) {
                    Text(stringResource(R.string.documents_empty))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(documents, key = { it.id }) { document ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${document.number} · ${document.type.name}", style = MaterialTheme.typography.titleMedium)
                                    Text(document.status.name)
                                    val localTotal = if (document.id == selectedWorkOrderId && document.type == ServiceDocumentType.WORK_ORDER) {
                                        workOrderItems.filter { !it.additional || it.approvalStatus == AdditionalWorkApprovalStatus.APPROVED }.sumOf { it.totalMinor }
                                    } else document.totalMinor
                                    Text("${localTotal / 100.0} ${document.currency}")
                                    if (document.type == ServiceDocumentType.WORK_ORDER && document.status == ServiceDocumentStatus.DRAFT) {
                                        OutlinedButton(onClick = { scope.launch { reloadWorkOrderItems(document.id) } }) { Text("Открыть состав работ") }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (document.status == ServiceDocumentStatus.DRAFT) Button(onClick = { onChangeDocumentStatus(document.id, ServiceDocumentStatus.ISSUED) }) { Text(stringResource(R.string.issue)) }
                                        if (document.status == ServiceDocumentStatus.ISSUED) OutlinedButton(onClick = { onChangeDocumentStatus(document.id, ServiceDocumentStatus.SIGNED) }) { Text(stringResource(R.string.sign)) }
                                    }
                                }
                            }
                        }
                        if (selectedWorkOrderId != null) {
                            item(key = "work-order-editor") {
                                WorkOrderEditor(
                                    items = workOrderItems,
                                    onAddBaseWork = {
                                        val organizationId = organization?.id ?: return@WorkOrderEditor
                                        val documentId = selectedWorkOrderId ?: return@WorkOrderEditor
                                        scope.launch {
                                            workOrderRepository.addCatalogItem(organizationId, user.id, documentId, additional = false)
                                            reloadWorkOrderItems(documentId)
                                        }
                                    },
                                    onAddAdditionalWork = {
                                        val organizationId = organization?.id ?: return@WorkOrderEditor
                                        val documentId = selectedWorkOrderId ?: return@WorkOrderEditor
                                        scope.launch {
                                            workOrderRepository.addCatalogItem(organizationId, user.id, documentId, additional = true)
                                            reloadWorkOrderItems(documentId)
                                        }
                                    },
                                    onResolve = { itemId, approve ->
                                        val organizationId = organization?.id ?: return@WorkOrderEditor
                                        val documentId = selectedWorkOrderId ?: return@WorkOrderEditor
                                        scope.launch {
                                            workOrderRepository.resolveAdditionalWork(organizationId, user.id, itemId, approve)
                                            reloadWorkOrderItems(documentId)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
            Tab.PAYMENTS -> {
                Button(onClick = { onCreatePayment(firstRequestId) }) { Text(stringResource(R.string.create_payment)) }
                if (payments.isEmpty()) Text(stringResource(R.string.payments_empty)) else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(payments, key = { it.id }) { payment ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${payment.amountMinor / 100.0} ${payment.currency}", style = MaterialTheme.typography.titleMedium)
                                Text(payment.status.name)
                                Text(payment.method.name)
                                if (payment.status == PaymentStatus.PLANNED) Button(onClick = { onMarkPaymentPaid(payment.id) }) { Text(stringResource(R.string.mark_paid)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkOrderEditor(
    items: List<WorkOrderItem>,
    onAddBaseWork: () -> Unit,
    onAddAdditionalWork: () -> Unit,
    onResolve: (String, Boolean) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Состав заказ-наряда", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddBaseWork) { Text("Добавить услугу") }
                OutlinedButton(onClick = onAddAdditionalWork) { Text("Доп. работа") }
            }
            if (items.isEmpty()) Text("Позиции пока не добавлены")
            items.forEach { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.title)
                        Text("${item.quantity} ${item.unit} × ${item.unitPriceMinor / 100.0} = ${item.totalMinor / 100.0}")
                        if (item.additional) {
                            Text("Дополнительная работа: ${item.approvalStatus.name}")
                            if (item.approvalStatus == AdditionalWorkApprovalStatus.PENDING) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { onResolve(item.id, true) }) { Text("Согласовать") }
                                    OutlinedButton(onClick = { onResolve(item.id, false) }) { Text("Отклонить") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
