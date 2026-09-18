package com.lexora.service.core.model

enum class BuildDiagnosticStatus { NO_BUILD_YET, RUNNING, SUCCEEDED, FAILED, CANCELLED }

data class BuildDiagnosticSummary(
    val buildId: String,
    val versionName: String?,
    val versionCode: Int?,
    val commitSha: String?,
    val variant: String,
    val status: BuildDiagnosticStatus,
    val startedAtEpochMs: Long? = null,
    val finishedAtEpochMs: Long? = null,
    val logPath: String? = null,
    val artifactPath: String? = null,
    val failureSummary: String? = null,
)
