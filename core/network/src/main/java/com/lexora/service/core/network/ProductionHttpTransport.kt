package com.lexora.service.core.network

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.math.min

class ProductionHttpTransport(
    private val configuration: ApiConfiguration,
    private val retryClassifier: RetryClassifier = RetryClassifier(),
    private val maxAttempts: Int = 3,
) : HttpTransport {
    init { require(maxAttempts in 1..5) }

    override suspend fun execute(request: ApiRequest): ApiResponse {
        var lastFailure: IOException? = null
        repeat(maxAttempts) { index ->
            val attempt = index + 1
            try {
                val response = executeOnce(request)
                if (attempt >= maxAttempts || !retryClassifier.shouldRetry(
                        request.method,
                        response.statusCode,
                        !request.idempotencyKey.isNullOrBlank(),
                    )
                ) return response
                sleepBeforeRetry(attempt, response.headers["Retry-After"])
            } catch (error: IOException) {
                lastFailure = error
                if (attempt >= maxAttempts || !retryClassifier.shouldRetry(
                        request.method,
                        null,
                        !request.idempotencyKey.isNullOrBlank(),
                    )
                ) throw error
                sleepBeforeRetry(attempt, null)
            }
        }
        throw lastFailure ?: IOException("HTTP request failed")
    }

    private fun executeOnce(request: ApiRequest): ApiResponse {
        val url = URL(buildUrl(request))
        if (configuration.environment.name != "DEVELOPMENT") {
            require(url.protocol.equals("https", ignoreCase = true)) { "HTTPS is required outside development" }
        }
        val correlationId = request.headers["X-Correlation-ID"] ?: UUID.randomUUID().toString()
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = request.method.name
            connectTimeout = configuration.connectTimeoutMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            readTimeout = configuration.readTimeoutMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            useCaches = false
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-Correlation-ID", correlationId)
            request.headers.forEach { (name, value) -> setRequestProperty(name, value) }
            request.idempotencyKey?.takeIf { it.isNotBlank() }?.let { setRequestProperty("Idempotency-Key", it) }
            if (request.body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        return try {
            request.body?.let { body ->
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..399) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            val headers = connection.headerFields
                .filterKeys { it != null }
                .mapValues { (_, values) -> values?.joinToString(",").orEmpty() }
            ApiResponse(
                statusCode = status,
                headers = headers,
                body = body,
                correlationId = connection.getHeaderField("X-Correlation-ID") ?: correlationId,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun buildUrl(request: ApiRequest): String {
        val base = configuration.baseUrl.trimEnd('/')
        val path = "/${request.path.trimStart('/')}"
        if (request.query.isEmpty()) return base + path
        val query = request.query.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        return "$base$path?$query"
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun sleepBeforeRetry(attempt: Int, retryAfter: String?) {
        val serverDelayMs = retryAfter?.toLongOrNull()?.coerceIn(0L, 30L)?.times(1_000L)
        val exponentialMs = min(500L * (1L shl (attempt - 1)), 4_000L)
        Thread.sleep(serverDelayMs ?: exponentialMs)
    }
}
