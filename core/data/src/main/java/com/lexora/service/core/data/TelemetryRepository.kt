package com.lexora.service.core.data

import com.lexora.service.core.model.TelemetryEvent
import com.lexora.service.core.model.TelemetryMetric

/** SRV-000089 — IoT and equipment telemetry foundation. */
interface TelemetryRepository {
    suspend fun metrics(organizationId: String, equipmentId: String, fromEpochMs: Long? = null): List<TelemetryMetric>
    suspend fun events(organizationId: String, equipmentId: String, fromEpochMs: Long? = null): List<TelemetryEvent>
    suspend fun recordMetric(metric: TelemetryMetric): TelemetryMetric
    suspend fun recordEvent(event: TelemetryEvent): TelemetryEvent
}
