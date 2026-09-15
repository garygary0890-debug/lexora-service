package com.lexora.service.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexora.service.core.data.IntegrationDescriptor
import com.lexora.service.core.model.Payment
import com.lexora.service.core.model.PaymentStatus
import com.lexora.service.core.model.RequestStatus
import com.lexora.service.core.model.ServiceDocument
import com.lexora.service.core.model.ServiceDocumentStatus
import com.lexora.service.core.model.ServiceRequest
import com.lexora.service.core.model.ServiceVisit
import com.lexora.service.core.model.VisitStatus

@Composable
fun ReportsScreen(
    requests: List<ServiceRequest>,
    visits: List<ServiceVisit>,
    documents: List<ServiceDocument>,
    payments: List<Payment>,
    integrations: List<IntegrationDescriptor>,
) {
    val openRequests = requests.count { it.status != RequestStatus.CLOSED && it.status != RequestStatus.CANCELLED }
    val completedVisits = visits.count { it.status == VisitStatus.COMPLETED }
    val activeVisits = visits.count { it.status != VisitStatus.COMPLETED && it.status != VisitStatus.CANCELLED }
    val issuedDocuments = documents.count { it.status == ServiceDocumentStatus.ISSUED || it.status == ServiceDocumentStatus.SIGNED }
    val paidMinor = payments.filter { it.status == PaymentStatus.PAID }.sumOf { it.amountMinor }
    val plannedMinor = payments.filter { it.status == PaymentStatus.PLANNED }.sumOf { it.amountMinor }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Отчёты и интеграции", style = MaterialTheme.typography.headlineMedium)
        Text("Операционный срез", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Открытые заявки", openRequests.toString(), Modifier.weight(1f))
            MetricCard("Активные выезды", activeVisits.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Завершённые выезды", completedVisits.toString(), Modifier.weight(1f))
            MetricCard("Выданные документы", issuedDocuments.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Оплачено", money(paidMinor), Modifier.weight(1f))
            MetricCard("Ожидается", money(plannedMinor), Modifier.weight(1f))
        }

        Text("Статусы заявок", style = MaterialTheme.typography.titleMedium)
        RequestStatus.entries.forEach { status ->
            val count = requests.count { it.status == status }
            if (count > 0) Text("${status.name}: $count")
        }

        Text("Интеграционный контур", style = MaterialTheme.typography.titleMedium)
        integrations.forEach { integration ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(integration.title, style = MaterialTheme.typography.titleSmall)
                    Text(integration.description, style = MaterialTheme.typography.bodyMedium)
                    Text("Статус: ${integration.state.name}", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

private fun money(minor: Long): String = "%.2f ₽".format(minor / 100.0)
