package com.lexora.service.core.network

import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime

class LexoraBackendAuthApi(
    private val configuration: ApiConfiguration,
    private val transport: HttpTransport,
    private val tokenProvider: TokenProvider,
    private val clock: () -> Long = System::currentTimeMillis,
) : RefreshTokenApi {

    suspend fun login(email: String, password: CharArray, organizationId: String): AuthTokens {
        require(email.isNotBlank())
        require(password.isNotEmpty())
        require(organizationId.isNotBlank())
        val passwordValue = String(password)
        try {
            val body = JSONObject()
                .put("email", email.trim())
                .put("password", passwordValue)
                .put("organizationId", organizationId)
            val response = transport.execute(
                ApiRequest(HttpMethod.POST, versioned("auth/login"), body = body.toString()),
            )
            val tokens = parseTokens(requireSuccess(response, "Login failed"))
            tokenProvider.save(tokens)
            return tokens
        } finally {
            password.fill('\u0000')
        }
    }

    override suspend fun refresh(refreshToken: String): AuthTokens? {
        val response = transport.execute(
            ApiRequest(
                HttpMethod.POST,
                versioned("auth/refresh"),
                body = JSONObject().put("refreshToken", refreshToken).toString(),
            ),
        )
        if (response.statusCode == 400 || response.statusCode == 401 || response.statusCode == 403) {
            tokenProvider.clear()
            return null
        }
        return parseTokens(requireSuccess(response, "Token refresh failed"))
    }

    suspend fun logout() {
        val current = tokenProvider.current()
        if (current == null) return
        try {
            val response = transport.execute(
                ApiRequest(
                    HttpMethod.POST,
                    versioned("auth/logout"),
                    body = JSONObject().put("refreshToken", current.refreshToken).toString(),
                ),
            )
            if (!response.successful && response.statusCode !in setOf(400, 401, 404)) {
                throw BackendApiException(response.statusCode, response.correlationId, apiMessage(response))
            }
        } finally {
            tokenProvider.clear()
        }
    }

    suspend fun touchSession() {
        val current = tokenProvider.current() ?: throw AuthenticationExpiredException("Authentication required")
        val response = transport.execute(
            ApiRequest(
                HttpMethod.POST,
                versioned("auth/session/activity"),
                body = JSONObject().put("refreshToken", current.refreshToken).toString(),
            ),
        )
        requireSuccess(response, "Session activity update failed")
    }

    suspend fun sessions(client: VersionedApiClient): List<BackendSession> {
        val response = requireSuccess(
            client.execute(ApiRequest(HttpMethod.GET, "auth/sessions")),
            "Unable to load sessions",
        )
        val array = JSONArray(response.body.orEmpty())
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    BackendSession(
                        id = item.getString("id"),
                        createdAt = item.optString("createdAt"),
                        expiresAt = item.optString("expiresAt"),
                        lastUsedAt = item.optString("lastUsedAt").takeIf { it.isNotBlank() && it != "null" },
                    ),
                )
            }
        }
    }

    suspend fun revokeSession(client: VersionedApiClient, sessionId: String) {
        val response = client.execute(ApiRequest(HttpMethod.DELETE, "auth/sessions/$sessionId"))
        requireSuccess(response, "Unable to revoke session")
    }

    suspend fun revokeAllSessions(client: VersionedApiClient) {
        val response = client.execute(ApiRequest(HttpMethod.DELETE, "auth/sessions"))
        requireSuccess(response, "Unable to revoke sessions")
        tokenProvider.clear()
    }

    private fun parseTokens(response: ApiResponse): AuthTokens {
        val json = JSONObject(response.body ?: throw BackendProtocolException("Auth response has no body"))
        val expiresInSeconds = json.optLong("accessTokenExpiresInSeconds", 900L).coerceAtLeast(1L)
        val refreshExpiresAt = runCatching {
            OffsetDateTime.parse(json.getString("refreshTokenExpiresAt")).toInstant().toEpochMilli()
        }.getOrElse { throw BackendProtocolException("Invalid refreshTokenExpiresAt") }
        val permissions = mutableSetOf<String>()
        json.optJSONArray("permissions")?.let { values ->
            for (index in 0 until values.length()) values.optString(index).takeIf { it.isNotBlank() }?.let(permissions::add)
        }
        return AuthTokens(
            accessToken = json.getString("accessToken"),
            accessExpiresAtEpochMs = clock() + expiresInSeconds * 1_000L,
            refreshToken = json.getString("refreshToken"),
            refreshExpiresAtEpochMs = refreshExpiresAt,
            organizationId = json.optString("organizationId").takeIf { it.isNotBlank() },
            membershipId = json.optString("membershipId").takeIf { it.isNotBlank() },
            permissions = permissions,
        )
    }

    private fun requireSuccess(response: ApiResponse, fallback: String): ApiResponse {
        if (response.successful) return response
        throw BackendApiException(response.statusCode, response.correlationId, apiMessage(response).ifBlank { fallback })
    }

    private fun apiMessage(response: ApiResponse): String = runCatching {
        JSONObject(response.body.orEmpty()).optString("message")
    }.getOrDefault("")

    private fun versioned(path: String) = "/api/${configuration.apiVersion}/${path.trimStart('/')}"
}

data class BackendSession(
    val id: String,
    val createdAt: String,
    val expiresAt: String,
    val lastUsedAt: String?,
)

class BackendApiException(
    val statusCode: Int,
    val correlationId: String?,
    message: String,
) : IllegalStateException(message)

class BackendProtocolException(message: String) : IllegalStateException(message)
