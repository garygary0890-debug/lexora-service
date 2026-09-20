package com.lexora.service.core.network

import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime

private const val SERVICE_PRODUCT_CODE = "LEXORA_SERVICE"

data class AuthTokens(
    val accessToken: String,
    val accessExpiresAtEpochMs: Long,
    val refreshToken: String,
    val refreshExpiresAtEpochMs: Long,
    val organizationId: String? = null,
    val membershipId: String? = null,
    val productCode: String,
    val permissions: Set<String> = emptySet(),
    val globalOwner: Boolean = false,
) {
    init { require(productCode == SERVICE_PRODUCT_CODE) { "Server session belongs to another Lexora product" } }

    fun accessExpired(nowEpochMs: Long, clockSkewMs: Long = 30_000L): Boolean =
        nowEpochMs + clockSkewMs >= accessExpiresAtEpochMs

    fun refreshExpired(nowEpochMs: Long, clockSkewMs: Long = 30_000L): Boolean =
        nowEpochMs + clockSkewMs >= refreshExpiresAtEpochMs
}

interface TokenProvider {
    suspend fun current(): AuthTokens?
    suspend fun save(tokens: AuthTokens)
    suspend fun clear()
}

interface RefreshTokenApi { suspend fun refresh(refreshToken: String): AuthTokens? }
class AuthenticationExpiredException(message: String) : IllegalStateException(message)

/** Lexora Backend is the authority for authentication, license, roles and effective access. */
class LexoraAuthApi(
    private val configuration: ApiConfiguration,
    private val transport: HttpTransport,
    private val clock: () -> Long = System::currentTimeMillis,
) : RefreshTokenApi {
    suspend fun login(email: String, password: String, organizationId: String): AuthTokens {
        require(email.isNotBlank() && password.isNotBlank() && organizationId.isNotBlank())
        return parseTokens(
            executeAuth(
                "login",
                JSONObject().put("email", email.trim()).put("password", password)
                    .put("organizationId", organizationId).put("productCode", SERVICE_PRODUCT_CODE),
            ),
            "Login failed",
        )
    }

    override suspend fun refresh(refreshToken: String): AuthTokens = parseTokens(
        executeAuth("refresh", JSONObject().put("refreshToken", refreshToken)),
        "Token refresh failed",
    )

    suspend fun logout(refreshToken: String) {
        val response = executeAuth("logout", JSONObject().put("refreshToken", refreshToken))
        require(response.successful) { apiError("Logout failed", response) }
    }

    suspend fun touchSession(refreshToken: String): SessionActivity {
        val response = executeAuth("session/activity", JSONObject().put("refreshToken", refreshToken))
        require(response.successful) { apiError("Session activity update failed", response) }
        val json = JSONObject(response.body.orEmpty())
        return SessionActivity(
            expiresAtEpochMs = OffsetDateTime.parse(json.getString("expiresAt")).toInstant().toEpochMilli(),
            deviceTrusted = json.optBoolean("deviceTrusted", true),
        )
    }

    private suspend fun executeAuth(path: String, body: JSONObject): ApiResponse = transport.execute(
        ApiRequest(HttpMethod.POST, "/api/${configuration.apiVersion}/auth/$path", body = body.toString(), headers = mapOf("Accept" to "application/json")),
    )

    private fun parseTokens(response: ApiResponse, prefix: String): AuthTokens {
        require(response.successful) { apiError(prefix, response) }
        val json = JSONObject(response.body.orEmpty())
        require(json.getString("productCode") == SERVICE_PRODUCT_CODE) { "Backend returned a session for another product" }
        val accessTtlSeconds = json.optLong("accessTokenExpiresInSeconds", 900L)
        return AuthTokens(
            accessToken = json.getString("accessToken"),
            accessExpiresAtEpochMs = clock() + accessTtlSeconds * 1000L,
            refreshToken = json.getString("refreshToken"),
            refreshExpiresAtEpochMs = OffsetDateTime.parse(json.getString("refreshTokenExpiresAt")).toInstant().toEpochMilli(),
            organizationId = json.optString("organizationId").takeIf(String::isNotBlank),
            membershipId = json.optString("membershipId").takeIf(String::isNotBlank),
            productCode = json.getString("productCode"),
            permissions = json.optJSONArray("permissions").toStringSet(),
            globalOwner = json.optBoolean("globalOwner", false),
        )
    }

    private fun apiError(prefix: String, response: ApiResponse): String {
        val serverMessage = runCatching { JSONObject(response.body.orEmpty()).optString("message") }.getOrNull().orEmpty()
        return if (serverMessage.isBlank()) "$prefix (HTTP ${response.statusCode})" else "$prefix: $serverMessage"
    }
}

