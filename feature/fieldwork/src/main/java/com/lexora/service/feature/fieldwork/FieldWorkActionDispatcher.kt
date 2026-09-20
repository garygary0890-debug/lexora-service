package com.lexora.service.feature.fieldwork

/**
 * Keeps the existing app navigation call site source-compatible while the field-work
 * feature exposes the expanded execution/warehouse action set. The active ViewModel
 * rebinds this bridge whenever the organization/user scoped screen is created.
 */
internal object FieldWorkActionDispatcher {
    var saveTechnicalConclusion: (String) -> Unit = {}
    var saveClientSignature: (String, String) -> Unit = { _, _ -> }
    var addWork: (String, Double, String?, String?) -> Unit = { _, _, _, _ -> }
    var useMaterial: (String, String, Double, String?) -> Unit = { _, _, _, _ -> }
    var createWarehouse: (String, String?) -> Unit = { _, _ -> }
    var createInventoryItem: (String, String, String, Double?) -> Unit = { _, _, _, _ -> }
    var receipt: (String, String, Double, String?) -> Unit = { _, _, _, _ -> }
    var writeOff: (String, String, Double, String?) -> Unit = { _, _, _, _ -> }
    var transfer: (String, String, String, Double, String?) -> Unit = { _, _, _, _, _ -> }
    var reserve: (String, String, Double, String?, String?, String?) -> Unit = { _, _, _, _, _, _ -> }
    var releaseReservation: (String, String, Double, String?, String?, String?) -> Unit = { _, _, _, _, _, _ -> }

    fun bind(viewModel: FieldWorkViewModel) {
        saveTechnicalConclusion = viewModel::saveTechnicalConclusion
        saveClientSignature = viewModel::saveClientSignature
        addWork = viewModel::addWork
        useMaterial = viewModel::useMaterial
        createWarehouse = viewModel::createWarehouse
        createInventoryItem = viewModel::createInventoryItem
        receipt = viewModel::receipt
        writeOff = viewModel::writeOff
        transfer = viewModel::transfer
        reserve = viewModel::reserve
        releaseReservation = viewModel::releaseReservation
    }
}
