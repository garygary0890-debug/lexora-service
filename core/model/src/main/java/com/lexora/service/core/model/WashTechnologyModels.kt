package com.lexora.service.core.model

enum class WashPostStatus { FREE, RESERVED, OCCUPIED, PREPARING, UNAVAILABLE }

data class WashPost(
    val id: String,
    val organizationId: String,
    val branchId: String,
    val name: String,
    val supportedServiceCodes: Set<String> = emptySet(),
    val status: WashPostStatus = WashPostStatus.FREE,
    val active: Boolean = true,
)

data class WashTechnologyStep(
    val code: String,
    val title: String,
    val order: Int,
    val required: Boolean = true,
    val normativeMinutes: Int? = null,
    val qualityCheckpoint: Boolean = false,
    val photoRequired: Boolean = false,
)

data class WashTechnologyCard(
    val id: String,
    val organizationId: String,
    val serviceCode: String,
    val version: Int,
    val steps: List<WashTechnologyStep>,
    val active: Boolean = true,
)

data class ChemicalConsumption(
    val id: String,
    val organizationId: String,
    val washOrderId: String,
    val materialId: String,
    val normativeQuantity: Double? = null,
    val actualQuantity: Double,
    val recordedAtEpochMs: Long,
)
