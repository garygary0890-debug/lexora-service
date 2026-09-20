package com.lexora.service.feature.reports

import androidx.compose.runtime.mutableStateOf
import com.lexora.service.core.data.ReportHubRuntimeDependencies
import com.lexora.service.core.domain.ReportHubOperations
import com.lexora.service.core.domain.ServiceOperations
import com.lexora.service.core.model.*
import com.lexora.service.core.presentation.LexoraViewModel

data class ReportsUiState(
    val loading: Boolean = true,
    val requests: List<ServiceRequest> = emptyList(),
    val visits: List<ServiceVisit> = emptyList(),
    val documents: List<ServiceDocument> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val integrations: List<IntegrationDescriptor> = emptyList(),
    val reportHub: ReportHubSnapshot? = null,
    val selectedBranchIds: Set<String> = emptySet(),
    val periodDays: Int = 30,
    val csvPreview: String? = null,
    val error: String? = null,
)

object ReportsUiRuntime {
    val latest = mutableStateOf<ReportHubSnapshot?>(null)
    val csv = mutableStateOf<String?>(null)
}

class ReportsViewModel(
    private val organizationId: String,
    private val operations: ServiceOperations,
    private val integrations: List<IntegrationDescriptor>,
    private val reportHub: ReportHubOperations = ReportHubRuntimeDependencies.operations(),
) : LexoraViewModel<ReportsUiState>(ReportsUiState()) {
    init { reload() }

    fun setPeriodDays(days: Int) {
        require(days in 1..3660)
        updateState { it.copy(periodDays = days) }
        reload()
    }

    fun toggleBranch(branchId: String) {
        updateState { current ->
            val next = current.selectedBranchIds.toMutableSet().apply {
                if (!add(branchId)) remove(branchId)
            }
            current.copy(selectedBranchIds = next)
        }
        reload()
    }

    fun exportCsv() = launchSafely(onError = ::fail) {
        val hub = state.value.reportHub ?: return@launchSafely
        val csv = reportHub.exportCsv(hub)
        ReportsUiRuntime.csv.value = csv
        updateState { it.copy(csvPreview = csv) }
    }

    fun reload() = launchSafely(onError = ::fail) {
        updateState { it.copy(loading = true, error = null) }
        val finance = operations.finance(organizationId)
        val now = System.currentTimeMillis()
        val from = now - state.value.periodDays.toLong() * DAY_MS
        val hub = reportHub.snapshot(
            organizationId = organizationId,
            branchIds = state.value.selectedBranchIds,
            fromEpochMs = from,
            toEpochMs = now,
            nowEpochMs = now,
        )
        ReportsUiRuntime.latest.value = hub
        setState(
            state.value.copy(
                loading = false,
                requests = operations.requests(organizationId),
                visits = operations.visits(organizationId, null).visits,
                documents = finance.documents,
                payments = finance.payments,
                integrations = integrations,
                reportHub = hub,
                error = null,
            ),
        )
    }

    private fun fail(error: Throwable) =
        updateState { it.copy(loading = false, error = error.message ?: "reports_failed") }

    companion object { private const val DAY_MS = 24L * 60L * 60L * 1000L }
}
