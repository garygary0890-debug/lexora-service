package com.lexora.service.core.network

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.math.min

class UrlConnectionHttpTransport(
    private val configuration: ApiConfiguration,
    private val retryClassifier: RetryClassifier = RetryClassifier(),
    private val maxAttempts: Int = 3,
    private val sleeper: (Long) -> Unit = Thread::sleep,
) : HttpTransport {
    init { require(maxAttempts in 1..5) }

    override suspend fun execute(request: ApiRequest): ApiResponse {
        var attempt = 1
        var lastFailure: IOException? = null
        while (attempt <= maxAttempts) {
            try {
                val response = executeOnce(request)
                if (attempt == maxAttempts || !retryClassifier.shouldRetry(
                        request.method,
                        response.statusCode,
                        !request.idempotencyKey.isNullOrBlank(),
                    )
                ) return response
            } catch (error: IOException) {
                lastFailure = error
                if (attempt == maxAttempts || !retryClassifier.shouldRetry(
                        request.method,
                        null,
                        !request.idempotencyKey.isNullOrBlank(),
                    )
                ) throw error
            }
            sleeper(backoffMs(attempt))
            attempt++
        }
        throw lastFailure ?: IOException("Network request failed")
    }

    private fun executeOnce(request: ApiRequest): ApiResponse {
        val url = buildUrl(request)
        val connection = (url.toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = request.method.name
            connectTimeout = configuration.connectTimeoutMs.toInt()
            readTimeout = configuration.readTimeoutMs.toInt()
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-Correlation-Id", request.headers["X-Correlation-Id"] ?: UUID.randomUUID().toString())
            request.idempotencyKey?.takeIf(String::isNotBlank)?.let { setRequestProperty("Idempotency-Key", it) }
            request.headers.forEach { (name, value) -> setRequestProperty(name, value) }
            if (request.body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        return try {
            request.body?.let { body ->
                connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
            val headers = connection.headerFields
                .filterKeys { it != null }
                .mapValues { (_, values) -> values.joinToString(",") }
            ApiResponse(
                statusCode = status,
                headers = headers,
                body = body,
                correlationId = connection.getHeaderField("X-Correlation-Id"),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun buildUrl(request: ApiRequest): URI {
        val base = configuration.baseUrl.trimEnd('/')
        val path = request.path.let { if (it.startsWith('/')) it else "/$it" }
        val query = request.query.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        return URI.create(base + path + if (query.isBlank()) "" else "?$query")
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun backoffMs(attempt: Int): Long = min(500L * (1L shl (attempt - 1)), 4_000L)
}
