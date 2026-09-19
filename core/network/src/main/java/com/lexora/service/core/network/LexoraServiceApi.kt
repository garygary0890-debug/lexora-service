package com.lexora.service.core.network

import org.json.JSONArray
import org.json.JSONObject

data class BackendServiceClient(
    val id: String,
    val organizationId: String?,
    val displayName: String,
    val phone: String?,
    val email: String?,
    val version: Long,
)

data class BackendServiceAsset(
    val id: String,
    val clientId: String,
    val assetType: String,
    val displayName: String,
    val externalIdentifier: String?,
    val version: Long,
)

data class BackendServiceWorkOrder(
    val id: String,
    val clientId: String,
    val assetId: String?,
    val orderNumber: String,
    val status: String,
    val totalAmount: String,
    val version: Long,
)

class LexoraServiceApi(private val client: VersionedApiClient) {
    suspend fun clients(organizationId: String): List<BackendServiceClient> =
        array(client.execute(serviceRequest(HttpMethod.GET, organizationId, "clients")), "Load clients failed") { item ->
            BackendServiceClient(
                id = item.getString("id"),
                organizationId = item.optJSONObject("organization")?.optString("id")?.takeIf(String::isNotBlank),
                displayName = item.getString("displayName"),
                phone = item.optNullableText("phone"),
                email = item.optNullableText("email"),
                version = item.optLong("lockVersion", 0L),
            )
        }

    suspend fun assets(organizationId: String): List<BackendServiceAsset> =
        array(client.execute(serviceRequest(HttpMethod.GET, organizationId, "assets")), "Load assets failed") { item ->
            BackendServiceAsset(
                id = item.getString("id"),
                clientId = item.getJSONObject("client").getString("id"),
                assetType = item.getString("assetType"),
                displayName = item.getString("displayName"),
                externalIdentifier = item.optNullableText("externalIdentifier"),
                version = item.optLong("lockVersion", 0L),
            )
        }

    suspend fun workOrders(organizationId: String): List<BackendServiceWorkOrder> =
        array(client.execute(serviceRequest(HttpMethod.GET, organizationId, "work-orders")), "Load work orders failed") { item ->
            BackendServiceWorkOrder(
                id = item.getString("id"),
                clientId = item.getJSONObject("client").getString("id"),
                assetId = item.optJSONObject("asset")?.optString("id")?.takeIf(String::isNotBlank),
                orderNumber = item.getString("orderNumber"),
                status = item.getString("status"),
                totalAmount = item.opt("totalAmount")?.toString() ?: "0",
                version = item.optLong("lockVersion", 0L),
            )
        }

    suspend fun createClient(
        organizationId: String,
        displayName: String,
        phone: String?,
        email: String?,
        idempotencyKey: String,
    ): ApiResponse = requireSuccess(
        client.execute(
            serviceRequest(
                HttpMethod.POST,
                organizationId,
                "clients",
                JSONObject()
                    .put("displayName", displayName)
                    .put("phone", phone)
                    .put("email", email)
                    .toString(),
                idempotencyKey,
            ),
        ),
        "Create client failed",
    )

    suspend fun createAsset(
        organizationId: String,
        clientId: String,
        displayName: String,
        assetType: String,
        externalIdentifier: String?,
        idempotencyKey: String,
    ): ApiResponse = requireSuccess(
        client.execute(
            serviceRequest(
                HttpMethod.POST,
                organizationId,
                "assets",
                JSONObject()
                    .put("clientId", clientId)
                    .put("displayName", displayName)
                    .put("assetType", assetType)
                    .put("externalIdentifier", externalIdentifier)
                    .toString(),
                idempotencyKey,
            ),
        ),
        "Create asset failed",
    )

    private fun serviceRequest(
        method: HttpMethod,
        organizationId: String,
        suffix: String,
        body: String? = null,
        idempotencyKey: String? = null,
    ) = ApiRequest(
        method = method,
        path = "service/organizations/$organizationId/$suffix",
        body = body,
        idempotencyKey = idempotencyKey,
        organizationId = organizationId,
    )

    private fun requireSuccess(response: ApiResponse, fallback: String): ApiResponse {
        if (response.successful) return response
        val message = runCatching { JSONObject(response.body.orEmpty()).optString("message") }.getOrDefault("")
        throw BackendApiException(response.statusCode, response.correlationId, message.ifBlank { fallback })
    }

    private inline fun <T> array(response: ApiResponse, fallback: String, mapper: (JSONObject) -> T): List<T> {
        requireSuccess(response, fallback)
        val values = JSONArray(response.body ?: "[]")
        return buildList {
            for (index in 0 until values.length()) add(mapper(values.getJSONObject(index)))
        }
    }
}

private fun JSONObject.optNullableText(name: String): String? =
    opt(name)?.takeUnless { it == JSONObject.NULL }?.toString()?.takeIf { it.isNotBlank() }
