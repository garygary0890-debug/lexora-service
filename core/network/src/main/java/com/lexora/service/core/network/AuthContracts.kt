package com.lexora.service.core.network

data class AuthTokens(
    val accessToken: String,
    val accessExpiresAtEpochMs: Long,
    val refreshToken: String,
    val refreshExpiresAtEpochMs: Long,
    val organizationId: String? = null,
    val membershipId: String? = null,
    val permissions: Set<String> = emptySet(),
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

class AuthenticationExpiredException(message: String) : IllegalStateException(message)

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
        val current = tokenProvider.current() ?: throw AuthenticationExpiredException("Authentication required")
        val effective = if (current.accessExpired(clock())) refresh(current) else current
        val response = transport.execute(authorized(versioned, effective))
        if (response.statusCode != 401) return response

        // A 401 can mean that the access token was revoked or expired server-side.
        // Refresh exactly once, then retry exactly once to avoid loops.
        val refreshed = refresh(effective)
        val retried = transport.execute(authorized(versioned, refreshed))
        if (retried.statusCode == 401) {
            tokenProvider.clear()
            throw AuthenticationExpiredException("Server session is no longer valid")
        }
        return retried
    }

    private suspend fun refresh(tokens: AuthTokens): AuthTokens {
        if (tokens.refreshExpired(clock())) {
            tokenProvider.clear()
            throw AuthenticationExpiredException("Refresh token expired")
        }
        val refreshed = refreshTokenApi.refresh(tokens.refreshToken)
        if (refreshed == null) {
            tokenProvider.clear()
            throw AuthenticationExpiredException("Token refresh failed")
        }
        tokenProvider.save(refreshed)
        return refreshed
    }

    private fun authorized(request: ApiRequest, tokens: AuthTokens): ApiRequest = request.copy(
        headers = request.headers + mapOf(
            "Authorization" to "Bearer ${tokens.accessToken}",
            "Accept" to "application/json",
        ) + (request.organizationId ?: tokens.organizationId)
            ?.takeIf { it.isNotBlank() }
            ?.let { mapOf("X-Lexora-Organization" to it) }
            .orEmpty(),
    )

    private fun versionedPath(path: String): String =
        "/api/${configuration.apiVersion}/${path.trim().trimStart('/')}"
}
