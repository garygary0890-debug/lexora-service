package com.lexora.service.core.data

import com.lexora.service.core.database.ClientEntity
import com.lexora.service.core.database.ServiceDao
import com.lexora.service.core.database.ServiceDocumentEntity
import com.lexora.service.core.database.ServiceObjectEntity
import com.lexora.service.core.database.VehicleEntity
import com.lexora.service.core.model.SyncOperation
import com.lexora.service.core.model.SyncOperationType
import com.lexora.service.core.network.RemoteSyncChange
import com.lexora.service.core.network.SyncMutation
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode

class LexoraServiceMutationMapper(private val metadata: ServiceSyncMetadataStore) {
    fun map(operation: SyncOperation): SyncMutation? {
        if (operation.operationType == SyncOperationType.DELETE) return null
        val source = operation.payloadJson?.let(::JSONObject) ?: JSONObject()
        return when (operation.entityType.lowercase()) {
            "client", "serviceclient" -> SyncMutation(
                clientMutationId = operation.idempotencyKey,
                mutationType = "service.client.upsert",
                entityType = "ServiceClient",
                entityId = operation.entityId,
                expectedVersion = metadata.version(operation.organizationId, "ServiceClient", operation.entityId),
                payloadJson = JSONObject()
                    .put("displayName", source.optString("displayName"))
                    .putNullable("phone", source.optNullableString("phone"))
                    .putNullable("email", source.optNullableString("email"))
                    .toString(),
            )

            "vehicle", "serviceasset" -> {
                val clientId = source.optNullableString("clientId") ?: return null
                val registration = source.optNullableString("registrationNumber")
                    ?: source.optNullableString("externalIdentifier")
                    ?: operation.entityId
                val display = listOfNotNull(
                    source.optNullableString("make"),
                    source.optNullableString("model"),
                    registration,
                ).joinToString(" ").ifBlank { registration }
                SyncMutation(
                    clientMutationId = operation.idempotencyKey,
                    mutationType = "service.asset.upsert",
                    entityType = "ServiceAsset",
                    entityId = operation.entityId,
                    expectedVersion = metadata.version(operation.organizationId, "ServiceAsset", operation.entityId),
                    payloadJson = JSONObject()
                        .put("clientId", clientId)
                        .put("assetType", "VEHICLE")
                        .put("displayName", display)
                        .put("externalIdentifier", registration)
                        .toString(),
                )
            }

            "servicedocument", "serviceworkorder" -> {
                val type = source.optString("type", "WORK_ORDER")
                if (!type.equals("WORK_ORDER", ignoreCase = true)) return null
                val clientId = source.optNullableString("clientId") ?: return null
                val totalMinor = source.optLong("totalMinor", 0L)
                SyncMutation(
                    clientMutationId = operation.idempotencyKey,
                    mutationType = "service.work-order.upsert",
                    entityType = "ServiceWorkOrder",
                    entityId = operation.entityId,
                    expectedVersion = metadata.version(operation.organizationId, "ServiceWorkOrder", operation.entityId),
                    payloadJson = JSONObject()
                        .put("clientId", clientId)
                        .putNullable("assetId", source.optNullableString("assetId") ?: source.optNullableString("vehicleId"))
                        .put("orderNumber", source.optNullableString("number") ?: source.optNullableString("orderNumber") ?: operation.entityId)
                        .put("status", source.optNullableString("status") ?: "DRAFT")
                        .put("totalAmount", BigDecimal.valueOf(totalMinor, 2).toPlainString())
                        .toString(),
                )
            }

            else -> null
        }
    }
}

