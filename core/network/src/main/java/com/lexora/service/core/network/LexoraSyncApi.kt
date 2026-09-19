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

data class SyncChangesPage(
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

class LexoraSyncApi(private val client: VersionedApiClient) {
    suspend fun changes(organizationId: String, after: Long, limit: Int = 200): SyncChangesPage {
        require(organizationId.isNotBlank())
        require(after >= 0L)
        require(limit in 1..500)
        val response = client.execute(
            ApiRequest(
                method = HttpMethod.GET,
                path = "organizations/$organizationId/sync/changes",
                query = mapOf("after" to after.toString(), "limit" to limit.toString()),
                organizationId = organizationId,
            ),
        )
        require(response.successful) { "Sync pull failed (HTTP ${response.statusCode})" }
        val root = JSONObject(response.body.orEmpty())
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
                        payloadJson = item.optString("payloadJson").takeIf { it.isNotBlank() && it != "null" },
                    ),
                )
            }
        }
        return SyncChangesPage(items, root.optLong("nextCursor", after))
    }

    suspend fun push(organizationId: String, mutations: List<SyncMutation>): List<SyncMutationResult> {
        if (mutations.isEmpty()) return emptyList()
        val items = JSONArray()
        mutations.forEach { mutation ->
            items.put(
                JSONObject()
                    .put("clientMutationId", mutation.clientMutationId)
                    .put("mutationType", mutation.mutationType)
                    .put("entityType", mutation.entityType)
                    .put("entityId", mutation.entityId)
                    .apply {
                        mutation.expectedVersion?.let { put("expectedVersion", it) }
                        mutation.payloadJson?.let { put("payload", JSONObject(it)) }
                    },
            )
        }
        val response = client.execute(
            ApiRequest(
                method = HttpMethod.POST,
                path = "organizations/$organizationId/sync/push",
                organizationId = organizationId,
                idempotencyKey = mutations.joinToString("|") { it.clientMutationId },
                body = JSONObject().put("items", items).toString(),
            ),
        )
        require(response.successful) { "Sync push failed (HTTP ${response.statusCode})" }
        val responseItems = JSONObject(response.body.orEmpty()).optJSONArray("items") ?: JSONArray()
        return buildList {
            for (index in 0 until responseItems.length()) {
                val item = responseItems.getJSONObject(index)
                add(
                    SyncMutationResult(
                        clientMutationId = item.getString("clientMutationId"),
                        status = item.getString("status"),
                        resultCode = item.optString("resultCode").takeIf { it.isNotBlank() && it != "null" },
                        expectedVersion = item.optLongOrNull("expectedVersion"),
                        resultingVersion = item.optLongOrNull("resultingVersion"),
                        currentVersion = item.optLongOrNull("currentVersion"),
                    ),
                )
            }
        }
    }
}

private fun JSONObject.optLongOrNull(name: String): Long? =
    if (has(name) && !isNull(name)) getLong(name) else null
