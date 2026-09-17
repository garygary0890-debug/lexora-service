package com.lexora.service.core.model

enum class DeploymentEnvironment { DEVELOPMENT, STAGING, PRODUCTION }

data class EnvironmentEndpoint(
    val environment: DeploymentEnvironment,
    val apiBaseUrl: String,
    val objectStorageBaseUrl: String?,
    val allowDebugLogging: Boolean,
    val allowTestCredentials: Boolean,
)

data class EnvironmentFeatureFlags(
    val environment: DeploymentEnvironment,
    val flags: Map<String, Boolean>,
)

data class EnvironmentValidationResult(
    val environment: DeploymentEnvironment,
    val valid: Boolean,
    val violations: List<String> = emptyList(),
)
