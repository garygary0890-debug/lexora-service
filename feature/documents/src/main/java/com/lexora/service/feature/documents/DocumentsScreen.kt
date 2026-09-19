package com.lexora.service.feature.documents

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.AdditionalWorkApprovalStatus
import com.lexora.service.core.model.ContractStatus
import com.lexora.service.core.model.Payment
import com.lexora.service.core.model.PaymentStatus
import com.lexora.service.core.model.ServiceContract
import com.lexora.service.core.model.ServiceDocument
import com.lexora.service.core.model.ServiceDocumentStatus
import com.lexora.service.core.model.ServiceDocumentType
import com.lexora.service.core.model.ServiceRequest
import com.lexora.service.core.model.WorkOrderItem

private enum class Tab { DOCUMENTS, PAYMENTS, CONTRACTS }

@Composable
fun DocumentsScreen(viewModel: DocumentsViewModel) {
    val state by viewModel.state.collectAsState()
    val documents = state.documents
    val payments = state.payments
    val requests = state.requests
    val selectedWorkOrderId = state.selectedWorkOrderId
    val workOrderItems = state.workOrderItems
    val contracts = state.contracts
    val archivedContracts = state.archivedContracts
    var tab by remember { mutableStateOf(Tab.DOCUMENTS) }
    var pendingImportDocumentId by remember { mutableStateOf<String?>(null) }
    var pendingExportDocumentId by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val documentId = pendingImportDocumentId
        pendingImportDocumentId = null
        if (uri != null && documentId != null) viewModel.importAttachment(documentId, uri.toString())
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val documentId = pendingExportDocumentId
        pendingExportDocumentId = null
        if (uri != null && documentId != null) viewModel.exportDocument(documentId, uri.toString())
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.documents_title), style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == Tab.DOCUMENTS, onClick = { tab = Tab.DOCUMENTS }, label = { Text(stringResource(R.string.documents_tab)) })
            FilterChip(selected = tab == Tab.PAYMENTS, onClick = { tab = Tab.PAYMENTS }, label = { Text(stringResource(R.string.payments_tab)) })
            FilterChip(selected = tab == Tab.CONTRACTS, onClick = { tab = Tab.CONTRACTS }, label = { Text("Договоры") })
        }
        state.message?.let { Text(it) }
        val firstRequest = requests.firstOrNull()
        val firstRequestId = firstRequest?.id
        when (tab) {
            Tab.DOCUMENTS -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.createDocument(ServiceDocumentType.WORK_ORDER, firstRequestId) }) { Text(stringResource(R.string.create_work_order)) }
                    OutlinedButton(onClick = { viewModel.createDocument(ServiceDocumentType.ACT, firstRequestId) }) { Text(stringResource(R.string.create_act)) }
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
                                    document.externalFileRef?.let { ref ->
                                        Text("Файл: ${ref.substringAfterLast('/').substringAfterLast("\\")}")
                                    }
                                    if (document.type == ServiceDocumentType.WORK_ORDER && document.status == ServiceDocumentStatus.DRAFT) {
                                        OutlinedButton(onClick = { viewModel.selectWorkOrder(document.id) }) { Text("Открыть состав работ") }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = {
                                            pendingImportDocumentId = document.id
                                                            importLauncher.launch(arrayOf("*/*"))
                                        }) { Text("Импорт файла") }
                                        OutlinedButton(onClick = {
                                            pendingExportDocumentId = document.id
                                                            val suggestedName = document.externalFileRef
                                                ?.substringAfterLast('/')
                                                ?.substringAfterLast("\\")
                                                ?.takeIf { it.isNotBlank() }
                                                ?: "${document.number}.txt"
                                            exportLauncher.launch(suggestedName)
                                        }) { Text("Экспорт") }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (document.status == ServiceDocumentStatus.DRAFT) Button(onClick = { viewModel.changeDocumentStatus(document.id, ServiceDocumentStatus.ISSUED) }) { Text(stringResource(R.string.issue)) }
                                        if (document.status == ServiceDocumentStatus.ISSUED) OutlinedButton(onClick = { viewModel.changeDocumentStatus(document.id, ServiceDocumentStatus.SIGNED) }) { Text(stringResource(R.string.sign)) }
                                    }
                                }
                            }
                        }
                        if (selectedWorkOrderId != null) {
                            item(key = "work-order-editor") {
                                WorkOrderEditor(
                                    items = workOrderItems,
                                    onAddBaseWork = { viewModel.addWork(additional = false) },
                                    onAddAdditionalWork = { viewModel.addWork(additional = true) },
                                    onResolve = { itemId, approve, comment -> viewModel.resolveWork(itemId, approve, comment) },
                                )
                            }
                        }
                    }
                }
            }
            Tab.PAYMENTS -> {
                Button(onClick = { viewModel.createPayment(firstRequestId) }) { Text(stringResource(R.string.create_payment)) }
                if (payments.isEmpty()) Text(stringResource(R.string.payments_empty)) else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(payments, key = { it.id }) { payment ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${payment.amountMinor / 100.0} ${payment.currency}", style = MaterialTheme.typography.titleMedium)
                                Text(payment.status.name)
                                Text(payment.method.name)
                                if (payment.status == PaymentStatus.PLANNED) Button(onClick = { viewModel.markPaymentPaid(payment.id) }) { Text(stringResource(R.string.mark_paid)) }
                            }
                        }
                    }
                }
            }
            Tab.CONTRACTS -> {
                ContractsSection(
                    contracts = contracts,
                    archivedContracts = archivedContracts,
                    onCreate = { viewModel.createContract() },
                    onChangeStatus = { contractId, status -> viewModel.changeContractStatus(contractId, status) },
                    onArchive = { contractId -> viewModel.archiveContract(contractId, restore = false) },
                    onRestore = { contractId -> viewModel.archiveContract(contractId, restore = true) },
                )
            }
        }
    }
}

@Composable
private fun WorkOrderEditor(
    items: List<WorkOrderItem>,
    onAddBaseWork: () -> Unit,
    onAddAdditionalWork: () -> Unit,
    onResolve: (String, Boolean, String?) -> Unit,
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
                var approvalComment by remember(item.id, item.approvalComment) {
                    mutableStateOf(item.approvalComment.orEmpty())
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.title)
                        Text("${item.quantity} ${item.unit} × ${item.unitPriceMinor / 100.0} = ${item.totalMinor / 100.0}")
                        if (item.additional) {
                            Text("Дополнительная работа: ${item.approvalStatus.name}")
                            if (item.approvalStatus == AdditionalWorkApprovalStatus.PENDING) {
                                OutlinedTextField(
                                    value = approvalComment,
                                    onValueChange = { approvalComment = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Комментарий к согласованию") },
                                    placeholder = { Text("Например: согласовано по телефону или причина отказа") },
                                    minLines = 2,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { onResolve(item.id, true, approvalComment) }) { Text("Согласовать") }
                                    OutlinedButton(onClick = { onResolve(item.id, false, approvalComment) }) { Text("Отклонить") }
                                }
                            } else {
                                item.approvalComment?.takeIf { it.isNotBlank() }?.let { comment ->
                                    Text("Комментарий: $comment")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
