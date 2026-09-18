package com.lexora.service.core.data

import com.lexora.service.core.model.ClientPortalSnapshot

/** SRV-000091 — client portal data contract prepared for future web/mobile external access. */
interface ClientPortalRepository {
    suspend fun snapshot(organizationId: String, clientId: String): ClientPortalSnapshot
    suspend fun unreadNotifications(organizationId: String, clientId: String): Int
    suspend fun loyaltyBalance(organizationId: String, clientId: String): Long
}
