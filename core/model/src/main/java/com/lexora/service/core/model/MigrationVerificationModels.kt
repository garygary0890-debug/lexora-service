package com.lexora.service.core.model

enum class MigrationVerificationStatus { NOT_RUN, PASSED, FAILED, ROLLED_BACK }

data class MigrationVerification(
    val id: String,
    val fromSchemaVersion: Int,
    val toSchemaVersion: Int,
    val fromAppVersion: String? = null,
    val toAppVersion: String? = null,
    val backupId: String? = null,
    val status: MigrationVerificationStatus,
    val dataPreserved: Boolean? = null,
    val rollbackAvailable: Boolean,
    val verifiedAtEpochMs: Long? = null,
    val notes: String? = null,
)
