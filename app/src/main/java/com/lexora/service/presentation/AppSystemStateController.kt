package com.lexora.service.presentation

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lexora.service.core.data.SyncQueueRepository
import com.lexora.service.core.domain.SystemStatePolicy
import com.lexora.service.core.model.SyncIssue
import com.lexora.service.core.model.SystemUiState
import com.lexora.service.core.network.AndroidConnectivityMonitor
import java.util.UUID

class AppSystemStateController(
    private val connectivity: AndroidConnectivityMonitor,
    private val syncQueue: SyncQueueRepository,
    private val policy: SystemStatePolicy = SystemStatePolicy(),
) {
    var online by mutableStateOf(connectivity.isOnline())
        private set
    var loading by mutableStateOf(true)
        private set
    var syncIssue by mutableStateOf<SyncIssue?>(null)
        private set
    var technicalReference by mutableStateOf<String?>(null)
        private set

    fun start(): AutoCloseable = connectivity.register { online = it }
    fun updateLoading(value: Boolean) { loading = value }

    suspend fun refreshSyncIssue(organizationId: String) {
        syncIssue = syncQueue.syncIssue(organizationId)
    }

    fun state(
        hasContent: Boolean,
        permissionGranted: Boolean = true,
        permissionMessage: String? = null,
    ): SystemUiState = policy.resolve(
        loading = loading,
        hasContent = hasContent,
        online = online,
        permissionGranted = permissionGranted,
        permissionMessage = permissionMessage,
        syncIssue = syncIssue,
        technicalReference = technicalReference,
    )

    fun reportTechnicalError(error: Throwable): String {
        val reference = UUID.randomUUID().toString().take(8).uppercase()
        Log.e("LexoraService", "Technical error [$reference]", error)
        technicalReference = reference
        return reference
    }

    fun clearTechnicalError() { technicalReference = null }
}
