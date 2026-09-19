package com.lexora.service.core.domain

import com.lexora.service.core.model.RequiredSystemState
import com.lexora.service.core.model.SyncIssue
import com.lexora.service.core.model.SystemUiState

class SystemStatePolicy {
    fun resolve(
        loading: Boolean,
        hasContent: Boolean,
        online: Boolean,
        permissionGranted: Boolean = true,
        permissionMessage: String? = null,
        syncIssue: SyncIssue? = null,
        technicalReference: String? = null,
    ): SystemUiState {
        val primary = when {
            technicalReference != null -> RequiredSystemState.FATAL_TECHNICAL_ERROR
            !permissionGranted -> RequiredSystemState.PERMISSION_DENIED
            loading && !hasContent -> RequiredSystemState.LOADING
            !hasContent -> RequiredSystemState.EMPTY
            !online -> RequiredSystemState.OFFLINE
            syncIssue?.requiresUserAction == true -> RequiredSystemState.SYNC_ERROR
            else -> RequiredSystemState.CONTENT
        }
        return SystemUiState(
            primary = primary,
            offline = !online,
            syncIssue = syncIssue,
            permissionMessage = permissionMessage,
            technicalReference = technicalReference,
        )
    }
}
