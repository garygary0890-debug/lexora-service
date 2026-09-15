package com.lexora.service.core.model

enum class ServiceHistorySourceType {
    SERVICE_REQUEST,
    SERVICE_VISIT,
    SERVICE_DOCUMENT,
    PAYMENT,
    MANUAL,
}

data class ServiceHistoryRecord(
    val id: String,
    val organizationId: String,
    val vehicleId: String,
    val sourceType: ServiceHistorySourceType,
    val sourceId: String? = null,
    val title: String,
    val description: String? = null,
    val mileageKm: Int? = null,
    val occurredAtEpochMs: Long,
)
