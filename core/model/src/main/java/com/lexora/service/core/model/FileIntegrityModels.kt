package com.lexora.service.core.model

enum class FileVersionState { ACTIVE, SUPERSEDED, REVOKED, ARCHIVED }

data class FileVersionRecord(
    val fileId: String,
    val version: Int,
    val checksumSha256: String,
    val sizeBytes: Long,
    val mimeType: String,
    val createdByUserId: String,
    val createdAtEpochMs: Long,
    val state: FileVersionState = FileVersionState.ACTIVE,
)

data class FileIntegrityCheck(
    val fileId: String,
    val expectedChecksumSha256: String,
    val actualChecksumSha256: String?,
    val valid: Boolean,
    val checkedAtEpochMs: Long,
)

data class FileReplacementRequest(
    val fileId: String,
    val replacingVersion: Int,
    val reason: String,
    val requestedByUserId: String,
    val requestedAtEpochMs: Long,
)
