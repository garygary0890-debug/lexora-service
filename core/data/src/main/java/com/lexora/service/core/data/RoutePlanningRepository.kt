package com.lexora.service.core.data

import com.lexora.service.core.model.OptimizedRoute
import com.lexora.service.core.model.RouteStop

/** SRV-000087 — route optimization foundation for field service. */
interface RoutePlanningRepository {
    suspend fun buildRoute(
        organizationId: String,
        employeeId: String,
        branchId: String?,
        stops: List<RouteStop>,
    ): OptimizedRoute

    suspend fun saveManualRoute(organizationId: String, route: OptimizedRoute): OptimizedRoute
    suspend fun currentRoute(organizationId: String, employeeId: String): OptimizedRoute?
}
