package com.lexora.service.core.data

import com.lexora.service.core.database.AuditEventEntity
import com.lexora.service.core.database.PortalAccessGrantEntity
import com.lexora.service.core.database.PublicBookingEntity
import com.lexora.service.core.database.PublicPortalDao
import com.lexora.service.core.database.ServiceDao
import com.lexora.service.core.model.PortalAccessGrant
import com.lexora.service.core.model.PortalAccessStatus
import com.lexora.service.core.model.PublicBooking
import com.lexora.service.core.model.PublicBookingStatus
import com.lexora.service.core.model.SyncState
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

class PublicPortalRepository(
    private val portalDao: PublicPortalDao,
    private val serviceDao: ServiceDao,
) {
    suspend fun bookings(organizationId: String): List<PublicBooking> =
        portalDao.bookings(organizationId).map(PublicBookingEntity::toModel)

    suspend fun registerBooking(
        organizationId: String,
        branchId: String?,
        clientId: String?,
        vehicleId: String?,
        serviceCatalogItemId: String?,
        contactName: String,
        phone: String?,
        email: String?,
        desiredAtEpochMs: Long?,
        comment: String?,
        source: String = "WEB",
    ): PublicBooking {
        require(contactName.isNotBlank()) { "Имя клиента обязательно" }
        require(!phone.isNullOrBlank() || !email.isNullOrBlank()) { "Необходим телефон или email для связи" }
        val now = System.currentTimeMillis()
        val entity = PublicBookingEntity(
            id = UUID.randomUUID().toString(),
            organizationId = organizationId,
            branchId = branchId,
            clientId = clientId,
            vehicleId = vehicleId,
            serviceCatalogItemId = serviceCatalogItemId,
            contactName = contactName.trim(),
            phone = phone?.trim()?.ifBlank { null },
            email = email?.trim()?.ifBlank { null },
            desiredAtEpochMs = desiredAtEpochMs,
            comment = comment?.trim()?.ifBlank { null },
            status = PublicBookingStatus.NEW.name,
            convertedRequestId = null,
            source = source,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            syncState = SyncState.PENDING_CREATE.name,
        )
        portalDao.upsertBooking(entity)
        audit(organizationId, null, "PUBLIC_BOOKING", entity.id, "CREATE", "Создана публичная запись")
        return entity.toModel()
    }

    suspend fun changeBookingStatus(
        organizationId: String,
        userId: String,
        bookingId: String,
        target: PublicBookingStatus,
        convertedRequestId: String? = null,
    ) {
        val current = portalDao.booking(bookingId) ?: error("Онлайн-запись не найдена")
        require(current.organizationId == organizationId) { "Запись относится к другой организации" }
        val from = PublicBookingStatus.valueOf(current.status)
        require(isAllowed(from, target)) { "Недопустимый переход статуса: $from → $target" }
        if (target == PublicBookingStatus.CONVERTED_TO_REQUEST) {
            require(!convertedRequestId.isNullOrBlank()) { "Для конвертации требуется requestId" }
        }
        portalDao.updateBookingStatus(
            id = bookingId,
            status = target.name,
            convertedRequestId = convertedRequestId ?: current.convertedRequestId,
            syncState = SyncState.PENDING_UPDATE.name,
            updatedAtEpochMs = System.currentTimeMillis(),
        )
        audit(organizationId, userId, "PUBLIC_BOOKING", bookingId, "STATUS_CHANGE", "$from → $target")
    }

    /**
     * Возвращает открытый токен только один раз. В БД сохраняется исключительно SHA-256 hash.
     */
    suspend fun issueAccessGrant(
        organizationId: String,
        userId: String,
        clientId: String,
        expiresAtEpochMs: Long,
    ): IssuedPortalAccess {
        val now = System.currentTimeMillis()
        require(expiresAtEpochMs > now) { "Срок доступа должен быть в будущем" }
        val rawToken = generateToken()
        val entity = PortalAccessGrantEntity(
            id = UUID.randomUUID().toString(),
            organizationId = organizationId,
            clientId = clientId,
            tokenHash = sha256(rawToken),
            status = PortalAccessStatus.ACTIVE.name,
            expiresAtEpochMs = expiresAtEpochMs,
            revokedAtEpochMs = null,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            syncState = SyncState.PENDING_CREATE.name,
        )
        portalDao.upsertAccessGrant(entity)
        audit(organizationId, userId, "PORTAL_ACCESS", entity.id, "ISSUE", "Выдан доступ к клиентскому кабинету")
        return IssuedPortalAccess(rawToken = rawToken, grant = entity.toModel())
    }

    suspend fun validateAccessToken(
        organizationId: String,
        rawToken: String,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): PortalAccessGrant? {
        if (rawToken.isBlank()) return null
        val entity = portalDao.accessGrantByTokenHash(organizationId, sha256(rawToken)) ?: return null
        if (entity.status != PortalAccessStatus.ACTIVE.name || entity.expiresAtEpochMs <= nowEpochMs) return null
        return entity.toModel()
    }

    suspend fun revokeAccessGrant(organizationId: String, userId: String, grantId: String, clientId: String) {
        val grants = portalDao.accessGrants(organizationId, clientId)
        val current = grants.firstOrNull { it.id == grantId } ?: error("Доступ не найден")
        val now = System.currentTimeMillis()
        portalDao.updateAccessGrantStatus(
            id = current.id,
            status = PortalAccessStatus.REVOKED.name,
            revokedAtEpochMs = now,
            syncState = SyncState.PENDING_UPDATE.name,
            updatedAtEpochMs = now,
        )
        audit(organizationId, userId, "PORTAL_ACCESS", current.id, "REVOKE", "Доступ к клиентскому кабинету отозван")
    }

    private fun isAllowed(from: PublicBookingStatus, to: PublicBookingStatus): Boolean = when (from) {
        PublicBookingStatus.NEW -> to in setOf(PublicBookingStatus.CONFIRMED, PublicBookingStatus.CANCELLED, PublicBookingStatus.REJECTED)
        PublicBookingStatus.CONFIRMED -> to in setOf(PublicBookingStatus.CONVERTED_TO_REQUEST, PublicBookingStatus.CANCELLED)
        PublicBookingStatus.CONVERTED_TO_REQUEST,
        PublicBookingStatus.CANCELLED,
        PublicBookingStatus.REJECTED -> false
    }

    private suspend fun audit(
        organizationId: String,
        userId: String?,
        entityType: String,
        entityId: String,
        action: String,
        summary: String,
    ) {
        serviceDao.insertAuditEvent(
            AuditEventEntity(
                id = UUID.randomUUID().toString(),
                organizationId = organizationId,
                userId = userId ?: "PUBLIC",
                entityType = entityType,
                entityId = entityId,
                action = action,
                summary = summary,
                occurredAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

data class IssuedPortalAccess(
    val rawToken: String,
    val grant: PortalAccessGrant,
)

private fun PublicBookingEntity.toModel() = PublicBooking(
    id = id,
    organizationId = organizationId,
    branchId = branchId,
    clientId = clientId,
    vehicleId = vehicleId,
    serviceCatalogItemId = serviceCatalogItemId,
    contactName = contactName,
    phone = phone,
    email = email,
    desiredAtEpochMs = desiredAtEpochMs,
    comment = comment,
    status = PublicBookingStatus.valueOf(status),
    convertedRequestId = convertedRequestId,
    source = source,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    syncState = SyncState.valueOf(syncState),
)

private fun PortalAccessGrantEntity.toModel() = PortalAccessGrant(
    id = id,
    organizationId = organizationId,
    clientId = clientId,
    tokenHash = tokenHash,
    status = PortalAccessStatus.valueOf(status),
    expiresAtEpochMs = expiresAtEpochMs,
    revokedAtEpochMs = revokedAtEpochMs,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    syncState = SyncState.valueOf(syncState),
)
