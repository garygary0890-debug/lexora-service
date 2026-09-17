package com.lexora.service.core.model

enum class DiagnosticSeverity { DEBUG, INFO, WARNING, ERROR, FATAL }

data class DiagnosticEvent(
    val id: String,
    val component: String,
    val severity: DiagnosticSeverity,
    val code: String,
    val occurredAtEpochMs: Long,
    val correlationId: String? = null,
    val organizationId: String? = null,
    val userId: String? = null,
    val safeMessage: String,
)

data class RuntimeMetric(
    val name: String,
    val value: Double,
    val unit: String,
    val measuredAtEpochMs: Long,
    val tags: Map<String, String> = emptyMap(),
)

data class ComponentHealth(
    val component: String,
    val healthy: Boolean,
    val checkedAtEpochMs: Long,
    val latencyMs: Long? = null,
    val dependencyFailures: List<String> = emptyList(),
)
