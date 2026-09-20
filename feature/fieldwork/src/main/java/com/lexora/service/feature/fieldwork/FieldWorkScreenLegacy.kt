package com.lexora.service.feature.fieldwork

import androidx.compose.runtime.Composable
import com.lexora.service.core.model.VisitStatus

/** Existing app navigation entry point; delegates to the extended field-work UI. */
@Composable
fun FieldWorkScreen(
    state: FieldWorkUiState,
    onCreateVisit: (String, String?) -> Unit,
    onAssignRequest: (String, String?) -> Unit,
    onSelectVisit: (String) -> Unit,
    onChangeVisitStatus: (String, VisitStatus) -> Unit,
    onAddChecklistItem: (String) -> Unit,
    onToggleChecklistItem: (String) -> Unit,
) = FieldWorkScreen(
    state = state,
    onCreateVisit = onCreateVisit,
    onAssignRequest = onAssignRequest,
    onSelectVisit = onSelectVisit,
    onChangeVisitStatus = onChangeVisitStatus,
    onAddChecklistItem = onAddChecklistItem,
    onToggleChecklistItem = onToggleChecklistItem,
    onSaveTechnicalConclusion = FieldWorkActionDispatcher.saveTechnicalConclusion,
    onSaveClientSignature = FieldWorkActionDispatcher.saveClientSignature,
    onAddWork = FieldWorkActionDispatcher.addWork,
    onUseMaterial = FieldWorkActionDispatcher.useMaterial,
    onCreateWarehouse = FieldWorkActionDispatcher.createWarehouse,
    onCreateInventoryItem = FieldWorkActionDispatcher.createInventoryItem,
    onReceipt = FieldWorkActionDispatcher.receipt,
    onWriteOff = FieldWorkActionDispatcher.writeOff,
    onTransfer = FieldWorkActionDispatcher.transfer,
    onReserve = FieldWorkActionDispatcher.reserve,
    onReleaseReservation = FieldWorkActionDispatcher.releaseReservation,
)
