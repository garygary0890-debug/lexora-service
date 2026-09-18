package com.lexora.service.core.model

enum class SmokeCheckStatus { NOT_RUN, PASSED, FAILED, BLOCKED }

data class SmokeCheck(
    val id: String,
    val title: String,
    val scenarioCode: CriticalScenarioCode? = null,
    val requiredForRelease: Boolean = true,
)

data class SmokeRun(
    val id: String,
    val versionName: String,
    val versionCode: Int,
    val commitSha: String?,
    val deviceOrEmulator: String,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long? = null,
    val results: Map<String, SmokeCheckStatus> = emptyMap(),
)
