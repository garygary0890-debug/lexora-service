package com.lexora.service.feature.planning

import com.lexora.service.core.domain.LoadPlanningUseCase
import com.lexora.service.core.model.PlanningMode
import com.lexora.service.core.model.PlanningSnapshot
import com.lexora.service.core.presentation.LexoraViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class PlanningUiState(
    val mode: PlanningMode = PlanningMode.DAY,
    val anchorDate: LocalDate = LocalDate.now(),
    val loading: Boolean = true,
    val snapshot: PlanningSnapshot? = null,
    val error: String? = null,
)

class PlanningViewModel(
    private val organizationId: String,
    private val loadPlanning: LoadPlanningUseCase,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : LexoraViewModel<PlanningUiState>(PlanningUiState(anchorDate = LocalDate.now(zoneId))) {
    init { reload() }

    fun setMode(mode: PlanningMode) {
        updateState { it.copy(mode = mode) }
        reload()
    }

    fun previous() {
        updateState { it.copy(anchorDate = shift(it.anchorDate, it.mode, -1)) }
        reload()
    }

    fun next() {
        updateState { it.copy(anchorDate = shift(it.anchorDate, it.mode, 1)) }
        reload()
    }

    fun today() {
        updateState { it.copy(anchorDate = LocalDate.now(zoneId)) }
        reload()
    }

    fun reload() = launchSafely(onError = { error ->
        updateState { it.copy(loading = false, error = error.message ?: "planning_load_failed") }
    }) {
        val current = state.value
        val (start, end) = range(current.anchorDate, current.mode)
        updateState { it.copy(loading = true, error = null) }
        val snapshot = loadPlanning(organizationId, start, end)
        updateState { it.copy(loading = false, snapshot = snapshot) }
    }

    private fun range(anchor: LocalDate, mode: PlanningMode): Pair<Long, Long> {
        val startDate = when (mode) {
            PlanningMode.DAY -> anchor
            PlanningMode.WEEK -> anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            PlanningMode.MONTH -> anchor.withDayOfMonth(1)
        }
        val endDate = when (mode) {
            PlanningMode.DAY -> startDate.plusDays(1)
            PlanningMode.WEEK -> startDate.plusWeeks(1)
            PlanningMode.MONTH -> startDate.plusMonths(1)
        }
        return startDate.atStartOfDay(zoneId).toInstant().toEpochMilli() to endDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    private fun shift(date: LocalDate, mode: PlanningMode, direction: Long): LocalDate = when (mode) {
        PlanningMode.DAY -> date.plusDays(direction)
        PlanningMode.WEEK -> date.plusWeeks(direction)
        PlanningMode.MONTH -> date.plusMonths(direction)
    }
}

