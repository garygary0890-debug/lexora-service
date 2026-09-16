package com.lexora.service.core.network

data class AuthTokens(
    val accessToken: String,
    val accessExpiresAtEpochMs: Long,
    val refreshToken: String,
    val refreshExpiresAtEpochMs: Long,
) {
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

interface RefreshTokenApi {
    suspend fun refresh(refreshToken: String): AuthTokens?
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
        val current = requireNotNull(tokenProvider.current()) { "Authentication required" }
        val effective = if (current.accessExpired(clock())) refresh(current) else current
        val first = authorized(versioned, effective)
        val response = transport.execute(first)
        if (response.statusCode != 401) return response
        val refreshed = refresh(effective)
        return transport.execute(authorized(versioned, refreshed))
    }

    private suspend fun refresh(tokens: AuthTokens): AuthTokens {
        require(!tokens.refreshExpired(clock())) { "Refresh token expired" }
        val refreshed = requireNotNull(refreshTokenApi.refresh(tokens.refreshToken)) { "Token refresh failed" }
        tokenProvider.save(refreshed)
        return refreshed
    }

    private fun authorized(request: ApiRequest, tokens: AuthTokens): ApiRequest = request.copy(
        headers = request.headers + mapOf(
            "Authorization" to "Bearer ${tokens.accessToken}",
            "Accept" to "application/json",
        ) + request.organizationId?.takeIf { it.isNotBlank() }
            ?.let { mapOf("X-Lexora-Organization" to it) }
            .orEmpty(),
    )

    private fun versionedPath(path: String): String =
        "/api/${configuration.apiVersion}/${path.trim().trimStart('/')}"
}
