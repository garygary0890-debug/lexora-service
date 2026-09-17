package com.lexora.service.core.model

enum class BackupScope { DATABASE, FILES, FULL }
enum class BackupState { SCHEDULED, RUNNING, SUCCEEDED, FAILED, VERIFIED }

data class BackupRecord(
    val id: String,
    val environment: DeploymentEnvironment,
    val scope: BackupScope,
    val state: BackupState,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long? = null,
    val storageRef: String? = null,
    val checksum: String? = null,
)

data class RestoreVerification(
    val backupId: String,
    val testedAtEpochMs: Long,
    val succeeded: Boolean,
    val restoredDatabase: Boolean,
    val restoredFiles: Boolean,
    val notes: String? = null,
)

data class RecoveryPlan(
    val environment: DeploymentEnvironment,
    val recoveryPointObjectiveMinutes: Int,
    val recoveryTimeObjectiveMinutes: Int,
    val requirePreMigrationBackup: Boolean = true,
)