class LexoraServiceRemoteChangeApplier(
    private val dao: ServiceDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun apply(organizationId: String, change: RemoteSyncChange) {
        require(change.entityVersion >= 0L)
        when (change.entityType) {
            "ServiceClient" -> applyClient(organizationId, change)
            "ServiceAsset" -> applyAsset(organizationId, change)
            "ServiceWorkOrder" -> applyWorkOrder(organizationId, change)
            // Other Lexora products share the same feed. Service ignores foreign entity types safely.
            else -> Unit
        }
    }

    private suspend fun applyClient(organizationId: String, change: RemoteSyncChange) {
        if (change.tombstone) {
            dao.archiveClient(change.entityId, "SYNCED", now())
            return
        }
        val payload = JSONObject(requireNotNull(change.payloadJson) { "ServiceClient payload is required" })
        val previous = dao.client(change.entityId)
        val timestamp = now()
        dao.upsertClient(
            ClientEntity(
                id = change.entityId,
                organizationId = organizationId,
                type = previous?.type ?: "PERSON",
                displayName = payload.optNullableString("displayName") ?: previous?.displayName ?: change.entityId,
                phone = payload.optNullableString("phone") ?: previous?.phone,
                email = payload.optNullableString("email") ?: previous?.email,
                taxId = previous?.taxId,
                kpp = previous?.kpp,
                registrationAddress = previous?.registrationAddress,
                actualAddress = previous?.actualAddress,
                note = previous?.note,
                consentPersonalData = previous?.consentPersonalData ?: false,
                archived = false,
                syncState = "SYNCED",
                createdAtEpochMs = previous?.createdAtEpochMs ?: timestamp,
                updatedAtEpochMs = timestamp,
            ),
        )
    }

    private suspend fun applyAsset(organizationId: String, change: RemoteSyncChange) {
        if (change.tombstone) {
            dao.archiveVehicle(change.entityId, "SYNCED", now())
            return
        }
        val payload = JSONObject(requireNotNull(change.payloadJson) { "ServiceAsset payload is required" })
        val assetType = payload.optString("assetType", "VEHICLE").uppercase()
        val timestamp = now()
        if (assetType == "VEHICLE") {
            val previous = dao.vehicle(change.entityId)
            dao.upsertVehicle(
                VehicleEntity(
                    id = change.entityId,
                    organizationId = organizationId,
                    clientId = payload.optNullableString("clientId") ?: previous?.clientId,
                    registrationNumber = payload.optNullableString("externalIdentifier")
                        ?: previous?.registrationNumber
                        ?: payload.optNullableString("displayName")
                        ?: change.entityId,
                    vin = previous?.vin,
                    make = previous?.make,
                    model = previous?.model,
                    year = previous?.year,
                    bodyType = previous?.bodyType,
                    color = previous?.color,
                    mileageKm = previous?.mileageKm,
                    archived = false,
                    syncState = "SYNCED",
                    createdAtEpochMs = previous?.createdAtEpochMs ?: timestamp,
                    updatedAtEpochMs = timestamp,
                ),
            )
        } else {
            val previous = dao.serviceObject(change.entityId)
            dao.upsertServiceObject(
                ServiceObjectEntity(
                    id = change.entityId,
                    organizationId = organizationId,
                    clientId = payload.optNullableString("clientId") ?: previous?.clientId,
                    name = payload.optNullableString("displayName") ?: previous?.name ?: change.entityId,
                    address = previous?.address,
                    accessMode = previous?.accessMode,
                    responsibleContact = previous?.responsibleContact,
                    archived = false,
                    syncState = "SYNCED",
                    createdAtEpochMs = previous?.createdAtEpochMs ?: timestamp,
                    updatedAtEpochMs = timestamp,
                ),
            )
        }
    }

    private suspend fun applyWorkOrder(organizationId: String, change: RemoteSyncChange) {
        if (change.tombstone) {
            dao.archiveServiceDocument(change.entityId, "SYNCED", now())
            return
        }
        val payload = JSONObject(requireNotNull(change.payloadJson) { "ServiceWorkOrder payload is required" })
        val previous = dao.serviceDocument(change.entityId)
        val timestamp = now()
        val amount = payload.optNullableString("totalAmount")
            ?.toBigDecimalOrNull()
            ?.multiply(BigDecimal(100))
            ?.setScale(0, RoundingMode.HALF_UP)
            ?.longValueExact()
            ?: previous?.totalMinor
            ?: 0L
        val serverStatus = payload.optNullableString("status")?.uppercase()
        val localStatus = serverStatus?.takeIf { it in setOf("DRAFT", "ISSUED", "SIGNED", "CANCELLED") } ?: "DRAFT"
        dao.upsertServiceDocument(
            ServiceDocumentEntity(
                id = change.entityId,
                organizationId = organizationId,
                requestId = previous?.requestId,
                visitId = previous?.visitId,
                clientId = payload.optNullableString("clientId") ?: previous?.clientId,
                type = "WORK_ORDER",
                number = payload.optNullableString("orderNumber") ?: previous?.number ?: change.entityId,
                status = localStatus,
                issuedAtEpochMs = previous?.issuedAtEpochMs,
                totalMinor = amount,
                currency = previous?.currency ?: "RUB",
                externalFileRef = previous?.externalFileRef,
                note = previous?.note,
                archived = false,
                syncState = "SYNCED",
                createdAtEpochMs = previous?.createdAtEpochMs ?: timestamp,
                updatedAtEpochMs = timestamp,
            ),
        )
    }
}

private fun JSONObject.optNullableString(name: String): String? =
    if (!has(name) || isNull(name)) null else optString(name).trim().takeIf(String::isNotBlank)

private fun JSONObject.putNullable(name: String, value: String?): JSONObject =
    if (value == null) put(name, JSONObject.NULL) else put(name, value)
