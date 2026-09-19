package com.lexora.service.core.network

import org.json.JSONArray
import org.json.JSONObject

data class RemoteSyncChange(
    val cursor: Long,
    val entityType: String,
    val entityId: String,
    val entityVersion: Long,
    val changeKind: String,
    val tombstone: Boolean,
    val payloadJson: String?,
)

data class RemoteSyncPage(
    val items: List<RemoteSyncChange>,
    val nextCursor: Long,
)

data class SyncMutation(
    val clientMutationId: String,
    val mutationType: String,
    val entityType: String,
    val entityId: String,
    val expectedVersion: Long? = null,
    val payloadJson: String? = null,
)

data class SyncMutationResult(
    val clientMutationId: String,
    val status: String,
    val resultCode: String?,
    val expectedVersion: Long?,
    val resultingVersion: Long?,
    val currentVersion: Long?,
)

class LexoraBackendSyncApi(private val client: VersionedApiClient) {
    suspend fun pull(organizationId: String, afterCursor: Long, limit: Int = 200): RemoteSyncPage {
        require(limit in 1..500)
        val response = client.execute(
            ApiRequest(
                method = HttpMethod.GET,
                path = "organizations/$organizationId/sync/changes",
                query = mapOf("after" to afterCursor.toString(), "limit" to limit.toString()),
                organizationId = organizationId,
            ),
        )
        requireSuccess(response, "Sync pull failed")
        val root = JSONObject(response.body ?: "{}")
        val array = root.optJSONArray("items") ?: JSONArray()
        val items = buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    RemoteSyncChange(
                        cursor = item.getLong("cursor"),
                        entityType = item.getString("entityType"),
                        entityId = item.getString("entityId"),
                        entityVersion = item.getLong("entityVersion"),
                        changeKind = item.getString("changeKind"),
                        tombstone = item.optBoolean("tombstone", false),
                        payloadJson = item.opt("payloadJson")?.takeUnless { it == JSONObject.NULL }?.toString(),
                    ),
                )
            }
        }
        return RemoteSyncPage(items, root.optLong("nextCursor", afterCursor))
    }

    suspend fun push(organizationId: String, mutations: List<SyncMutation>): List<SyncMutationResult> {
        if (mutations.isEmpty()) return emptyList()
        val items = JSONArray()
        mutations.forEach { mutation ->
            val item = JSONObject()
                .put("clientMutationId", mutation.clientMutationId)
                .put("mutationType", mutation.mutationType)
                .put("entityType", mutation.entityType)
                .put("entityId", mutation.entityId)
            mutation.expectedVersion?.let { item.put("expectedVersion", it) }
            mutation.payloadJson?.let { raw ->
                item.put("payload", runCatching { JSONObject(raw) }.getOrElse { raw })
            }
            items.put(item)
        }
        val response = client.execute(
            ApiRequest(
                method = HttpMethod.POST,
                path = "organizations/$organizationId/sync/push",
                body = JSONObject().put("items", items).toString(),
                idempotencyKey = "sync:$organizationId:${mutations.joinToString(",") { it.clientMutationId }}",
                organizationId = organizationId,
            ),
        )
        requireSuccess(response, "Sync push failed")
        val resultArray = JSONObject(response.body ?: "{}").optJSONArray("items") ?: JSONArray()
        return buildList {
            for (index in 0 until resultArray.length()) {
                val item = resultArray.getJSONObject(index)
                add(
                    SyncMutationResult(
                        clientMutationId = item.getString("clientMutationId"),
                        status = item.getString("status"),
                        resultCode = item.optNullableString("resultCode"),
                        expectedVersion = item.optNullableLong("expectedVersion"),
                        resultingVersion = item.optNullableLong("resultingVersion"),
                        currentVersion = item.optNullableLong("currentVersion"),
                    ),
                )
            }
        }
    }

    private fun requireSuccess(response: ApiResponse, fallback: String) {
        if (response.successful) return
        val message = runCatching { JSONObject(response.body.orEmpty()).optString("message") }.getOrDefault("")
        throw BackendApiException(response.statusCode, response.correlationId, message.ifBlank { fallback })
    }
}

private fun JSONObject.optNullableString(name: String): String? =
    opt(name)?.takeUnless { it == JSONObject.NULL }?.toString()?.takeIf { it.isNotBlank() }

private fun JSONObject.optNullableLong(name: String): Long? =
    opt(name)?.takeUnless { it == JSONObject.NULL }?.let { value ->
        when (value) {
            is Number -> value.toLong()
            else -> value.toString().toLongOrNull()
        }
    }
