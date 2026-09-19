package com.lexora.service.feature.documents

import com.lexora.service.core.domain.DocumentFeatureOperations
import com.lexora.service.core.domain.ServiceOperations
import com.lexora.service.core.model.*
import com.lexora.service.core.presentation.LexoraViewModel

data class DocumentsUiState(
    val loading: Boolean = true,
    val documents: List<ServiceDocument> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val requests: List<ServiceRequest> = emptyList(),
    val selectedWorkOrderId: String? = null,
    val workOrderItems: List<WorkOrderItem> = emptyList(),
    val contracts: List<ServiceContract> = emptyList(),
    val archivedContracts: List<ServiceContract> = emptyList(),
    val message: String? = null,
)

class DocumentsViewModel(
    private val organizationId: String,
    private val userId: String,
    private val operations: ServiceOperations,
    private val feature: DocumentFeatureOperations,
) : LexoraViewModel<DocumentsUiState>(DocumentsUiState()) {
    init { reload() }

    fun reload() = launchSafely(::fail) { refresh() }

    fun selectWorkOrder(id: String?) = launchSafely(::fail) { refresh(id) }
    fun createDocument(type: ServiceDocumentType, requestId: String?) = launchSafely(::fail) {
        operations.createDocument(organizationId, userId, type, requestId); refresh()
    }
    fun changeDocumentStatus(id: String, target: ServiceDocumentStatus) = launchSafely(::fail) {
        operations.changeDocumentStatus(organizationId, userId, id, target); refresh()
    }
    fun createPayment(requestId: String?) = launchSafely(::fail) {
        operations.createPayment(organizationId, userId, requestId, currentState.documents); refresh()
    }
    fun markPaymentPaid(id: String) = launchSafely(::fail) {
        operations.markPaymentPaid(organizationId, userId, id); refresh()
    }
    fun addWork(additional: Boolean) = launchSafely(::fail) {
        val id = currentState.selectedWorkOrderId ?: return@launchSafely
        feature.addWorkOrderCatalogItem(organizationId, userId, id, additional); refresh(id)
    }
    fun resolveWork(itemId: String, approve: Boolean, comment: String?) = launchSafely(::fail) {
        feature.resolveAdditionalWork(organizationId, userId, itemId, approve, comment); refresh(currentState.selectedWorkOrderId)
    }
    fun createContract() = launchSafely(::fail) {
        val request = currentState.requests.firstOrNull() ?: return@launchSafely
        val clientId = request.clientId ?: return@launchSafely
        feature.createContract(organizationId, userId, clientId, request.branchId, "Договор на сервисное обслуживание")
        refresh(currentState.selectedWorkOrderId)
    }
    fun changeContractStatus(id: String, status: ContractStatus) = launchSafely(::fail) {
        feature.changeContractStatus(organizationId, userId, id, status); refresh(currentState.selectedWorkOrderId)
    }
    fun archiveContract(id: String, restore: Boolean) = launchSafely(::fail) {
        feature.archiveContract(organizationId, userId, id, restore); refresh(currentState.selectedWorkOrderId)
    }
    fun importAttachment(documentId: String, uri: String) = launchSafely(::fail) {
        val result = feature.importAttachment(organizationId, userId, documentId, uri)
        updateState { it.copy(message = result.fold({ name -> "Файл импортирован: $name" }, { e -> "Ошибка импорта: ${e.message ?: "неизвестная ошибка"}" })) }
        refresh(currentState.selectedWorkOrderId)
    }
    fun exportDocument(documentId: String, uri: String) = launchSafely(::fail) {
        val result = feature.exportDocument(organizationId, userId, documentId, uri)
        updateState { it.copy(message = result.fold({ name -> "Документ экспортирован: $name" }, { e -> "Ошибка экспорта: ${e.message ?: "неизвестная ошибка"}" })) }
    }
    private suspend fun refresh(preferredWorkOrderId: String? = currentState.selectedWorkOrderId) {
        val finance = operations.finance(organizationId)
        val requests = operations.requests(organizationId)
        val selected = preferredWorkOrderId?.takeIf { id -> finance.documents.any { it.id == id && it.type == ServiceDocumentType.WORK_ORDER } }
            ?: finance.documents.firstOrNull { it.type == ServiceDocumentType.WORK_ORDER }?.id
        val extra = feature.snapshot(organizationId, selected)
        setState(DocumentsUiState(false, finance.documents, finance.payments, requests, selected, extra.workOrderItems, extra.contracts, extra.archivedContracts, currentState.message))
    }
    private fun fail(error: Throwable) = updateState { it.copy(loading = false, message = error.message ?: "documents_failed") }
}
