package com.lexora.service.core.model

enum class BuildResultStatus { NOT_RUN, RUNNING, SUCCEEDED, FAILED }
enum class ReleaseChannel { DEVELOPMENT, TEST, STAGING, PRODUCTION }

data class BuildRecord(
    val id: String,
    val versionName: String,
    val versionCode: Int,
    val commitSha: String,
    val channel: ReleaseChannel,
    val status: BuildResultStatus,
    val artifactName: String? = null,
    val startedAtEpochMs: Long? = null,
    val finishedAtEpochMs: Long? = null,
    val diagnosticsPath: String? = null,
)

data class ReleaseReadiness(
    val buildId: String,
    val cleanBuildPassed: Boolean,
    val criticalChecksPassed: Boolean,
    val smokePassed: Boolean,
    val migrationsPassed: Boolean,
    val documentationCurrent: Boolean,
    val knownLimitations: List<String> = emptyList(),
)
