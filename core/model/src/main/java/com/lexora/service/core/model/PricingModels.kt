package com.lexora.service.core.model

data class PriceList(
    val id: String,
    val organizationId: String,
    val branchId: String? = null,
    val moduleCode: String? = null,
    val clientTypeCode: String? = null,
    val name: String,
    val validFromEpochMs: Long? = null,
    val validToEpochMs: Long? = null,
    val active: Boolean = true,
)

data class PriceListItem(
    val id: String,
    val priceListId: String,
    val code: String,
    val name: String,
    val categoryCode: String? = null,
    val unit: String,
    val priceMinor: Long,
    val taxRateBasisPoints: Int? = null,
    val durationMinutes: Int? = null,
    val active: Boolean = true,
)

data class AppliedPriceSnapshot(
    val priceListId: String,
    val priceListItemId: String,
    val itemCode: String,
    val unitPriceMinor: Long,
    val capturedAtEpochMs: Long,
)
