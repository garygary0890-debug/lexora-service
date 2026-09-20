package com.lexora.service.core.domain

import com.lexora.service.core.model.ReportHubSnapshot

interface ReportHubOperations {
    suspend fun snapshot(
        organizationId: String,
        branchIds: Set<String>,
        fromEpochMs: Long,
        toEpochMs: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): ReportHubSnapshot

    suspend fun exportCsv(snapshot: ReportHubSnapshot): String
}
