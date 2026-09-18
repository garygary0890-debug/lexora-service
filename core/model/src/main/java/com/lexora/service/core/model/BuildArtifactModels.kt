package com.lexora.service.core.model

enum class ArtifactType { APK, AAB, MAPPING, TEST_REPORT, BUILD_LOG, OTHER }

data class BuildArtifactRecord(
    val id: String,
    val buildId: String,
    val type: ArtifactType,
    val fileName: String,
    val versionName: String,
    val versionCode: Int,
    val commitSha: String?,
    val checksumSha256: String? = null,
    val sizeBytes: Long? = null,
    val createdAtEpochMs: Long,
)
