package com.lexora.service.feature.reports

import com.lexora.service.core.domain.CustomerCareOperations
import com.lexora.service.core.domain.CustomerCareSnapshot
import com.lexora.service.core.model.QualityControlRecord
import com.lexora.service.core.presentation.LexoraViewModel

data class CustomerCareUiState(
    val loading: Boolean = true,
    val data: CustomerCareSnapshot? = null,
    val message: String? = null,
)

class CustomerCareViewModel(
    private val organizationId: String,
    private val operations: CustomerCareOperations,
) : LexoraViewModel<CustomerCareUiState>(CustomerCareUiState()) {
    init { reload() }

    fun reload() = launchSafely(::fail) {
        setState(CustomerCareUiState(false, operations.snapshot(organizationId)))
    }

    fun accrue(clientId: String, points: Long) = launchSafely(::fail) {
        operations.accrue(organizationId, clientId, points, comment = "Ручное начисление"); reload()
    }
    fun redeem(clientId: String, points: Long) = launchSafely(::fail) {
        operations.redeem(organizationId, clientId, points, comment = "Ручное списание"); reload()
    }

    fun createQualityCheck(requestId: String) = launchSafely(::fail) {
        operations.createQualityCheck(organizationId, requestId); reload()
    }

    fun completeQualityCheck(record: QualityControlRecord, rating: Int, checklist: String, issue: String?) = launchSafely(::fail) {
        operations.completeQualityCheck(record, rating, checklist, issue); reload()
    }

    fun resolveQualityIssue(record: QualityControlRecord, resolution: String) = launchSafely(::fail) {
        operations.resolveQualityIssue(record, resolution); reload()
    }

    private fun fail(error: Throwable) = updateState { it.copy(loading = false, message = error.message ?: "customer_care_failed") }
}
