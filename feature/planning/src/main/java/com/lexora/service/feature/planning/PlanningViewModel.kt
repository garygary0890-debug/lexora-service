package com.lexora.service.feature.planning

import com.lexora.service.core.domain.LoadPlanningUseCase
import com.lexora.service.core.domain.ServiceOperations
import com.lexora.service.core.model.PlanningMode
import com.lexora.service.core.model.PlanningSnapshot
import com.lexora.service.core.model.RequestPriority
import com.lexora.service.core.model.SaveRequestCommand
import com.lexora.service.core.presentation.LexoraViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class PlanningUiState(
    val organizationId: String = "",
    val mode: PlanningMode = PlanningMode.DAY,
    val anchorDate: LocalDate = LocalDate.now(),
    val loading: Boolean = true,
    val snapshot: PlanningSnapshot? = null,
    val error: String? = null,
)

data class PlanningEntryDraft(
    val title: String,
    val description: String,
    val startAtEpochMs: Long,
    val endAtEpochMs: Long,
    val employeeId: String?,
    val branchId: String?,
    val teamName: String?,
) {
    init {
        require(title.isNotBlank()) { "Название записи обязательно" }
        require(endAtEpochMs > startAtEpochMs) { "Время окончания должно быть позже начала" }
    }
}

fun PlanningEntryDraft.toSaveRequestCommand(): SaveRequestCommand = SaveRequestCommand(
    title = title.trim(),
    description = description.trim(),
    priority = RequestPriority.NORMAL,
    clientId = null,
    serviceObjectId = null,
    equipmentId = null,
    contractId = null,
    branchId = branchId,
    assigneeEmployeeId = employeeId,
    assigneeTeamName = teamName?.trim()?.ifBlank { null },
    plannedAtEpochMs = startAtEpochMs,
    plannedEndEpochMs = endAtEpochMs,
    dueAtEpochMs = endAtEpochMs,
    slaReactionMinutes = null,
    slaResolutionMinutes = null,
)

class PlanningViewModel(
    private val organizationId: String,
    private val userId: String,
    private val loadPlanning: LoadPlanningUseCase,
    private val operations: ServiceOperations,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : LexoraViewModel<PlanningUiState>(PlanningUiState(organizationId = organizationId, anchorDate = LocalDate.now(zoneId))) {
    init { reload() }

    fun setMode(mode: PlanningMode) {
        updateState { it.copy(mode = mode) }
        reload()
    }

    fun cycleMode() {
        val nextMode = when (state.value.mode) {
            PlanningMode.DAY -> PlanningMode.WEEK
            PlanningMode.WEEK -> PlanningMode.MONTH
            PlanningMode.MONTH -> PlanningMode.DAY
        }
        setMode(nextMode)
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

    fun createEntry(draft: PlanningEntryDraft) = launchSafely(onError = { error ->
        updateState { it.copy(loading = false, error = error.message ?: "planning_create_failed") }
    }) {
        val requestId = operations.saveRequest(organizationId, userId, draft.toSaveRequestCommand(), null)
        operations.createVisit(organizationId, userId, requestId, draft.employeeId)
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