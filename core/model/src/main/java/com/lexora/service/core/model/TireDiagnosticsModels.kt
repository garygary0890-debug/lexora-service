package com.lexora.service.core.model

data class TireWheelCondition(
    val positionCode: String,
    val treadDepthMm: Double? = null,
    val pressureKpa: Double? = null,
    val tireBrand: String? = null,
    val tireModel: String? = null,
    val productionCode: String? = null,
    val damageCodes: Set<String> = emptySet(),
    val rimCondition: String? = null,
    val recommendation: String? = null,
    val attachmentIds: List<String> = emptyList(),
)

data class TireDiagnosticRecord(
    val id: String,
    val organizationId: String,
    val vehicleId: String,
    val tireSetId: String? = null,
    val tireOrderId: String? = null,
    val inspectedAtEpochMs: Long,
    val inspectorEmployeeId: String? = null,
    val wheels: List<TireWheelCondition>,
)

data class TireStorageRecord(
    val id: String,
    val organizationId: String,
    val branchId: String,
    val vehicleId: String,
    val tireSetId: String,
    val locationCode: String,
    val acceptedAtEpochMs: Long,
    val dueAtEpochMs: Long? = null,
    val releasedAtEpochMs: Long? = null,
    val conditionNote: String? = null,
    val attachmentIds: List<String> = emptyList(),
)
