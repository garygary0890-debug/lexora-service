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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.*

private enum class ReportsSection { OPERATIONS, CUSTOMER_CARE }

@Composable
fun ReportsScreen(
    requests: List<ServiceRequest>,
    visits: List<ServiceVisit>,
    documents: List<ServiceDocument>,
    payments: List<Payment>,
    integrations: List<IntegrationDescriptor>,
    customerCareViewModel: CustomerCareViewModel,
) {
    var section by remember { mutableStateOf(ReportsSection.OPERATIONS) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Отчёты и интеграции", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = section == ReportsSection.OPERATIONS,
                onClick = { section = ReportsSection.OPERATIONS },
                label = { Text("Операционные отчёты") },
            )
            FilterChip(
                selected = section == ReportsSection.CUSTOMER_CARE,
                onClick = { section = ReportsSection.CUSTOMER_CARE },
                label = { Text("Лояльность и качество") },
            )
        }

        if (section == ReportsSection.CUSTOMER_CARE) {
            CustomerCareScreen(requests = requests, viewModel = customerCareViewModel)
        } else {
            OperationsContent(requests, visits, documents, payments, integrations)
        }
    }
}

@Composable
private fun OperationsContent(
    requests: List<ServiceRequest>,
    visits: List<ServiceVisit>,
    documents: List<ServiceDocument>,
    payments: List<Payment>,
    integrations: List<IntegrationDescriptor>,
) {
    val hub by ReportsUiRuntime.latest
    val openRequests = requests.count { it.status != RequestStatus.CLOSED && it.status != RequestStatus.CANCELLED }
    val completedVisits = visits.count { it.status == VisitStatus.COMPLETED }
    val activeVisits = visits.count { it.status != VisitStatus.COMPLETED && it.status != VisitStatus.CANCELLED }
    val issuedDocuments = documents.count { it.status == ServiceDocumentStatus.ISSUED || it.status == ServiceDocumentStatus.SIGNED }
    val paidMinor = payments.filter { it.status == PaymentStatus.PAID }.sumOf { it.amountMinor }
    val plannedMinor = payments.filter { it.status == PaymentStatus.PLANNED }.sumOf { it.amountMinor }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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

        hub?.let { report ->
            Text("1. Заявки по статусам", style = MaterialTheme.typography.titleMedium)
            report.requestStatuses.forEach { Text("${it.status.name}: ${it.count}") }

            Text("2. Загрузка сотрудников и филиалов", style = MaterialTheme.typography.titleMedium)
            if (report.workload.isEmpty()) Text("Нет данных за выбранный период")
            report.workload.forEach { Text("${it.employeeId} · ${it.branchId ?: "без филиала"}: ${it.visitCount} выездов") }

            Text("3. Выработка сотрудников", style = MaterialTheme.typography.titleMedium)
            report.employeeOutput.forEach { Text("${it.employeeId}: ${it.completedVisits} выездов · ${it.workQuantity} ед. работ") }

            Text("4. Финансы", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Выручка", money(report.financial.netRevenueMinor), Modifier.weight(1f))
                MetricCard("Долг", money(report.financial.debtMinor), Modifier.weight(1f))
                MetricCard("Просрочено", money(report.financial.overdueDebtMinor), Modifier.weight(1f))
            }

            Text("5. SLA", style = MaterialTheme.typography.titleMedium)
            Text("Нарушения реакции: ${report.sla.reactionBreachedCount}; выполнения: ${report.sla.resolutionBreachedCount}")
            Text("Средняя реакция: ${minutes(report.sla.averageReactionMinutes)}; выполнение: ${minutes(report.sla.averageResolutionMinutes)}")

            Text("6. Повторные обращения по оборудованию", style = MaterialTheme.typography.titleMedium)
            if (report.repeatIssues.rows.isEmpty()) Text("Повторные обращения не выявлены")
            report.repeatIssues.rows.forEach { Text("${it.equipmentId} · ${it.issueKey}: ${it.requestIds.size}") }

            Text("7. Расход материалов", style = MaterialTheme.typography.titleMedium)
            if (report.materials.isEmpty()) Text("Расход материалов не зафиксирован")
            report.materials.forEach { Text("${it.materialCode ?: it.title}: ${it.quantity} ${it.unit.orEmpty()}") }
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
private fun minutes(value: Double?): String = value?.let { "%.1f мин".format(it) } ?: "нет данных"
