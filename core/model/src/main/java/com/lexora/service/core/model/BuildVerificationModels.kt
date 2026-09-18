package com.lexora.service.core.model

data class BuildVerification(
    val buildId: String,
    val diagnosticStatus: BuildDiagnosticStatus,
    val expectedArtifactType: ArtifactType,
    val artifactRecordId: String? = null,
    val exitCode: Int? = null,
    val verifiedAtEpochMs: Long? = null,
) {
    val successful: Boolean
        get() = diagnosticStatus == BuildDiagnosticStatus.SUCCEEDED && exitCode == 0 && artifactRecordId != null
}
