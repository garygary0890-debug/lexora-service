package com.lexora.service.feature.audit

import com.lexora.service.core.domain.AuditOperations
import com.lexora.service.core.model.AuditFilter
import com.lexora.service.core.model.AuditRecord
import com.lexora.service.core.presentation.LexoraViewModel

data class AuditUiState(
    val query: String = "",
    val loading: Boolean = true,
    val records: List<AuditRecord> = emptyList(),
    val error: String? = null,
)

class AuditViewModel(
    private val organizationId: String,
    private val audit: AuditOperations,
) : LexoraViewModel<AuditUiState>(AuditUiState()) {
    init { reload() }

    fun setQuery(value: String) {
        updateState { it.copy(query = value) }
        reload()
    }

    fun reload() = launchSafely(onError = { error ->
        updateState { it.copy(loading = false, error = error.message ?: "audit_failed") }
    }) {
        val query = state.value.query
        updateState { it.copy(loading = true, error = null) }
        val records = audit.records(organizationId, AuditFilter(query = query))
        updateState { it.copy(loading = false, records = records) }
    }
}

