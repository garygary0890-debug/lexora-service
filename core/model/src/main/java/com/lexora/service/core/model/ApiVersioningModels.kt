package com.lexora.service.core.model

data class ApiVersion(
    val major: Int,
    val minor: Int = 0,
    val deprecated: Boolean = false,
    val sunsetAtEpochMs: Long? = null,
)

data class ApiCompatibilityRange(
    val minimumClientVersionCode: Int,
    val maximumClientVersionCode: Int? = null,
    val supportedApiVersions: Set<Int>,
)

data class ApiRequestContext(
    val requestId: String,
    val apiVersion: Int,
    val organizationId: String?,
    val userId: String?,
    val clientVersionCode: Int,
    val idempotencyKey: String? = null,
)

data class ApiCompatibilityDecision(
    val allowed: Boolean,
    val selectedApiVersion: Int?,
    val upgradeRequired: Boolean = false,
    val reasonCode: String? = null,
)
