package com.lexora.service.core.model

data class RegressionTarget(
    val id: String,
    val area: String,
    val scenarioCodes: Set<CriticalScenarioCode> = emptySet(),
    val relatedSrvIds: Set<String> = emptySet(),
    val requiredThemes: Set<String> = emptySet(),
    val requiredDeviceClasses: Set<String> = emptySet(),
)

data class RegressionRun(
    val id: String,
    val versionName: String,
    val commitSha: String?,
    val targetResults: Map<String, TestCaseStatus>,
    val executedAtEpochMs: Long,
)