data class SessionActivity(val expiresAtEpochMs: Long, val deviceTrusted: Boolean)

class AuthSessionManager(private val authApi: LexoraAuthApi, private val tokenProvider: TokenProvider) {
    suspend fun login(email: String, password: String, organizationId: String): AuthTokens =
        authApi.login(email, password, organizationId).also { tokenProvider.save(it) }
    suspend fun restore(): AuthTokens? = tokenProvider.current()?.takeIf { it.productCode == SERVICE_PRODUCT_CODE }
    suspend fun logout() {
        val current = tokenProvider.current()
        try { current?.refreshToken?.let { authApi.logout(it) } } finally { tokenProvider.clear() }
    }
    suspend fun clearLocalSession() = tokenProvider.clear()
}

class VersionedApiClient(
    private val configuration: ApiConfiguration,
    private val transport: HttpTransport,
    private val tokenProvider: TokenProvider,
    private val refreshTokenApi: RefreshTokenApi,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun execute(request: ApiRequest, authenticated: Boolean = true): ApiResponse {
        val versioned = request.copy(path = versionedPath(request.path))
        if (!authenticated) return transport.execute(versioned)
        val current = tokenProvider.current()?.takeIf { it.productCode == SERVICE_PRODUCT_CODE }
            ?: throw AuthenticationExpiredException("Authentication required")
        val effective = if (current.accessExpired(clock())) refresh(current) else current
        val response = transport.execute(authorized(versioned, effective))
        if (response.statusCode != 401) return response
        val refreshed = refresh(effective)
        val retried = transport.execute(authorized(versioned, refreshed))
        if (retried.statusCode == 401) { tokenProvider.clear(); throw AuthenticationExpiredException("Server session is no longer valid") }
        return retried
    }

    suspend fun revokeSession(sessionId: String): ApiResponse = execute(ApiRequest(HttpMethod.DELETE, "auth/sessions/$sessionId"))
    suspend fun revokeAllSessions(): ApiResponse = execute(ApiRequest(HttpMethod.DELETE, "auth/sessions"))

    private suspend fun refresh(tokens: AuthTokens): AuthTokens {
        if (tokens.refreshExpired(clock())) { tokenProvider.clear(); throw AuthenticationExpiredException("Refresh token expired") }
        val refreshed = refreshTokenApi.refresh(tokens.refreshToken)
        if (refreshed == null || refreshed.productCode != SERVICE_PRODUCT_CODE) { tokenProvider.clear(); throw AuthenticationExpiredException("Token refresh failed") }
        tokenProvider.save(refreshed); return refreshed
    }

    private fun authorized(request: ApiRequest, tokens: AuthTokens): ApiRequest = request.copy(
        headers = request.headers + mapOf("Authorization" to "Bearer ${tokens.accessToken}", "Accept" to "application/json") +
            (request.organizationId ?: tokens.organizationId)?.takeIf { it.isNotBlank() }?.let { mapOf("X-Lexora-Organization" to it) }.orEmpty(),
    )

    private fun versionedPath(path: String): String = if (path.startsWith("/api/")) path else "/api/${configuration.apiVersion}/${path.trim().trimStart('/')}"
}

private fun JSONArray?.toStringSet(): Set<String> = if (this == null) emptySet() else buildSet {
    for (index in 0 until length()) optString(index).takeIf(String::isNotBlank)?.let(::add)
}
