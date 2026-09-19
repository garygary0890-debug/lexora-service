package com.lexora.service.core.network

import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime

class TrustedAdminSsoApi(
    private val configuration: ApiConfiguration,
    private val transport: HttpTransport,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun exchange(
        authorizationCode: String,
        codeVerifier: String,
        targetProductCode: String,
    ): AuthTokens {
        require(authorizationCode.isNotBlank())
        require(codeVerifier.length in 43..128)
        require(targetProductCode.isNotBlank())
        val response = transport.execute(
            ApiRequest(
                method = HttpMethod.POST,
                path = "/api/${configuration.apiVersion}/auth/admin-sso/exchange",
                headers = mapOf("Accept" to "application/json"),
                body = JSONObject()
                    .put("authorizationCode", authorizationCode)
                    .put("codeVerifier", codeVerifier)
                    .put("targetProductCode", targetProductCode)
                    .toString(),
            ),
        )
        require(response.successful) {
            val message = runCatching { JSONObject(response.body.orEmpty()).optString("message") }.getOrNull().orEmpty()
            message.ifBlank { "Trusted Lexora Admin SSO exchange failed (HTTP ${response.statusCode})" }
        }
        return parseTokens(JSONObject(response.body.orEmpty()))
    }

    private fun parseTokens(json: JSONObject): AuthTokens {
        val accessTtlSeconds = json.optLong("accessTokenExpiresInSeconds", 900L)
        return AuthTokens(
            accessToken = json.getString("accessToken"),
            accessExpiresAtEpochMs = clock() + accessTtlSeconds * 1000L,
            refreshToken = json.getString("refreshToken"),
            refreshExpiresAtEpochMs = OffsetDateTime.parse(json.getString("refreshTokenExpiresAt")).toInstant().toEpochMilli(),
            organizationId = json.optString("organizationId").takeIf(String::isNotBlank),
            membershipId = json.optString("membershipId").takeIf(String::isNotBlank),
            permissions = json.optJSONArray("permissions").toStringSet(),
            globalOwner = json.optBoolean("globalOwner", false),
        )
    }
}

private fun JSONArray?.toStringSet(): Set<String> {
    if (this == null) return emptySet()
    return buildSet {
        for (index in 0 until length()) optString(index).takeIf(String::isNotBlank)?.let(::add)
    }
}
