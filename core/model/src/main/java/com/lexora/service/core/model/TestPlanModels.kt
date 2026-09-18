package com.lexora.service.core.model

enum class TestLevel { UNIT, REPOSITORY, UI, API_INTEGRATION, SMOKE, MANUAL, REGRESSION }
enum class TestCaseStatus { NOT_RUN, PASSED, FAILED, BLOCKED, SKIPPED }

data class TestCaseDefinition(
    val id: String,
    val srvId: String,
    val level: TestLevel,
    val title: String,
    val preconditions: List<String> = emptyList(),
    val steps: List<String>,
    val expectedResult: String,
)

data class TestCaseResult(
    val testCaseId: String,
    val status: TestCaseStatus,
    val executedAtEpochMs: Long? = null,
    val buildVersion: String? = null,
    val notes: String? = null,
)
