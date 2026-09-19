package com.lexora.service.core.model

enum class IntegrationState { PLANNED, READY_FOR_CONFIGURATION, CONNECTED, ERROR }

data class IntegrationDescriptor(
    val id: String,
    val title: String,
    val description: String,
    val state: IntegrationState,
    val supportsIdempotency: Boolean = true,
    val supportsRetry: Boolean = true,
)
