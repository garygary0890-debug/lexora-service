package com.lexora.service.core.network

enum class HttpMethod { GET, POST, PUT, PATCH, DELETE }

data class ApiRequest(
    val method: HttpMethod,
    val path: String,
    val query: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val idempotencyKey: String? = null,
    val organizationId: String? = null,
)

data class ApiResponse(
    val statusCode: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val correlationId: String? = null,
) {
    val successful: Boolean get() = statusCode in 200..299
}

interface HttpTransport {
    suspend fun execute(request: ApiRequest): ApiResponse
}

class RetryClassifier {
    fun shouldRetry(method: HttpMethod, statusCode: Int?, hasIdempotencyKey: Boolean): Boolean {
        val safe = method in setOf(HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE) || hasIdempotencyKey
        if (!safe) return false
        return statusCode == null || statusCode in setOf(408, 425, 429) || statusCode in 500..599
    }
}
