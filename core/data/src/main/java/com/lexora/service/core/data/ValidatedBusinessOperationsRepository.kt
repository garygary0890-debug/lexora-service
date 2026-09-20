package com.lexora.service.core.data

import com.lexora.service.core.database.ContractDao
import com.lexora.service.core.database.ServiceDao
import com.lexora.service.core.model.*

/** Ensures cross-module links stored by finance/tasks point to real records of the same organization. */
class ValidatedBusinessOperationsRepository(
    private val delegate: BusinessOperationsRepository,
    private val serviceDao: ServiceDao,
    private val contractDao: ContractDao,
) : BusinessOperationsRepository by delegate {

    override suspend fun createPayment(payment: ManagedPayment, actorUserId: String): ManagedPayment {
        validateCommonLinks(payment.organizationId, payment.clientId, payment.requestId)
        payment.invoiceDocumentId?.let { validateDocument(payment.organizationId, it, "Счет") }
        payment.orderDocumentId?.let { validateDocument(payment.organizationId, it, "Заказ") }
        payment.contractId?.let { id ->
            val contract = contractDao.contract(id) ?: error("Договор не найден")
            require(contract.organizationId == payment.organizationId && !contract.archived) { "Договор относится к другой организации или находится в архиве" }
        }
        return delegate.createPayment(payment, actorUserId)
    }

    override suspend fun saveTask(task: LinkedServiceTask, actorUserId: String): LinkedServiceTask {
        require(task.requestId != null || task.clientId != null || task.documentId != null) { "Задача должна быть связана с заявкой, клиентом или документом" }
        validateCommonLinks(task.organizationId, task.clientId, task.requestId)
        task.documentId?.let { validateDocument(task.organizationId, it, "Документ") }
        return delegate.saveTask(task, actorUserId)
    }

    private suspend fun validateCommonLinks(organizationId: String, clientId: String?, requestId: String?) {
        clientId?.let { id ->
            val client = serviceDao.client(id) ?: error("Клиент не найден")
            require(client.organizationId == organizationId && !client.archived) { "Клиент относится к другой организации или находится в архиве" }
        }
        requestId?.let { id ->
            val request = serviceDao.serviceRequest(id) ?: error("Заявка не найдена")
            require(request.organizationId == organizationId && !request.archived) { "Заявка относится к другой организации или находится в архиве" }
        }
    }

    private suspend fun validateDocument(organizationId: String, documentId: String, label: String) {
        val document = serviceDao.serviceDocument(documentId) ?: error("$label не найден")
        require(document.organizationId == organizationId && !document.archived) { "$label относится к другой организации или находится в архиве" }
    }
}
