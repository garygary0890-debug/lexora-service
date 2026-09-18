package com.lexora.service.core.model

/** SRV-000093 — shared server contract for future Android/Web/Desktop clients. */
enum class ClientPlatform { ANDROID, WEB, DESKTOP }

data class ClientCapabilitySet(
    val platform: ClientPlatform,
    val clientVersion: String,
    val apiVersion: String,
    val supportsOffline: Boolean,
    val supportsFileUpload: Boolean,
    val supportsPush: Boolean,
    val supportedModuleIds: Set<String> = emptySet(),
)

data class ClientCompatibilityResult(
    val compatible: Boolean,
    val minimumSupportedVersion: String? = null,
    val requiredApiVersion: String? = null,
    val disabledCapabilities: Set<String> = emptySet(),
    val message: String? = null,
)
