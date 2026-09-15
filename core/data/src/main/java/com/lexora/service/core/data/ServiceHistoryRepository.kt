package com.lexora.service.core.data

import android.content.Context
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.database.ServiceDao
import com.lexora.service.core.database.ServiceHistoryEntity
import com.lexora.service.core.model.PaymentStatus
import com.lexora.service.core.model.RequestStatus
import com.lexora.service.core.model.ServiceDocumentStatus
import com.lexora.service.core.model.ServiceHistoryRecord
import com.lexora.service.core.model.ServiceHistorySourceType
import com.lexora.service.core.model.VisitStatus

class ServiceHistoryRepository(
    private val dao: ServiceDao,
) {
    suspend fun history(vehicleId: String): List<ServiceHistoryRecord> {
        materialize(vehicleId)
        return dao.serviceHistory(vehicleId).map(ServiceHistoryEntity::toModel)
    }

    private suspend fun materialize(vehicleId: String) {
        val vehicle = dao.vehicle(vehicleId) ?: return
        val organizationId = vehicle.organizationId
        val mileage = vehicle.mileageKm
        val requests = dao.serviceRequests(organizationId).filter { it.vehicleId == vehicleId }

        requests.filter { it.status == RequestStatus.CLOSED.name && it.closedAtEpochMs != null }.forEach { request ->
            val occurredAt = requireNotNull(request.closedAtEpochMs)
            upsertDeterministic(
                id = "history-request-${request.id}",
                organizationId = organizationId,
                vehicleId = vehicleId,
                sourceType = ServiceHistorySourceType.SERVICE_REQUEST,
                sourceId = request.id,
                title = "Заявка ${request.number} закрыта",
                description = request.title,
                mileageKm = mileage,
                occurredAt = occurredAt,
            )

            dao.visitsForRequest(request.id)
                .filter { it.status == VisitStatus.COMPLETED.name && it.actualEndEpochMs != null }
                .forEach { visit ->
                    upsertDeterministic(
                        id = "history-visit-${visit.id}",
                        organizationId = organizationId,
                        vehicleId = vehicleId,
                        sourceType = ServiceHistorySourceType.SERVICE_VISIT,
                        sourceId = visit.id,
                        title = "Работы завершены",
                        description = visit.resultNote ?: "Выезд по заявке ${request.number}",
                        mileageKm = mileage,
                        occurredAt = requireNotNull(visit.actualEndEpochMs),
                    )
                }
        }

        val requestIds = requests.mapTo(hashSetOf()) { it.id }
        dao.serviceDocuments(organizationId)
            .filter { it.requestId in requestIds && it.status == ServiceDocumentStatus.SIGNED.name }
            .forEach { document ->
                val request = requests.firstOrNull { it.id == document.requestId } ?: return@forEach
                upsertDeterministic(
                    id = "history-document-${document.id}",
                    organizationId = organizationId,
                    vehicleId = vehicleId,
                    sourceType = ServiceHistorySourceType.SERVICE_DOCUMENT,
                    sourceId = document.id,
                    title = "Документ ${document.number} подписан",
                    description = "${document.type} · заявка ${request.number}",
                    mileageKm = mileage,
                    occurredAt = document.issuedAtEpochMs ?: request.closedAtEpochMs ?: document.updatedAtEpochMs,
                )
            }

        dao.payments(organizationId)
            .filter { it.requestId in requestIds && it.status == PaymentStatus.PAID.name && it.paidAtEpochMs != null }
            .forEach { payment ->
                val request = requests.firstOrNull { it.id == payment.requestId } ?: return@forEach
                upsertDeterministic(
                    id = "history-payment-${payment.id}",
                    organizationId = organizationId,
                    vehicleId = vehicleId,
                    sourceType = ServiceHistorySourceType.PAYMENT,
                    sourceId = payment.id,
                    title = "Оплата получена",
                    description = "${payment.amountMinor / 100.0} ${payment.currency} · заявка ${request.number}",
                    mileageKm = mileage,
                    occurredAt = requireNotNull(payment.paidAtEpochMs),
                )
            }
    }

    private suspend fun upsertDeterministic(
        id: String,
        organizationId: String,
        vehicleId: String,
        sourceType: ServiceHistorySourceType,
        sourceId: String?,
        title: String,
        description: String?,
        mileageKm: Int?,
        occurredAt: Long,
    ) {
        dao.insertServiceHistory(
            ServiceHistoryEntity(
                id = id,
                organizationId = organizationId,
                vehicleId = vehicleId,
                sourceType = sourceType.name,
                sourceId = sourceId,
                title = title,
                description = description,
                mileageKm = mileageKm,
                occurredAtEpochMs = occurredAt,
                createdAtEpochMs = occurredAt,
            ),
        )
    }

    companion object {
        fun create(context: Context): ServiceHistoryRepository =
            ServiceHistoryRepository(
                LexoraServiceDatabase.create(context.applicationContext).serviceDao(),
            )
    }
}

private fun ServiceHistoryEntity.toModel() = ServiceHistoryRecord(
    id = id,
    organizationId = organizationId,
    vehicleId = vehicleId,
    sourceType = runCatching { ServiceHistorySourceType.valueOf(sourceType) }
        .getOrDefault(ServiceHistorySourceType.MANUAL),
    sourceId = sourceId,
    title = title,
    description = description,
    mileageKm = mileageKm,
    occurredAtEpochMs = occurredAtEpochMs,
)
