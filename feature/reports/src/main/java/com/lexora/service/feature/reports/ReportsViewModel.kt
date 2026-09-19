package com.lexora.service.feature.reports

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
    val error: String? = null,
)

class ReportsViewModel(
    private val organizationId: String,
    private val operations: ServiceOperations,
    private val integrations: List<IntegrationDescriptor>,
) : LexoraViewModel<ReportsUiState>(ReportsUiState()) {
    init { reload() }

    fun reload() = launchSafely(onError = { error ->
        updateState { it.copy(loading = false, error = error.message ?: "reports_failed") }
    }) {
        val finance = operations.finance(organizationId)
        setState(
            ReportsUiState(
                loading = false,
                requests = operations.requests(organizationId),
                visits = operations.visits(organizationId, null).visits,
                documents = finance.documents,
                payments = finance.payments,
                integrations = integrations,
            ),
        )
    }
}

