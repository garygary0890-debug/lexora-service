package com.lexora.service.core.model

data class ReleaseReadinessChecklist(
    val versionName: String,
    val versionCode: Int,
    val releaseBuildSucceeded: Boolean,
    val signedWithApprovedKey: Boolean,
    val upgradeVerified: Boolean,
    val smokePassed: Boolean,
    val noOpenCriticalDefects: Boolean,
    val serverMigrationsVerified: Boolean,
    val rollbackPlanVerified: Boolean,
    val backupPlanned: Boolean,
    val traceabilityRecorded: Boolean,
    val environmentIsolationVerified: Boolean,
) {
    val ready: Boolean
        get() = releaseBuildSucceeded && signedWithApprovedKey && upgradeVerified && smokePassed &&
            noOpenCriticalDefects && serverMigrationsVerified && rollbackPlanVerified && backupPlanned &&
            traceabilityRecorded && environmentIsolationVerified
}
