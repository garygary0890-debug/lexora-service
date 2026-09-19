package com.lexora.service.feature.home

import com.lexora.service.core.domain.LoadOperationalDashboardUseCase
import com.lexora.service.core.model.OperationalDashboard
import com.lexora.service.core.presentation.LexoraViewModel
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val loading: Boolean = true,
    val dashboard: OperationalDashboard? = null,
    val error: String? = null,
)

class HomeViewModel(
    private val organizationId: String,
    private val loadDashboard: LoadOperationalDashboardUseCase,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : LexoraViewModel<HomeUiState>(HomeUiState()) {
    init { refresh() }

    fun refresh() = launchSafely(onError = { error ->
        updateState { it.copy(loading = false, error = error.message ?: "dashboard_load_failed") }
    }) {
        updateState { it.copy(loading = true, error = null) }
        val today = LocalDate.now(zoneId)
        val start = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dashboard = loadDashboard(organizationId, System.currentTimeMillis(), start, end)
        setState(HomeUiState(loading = false, dashboard = dashboard))
    }
}

