package com.lexora.service.core.network

import com.lexora.service.core.model.RuntimeEnvironment

data class ApiConfiguration(
    val baseUrl: String,
    val apiVersion: String = "v1",
    val environment: RuntimeEnvironment,
    val connectTimeoutMs: Long = 15_000L,
    val readTimeoutMs: Long = 30_000L,
) {
    init {
        require(baseUrl.startsWith("https://") || environment == RuntimeEnvironment.DEVELOPMENT)
        require(apiVersion.matches(Regex("v[0-9]+")))
        require(connectTimeoutMs > 0 && readTimeoutMs > 0)
    }
}
