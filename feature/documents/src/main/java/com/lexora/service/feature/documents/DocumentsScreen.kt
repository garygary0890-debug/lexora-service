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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.Payment
import com.lexora.service.core.model.PaymentStatus
import com.lexora.service.core.model.ServiceDocument
import com.lexora.service.core.model.ServiceDocumentStatus
import com.lexora.service.core.model.ServiceDocumentType
import com.lexora.service.core.model.ServiceRequest

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
    var tab by remember { mutableStateOf(Tab.DOCUMENTS) }
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
                if (documents.isEmpty()) Text(stringResource(R.string.documents_empty)) else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(documents, key = { it.id }) { document ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${document.number} · ${document.type.name}", style = MaterialTheme.typography.titleMedium)
                                Text(document.status.name)
                                Text("${document.totalMinor / 100.0} ${document.currency}")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (document.status == ServiceDocumentStatus.DRAFT) Button(onClick = { onChangeDocumentStatus(document.id, ServiceDocumentStatus.ISSUED) }) { Text(stringResource(R.string.issue)) }
                                    if (document.status == ServiceDocumentStatus.ISSUED) OutlinedButton(onClick = { onChangeDocumentStatus(document.id, ServiceDocumentStatus.SIGNED) }) { Text(stringResource(R.string.sign)) }
                                }
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
