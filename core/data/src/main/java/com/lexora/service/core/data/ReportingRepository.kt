package com.lexora.service.core.data

import com.lexora.service.core.model.ReportSnapshot

/** SRV-000094 — reporting contract for operational and management analytics. */
interface ReportingRepository {
    suspend fun snapshot(
        organizationId: String,
        branchIds: Set<String> = emptySet(),
        fromEpochMs: Long? = null,
        toEpochMs: Long? = null,
    ): ReportSnapshot

    suspend fun exportCsv(
        organizationId: String,
        reportCode: String,
        branchIds: Set<String> = emptySet(),
    ): ByteArray
}
