package com.lexora.service.core.model

enum class RequiredSystemState {
    LOADING,
    EMPTY,
    CONTENT,
    OFFLINE,
    SYNC_ERROR,
    PERMISSION_DENIED,
    FATAL_TECHNICAL_ERROR,
}

data class SyncIssue(
    val pendingCount: Int = 0,
    val retryCount: Int = 0,
    val failedCount: Int = 0,
    val conflictCount: Int = 0,
    val lastError: String? = null,
) {
    val requiresUserAction: Boolean get() = failedCount > 0 || conflictCount > 0
}

data class SystemUiState(
    val primary: RequiredSystemState = RequiredSystemState.LOADING,
    val offline: Boolean = false,
    val syncIssue: SyncIssue? = null,
    val permissionMessage: String? = null,
    val technicalReference: String? = null,
)
